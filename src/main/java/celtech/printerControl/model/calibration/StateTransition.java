/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.calibration.StateTransitionManager.GUIName;
import java.util.concurrent.Callable;

/**
 * StateTransition represents a transition from the fromState to the toState. The toState is reached
 * after the action is called, if it was set. If the action fails then the transition goes to
 * the transitionFailedState. If the guiName is AUTO is then the transition is run automatically
 * whenever the fromState is reached.
 * @author tony
 */
public class StateTransition<T>
{
    final T fromState;
    final T toState;
    final T transitionFailedState;
    final T transitionCancelledState;
    final StateTransitionManager.GUIName guiName;
    final Callable<Boolean> action;

    public StateTransition(T fromState, StateTransitionManager.GUIName guiName, 
        T toState, Callable action, T failedState, T cancelledState)
    {
        this.fromState = fromState;
        this.toState = toState;
        this.guiName = guiName;
        this.action = action;
        transitionFailedState = failedState;
        transitionCancelledState = cancelledState;
    }
    
    public StateTransition(T fromState, StateTransitionManager.GUIName guiName, 
        T toState, T failedState, T cancelledState)
    {
        this(fromState, guiName, toState, null, failedState, cancelledState);
    }    
    
    public GUIName getGUIName() {
        return guiName;
    }
}
