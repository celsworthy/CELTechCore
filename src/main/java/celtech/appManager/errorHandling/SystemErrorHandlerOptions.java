/*
 * Copyright 2014 CEL UK
 */
package celtech.appManager.errorHandling;

import celtech.Lookup;

/**
 *
 * @author tony
 */
public enum SystemErrorHandlerOptions
{
    
    ABORT, CLEAR_CONTINUE, RETRY, OK, OK_ABORT, OK_CONTINUE;
    
    public String getLocalisedErrorTitle()
    {
        return Lookup.i18n("error.handler." + name() + ".title");
    }

    public String getLocalisedErrorMessage()
    {
        return Lookup.i18n("error.handler." + name() + ".message");
    }
}
