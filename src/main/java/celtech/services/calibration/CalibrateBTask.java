/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.calibration;

import celtech.configuration.HeaterMode;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.services.ControllableService;
import celtech.utils.PrinterUtils;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrateBTask extends Task<NozzleBCalibrationStepResult> implements
    ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrateBTask.class.getName());
    private NozzleOpeningCalibrationState desiredState = null;
    private int nozzleNumber = -1;

    private Printer printer = null;
    private boolean keyPressed = false;
    private boolean lookingForKeyPress = false;

    /**
     *
     * @param desiredState
     */
    public CalibrateBTask(NozzleOpeningCalibrationState desiredState, Printer printer)
    {
        this.desiredState = desiredState;
        this.printer = printer;
    }

    /**
     *
     * @param desiredState
     * @param nozzleNumber
     */
    public CalibrateBTask(NozzleOpeningCalibrationState desiredState, int nozzleNumber, Printer printer)
    {
        this.desiredState = desiredState;
        this.nozzleNumber = nozzleNumber;
        this.printer = printer;
    }

    @Override
    protected NozzleBCalibrationStepResult call() throws Exception
    {
        boolean success = false;

        switch (desiredState)
        {
            case HEATING:
                try
                {
                    printer.transmitDirectGCode("M104", false);
                    if (PrinterUtils.waitOnBusy(printer, this) == false)
                    {
                        printer.transmitStoredGCode("Home_all");
                        if (PrinterUtils.waitOnMacroFinished(printer, this) == false
                            && isCancelled() == false)
                        {
                            printer.transmitDirectGCode("G0 Z50", false);
                            if (PrinterUtils.waitOnBusy(printer, this) == false
                                && isCancelled() == false)
                            {
                                printer.transmitDirectGCode("M104", false);
                                if (printer.getNozzleHeaterMode() == HeaterMode.FIRST_LAYER)
                                {
                                    PrinterUtils.waitUntilTemperatureIsReached(
                                        printer.extruderTemperatureProperty(), this,
                                        printer.getNozzleFirstLayerTargetTemperature(), 5, 300);
                                } else
                                {
                                    PrinterUtils.waitUntilTemperatureIsReached(
                                        printer.extruderTemperatureProperty(), this,
                                        printer.getNozzleTargetTemperature(), 5, 300);
                                }
                                printer.transmitDirectGCode(GCodeConstants.switchOnHeadLEDs,
                                                            false);
                            }
                        }
                    }

                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                } catch (InterruptedException ex)
                {
                    steno.error("Interrrupted during needle valve calibration - mode="
                        + desiredState.name());
                }

                break;
            case NO_MATERIAL_CHECK:
                extrudeUntilStall(0);
                extrudeUntilStall(1);
                break;
            case PRE_CALIBRATION_PRIMING_FINE:
                success = extrudeUntilStall(0);
                break;
            case PRE_CALIBRATION_PRIMING_FILL:
                success = extrudeUntilStall(1);
                break;
            case CONFIRM_NO_MATERIAL:
                success = extrudeUntilStall(0);
                success = extrudeUntilStall(1);
                break;
            case CONFIRM_MATERIAL_EXTRUDING_FINE:
                try
                {
                    printer.transmitDirectGCode("T" + 0, false);
                    printer.transmitDirectGCode("G0 B1", false);
                    printer.transmitDirectGCode("G1 E10 F75", false);
                    PrinterUtils.waitOnBusy(printer, this);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                }
                break;
            case CONFIRM_MATERIAL_EXTRUDING_FILL:
                try
                {
                    printer.transmitDirectGCode("T" + 1, false);
                    printer.transmitDirectGCode("G0 B1", false);
                    printer.transmitDirectGCode("G1 E10 F100", false);
                    PrinterUtils.waitOnBusy(printer, this);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                }
                break;
        }

        return new NozzleBCalibrationStepResult(desiredState, success);
    }

    private void extrudeForNozzle(int nozzleNumber) throws RoboxCommsException
    {
        printer.transmitDirectGCode("T" + nozzleNumber, false);
        printer.transmitDirectGCode("G0 B2", false);
        if (nozzleNumber == 0)
        {
            printer.transmitDirectGCode("G1 E10 F75", false);
        } else
        {
            printer.transmitDirectGCode("G1 E10 F100", false);
        }
        PrinterUtils.waitOnBusy(printer, this);
    }

    private boolean extrudeUntilStall(int nozzleNumber)
    {
        boolean success = false;
        try
        {
            // select nozzle
            printer.transmitDirectGCode("T" + nozzleNumber, false);
            // G36 = extrude until stall E700 = top extruder F2000 = feed rate mm/min (?)
            // extrude either requested volume or until filament slips
            printer.transmitDirectGCode("G36 E700 F2000", false);
            PrinterUtils.waitOnBusy(printer, this);

            success = true;
        } catch (RoboxCommsException ex)
        {
            steno.error("Error in needle valve priming - mode=" + desiredState.name());
        }
        return success;
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
