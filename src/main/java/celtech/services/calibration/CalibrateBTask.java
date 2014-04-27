/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.calibration;

import celtech.configuration.HeaterMode;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.services.ControllableService;
import java.util.ResourceBundle;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrateBTask extends Task<CalibrationStepResult> implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(CalibrateBTask.class.getName());
    private CalibrationState desiredState = null;
    private int nozzleNumber = -1;

    private Printer printerToUse = null;
    private String progressTitle = null;
    private String initialisingMessage = null;
    private String heatingMessage = null;
    private String readyToBeginMessage = null;
    private String pressAKeyMessage = null;
    private String pressAKeyToContinueMessage = null;
    private String preparingExtruderMessage = null;
    private ResourceBundle i18nBundle = null;
    private boolean keyPressed = false;
    private int progressPercent = 0;
    private boolean lookingForKeyPress = false;

    public CalibrateBTask(CalibrationState desiredState)
    {
        this.desiredState = desiredState;
    }

    public CalibrateBTask(CalibrationState desiredState, int nozzleNumber)
    {
        this.desiredState = desiredState;
        this.nozzleNumber = nozzleNumber;
    }

    @Override
    protected CalibrationStepResult call() throws Exception
    {
        boolean success = false;

        StatusScreenState statusScreenState = StatusScreenState.getInstance();
        printerToUse = statusScreenState.getCurrentlySelectedPrinter();

        switch (desiredState)
        {
            case INITIALISING:
                try
                {
                    printerToUse.transmitDirectGCode("G90", false);
                    waitOnBusy();
                    printerToUse.transmitDirectGCode("G0 B0", false);
                    waitOnBusy();
                    printerToUse.transmitDirectGCode("G28 X Y", false);
                    waitOnBusy();
                    printerToUse.transmitDirectGCode("G0 X116.5 Y75", false);
                    waitOnBusy();
                    printerToUse.transmitDirectGCode("G28 Z", false);
                    waitOnBusy();
                    printerToUse.transmitDirectGCode("G0 Z50", false);
                    waitOnBusy();
                    success = true;
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                } catch (InterruptedException ex)
                {
                    steno.error("Interrrupted during needle valve calibration - mode=" + desiredState.name());
                }

                break;
            case HEATING:
                try
                {
                    printerToUse.transmitDirectGCode("M104", false);
                    if (printerToUse.getNozzleHeaterMode() == HeaterMode.FIRST_LAYER)
                    {
                        waitUntilNozzleReaches(printerToUse.getNozzleFirstLayerTargetTemperature(), 5);
                    } else
                    {
                        waitUntilNozzleReaches(printerToUse.getNozzleTargetTemperature(), 5);
                    }
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                } catch (InterruptedException ex)
                {
                    steno.error("Interrrupted during needle valve calibration - mode=" + desiredState.name());
                }

                break;
            case PRIMING:
                extrudeUntilStall();
                break;
            case MATERIAL_EXTRUDING_CHECK:
                try
                {
                    printerToUse.transmitDirectGCode("T" + nozzleNumber, false);
                    printerToUse.transmitDirectGCode("G0 B2", false);
                    if (nozzleNumber == 0)
                    {
                        printerToUse.transmitDirectGCode("G1 E10 F75", false);
                    } else
                    {
                        printerToUse.transmitDirectGCode("G1 E10 F100", false);
                    }
                    waitOnBusy();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                }
                break;
            case PRE_CALIBRATION_PRIMING:
                success = extrudeUntilStall();
                break;
            case CONFIRM_MATERIAL_EXTRUDING:
                try
                {
                    printerToUse.transmitDirectGCode("T" + nozzleNumber, false);
                    printerToUse.transmitDirectGCode("G0 B1", false);
                    if (nozzleNumber == 0)
                    {
                        printerToUse.transmitDirectGCode("G1 E10 F75", false);
                    } else
                    {
                        printerToUse.transmitDirectGCode("G1 E10 F100", false);
                    }
                    waitOnBusy();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                }
                break;
        }

        return new CalibrationStepResult(desiredState, success);
    }

    private boolean extrudeUntilStall()
    {
        boolean success = false;
        try
        {
            printerToUse.transmitDirectGCode("M909 S4", false);
            waitOnBusy();

            printerToUse.transmitDirectGCode("T" + nozzleNumber, false);

            AckResponse errors = printerToUse.transmitReportErrors();
            if (errors.isError())
            {
                printerToUse.transmitResetErrors();
            }

            while (errors.isEFilamentSlipError() == false && isCancelled() == false)
            {
                printerToUse.transmitDirectGCode("G0 E10", false);
                waitOnBusy();

                errors = printerToUse.transmitReportErrors();
            }

            printerToUse.transmitResetErrors();

            printerToUse.transmitDirectGCode("M909 S70", false);
            waitOnBusy();

            success = true;
        } catch (RoboxCommsException ex)
        {
            steno.error("Error in needle valve priming - mode=" + desiredState.name());
        } catch (InterruptedException ex)
        {
            steno.error("Interrrupted during needle valve priming - mode=" + desiredState.name());
        }
        return success;
    }

    private void waitForKeyPress() throws InterruptedException
    {
        lookingForKeyPress = true;
        while (keyPressed == false && isCancelled() == false)
        {
            Thread.sleep(100);
        }
        keyPressed = false;
        lookingForKeyPress = false;
    }

    private void waitOnBusy() throws InterruptedException
    {
        Thread.sleep(100);
        while (printerToUse.busyProperty().get() == true && isCancelled() == false)
        {
            Thread.sleep(100);
        }
    }

    private void waitUntilNozzleReaches(int temperature, int tolerance) throws InterruptedException
    {
        int minTemp = temperature - tolerance;
        int maxTemp = temperature + tolerance;

        while ((printerToUse.extruderTemperatureProperty().get() < minTemp || printerToUse.extruderTemperatureProperty().get() > maxTemp) && isCancelled() == false)
        {
            Thread.sleep(100);
        }
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

    public void keyPressed()
    {
        if (lookingForKeyPress)
        {
            keyPressed = true;
        }
    }
}
