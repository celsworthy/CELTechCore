/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.services.calibration.NozzleOpeningCalibrationState;

/**
 *
 * @author tony
 */
public class NozzleOpeningStateTransitionManager extends StateTransitionManager<NozzleOpeningCalibrationState>
{

    public NozzleOpeningStateTransitionManager(Transitions<NozzleOpeningCalibrationState> transitions)
    {
        super(transitions, NozzleOpeningCalibrationState.IDLE, NozzleOpeningCalibrationState.CANCELLED);
    }

}
