/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.services.calibration.NozzleOpeningCalibrationState;
import celtech.services.calibration.Transitions;

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
