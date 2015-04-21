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
                                            PurgeState.PURGING,
                                            PurgeState.FAILED));

        // PURGING
        transitions.add(new StateTransition(PurgeState.PURGING,
                                            StateTransitionManager.GUIName.NEXT,
                                            PurgeState.FINISHED,
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
