/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import java.util.Map;
import java.util.Set;

/**
 * The Transitions interface provides access to the 
 * allowed transitions {@link #getTransitions() getTransitions} of the state machine and
 * the actions to be performed when a state is arrived at {@link getArrivals() getArrivals}.
 * Actions which are set as part of a transition will run when the transition is initiated
 * and the toState will not be reached until the action has completed. Actions which are
 * declared as arrival actions will start immediately after the arrival state is reached.
 * @author tony
 */
public interface Transitions<StateType>
{
    /**
     * Return the set of {@link StateTransition}s that are valid for this state machine.
     */
    public Set<StateTransition<StateType>> getTransitions();
    /**
     * Return a Map of state to {@link ArrivalAction} that should run when the given states
     * are reached.
     * @return 
     */
    public Map<StateType, ArrivalAction<StateType>> getArrivals();

}
