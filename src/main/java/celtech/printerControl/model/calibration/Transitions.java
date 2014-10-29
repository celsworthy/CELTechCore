/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import java.util.Map;
import java.util.Set;

/**
 * The Transitions interface provides access to the 
 * allowed transitions {@link #getTransitions() getTransitions} of the state machine and
 * the actions to be performed when a state is arrived at {@link getArrivals() getArrivals}.
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
    /**
     * This should stop any currently running actions (if required).
     * @throws Exception 
     */
    public void cancel() throws Exception;
}
