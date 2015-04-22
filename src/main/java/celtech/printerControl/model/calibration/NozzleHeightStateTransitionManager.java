/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.StateTransitionManager;
import celtech.printerControl.model.Transitions;
import celtech.printerControl.model.CalibrationNozzleHeightActions;
import javafx.beans.property.ReadOnlyDoubleProperty;

/**
 *
 * @author tony
 */
public class NozzleHeightStateTransitionManager extends StateTransitionManager<NozzleOffsetCalibrationState>
{

    private final CalibrationNozzleHeightActions actions;

    public NozzleHeightStateTransitionManager(Transitions transitions,
        CalibrationNozzleHeightActions actions)
    {
        super(transitions, NozzleOffsetCalibrationState.IDLE,
              NozzleOffsetCalibrationState.CANCELLED);
        this.actions = actions;
    }

    public ReadOnlyDoubleProperty getZcoProperty()
    {
        return actions.getZcoGUITProperty();
    }

}
