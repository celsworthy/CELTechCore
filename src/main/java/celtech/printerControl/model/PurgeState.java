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

    IDLE("purgeMaterial.explanation", true),
    INITIALISING("purgeMaterial.temperatureInstruction", true),
    CONFIRM_TEMPERATURE("purgeMaterial.temperatureInstruction", true),
    HEATING("purgeMaterial.heating", true),
    RUNNING_PURGE("purgeMaterial.inProgress", true),
    FINISHED("purgeMaterial.purgeComplete", false),
    CANCELLED("", false),
    CANCELLING("", false),
    DONE("", false),
    FAILED("purgeMaterial.failed", false);

    private final String stepTitleResource;
    private boolean showCancel;

    private PurgeState(String stepTitleResource, boolean showCancel)
    {
        this.stepTitleResource = stepTitleResource;
        this.showCancel = showCancel;
    }

    public String getStepTitle()
    {
        if (stepTitleResource != "")
        {
            return Lookup.i18n(stepTitleResource);
        } else
        {
            return "";
        }
    }

    public boolean showCancelButton()
    {
        return showCancel;
    }

}
