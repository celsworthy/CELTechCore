/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.Lookup;
import celtech.utils.tasks.Cancellable;
import celtech.utils.tasks.SimpleCancellable;
import celtech.utils.tasks.TaskExecutor;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * The StateTransitionManager maintains the state, and follows transitions from one state to the
 * other. Transitions ({@link StateTransition}) can have actions which will be called when the
 * transition is followed.
 * <p>
 * It also functions as the data transfer interface between the StateActions instance and the GUI.
 * GUIs therefore only need to deal with the StateTransitionManager.
 * </p><p>
 * GUIs should call {@link #getTransitions() getTransitions} and for each transition returned there
 * is a GUIName. This indicates which transitions are available to the user e.g. Next, Back, Retry,
 * Up.
 * </p>
 * <p>
 * If the user selects e.g. Next, then
 * {@link #followTransition(GUIName guiName) followTransition(guiName)} should be called. This will
 * cause the StateTransitionManager to follow that transition to its toState, executing the
 * appropriate action if it is present.
 * </p>
 * <p>
 * The GUI can allow the user to cancel the whole process (even during a long-running transition) by
 * calling the {@link #cancel() cancel} method. The StateTransitionManager will then move to the
 * cancelledState state, after allowing any ongoing transition/arrival to complete. Actions should
 * listen for user/error states and terminate themselves early in that case {
 *
 * @see StateTransitionActions#userOrErrorCancellable}.
 * </p>
 * <p>
 * All StateTransitionManager methods should be called from the GUI thread. All actions are run in a
 * new thread.
 *
 * @author tony
 */
public class StateTransitionManager<StateType>
{

    public interface StateTransitionActionsFactory
    {

        public StateTransitionActions makeStateTransitionActions(Cancellable userCancellable,
            Cancellable errorCancellable);
    }

    public interface TransitionsFactory
    {

        public Transitions makeTransition(StateTransitionActions actions);
    }

    /**
     * An enum of GUI transitions. Any number of new values can be freely added.
     */
    public enum GUIName
    {

        START, CANCEL, BACK, NEXT, RETRY, COMPLETE, YES, NO, UP, DOWN, A_BUTTON, B_BUTTON, AUTO;
    }

    private final Stenographer steno = StenographerFactory.getStenographer(
        StateTransitionManager.class.getName());

    protected StateTransitionActions actions;

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
     * A copy of {@link #state} this is only updated in the GUI thread
     */
    private final ObjectProperty<StateType> stateGUIT;

    /**
     * userCancellable is set from the {@link #cancel() cancel method} when the user requests a
     * cancellation. It will cause the state machine to go to the cancelledState. It is usually
     * triggered by the user clicking the cancel button. The StateTransitionActions instance should
     * always be listening to this and should stop any ongoing actions if cancelled is set to true.
     */
    private final Cancellable userCancellable = new SimpleCancellable();

    /**
     * errorCancellable is set programmatically when a fatal error occurs outside of the normal flow
     * of transitions (e.g a printer error). It will cause the state machine to go to the
     * failedState. it is usually set by a printer error consumer. The StateTransitionActions
     * instance should always be listening to this and should stop any ongoing actions if cancelled
     * is set to true.
     */
    private final Cancellable errorCancellable = new SimpleCancellable();

    private final StateType cancelledState;
    private final StateType failedState;
    private final StateType initialState;

    private boolean runningAction;

    /**
     * Return the current state as a property. This variable is only updated on the GUI thread.
     *
     * @return the current state.
     */
    public ReadOnlyObjectProperty<StateType> stateGUITProperty()
    {
        return stateGUIT;
    }

    /**
     * Construct a StateTransitionManager.
     *
     * @param stateTransitionActionsFactory
     * @param transitionsFactory
     * @param initialState The initial state that the machine will start in.
     * @param cancelledState The state to go to if {@link cancel() cancel} is called.
     * @param failedState The state to go to if {@link #errorCancellable} is set.
     */
    public StateTransitionManager(StateTransitionActionsFactory stateTransitionActionsFactory,
        TransitionsFactory transitionsFactory, StateType initialState,
        StateType cancelledState, StateType failedState)
    {
        actions = stateTransitionActionsFactory.makeStateTransitionActions(
            userCancellable,
            errorCancellable);
        Transitions<StateType> transitions = transitionsFactory.makeTransition(actions);

        this.allowedTransitions = transitions.getTransitions();
        this.arrivals = transitions.getArrivals();
        this.cancelledState = cancelledState;
        this.failedState = failedState;
        this.initialState = initialState;

        state = new SimpleObjectProperty<>(initialState);
        stateGUIT = new SimpleObjectProperty<>(initialState);
        state.addListener(
            (ObservableValue<? extends StateType> observable, StateType oldValue, StateType newValue) ->
            {
                Lookup.getTaskExecutor().runOnGUIThread(() ->
                    {
                        stateGUIT.set(state.get());
                });
            });

        userCancellable.cancelled().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                userCancelRequested();
            });
        errorCancellable.cancelled().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                errorCancelRequested();
            });
    }

    /**
     * Initialise all variables and set state to the initial state. The intention is that the state
     * machine can be restarted at any time.
     */
    public void start()
    {
        userCancellable.cancelled().set(false);
        errorCancellable.cancelled().set(false);
        runningAction = false;
        actions.initialise();
        setState(initialState);
    }

    /**
     * Get the transitions that can be followed from the current {@link #state}.
     *
     * @return
     */
    public Set<StateTransition<StateType>> getTransitions()
    {
        return getTransitions(state.get());
    }

    public Set<StateTransition<StateType>> getTransitions(StateType state)
    {
        Set<StateTransition<StateType>> transitions = new HashSet<>();
        for (StateTransition<StateType> allowedTransition : allowedTransitions)
        {
            if (allowedTransition.fromState == state)
            {
                transitions.add(allowedTransition);
            }
        }
        return transitions;
    }

    /**
     * Set the current state. Process any relevant {@link ArrivalAction} and if there is an AUTO
     * transition from this state the follow it.
     *
     * @param state
     */
    private void setState(StateType state)
    {
        steno.debug("Set State to " + state + " for " + this);
        this.state.set(state);
        processArrivedAtState(state);
        followAutoTransitionIfPresent(state);
    }

    private void processArrivedAtState(StateType state)
    {

        if (arrivals.containsKey(state))
        {
            runningAction = true;

            ArrivalAction<StateType> arrival = arrivals.get(state);

            TaskExecutor.NoArgsVoidFunc nullAction = () ->
            {
                runningAction = false;
                if (userCancellable.cancelled().get() || errorCancellable.cancelled().get())
                {
                    doCancelOrErrorDetected();
                }
            };

            TaskExecutor.NoArgsVoidFunc gotToFailedState = () ->
            {
                runningAction = false;
                if (arrival.failedState != null)
                {
                    setState(arrival.failedState);
                } else
                {
                    setState(failedState);
                }
            };

            String taskName = String.format("State arrival at %s", state);

            Lookup.getTaskExecutor().runAsTask(arrival.action, nullAction,
                                               gotToFailedState, taskName);
        }
    }

    /**
     * doCancelOrErrorDetected is called after an error or cancel. If the error/cancel occurred
     * during a transition or arrival then the transition/arrival is allowed to complete before this
     * is called.
     */
    private void doCancelOrErrorDetected()
    {
        try
        {
            actions.resetAfterCancelOrError();
        } catch (Exception ex)
        {
            steno.error("Error processing reset after cancel / error");
        }
        setState(cancelledState);
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
     * declared then call it. If the action succeeds (or if there is no action) then move to the
     * toState of the relevant {@link StateTransition}. If the action fails (i.e. throws an
     * exception) then move to the {@link StateTransition#transitionFailedState}.
     *
     * @param guiName
     */
    public void followTransition(GUIName guiName)
    {

        StateTransition<StateType> stateTransition = getTransitionForGUIName(guiName);

        if (stateTransition == null)
        {
            throw new RuntimeException("No transition found from state " + state.get()
                + " for action " + guiName + " for " + this);
        }

        steno.debug("Follow transition " + guiName + " " + stateTransition.fromState + " "
            + stateTransition.toState);

        if (stateTransition.action == null)
        {
            setState(stateTransition.toState);

        } else
        {

            runningAction = true;

            TaskExecutor.NoArgsVoidFunc goToNextState = () ->
            {
                runningAction = false;
                if (!userCancellable.cancelled().get() && !errorCancellable.cancelled().get())
                {
                    setState(stateTransition.toState);
                } else
                {
                    steno.debug("Cancel detected during transition action");
                    doCancelOrErrorDetected();
                }
            };

            TaskExecutor.NoArgsVoidFunc gotToFailedState = () ->
            {
                runningAction = false;
                if (!userCancellable.cancelled().get() && !errorCancellable.cancelled().get())
                {
                    if (stateTransition.transitionFailedState != null)
                    {
                        setState(stateTransition.transitionFailedState);
                    } else
                    {
                        setState(failedState);
                    }
                } else
                {
                    // if there is a cancel during a fail then we only process failed action and do
                    // not call doCancelOrErrorDetected.
                    setState(failedState);
                }
            };

            String taskName = String.format("State transition from %s to %s",
                                            stateTransition.fromState, stateTransition.toState);

            Lookup.getTaskExecutor().runAsTask(stateTransition.action, goToNextState,
                                               gotToFailedState, taskName);
        }
    }

    /**
     * If the given (newly entered) state has an AUTO transition then follow it.
     */
    private void followAutoTransitionIfPresent(StateType state)
    {
        for (StateTransition allowedTransition : getTransitions(state))
        {
            if (allowedTransition.guiName == GUIName.AUTO)
            {
                followTransition(GUIName.AUTO);
            }
        }
    }

    /**
     * Try to cancel any ongoing transition then move to the {@link cancelledState}. Any actions
     * tied to active state transitions / arrivals should either be listening to changes on this
     * userCancellable and stop themselves, or StateTransitionActions.whenUserCancelDetected should
     * stop them instead.
     */
    public void cancel()
    {
        userCancellable.cancelled().set(true);
    }

    private void userCancelRequested()
    {
        if (!runningAction)
        {
            doCancelOrErrorDetected();
        }
    }

    private void errorCancelRequested()
    {
        if (!runningAction)
        {
            doCancelOrErrorDetected();
        }
    }

}
