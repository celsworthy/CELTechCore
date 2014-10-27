/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.Lookup;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class StateTransitionManager<StateType>
{

    public enum GUIName
    {
        START, CANCEL, BACK, NEXT, RETRY, COMPLETE, YES, NO, UP, DOWN, A_BUTTON, B_BUTTON, AUTO;
    }

    private final Stenographer steno = StenographerFactory.getStenographer(
        StateTransitionManager.class.getName());

    Set<StateTransition<StateType>> allowedTransitions;
    Map<StateType, ArrivalAction<StateType>> arrivals;

    private final ObjectProperty<StateType> state;

    public ReadOnlyObjectProperty<StateType> stateProperty()
    {
        return state;
    }

    public StateTransitionManager(Set<StateTransition<StateType>> allowedTransitions,
        Map<StateType, ArrivalAction<StateType>> arrivals, StateType initialState)
    {
        this.allowedTransitions = allowedTransitions;
        this.arrivals = arrivals;
        state = new SimpleObjectProperty<>(initialState);
    }

    public Set<StateTransition<StateType>> getTransitions()
    {
        Set<StateTransition<StateType>> transitions = new HashSet<>();
        for (StateTransition<StateType> allowedTransition : allowedTransitions)
        {
            if (allowedTransition.fromState == state.get())
            {
                transitions.add(allowedTransition);
            }
        }
        return transitions;
    }

    private void setState(StateType state)
    {
        this.state.set(state);
        processArrivedAtState(state);
        followAutoTransitionIfPresent();
    }
    
    private void processArrivedAtState(StateType state)
    {
        
        if (arrivals.containsKey(state)) {
            ArrivalAction<StateType> arrival = arrivals.get(state);
            
            EventHandler<WorkerStateEvent> nullAction = (WorkerStateEvent event) ->
            {
            };

            EventHandler<WorkerStateEvent> gotToFailedState = (WorkerStateEvent event) ->
            {
                setState(arrival.failedState);
            };

            String taskName = String.format("State arrival at %s", state);

            Lookup.getTaskExecutor().runAsTask(arrival.action, nullAction,
                                               gotToFailedState,
                                               taskName);
        }
    }

    private StateTransition getTransitionForGUIName(GUIName guiName)
    {
        StateTransition foundTransition = null;
        for (StateTransition transition : getTransitions())
        {
            if (transition.guiName == guiName)
            {
                foundTransition = transition;
                break;
            }
        }
        return foundTransition;
    }

    public void followTransition(GUIName guiName)
    {

        StateTransition<StateType> stateTransition = getTransitionForGUIName(guiName);

        if (stateTransition == null)
        {
            throw new RuntimeException("No transition found from state " + state.get()
                + " for action " + guiName);
        }

        if (stateTransition.action == null)
        {
            setState(stateTransition.toState);

        } else
        {
            EventHandler<WorkerStateEvent> goToNextState = (WorkerStateEvent event) ->
            {
                setState(stateTransition.toState);
            };

            EventHandler<WorkerStateEvent> gotToFailedState = (WorkerStateEvent event) ->
            {
                setState(stateTransition.transitionFailedState);
            };

            String taskName = String.format("State transition from %s to %s",
                                            stateTransition.fromState, stateTransition.toState);

            Lookup.getTaskExecutor().runAsTask(stateTransition.action, goToNextState,
                                               gotToFailedState,
                                               taskName);
        }
    }

    /**
     * If the newly entered state has an AUTO transition then follow it.
     */
    private void followAutoTransitionIfPresent()
    {
        for (StateTransition allowedTransition : getTransitions())
        {
            if (allowedTransition.guiName == GUIName.AUTO)
            {
                followTransition(GUIName.AUTO);
            }
        }
    }

}
