/*
 * Copyright 2014 CEL UK
 */
package celtech.utils.tasks;

import java.util.concurrent.Callable;
import javafx.concurrent.Task;

/**
 *
 * @author tony
 */
class GenericTask extends Task
{
    private final Callable callable;

    public GenericTask(Callable runnable)
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
