/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.calibration;

import celtech.configuration.HeaterMode;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.model.Printer;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.services.ControllableService;
import celtech.utils.PrinterUtils;
import java.util.ResourceBundle;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrateBTask extends Task<NozzleBCalibrationStepResult> implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(CalibrateBTask.class.getName());
    private NozzleBCalibrationState desiredState = null;
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

    /**
     *
     * @param desiredState
     */
    public CalibrateBTask(NozzleBCalibrationState desiredState)
    {
        this.desiredState = desiredState;
    }

    /**
     *
     * @param desiredState
     * @param nozzleNumber
     */
    public CalibrateBTask(NozzleBCalibrationState desiredState, int nozzleNumber)
    {
        this.desiredState = desiredState;
        this.nozzleNumber = nozzleNumber;
    }

    @Override
    protected NozzleBCalibrationStepResult call() throws Exception
    {
        boolean success = false;

        StatusScreenState statusScreenState = StatusScreenState.getInstance();
        printerToUse = statusScreenState.getCurrentlySelectedPrinter();

        switch (desiredState)
        {
            case INITIALISING:
                try
                {
                    printerToUse.transmitDirectGCode("M104", false);
                    if (PrinterUtils.waitOnBusy(printerToUse, this) == false)
                    {
                        printerToUse.transmitStoredGCode("Home_all");
                        if (PrinterUtils.waitOnMacroFinished(printerToUse, this) == false && isCancelled() == false)
                        {
                            printerToUse.transmitDirectGCode("G0 Z50", false);
                            if (PrinterUtils.waitOnBusy(printerToUse, this) == false && isCancelled() == false)
                            {
                                success = true;
                            }
                        }
                    }
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                }
                break;
            case HEATING:
                try
                {
                    printerToUse.transmitDirectGCode("M104", false);
                    //TODO make this work with multiple heaters
                    if (printerToUse.headProperty().getNozzleHeaters().get(0).getHeaterModeProperty().get() == HeaterMode.FIRST_LAYER)
                    {
                        PrinterUtils.waitUntilTemperatureIsReached(printerToUse.extruderTemperatureProperty(), this, printerToUse.getNozzleFirstLayerTargetTemperature(), 5, 300);
                    } else
                    {
                        PrinterUtils.waitUntilTemperatureIsReached(printerToUse.extruderTemperatureProperty(), this, printerToUse.getNozzleTargetTemperature(), 5, 300);
                    }
                    printerToUse.transmitDirectGCode(GCodeConstants.switchOnHeadLEDs, false);
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
                    PrinterUtils.waitOnBusy(printerToUse, this);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                }
                break;
            case PRE_CALIBRATION_PRIMING:
                success = extrudeUntilStall();
                break;
            case POST_CALIBRATION_PRIMING:
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
                    PrinterUtils.waitOnBusy(printerToUse, this);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                }
                break;
        }

        return new NozzleBCalibrationStepResult(desiredState, success);
    }

    private boolean extrudeUntilStall()
    {
        boolean success = false;
        try
        {
            printerToUse.transmitDirectGCode("T" + nozzleNumber, false);

            printerToUse.transmitDirectGCode("G36 E700 F2000", false);
            PrinterUtils.waitOnBusy(printerToUse, this);

            success = true;
        } catch (RoboxCommsException ex)
        {
            steno.error("Error in needle valve priming - mode=" + desiredState.name());
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

    /**
     *
     * @return
     */
    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

    /**
     *
     */
    public void keyPressed()
    {
        if (lookingForKeyPress)
        {
            keyPressed = true;
        }
    }
}
