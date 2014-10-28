/*
 * Copyright 2014 CEL UK
 */
package celtech.services.calibration;

import celtech.printerControl.model.calibration.ArrivalAction;
import celtech.printerControl.model.calibration.StateTransition;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tony
 */
public interface Transitions<StateType>
{
    public Set<StateTransition<StateType>> getTransitions();
    public Map<StateType, ArrivalAction<StateType>> getArrivals();
    public void cancel() throws Exception;
}
