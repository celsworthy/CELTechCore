/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.calibration;

import celtech.printerControl.model.Printer;
import celtech.services.ControllableService;
import celtech.utils.PrinterUtils;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrateXAndYTask extends Task<CalibrationXAndYStepResult> implements
    ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrateXAndYTask.class.getName());
    private CalibrationXAndYState desiredState = null;

    private Printer printer = null;

    /**
     *
     * @param desiredState
     */
    public CalibrateXAndYTask(CalibrationXAndYState desiredState, Printer printer)
    {
        this.desiredState = desiredState;
        this.printer = printer;
    }

    @Override
    protected CalibrationXAndYStepResult call() throws Exception
    {
        boolean success = false;

        switch (desiredState)
        {
//            case HEATING:
//                try
//                {
//                    printer.transmitDirectGCode("M104", false);
//                    if (PrinterUtils.waitOnBusy(printer, this) == false)
//                    {
//                        printer.transmitStoredGCode("Home_all");
//                        if (PrinterUtils.waitOnMacroFinished(printer, this) == false
//                            && isCancelled() == false)
//                        {
//                            printer.transmitDirectGCode("G0 Z50", false);
//                            if (PrinterUtils.waitOnBusy(printer, this) == false
//                                && isCancelled() == false)
//                            {
//                                printer.transmitDirectGCode("M104", false);
//                                if (printer.getNozzleHeaterMode() == HeaterMode.FIRST_LAYER)
//                                {
//                                    PrinterUtils.waitUntilTemperatureIsReached(
//                                        printer.extruderTemperatureProperty(), this,
//                                        printer.getNozzleFirstLayerTargetTemperature(), 5, 300);
//                                } else
//                                {
//                                    PrinterUtils.waitUntilTemperatureIsReached(
//                                        printer.extruderTemperatureProperty(), this,
//                                        printer.getNozzleTargetTemperature(), 5, 300);
//                                }
//                                printer.transmitDirectGCode(GCodeConstants.switchOnHeadLEDs,
//                                                            false);
//                            }
//                        }
//                    }
//
//                } catch (RoboxCommsException ex)
//                {
//                    steno.error("Error in x and y calibration - mode=" + desiredState.name());
//                } catch (InterruptedException ex)
//                {
//                    steno.error("Interrrupted during x and y calibration - mode="
//                        + desiredState.name());
//                }
//
//                break;
            case PRINT_PATTERN:
//                printer.transmitStoredGCode("rbx_XY_offset_roboxised");
                printer.runMacro("tiny_robox");
                if (PrinterUtils.waitOnMacroFinished(printer, this) == true
                    || isCancelled())
                {
                      cancelRun();
                }
                success = true;
                break;
            case PRINT_CIRCLE:
//                printer.transmitStoredGCode("rbx_XY_offset_roboxised");
                printer.runMacro("tiny_robox");
                if (PrinterUtils.waitOnMacroFinished(printer, this) == true
                    || isCancelled())
                {
                      cancelRun();
                }
                success = true;
                break;                

        }

        return new CalibrationXAndYStepResult(desiredState, success);
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
