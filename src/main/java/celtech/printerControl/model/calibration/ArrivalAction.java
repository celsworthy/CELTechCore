/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import java.util.concurrent.Callable;

/**
 *
 * @author tony
 */
public class ArrivalAction<StateType>
{
    final Callable<Boolean> action;
    StateType failedState;

    public ArrivalAction(Callable<Boolean> action, StateType failedState)
    {
        this.action = action;
        this.failedState = failedState;
    }
}
