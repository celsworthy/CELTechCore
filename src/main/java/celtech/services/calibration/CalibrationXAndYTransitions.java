/*
 * Copyright 2014 CEL UK
 */
package celtech.services.calibration;

import celtech.printerControl.model.calibration.CalibrationAlignmentManager;
import celtech.printerControl.model.calibration.StateTransition;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 *
 * @author tony
 */
public class CalibrationXAndYTransitions
{
    
    CalibrationXAndYActions actions;
    Set<StateTransition> transitions;
    CalibrationAlignmentManager manager;
    
    public CalibrationXAndYTransitions(CalibrationXAndYActions actions)
    {
        this.actions = actions;
        transitions = new HashSet<>();
        
        // IDLE
        
        transitions.add(new StateTransition(CalibrationXAndYState.IDLE,
                                            CalibrationAlignmentManager.GUIName.START,
                                            CalibrationXAndYState.PRINT_PATTERN));
        
        // PRINT PATTERN
        
        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_PATTERN,
                                            CalibrationAlignmentManager.GUIName.AUTO,
                                            CalibrationXAndYState.GET_Y_OFFSET,
                                            (Callable) () ->
                                            {
                                                return actions.doSaveHeadAndPrintPattern();
                                            }));   
        
        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_PATTERN,
                                            CalibrationAlignmentManager.GUIName.CANCEL,
                                            CalibrationXAndYState.IDLE));           
        
        // GET Y OFFSET
        
        transitions.add(new StateTransition(CalibrationXAndYState.GET_Y_OFFSET,
                                            CalibrationAlignmentManager.GUIName.NEXT,
                                            CalibrationXAndYState.PRINT_CIRCLE));    
        
        transitions.add(new StateTransition(CalibrationXAndYState.GET_Y_OFFSET,
                                            CalibrationAlignmentManager.GUIName.RETRY,
                                            CalibrationXAndYState.PRINT_PATTERN));    
        
        transitions.add(new StateTransition(CalibrationXAndYState.GET_Y_OFFSET,
                                            CalibrationAlignmentManager.GUIName.CANCEL,
                                            CalibrationXAndYState.IDLE));   
        
        // PRINT CIRCLE
        
        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE,
                                            CalibrationAlignmentManager.GUIName.AUTO,
                                            CalibrationXAndYState.PRINT_CIRCLE_CHECK,
                                            (Callable) () ->
                                            {
                                                return actions.doSaveSettingsAndPrintCircle();
                                            }));   
        
        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE,
                                            CalibrationAlignmentManager.GUIName.CANCEL,
                                            CalibrationXAndYState.IDLE));   
        
        // PRINT CIRCLE CHECK
        
        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE_CHECK,
                                            CalibrationAlignmentManager.GUIName.NEXT,
                                            CalibrationXAndYState.FINISHED,
                                            (Callable) () ->
                                            {
                                                return actions.doFinishedAction();
                                            }));   
        
        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE_CHECK,
                                            CalibrationAlignmentManager.GUIName.CANCEL,
                                            CalibrationXAndYState.IDLE));             

        // FINISHED
        
        transitions.add(new StateTransition(CalibrationXAndYState.FINISHED,
                                            CalibrationAlignmentManager.GUIName.BACK,
                                            CalibrationXAndYState.IDLE));  
        
    }
    
    
    public Set<StateTransition> getTransitions() {
        return transitions;
    }
    
    
}
