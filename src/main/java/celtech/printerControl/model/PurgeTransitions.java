/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author tony
 */
public class PurgeTransitions extends Transitions<PurgeState>
{
    /**
     * In the constructor we must populate the arrivals and transitions.
     */
    PurgeTransitions(PurgeActions actions)
    {
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

        // IDLE -> INITIALISING
        transitions.add(new StateTransition(PurgeState.IDLE,
                                            StateTransitionManager.GUIName.START,
                                            PurgeState.INITIALISING,
                                            PurgeState.FAILED));

        // INITIALISING -> CONFIRM_TEMPERATURE
        transitions.add(new StateTransition(PurgeState.INITIALISING,
                                            StateTransitionManager.GUIName.AUTO,
                                            PurgeState.CONFIRM_TEMPERATURE,
                                            () ->
                                            {
                                                actions.doInitialiseAction();
                                            },
                                            PurgeState.FAILED));

        // CONFIRM_TEMPERATURE -> HEATING
        transitions.add(new StateTransition(PurgeState.CONFIRM_TEMPERATURE,
                                            StateTransitionManager.GUIName.NEXT,
                                            PurgeState.HEATING,
                                            PurgeState.FAILED));

        // HEATING -> RUNNING_PURGE
        transitions.add(new StateTransition(PurgeState.HEATING,
                                            StateTransitionManager.GUIName.AUTO,
                                            PurgeState.RUNNING_PURGE,
                                            () ->
                                            {
                                                actions.doHeatingAction();
                                            },
                                            PurgeState.FAILED));

        // RUNNING_PURGE -> FINISHED
        transitions.add(new StateTransition(PurgeState.RUNNING_PURGE,
                                            StateTransitionManager.GUIName.AUTO,
                                            PurgeState.FINISHED,
                                            () ->
                                            {
                                                actions.doRunPurgeAction();
                                            },
                                            PurgeState.FAILED));

        // FINISHED (OK) -> DONE
        transitions.add(new StateTransition(PurgeState.FINISHED,
                                            StateTransitionManager.GUIName.COMPLETE,
                                            PurgeState.DONE,
                                            PurgeState.FAILED));

        // FINISHED (RETRY) -> INITIALISING
        transitions.add(new StateTransition(PurgeState.FINISHED,
                                            StateTransitionManager.GUIName.RETRY,
                                            PurgeState.INITIALISING,
                                            PurgeState.FAILED));
        
        // FAILED(OK) -> DONE
        transitions.add(new StateTransition(PurgeState.FAILED,
                                            StateTransitionManager.GUIName.COMPLETE,
                                            PurgeState.DONE,
                                            PurgeState.DONE));        
    }

}
