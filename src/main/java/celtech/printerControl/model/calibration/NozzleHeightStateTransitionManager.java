/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.StateTransitionManager;
import celtech.printerControl.model.CalibrationNozzleHeightActions;
import javafx.beans.property.ReadOnlyDoubleProperty;

/**
 *
 * @author tony
 */
public class NozzleHeightStateTransitionManager extends StateTransitionManager<NozzleHeightCalibrationState>
{

    public NozzleHeightStateTransitionManager(StateTransitionActionsFactory stateTransitionActionsFactory,
        TransitionsFactory transitionsFactory)
    {
        super(stateTransitionActionsFactory, transitionsFactory, NozzleHeightCalibrationState.IDLE,
              NozzleHeightCalibrationState.CANCELLING, NozzleHeightCalibrationState.CANCELLED,
              NozzleHeightCalibrationState.FAILED);
    }

    public ReadOnlyDoubleProperty getZcoProperty()
    {
        return ((CalibrationNozzleHeightActions) actions).getZcoGUITProperty();
    }

}
