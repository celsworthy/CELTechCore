/*
 * Copyright 2014 CEL UK
 */
package celtech.services.calibration;

import celtech.printerControl.model.calibration.Transitions;
import celtech.printerControl.model.CalibrationXAndYActions;
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
public class CalibrationXAndYTransitions implements Transitions
{

    CalibrationXAndYActions actions;
    Set<StateTransition<CalibrationXAndYState>> transitions;
    Map<CalibrationXAndYState, ArrivalAction<CalibrationXAndYState>> arrivals;
    StateTransitionManager manager;

    public CalibrationXAndYTransitions(CalibrationXAndYActions actions)
    {
        this.actions = actions;
        transitions = new HashSet<>();
        arrivals = new HashMap<>();

        arrivals.put(CalibrationXAndYState.FINISHED,
                     new ArrivalAction<>(() ->
                         {
                             actions.doFinishedAction();
                     },
                                         CalibrationXAndYState.FAILED));

        arrivals.put(CalibrationXAndYState.FAILED,
                     new ArrivalAction<>(() ->
                         {
                             actions.doFailedAction();
                     },
                                         CalibrationXAndYState.FAILED));

        // IDLE
        transitions.add(new StateTransition(CalibrationXAndYState.IDLE,
                                            StateTransitionManager.GUIName.START,
                                            CalibrationXAndYState.PRINT_PATTERN,
                                            CalibrationXAndYState.FAILED));

        // PRINT PATTERN
        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_PATTERN,
                                            StateTransitionManager.GUIName.AUTO,
                                            CalibrationXAndYState.GET_Y_OFFSET,
                                            () ->
                                            {
                                                actions.doSaveHeadAndPrintPattern();
                                            },
                                            CalibrationXAndYState.FAILED));

        // GET Y OFFSET
        transitions.add(new StateTransition(CalibrationXAndYState.GET_Y_OFFSET,
                                            StateTransitionManager.GUIName.NEXT,
                                            CalibrationXAndYState.PRINT_CIRCLE,
                                            CalibrationXAndYState.FAILED));

        transitions.add(new StateTransition(CalibrationXAndYState.GET_Y_OFFSET,
                                            StateTransitionManager.GUIName.RETRY,
                                            CalibrationXAndYState.PRINT_PATTERN,
                                            CalibrationXAndYState.FAILED));

        // PRINT CIRCLE
        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE,
                                            StateTransitionManager.GUIName.AUTO,
                                            CalibrationXAndYState.PRINT_CIRCLE_CHECK,
                                            () ->
                                            {
                                                actions.doSaveSettingsAndPrintCircle();
                                            },
                                            CalibrationXAndYState.FAILED));

        // PRINT CIRCLE CHECK
        transitions.add(new StateTransition(CalibrationXAndYState.PRINT_CIRCLE_CHECK,
                                            StateTransitionManager.GUIName.NEXT,
                                            CalibrationXAndYState.FINISHED,
                                            CalibrationXAndYState.FAILED));

        // FINISHED
//        transitions.add(new StateTransition(CalibrationXAndYState.FINISHED,
//                                            StateTransitionManager.GUIName.BACK,
//                                            CalibrationXAndYState.DONE,
//                                            CalibrationXAndYState.FAILED));
//
//        // FAILED
//        transitions.add(new StateTransition(CalibrationXAndYState.FAILED,
//                                            StateTransitionManager.GUIName.BACK,
//                                            CalibrationXAndYState.DONE,
//                                            CalibrationXAndYState.DONE));

    }

    public Set<StateTransition<CalibrationXAndYState>> getTransitions()
    {
        return transitions;
    }

    public Map<CalibrationXAndYState, ArrivalAction<CalibrationXAndYState>> getArrivals()
    {
        return arrivals;
    }

    @Override
    public void cancel() throws Exception
    {
        actions.cancel();
    }

}
