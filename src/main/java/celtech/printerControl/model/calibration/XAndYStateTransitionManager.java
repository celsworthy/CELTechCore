/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.CalibrationXAndYActions;
import celtech.services.calibration.CalibrationXAndYState;

/**
 *
 * @author tony
 */
public class XAndYStateTransitionManager extends StateTransitionManager<CalibrationXAndYState>
{

    private final CalibrationXAndYActions actions;

    public XAndYStateTransitionManager(Transitions transitions,
        CalibrationXAndYActions actions)
    {
        super(transitions, CalibrationXAndYState.IDLE, CalibrationXAndYState.CANCELLED);
        this.actions = actions;
    }

    public void setXOffset(String xOffset)
    {
        actions.setXOffset(xOffset);
    }

    public void setYOffset(int yOffset)
    {
        actions.setYOffset(yOffset);
    }
}
