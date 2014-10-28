/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.Lookup;
import celtech.services.calibration.Transitions;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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

    Transitions<StateType> transitions;
    Set<StateTransition<StateType>> allowedTransitions;
    Map<StateType, ArrivalAction<StateType>> arrivals;

    private final ObjectProperty<StateType> state;
    private final StateType cancelledState;

    public ReadOnlyObjectProperty<StateType> stateProperty()
    {
        return state;
    }

    public StateTransitionManager(Transitions<StateType> transitions, StateType initialState,
        StateType cancelledState)
    {
        this.transitions = transitions;
        this.allowedTransitions = transitions.getTransitions();
        this.cancelledState = cancelledState;
        this.arrivals = transitions.getArrivals();
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

        if (arrivals.containsKey(state))
        {

            ArrivalAction<StateType> arrival = arrivals.get(state);

            Runnable nullAction = () ->
            {
            };

            Runnable gotToFailedState = () ->
            {
                setState(arrival.failedState);
            };

            // Currently can't cancel an 'arrivedAt' action
            Runnable whenCancelled = () ->
            {
            };

            String taskName = String.format("State arrival at %s", state);

            Lookup.getTaskExecutor().runAsTask(arrival.action, nullAction,
                                               gotToFailedState, whenCancelled,
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

            Runnable goToNextState = () ->
            {
                setState(stateTransition.toState);
            };

            Runnable gotToFailedState = () ->
            {
                setState(stateTransition.transitionFailedState);
            };

            Runnable whenCancelled = () ->
            {
                try
                {
                    cancel();
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                    steno.error("Error processing cancelled action " + ex);
                }
            };

            String taskName = String.format("State transition from %s to %s",
                                            stateTransition.fromState, stateTransition.toState);

            Lookup.getTaskExecutor().runAsTask(stateTransition.action, goToNextState,
                                               gotToFailedState, whenCancelled,
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
    
    public void cancel() {
        
        try
        {
            transitions.cancel();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            steno.error("Error doing cancelled action " + ex);
        }
        setState(cancelledState);
    }

}
