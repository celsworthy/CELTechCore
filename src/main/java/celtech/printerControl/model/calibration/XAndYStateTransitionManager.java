/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.StateTransitionManager;
import celtech.printerControl.model.Transitions;
import celtech.printerControl.model.CalibrationXAndYActions;

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
        super(transitions, CalibrationXAndYState.IDLE, CalibrationXAndYState.CANCELLED,
                                                       CalibrationXAndYState.FAILED);
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
