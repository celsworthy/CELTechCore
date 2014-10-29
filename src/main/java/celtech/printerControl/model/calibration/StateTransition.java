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
    /**
     * The state machine must be in the fromState for this transition to be applicable.
     */
    final T fromState;
    /**
     * The state machine will go to the toState when this transition is followed.
     */
    final T toState;
    /**
     * If the transition fails (the action throws an exception) then the state machine will
     * go to the transitionFailedState.
     */
    final T transitionFailedState;
    /**
     * The GUI action associated with this transition.
     */
    final StateTransitionManager.GUIName guiName;
    /**
     * If an action is declared then it must return a boolean. a return value of true
     * indicates the action succeeded, false indicates the action was cancelled. To indicate
     * a failure an exception should be raised.
     */
    final Callable<Boolean> action;

    public StateTransition(T fromState, StateTransitionManager.GUIName guiName, 
        T toState, Callable action, T failedState)
    {
        this.fromState = fromState;
        this.toState = toState;
        this.guiName = guiName;
        this.action = action;
        transitionFailedState = failedState;
    }
    
    public StateTransition(T fromState, StateTransitionManager.GUIName guiName, 
        T toState, T failedState)
    {
        this(fromState, guiName, toState, null, failedState);
    }    
    
    public GUIName getGUIName() {
        return guiName;
    }
}
