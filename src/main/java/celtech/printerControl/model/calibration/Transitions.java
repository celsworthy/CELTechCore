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
    public Set<StateTransition<StateType>> getTransitions();
    public Map<StateType, ArrivalAction<StateType>> getArrivals();
    public void cancel() throws Exception;
}
