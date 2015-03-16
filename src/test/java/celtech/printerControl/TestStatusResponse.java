/*
 * Copyright 2014 CEL UK
 */

package celtech.printerControl;

import celtech.configuration.EEPROMState;
import celtech.printerControl.comms.commands.rx.StatusResponse;

/**
 *
 * @author tony
 */
public class TestStatusResponse extends StatusResponse
{
    
    public EEPROMState getHeadEEPROMState()
    {
        return EEPROMState.NOT_PROGRAMMED;
    }
    
}
