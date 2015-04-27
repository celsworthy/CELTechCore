/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.StateTransitionManager;
import celtech.printerControl.model.CalibrationXAndYActions;

/**
 *
 * @author tony
 */
public class XAndYStateTransitionManager extends StateTransitionManager<CalibrationXAndYState>
{

    public XAndYStateTransitionManager(StateTransitionActionsFactory stateTransitionActionsFactory,
        TransitionsFactory transitionsFactory)
    {
        super(stateTransitionActionsFactory, transitionsFactory, CalibrationXAndYState.IDLE, 
                      CalibrationXAndYState.CANCELLING, CalibrationXAndYState.CANCELLED,
                      CalibrationXAndYState.FAILED);
    }

    public void setXOffset(String xOffset)
    {
        ((CalibrationXAndYActions) actions).setXOffset(xOffset);
    }

    public void setYOffset(int yOffset)
    {
        ((CalibrationXAndYActions) actions).setYOffset(yOffset);
    }
}
