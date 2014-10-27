/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.CalibrationNozzleOpeningActions;
import celtech.services.calibration.NozzleOpeningCalibrationState;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tony
 */
public class NozzleOpeningStateTransitionManager extends StateTransitionManager<NozzleOpeningCalibrationState>
{

    private final CalibrationNozzleOpeningActions actions;

    public NozzleOpeningStateTransitionManager(
        Set<StateTransition<NozzleOpeningCalibrationState>> allowedTransitions,
        Map<NozzleOpeningCalibrationState, ArrivalAction<NozzleOpeningCalibrationState>> arrivals,
        CalibrationNozzleOpeningActions actions)
    {
        super(allowedTransitions, arrivals, NozzleOpeningCalibrationState.IDLE);
        this.actions = actions;
    }

}
