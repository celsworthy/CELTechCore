/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.utils.tasks.Cancellable;
import celtech.utils.tasks.OredCancellable;
import javafx.beans.value.ObservableValue;

/**
 *
 * @author tony
 */
public abstract class StateTransitionActions
{
    Cancellable userCancellable;
    Cancellable errorCancellable;
    Cancellable userOrErrorCancellable;

    public StateTransitionActions(Cancellable userCancellable, Cancellable errorCancellable)
    {
        setUserCancellable(userCancellable);
        setErrorCancellable(errorCancellable);
    }
    
    
    
    void setUserCancellable(Cancellable cancellable)
    {
        // userCancellable is set when the user requests a cancel. The state machine also detects
        // this condition and goes to CANCELLED, so we only need to stop
        // any ongoing actions.
        this.userCancellable = cancellable;
        setUserOrErrorCancellable();
        cancellable.cancelled().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                whenUserCancelDetected();
            });
    }

    private void setUserOrErrorCancellable()
    {
        userOrErrorCancellable = new OredCancellable(userCancellable, errorCancellable);
    }

    void setErrorCancellable(Cancellable errorCancellable)
    {
        // errorCancellable is set when an out-of-band error occurs such as a printer fault. The
        // state machine will also detect this state and go to FAILED, so we only need to stop
        // any ongoing actions.
        this.errorCancellable = errorCancellable;
        setUserOrErrorCancellable();
        errorCancellable.cancelled().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                whenErrorDetected();
            });
    }

    abstract void whenUserCancelDetected();
    abstract void whenErrorDetected();
    
}
