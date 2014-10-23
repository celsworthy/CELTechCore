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
                                            NozzleOffsetCalibrationState.PREFAILED));

        // INITIALISING
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INITIALISING,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.HEATING,
                                            NozzleOffsetCalibrationState.PREFAILED));

        transitions.add(makeCancelledStateTransition(NozzleOffsetCalibrationState.INITIALISING));

        // HEATING
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.HEATING,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.HEAD_CLEAN_CHECK,
                                            (Callable) () ->
                                            {
                                                return actions.doInitialiseAndHeatBedAction();
                                            },
                                            NozzleOffsetCalibrationState.PREFAILED));

        transitions.add(makeCancelledStateTransition(NozzleOffsetCalibrationState.HEATING));

        // HEAD_CLEAN_CHECK
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.HEAD_CLEAN_CHECK,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE,
                                            NozzleOffsetCalibrationState.PREFAILED));

        transitions.add(makeCancelledStateTransition(NozzleOffsetCalibrationState.HEAD_CLEAN_CHECK));

        // MEASURE_Z_DIFFERENCE
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.INSERT_PAPER,
                                            (Callable) () ->
                                            {
                                                return actions.doMeasureZDifferenceAction();
                                            },
                                            NozzleOffsetCalibrationState.PREFAILED));

        transitions.add(makeCancelledStateTransition(NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE));

        // INSERT_PAPER 
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INSERT_PAPER,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.PROBING,
                                            (Callable) () ->
                                            {
                                                return actions.doHomeZAction();
                                            },
                                            NozzleOffsetCalibrationState.PREFAILED));

        transitions.add(makeCancelledStateTransition(NozzleOffsetCalibrationState.INSERT_PAPER));

        // PROBING
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.PROBING,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.LIFT_HEAD,
                                            (Callable) () ->
                                            {
                                                return actions.doLiftHeadAction();
                                            },
                                            NozzleOffsetCalibrationState.PREFAILED));

        transitions.add(makeCancelledStateTransition(NozzleOffsetCalibrationState.PROBING));
        
        // LIFT_HEAD
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.LIFT_HEAD,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.REPLACE_PEI_BED,
                                            NozzleOffsetCalibrationState.PREFAILED));

        transitions.add(makeCancelledStateTransition(NozzleOffsetCalibrationState.LIFT_HEAD));
        
        // REPLACE_PEI_BED
        
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.REPLACE_PEI_BED,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.FINISHED,
                                            (Callable) () ->
                                            {
                                                return actions.doFinishedAction();
                                            },
                                            NozzleOffsetCalibrationState.PREFAILED));

        transitions.add(makeCancelledStateTransition(NozzleOffsetCalibrationState.REPLACE_PEI_BED));
        
        
        // FINISHED
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.FINISHED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOffsetCalibrationState.IDLE,
                                            NozzleOffsetCalibrationState.PREFAILED));

        // PRE-FAILED
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.PREFAILED,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.FAILED,
                                            (Callable) () ->
                                            {
                                                return actions.doFailedAction();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));

    }

    private StateTransition makeCancelledStateTransition(NozzleOffsetCalibrationState fromState)
    {
        return new StateTransition(fromState,
                                   StateTransitionManager.GUIName.CANCEL,
                                   NozzleOffsetCalibrationState.IDLE,
                                   (Callable) () ->
                                   {
                                       return actions.doCancelledAction();
                                   },
                                   NozzleOffsetCalibrationState.PREFAILED);
    }

    public Set<StateTransition<NozzleOffsetCalibrationState>> getTransitions()
    {
        return transitions;
    }

}
