/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.Lookup;
import java.util.HashSet;
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

        START, CANCEL, BACK, NEXT, RETRY, COMPLETE, YES, NO, AUTO;
    }

    private final Stenographer steno = StenographerFactory.getStenographer(StateTransitionManager.class.getName());

    Set<StateTransition<StateType>> allowedTransitions;

    private final ObjectProperty<StateType> state;

    public ReadOnlyObjectProperty<StateType> stateProperty()
    {
        return state;
    }

    public StateTransitionManager(Set<StateTransition<StateType>> allowedTransitions, StateType initialState)
    {
        this.allowedTransitions = allowedTransitions;
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
        checkForAutoFollowOnState();
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
    private void checkForAutoFollowOnState()
    {
        for (StateTransition allowedTransition : getTransitions())
        {
            if (allowedTransition.guiName == GUIName.AUTO) {
                followTransition(GUIName.AUTO);
            }
        }
    }    
   
}
