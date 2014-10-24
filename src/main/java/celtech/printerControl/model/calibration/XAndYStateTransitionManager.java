/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.printerControl.model.CalibrationXAndYActions;
import celtech.services.calibration.CalibrationXAndYState;
import java.util.Set;

/**
 *
 * @author tony
 */
public class XAndYStateTransitionManager extends StateTransitionManager<CalibrationXAndYState>
{

    private final CalibrationXAndYActions actions;

    public XAndYStateTransitionManager(Set<StateTransition<CalibrationXAndYState>> allowedTransitions,
        CalibrationXAndYActions actions)
    {
        super(allowedTransitions, CalibrationXAndYState.IDLE);
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
