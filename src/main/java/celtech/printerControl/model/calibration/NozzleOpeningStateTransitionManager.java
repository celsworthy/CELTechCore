/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.StateTransitionManager;
import celtech.printerControl.model.Transitions;
import celtech.printerControl.model.CalibrationNozzleOpeningActions;
import javafx.beans.property.ReadOnlyFloatProperty;

/**
 *
 * @author tony
 */
public class NozzleOpeningStateTransitionManager extends StateTransitionManager<NozzleOpeningCalibrationState>
{
    private final CalibrationNozzleOpeningActions actions;

    public NozzleOpeningStateTransitionManager(Transitions<NozzleOpeningCalibrationState> transitions,
        CalibrationNozzleOpeningActions actions)
    {
        super(transitions, NozzleOpeningCalibrationState.IDLE, NozzleOpeningCalibrationState.CANCELLED);
        this.actions = actions;
    }
    
        public ReadOnlyFloatProperty getBPositionProperty()
    {
        return actions.getBPositionGUITProperty();
    }

}
