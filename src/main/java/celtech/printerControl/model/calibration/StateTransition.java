/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.calibration.StateTransitionManager.GUIName;
import celtech.services.calibration.CalibrationXAndYState;
import java.util.concurrent.Callable;

/**
 *
 * @author tony
 */
public class StateTransition
{
    final CalibrationXAndYState fromState;
    final CalibrationXAndYState toState;
    final CalibrationXAndYState transitionFailedState;
    final StateTransitionManager.GUIName guiName;
    final Callable<Boolean> action;

    public StateTransition(CalibrationXAndYState fromState, StateTransitionManager.GUIName guiName, 
        CalibrationXAndYState toState, Callable action)
    {
        this.fromState = fromState;
        this.toState = toState;
        this.guiName = guiName;
        this.action = action;
        transitionFailedState = CalibrationXAndYState.FAILED;
    }
    
    public StateTransition(CalibrationXAndYState fromState, StateTransitionManager.GUIName guiName, 
        CalibrationXAndYState toState)
    {
        this.fromState = fromState;
        this.toState = toState;
        this.guiName = guiName;
        this.action = null;
        transitionFailedState = CalibrationXAndYState.FAILED;
    }    
    
    public GUIName getGUIName() {
        return guiName;
    }
}
