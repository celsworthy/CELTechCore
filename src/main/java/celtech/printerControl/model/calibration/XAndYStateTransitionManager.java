/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.services.calibration.CalibrationXAndYActions;
import java.util.Set;

/**
 *
 * @author tony
 */
public class XAndYStateTransitionManager extends StateTransitionManager
{

    private final CalibrationXAndYActions actions;

    public XAndYStateTransitionManager(Set<StateTransition> allowedTransitions,
        CalibrationXAndYActions actions)
    {
        super(allowedTransitions);
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
