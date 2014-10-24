/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.CalibrationNozzleHeightActions;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import java.util.Set;

/**
 *
 * @author tony
 */
public class NozzleHeightStateTransitionManager extends StateTransitionManager<NozzleOffsetCalibrationState>
{

    private final CalibrationNozzleHeightActions actions;

    public NozzleHeightStateTransitionManager(
        Set<StateTransition<NozzleOffsetCalibrationState>> allowedTransitions,
        CalibrationNozzleHeightActions actions)
    {
        super(allowedTransitions, NozzleOffsetCalibrationState.IDLE);
        this.actions = actions;
    }

    public double getZco()
    {
        return actions.getZco();
    }

}
