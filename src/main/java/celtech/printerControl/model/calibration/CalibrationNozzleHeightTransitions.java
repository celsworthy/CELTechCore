/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.Transitions;
import celtech.printerControl.model.CalibrationNozzleHeightActions;
import celtech.printerControl.model.ArrivalAction;
import celtech.printerControl.model.StateTransitionManager;
import celtech.printerControl.model.StateTransition;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author tony
 */
public class CalibrationNozzleHeightTransitions extends Transitions<NozzleOffsetCalibrationState>
{

    public CalibrationNozzleHeightTransitions(CalibrationNozzleHeightActions actions)
    {
        
        arrivals = new HashMap<>();

        arrivals.put(NozzleOffsetCalibrationState.FINISHED,
                     new ArrivalAction<>(() ->
                         {
                             actions.doFinishedAction();
                     },
                                         NozzleOffsetCalibrationState.FAILED));

        arrivals.put(NozzleOffsetCalibrationState.FAILED,
                     new ArrivalAction<>(() ->
                         {
                             actions.doFailedAction();
                     },
                                         NozzleOffsetCalibrationState.DONE));

        transitions = new HashSet<>();

        // IDLE
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.IDLE,
                                            StateTransitionManager.GUIName.START,
                                            NozzleOffsetCalibrationState.INITIALISING));

        transitions.add(new StateTransition(NozzleOffsetCalibrationState.IDLE,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOffsetCalibrationState.DONE));

        // INITIALISING
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INITIALISING,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.HEATING));

        // HEATING
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.HEATING,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.HEAD_CLEAN_CHECK,
                                            () ->
                                            {
                                                actions.doInitialiseAndHeatNozzleAction();
                                            }));

        // HEAD_CLEAN_CHECK
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.HEAD_CLEAN_CHECK,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE));

        // MEASURE_Z_DIFFERENCE
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.INSERT_PAPER,
                                            () ->
                                            {
                                                actions.doMeasureZDifferenceAction();
                                            }));

        // INSERT_PAPER 
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INSERT_PAPER,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.PROBING,
                                            () ->
                                            {
                                                actions.doHomeZAction();
                                            }));

        // PROBING
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.PROBING,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.BRING_BED_FORWARD,
                                            () ->
                                            {
                                                actions.doLiftHeadAction();
                                            }));

        transitions.add(new StateTransition(NozzleOffsetCalibrationState.PROBING,
                                            StateTransitionManager.GUIName.UP,
                                            NozzleOffsetCalibrationState.INCREMENT_Z,
                                            () ->
                                            {
                                                actions.doIncrementZAction();
                                            }));

        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INCREMENT_Z,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.PROBING,
                                            NozzleOffsetCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOffsetCalibrationState.PROBING,
                                            StateTransitionManager.GUIName.DOWN,
                                            NozzleOffsetCalibrationState.DECREMENT_Z,
                                            () ->
                                            {
                                                actions.doDecrementZAction();
                                            }));

        transitions.add(new StateTransition(NozzleOffsetCalibrationState.DECREMENT_Z,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.PROBING));

        // BRING_BED_FORWARD
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.BRING_BED_FORWARD,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.REPLACE_PEI_BED,
                                            () ->
                                            {
                                                actions.doBringBedToFrontAndRaiseHead();
                                            }));

        // REPLACE_PEI_BED
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.REPLACE_PEI_BED,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.FINISHED));

        // FINISHED
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.FINISHED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOffsetCalibrationState.DONE));

        // FAILED
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.FAILED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOffsetCalibrationState.DONE,
                                            NozzleOffsetCalibrationState.DONE));

        transitions.add(new StateTransition(NozzleOffsetCalibrationState.FAILED,
                                            StateTransitionManager.GUIName.RETRY,
                                            NozzleOffsetCalibrationState.IDLE,
                                            NozzleOffsetCalibrationState.DONE));

    }
}
