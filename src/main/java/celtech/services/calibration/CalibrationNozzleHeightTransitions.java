/*
 * Copyright 2014 CEL UK
 */
package celtech.services.calibration;

import celtech.printerControl.model.calibration.Transitions;
import celtech.printerControl.model.CalibrationNozzleHeightActions;
import celtech.printerControl.model.calibration.ArrivalAction;
import celtech.printerControl.model.calibration.StateTransitionManager;
import celtech.printerControl.model.calibration.StateTransition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tony
 */
public class CalibrationNozzleHeightTransitions implements Transitions
{

    CalibrationNozzleHeightActions actions;
    Set<StateTransition<NozzleOffsetCalibrationState>> transitions;
    Map<NozzleOffsetCalibrationState, ArrivalAction<NozzleOffsetCalibrationState>> arrivals;
    StateTransitionManager manager;

    public CalibrationNozzleHeightTransitions(CalibrationNozzleHeightActions actions)
    {
        this.actions = actions;
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
                                         NozzleOffsetCalibrationState.FAILED));

//        arrivals.put(NozzleOffsetCalibrationState.REPLACE_PEI_BED,
//                     new ArrivalAction<>(() ->
//                         {
//                             actions.doBringBedToFront();
//                     },
//                                         NozzleOffsetCalibrationState.FAILED));

        transitions = new HashSet<>();

        // IDLE
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.IDLE,
                                            StateTransitionManager.GUIName.START,
                                            NozzleOffsetCalibrationState.INITIALISING,
                                            NozzleOffsetCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOffsetCalibrationState.IDLE,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOffsetCalibrationState.DONE,
                                            NozzleOffsetCalibrationState.FAILED));

        // INITIALISING
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INITIALISING,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.HEATING,
                                            NozzleOffsetCalibrationState.FAILED));

        // HEATING
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.HEATING,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.HEAD_CLEAN_CHECK,
                                            () ->
                                            {
                                                actions.doInitialiseAndHeatNozzleAction();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));

        // HEAD_CLEAN_CHECK
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.HEAD_CLEAN_CHECK,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE,
                                            NozzleOffsetCalibrationState.FAILED));

        // MEASURE_Z_DIFFERENCE
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.INSERT_PAPER,
                                            () ->
                                            {
                                                actions.doMeasureZDifferenceAction();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));

        // INSERT_PAPER 
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.INSERT_PAPER,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.PROBING,
                                            () ->
                                            {
                                                actions.doHomeZAction();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));

        // PROBING
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.PROBING,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.LIFT_HEAD,
                                            () ->
                                            {
                                                actions.doLiftHeadAction();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOffsetCalibrationState.PROBING,
                                            StateTransitionManager.GUIName.UP,
                                            NozzleOffsetCalibrationState.INCREMENT_Z,
                                            () ->
                                            {
                                                actions.doIncrementZAction();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));

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
                                            },
                                            NozzleOffsetCalibrationState.FAILED));

        transitions.add(new StateTransition(NozzleOffsetCalibrationState.DECREMENT_Z,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.PROBING,
                                            NozzleOffsetCalibrationState.FAILED));

        // LIFT_HEAD
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.LIFT_HEAD,
                                            StateTransitionManager.GUIName.AUTO,
                                            NozzleOffsetCalibrationState.REPLACE_PEI_BED,
                                            () ->
                                            {
                                                actions.doBringBedToFront();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));

        // REPLACE_PEI_BED
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.REPLACE_PEI_BED,
                                            StateTransitionManager.GUIName.NEXT,
                                            NozzleOffsetCalibrationState.FINISHED,
                                            () ->
                                            {
                                                actions.doFinishedAction();
                                            },
                                            NozzleOffsetCalibrationState.FAILED));

        // FINISHED
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.FINISHED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOffsetCalibrationState.DONE,
                                            NozzleOffsetCalibrationState.FAILED));

        // FAILED
        transitions.add(new StateTransition(NozzleOffsetCalibrationState.FAILED,
                                            StateTransitionManager.GUIName.BACK,
                                            NozzleOffsetCalibrationState.DONE,
                                            NozzleOffsetCalibrationState.DONE));

    }

    public Set<StateTransition<NozzleOffsetCalibrationState>> getTransitions()
    {
        return transitions;
    }

    public Map<NozzleOffsetCalibrationState, ArrivalAction<NozzleOffsetCalibrationState>> getArrivals()
    {
        return arrivals;
    }

    @Override
    public void cancel() throws Exception
    {
        actions.cancel();
    }

}
