/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.calibration.old;

import celtech.configuration.HeaterMode;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.NozzleHeater;
import celtech.printerControl.model.PrinterException;
import celtech.services.ControllableService;
import celtech.services.calibration.NozzleOpeningCalibrationState;
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

    private Printer printer = null;

    /**
     *
     * @param desiredState
     */
    public CalibrateBTask(NozzleOpeningCalibrationState desiredState, Printer printer)
    {
        this.desiredState = desiredState;
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
                    printer.goToTargetNozzleTemperature();
                    if (PrinterUtils.waitOnBusy(printer, this) == false)
                    {
                        printer.runMacroWithoutPurgeCheck("Home_all");
                        if (PrinterUtils.waitOnMacroFinished(printer, this) == false
                            && isCancelled() == false)
                        {
                            printer.goToZPosition(50);
                            if (PrinterUtils.waitOnBusy(printer, this) == false
                                && isCancelled() == false)
                            {
                                printer.goToTargetNozzleTemperature();
//                                if (printer.headProperty().get().getNozzleHeaters().size() < 1) {
//                                    
//                                }
                                if (printer.headProperty().get()
                                    .getNozzleHeaters().get(0)
                                    .heaterModeProperty().get() == HeaterMode.FIRST_LAYER)
                                {
                                    NozzleHeater nozzleHeater = printer.headProperty().get()
                                        .getNozzleHeaters().get(0);
                                    PrinterUtils.waitUntilTemperatureIsReached(
                                        nozzleHeater.nozzleTemperatureProperty(), this,
                                        nozzleHeater
                                        .nozzleFirstLayerTargetTemperatureProperty().get(), 5, 300);
                                } else
                                {
                                    NozzleHeater nozzleHeater = printer.headProperty().get()
                                        .getNozzleHeaters().get(0);
                                    PrinterUtils.waitUntilTemperatureIsReached(
                                        nozzleHeater.nozzleTemperatureProperty(), this,
                                        nozzleHeater
                                        .nozzleTargetTemperatureProperty().get(), 5, 300);
                                }
                                printer.switchOnHeadLEDs();
                            }
                        }
                    }

                } catch (PrinterException ex)
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
                break;
            case PRE_CALIBRATION_PRIMING_FINE:
                success = extrudeUntilStall(0);
                break;
            case PRE_CALIBRATION_PRIMING_FILL:
                success = extrudeUntilStall(1);
                break;
            case CONFIRM_MATERIAL_EXTRUDING:
                try
                {
                    printer.selectNozzle(0);
                    printer.openNozzleFully();
                    printer.sendRawGCode("G1 E10 F75", false);
                    PrinterUtils.waitOnBusy(printer, this);
                    printer.selectNozzle(1);
                    printer.openNozzleFully();
                    printer.sendRawGCode("G1 E10 F100", false);
                    PrinterUtils.waitOnBusy(printer, this);
                } catch (PrinterException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + desiredState.name());
                }
                break;
            case CONFIRM_NO_MATERIAL:
                printer.closeNozzleFully();
                success = extrudeUntilStall(0);
                break;
        }

        return new NozzleBCalibrationStepResult(desiredState, success);
    }

    private boolean extrudeUntilStall(int nozzleNumber)
    {
        boolean success = false;
        try
        {
            // select nozzle
            printer.selectNozzle(nozzleNumber);
            // G36 = extrude until stall E700 = top extruder F2000 = feed rate mm/min (?)
            // extrude either requested volume or until filament slips
            printer.sendRawGCode("G36 E700 F2000", false);
            PrinterUtils.waitOnBusy(printer, this);

            success = true;
        } catch (PrinterException ex)
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
}
