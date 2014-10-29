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
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * The StateTransitionManager maintains the state, and follows transitions from one state to
 * the other. Transitions {@link StateTransition} can have actions which will be called when the transition is followed.
 * <p>
 * GUIs should call  {@link #getTransitions() getTransitions} and for each transition returned ther is
 * a GUIName. This indicates what transitions are available to the user e.g. Next, Back, Retry, Up.
 * </p>
 * <p>
 * If the user selects e.g. Next, then {@link #followTransition(GUIName guiName) followTransition} should
 * be called. This will cause the StateTransitionManager to follow that transition to its toState,
 * executing the appropriate action if it is present.
 * </p>
 * <p>
 * The GUI can allow the user to cancel the whole process (even during a long-running
 * transition) by calling the {@link #cancel() cancel} method. The StateTransitionManager will then
 * move to the cancelledState state. If it is desired to run an action on the cancel then it
 * should be implemented in the {@link Transitions} class.
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
    /**
     * The actions {@link ArrivalAction} to perform when given states are arrived at.
     */
    Map<StateType, ArrivalAction<StateType>> arrivals;

    /**
     * The state that the machine is currently in.
     */
    private final ObjectProperty<StateType> state;
    /**
     * The state to go to if {@link cancel() cancel} is called.
     */
    private final StateType cancelledState;

    /**
     * Return the current state as a property.
     * @return the current state.
     */
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

    /**
     * Get the transitions that can be followed from the current {@link #state}.
     * @return 
     */
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

    /**
     * Set the current state. Process any relevant {@link ArrivalAction} and if there is
     * an AUTO transition from this state the follow it.
     * @param state 
     */
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

    /**
     * Follow the {@link StateTransition} associated with this GUIName. If there is an action
     * declared then call it. If the action succeeds (or if there is no action) then move to
     * the toState of the relevant {@link StateTransition}. If the action is cancelled (i.e.
     * returns false) then call {@link cancel() cancel}. If the action fails (i.e. throws
     * an exception) then move to the {@link StateTransition#transitionFailedState}.
     * @param guiName 
     */
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
    
    /**
     * Move to the {@link cancelledState}. Also call the cancel() method of {@link #transitions},
     * which might for instance cause any current long-running action to be stopped.
     */
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
