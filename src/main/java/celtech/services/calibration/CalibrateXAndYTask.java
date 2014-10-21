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
