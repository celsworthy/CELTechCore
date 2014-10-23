/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.Lookup;
import celtech.services.calibration.CalibrationXAndYState;
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
public class CalibrationAlignmentManager
{

    public enum GUIName
    {

        START, CANCEL, BACK, NEXT, RETRY, COMPLETE, YES, NO;
    }

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationAlignmentManager.class.getName());

    Set<StateTransition> allowedTransitions;

    private final ObjectProperty<CalibrationXAndYState> state;

    public ReadOnlyObjectProperty<CalibrationXAndYState> stateProperty()
    {
        return state;
    }

    public CalibrationAlignmentManager(Set<StateTransition> allowedTransitions)
    {
        this.allowedTransitions = allowedTransitions;
        state = new SimpleObjectProperty<>(CalibrationXAndYState.IDLE);
    }

    public Set<StateTransition> getTransitions()
    {
        Set<StateTransition> transitions = new HashSet<>();
        for (StateTransition allowedTransition : allowedTransitions)
        {
            if (allowedTransition.fromState == state.get())
            {
                transitions.add(allowedTransition);
            }
        }
        return transitions;
    }

    private void setState(CalibrationXAndYState state)
    {
        this.state.set(state);
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

        StateTransition stateTransition = getTransitionForGUIName(guiName);

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
}
