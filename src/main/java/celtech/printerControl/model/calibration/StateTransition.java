/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

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
    final CalibrationAlignmentManager.GUIName guiName;
    final Callable action;

    public StateTransition(CalibrationXAndYState fromState, CalibrationXAndYState toState, 
        CalibrationAlignmentManager.GUIName guiName, Callable action)
    {
        this.fromState = fromState;
        this.toState = toState;
        this.guiName = guiName;
        this.action = action;
        transitionFailedState = CalibrationXAndYState.FAILED;
    }
}
