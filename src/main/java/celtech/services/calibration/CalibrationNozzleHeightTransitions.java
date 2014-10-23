/*
 * Copyright 2014 CEL UK
 */
package celtech.services.calibration;

import celtech.printerControl.model.calibration.StateTransitionManager;
import celtech.printerControl.model.calibration.StateTransition;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 *
 * @author tony
 */
public class CalibrationNozzleHeightTransitions
{

    CalibrationNozzleHeightActions actions;
    Set<StateTransition<NozzleOffsetCalibrationState>> transitions;
    StateTransitionManager manager;

    public CalibrationNozzleHeightTransitions(CalibrationNozzleHeightActions actions)
    {
        this.actions = actions;
        transitions = new HashSet<>();

        // IDLE
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.IDLE,
                                            StateTransitionManager.GUIName.START,
                                            NozzleOffsetCalibrationState.INITIALISING,
                                            NozzleOffsetCalibrationState.FAILED));
        
        // INITIALISING
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INITIALISING,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.HEATING,
                                            NozzleOffsetCalibrationState.FAILED));  
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INITIALISING,
                                            StateTransitionManager.GUIName.CANCEL,
                                            NozzleOffsetCalibrationState.IDLE,
                                            NozzleOffsetCalibrationState.FAILED));          

        // HEATING
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.HEATING,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.HEAD_CLEAN_CHECK,
                                            (Callable) () ->
                                            {
                                                return actions.doInitialiseAndHeatBedAction();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.HEATING,
                                            StateTransitionManager.GUIName.CANCEL,
                                            NozzleOffsetCalibrationState.IDLE,
                                            NozzleOffsetCalibrationState.FAILED));         

        // HEAD_CLEAN_CHECK
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.HEAD_CLEAN_CHECK,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE,
                                            NozzleOffsetCalibrationState.FAILED)); 
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.HEAD_CLEAN_CHECK,
                                            StateTransitionManager.GUIName.CANCEL,
                                            NozzleOffsetCalibrationState.IDLE,
                                            NozzleOffsetCalibrationState.FAILED));  
        
        // MEASURE_Z_DIFFERENCE
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.INSERT_PAPER,
                                            (Callable) () ->
                                            {
                                                return actions.doMeasureZDifferenceAction();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE,
                                            StateTransitionManager.GUIName.CANCEL,
                                            NozzleOffsetCalibrationState.IDLE,
                                            NozzleOffsetCalibrationState.FAILED));    
        
        // INSERT_PAPER 
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INSERT_PAPER,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.PROBING,
                                            (Callable) () ->
                                            {
                                                return actions.doHomeZAction();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INSERT_PAPER,
                                            StateTransitionManager.GUIName.CANCEL,
                                            NozzleOffsetCalibrationState.IDLE,
                                            NozzleOffsetCalibrationState.FAILED));   
        
        // PROBING
      
        // FINISHED
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.FINISHED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOffsetCalibrationState.IDLE,
                                            NozzleOffsetCalibrationState.FAILED));

    }

    public Set<StateTransition<NozzleOffsetCalibrationState>> getTransitions()
    {
        return transitions;
    }

}
