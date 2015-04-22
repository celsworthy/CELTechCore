/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.Lookup;

/**
 *
 * @author tony
 */
public enum PurgeState
{

    IDLE("purgeMaterial.explanation"),
    INITIALISING("purgeMaterial.temperatureInstruction"),
    CONFIRM_TEMPERATURE("purgeMaterial.temperatureInstruction"),
    HEATING("purgeMaterial.heating"),
    RUNNING_PURGE("purgeMaterial.inProgress"),
    FINISHED("purgeMaterial.purgeComplete"),
    CANCELLED(""),
    FAILED("purgeMaterial.failed");

    private final String stepTitleResource;

    private PurgeState(String stepTitleResource)
    {
        this.stepTitleResource = stepTitleResource;
    }
    
    public String getStepTitle() {
        return Lookup.i18n(stepTitleResource);
    }

    public boolean showCancelButton()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
