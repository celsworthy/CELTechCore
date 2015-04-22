/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tony
 */
public class PurgeTransitions implements Transitions
{

    Set<StateTransition<PurgeState>> transitions;
    Map<PurgeState, ArrivalAction<PurgeState>> arrivals;
    private final PurgeActions actions;

    PurgeTransitions(PurgeActions actions)
    {
        this.actions = actions;

        arrivals = new HashMap<>();

        arrivals.put(PurgeState.FINISHED,
                     new ArrivalAction<>(() ->
                         {
                             actions.doFinishedAction();
                     },
                                         PurgeState.FAILED));

        arrivals.put(PurgeState.FAILED,
                     new ArrivalAction<>(() ->
                         {
                             actions.doFailedAction();
                     },
                                         PurgeState.FINISHED));

        transitions = new HashSet<>();

        // IDLE
        transitions.add(new StateTransition(PurgeState.IDLE,
                                            StateTransitionManager.GUIName.START,
                                            PurgeState.INITIALISING,
                                            PurgeState.FAILED));

        // INITIALISING
        transitions.add(new StateTransition(PurgeState.INITIALISING,
                                            StateTransitionManager.GUIName.AUTO,
                                            PurgeState.CONFIRM_TEMPERATURE,
                                            () ->
                                            {
                                                actions.doInitialiseAction();
                                            },
                                            PurgeState.FAILED));

        // CONFIRM_TEMPERATURE
        transitions.add(new StateTransition(PurgeState.CONFIRM_TEMPERATURE,
                                            StateTransitionManager.GUIName.NEXT,
                                            PurgeState.HEATING,
                                            PurgeState.FAILED));

        // HEATING
        transitions.add(new StateTransition(PurgeState.HEATING,
                                            StateTransitionManager.GUIName.AUTO,
                                            PurgeState.RUNNING_PURGE,
                                            () ->
                                            {
                                                actions.doHeatingAction();
                                            },
                                            PurgeState.FAILED));

        // RUNNING_PURGE
        transitions.add(new StateTransition(PurgeState.RUNNING_PURGE,
                                            StateTransitionManager.GUIName.AUTO,
                                            PurgeState.FINISHED,
                                            () ->
                                            {
                                                actions.doRunPurgeAction();
                                            },
                                            PurgeState.FAILED));

        // FINISHED (OK)
        transitions.add(new StateTransition(PurgeState.FINISHED,
                                            StateTransitionManager.GUIName.COMPLETE,
                                            PurgeState.DONE,
                                            PurgeState.FAILED));

        // FINISHED (RETRY)
        transitions.add(new StateTransition(PurgeState.FINISHED,
                                            StateTransitionManager.GUIName.RETRY,
                                            PurgeState.INITIALISING,
                                            PurgeState.FAILED));
    }

    @Override
    public Set getTransitions()
    {
        return transitions;
    }

    @Override
    public Map getArrivals()
    {
        return arrivals;
    }

    @Override
    public void cancel() throws Exception
    {
        actions.cancel();
    }

}
