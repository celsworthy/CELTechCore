/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.StateTransitionManager;
import celtech.printerControl.model.CalibrationNozzleOpeningActions;
import javafx.beans.property.ReadOnlyFloatProperty;

/**
 *
 * @author tony
 */
public class NozzleOpeningStateTransitionManager extends StateTransitionManager<NozzleOpeningCalibrationState>
{

    public NozzleOpeningStateTransitionManager(
        StateTransitionActionsFactory stateTransitionActionsFactory,
        TransitionsFactory transitionsFactory)
    {
        super(stateTransitionActionsFactory, transitionsFactory, NozzleOpeningCalibrationState.IDLE,
              NozzleOpeningCalibrationState.CANCELLING, NozzleOpeningCalibrationState.CANCELLED,
              NozzleOpeningCalibrationState.FAILED);
    }

    public ReadOnlyFloatProperty getBPositionProperty()
    {
        return ((CalibrationNozzleOpeningActions) actions).getBPositionGUITProperty();
    }

}
