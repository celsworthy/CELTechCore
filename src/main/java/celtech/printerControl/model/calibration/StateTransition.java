/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.calibration.StateTransitionManager.GUIName;
import celtech.utils.tasks.TaskExecutor;

/**
 * StateTransition represents a transition from the fromState to the toState. If the action has not been
 * set then the toState is reached either directly. If no action was set then it is called and the
 * toState is reached if the action did not fail (throw an exception). If the action fails (throws
 * an exception) then the transition goes to the transitionFailedState.
 * If the guiName is AUTO then the transition is run automatically whenever the fromState is reached.
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
     * If an action is declared then it takes no arguments and returns void. To indicate
     * a failure an exception should be raised.
     */
    final TaskExecutor.NoArgsVoidFunc action;

    public StateTransition(T fromState, StateTransitionManager.GUIName guiName, 
        T toState, TaskExecutor.NoArgsVoidFunc action, T failedState)
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
