/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.calibration.StateTransitionManager.GUIName;
import java.util.concurrent.Callable;

/**
 *
 * @author tony
 */
public class StateTransition<T>
{
    final T fromState;
    final T toState;
    final T transitionFailedState;
    final StateTransitionManager.GUIName guiName;
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
        this.fromState = fromState;
        this.toState = toState;
        this.guiName = guiName;
        this.action = null;
        transitionFailedState = failedState;
    }    
    
    public GUIName getGUIName() {
        return guiName;
    }
}
