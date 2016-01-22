/*
 * Copyright 2014 CEL UK
 */

package celtech.printerControl;

import celtech.comms.remote.EEPROMState;
import celtech.comms.remote.rx.StatusResponse;

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
