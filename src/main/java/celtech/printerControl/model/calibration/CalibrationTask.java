/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import java.util.concurrent.Callable;
import javafx.concurrent.Task;

/**
 *
 * @author tony
 */
class CalibrationTask extends Task
{
    private final Callable callable;

    public CalibrationTask(Callable runnable)
    {
        this.callable = runnable;
    }

    @Override
    protected Object call() throws Exception
    {
        callable.call();
        return null;
    }
    
}
