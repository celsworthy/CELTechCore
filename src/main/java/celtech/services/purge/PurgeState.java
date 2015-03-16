package celtech.services.purge;

import celtech.Lookup;

/**
 *
 * @author Ian
 */
public enum PurgeState
{

    /**
     *
     */
    IDLE("purgeMaterial.explanation", null),

    /**
     *
     */
    INITIALISING("purgeMaterial.temperatureInstruction", null),
    
    /**
     *
     */
    CONFIRM_TEMPERATURE("purgeMaterial.temperatureInstruction", null),

    /**
     *
     */
    HEATING("purgeMaterial.heating", null),

    /**
     *
     */
    RUNNING_PURGE("purgeMaterial.inProgress", null),

    /**
     *
     */
    FINISHED("purgeMaterial.purgeComplete", null),

    /**
     *
     */
    FAILED("purgeMaterial.failed", null);

    private String stepTitleResource = null;
    private String stepInstructionResource = null;

    private PurgeState(String stepTitleResource, String stepInstructionResource)
    {
        this.stepTitleResource = stepTitleResource;
        this.stepInstructionResource = stepInstructionResource;
    }

    /**
     *
     * @return
     */
    public PurgeState getNextState()
    {
        PurgeState returnState = null;

        PurgeState[] values = PurgeState.values();

        if (this != FINISHED && this != FAILED)
        {
            for (int i = 0; i < values.length; i++)
            {
                if (values[i] == this)
                {
                    returnState = values[i + 1];
                }
            }
        }

        return returnState;
    }

    /**
     *
     * @return
     */
    public String getStepTitle()
    {
        if (stepTitleResource == null)
        {
            return "";
        } else
        {
            return Lookup.i18n(stepTitleResource);
        }
    }

    /**
     *
     * @param suffix
     * @return
     */
    public String getStepTitle(String suffix)
    {
        if (stepTitleResource == null)
        {
            return "";
        } else
        {
            return Lookup.i18n(stepTitleResource + suffix);
        }
    }

    /**
     *
     * @return
     */
    public String getStepInstruction()
    {
        if (stepInstructionResource == null)
        {
            return "";
        } else
        {
            return Lookup.i18n(stepInstructionResource);
        }
    }

    /**
     *
     * @param suffix
     * @return
     */
    public String getStepInstruction(String suffix)
    {
        if (stepInstructionResource == null)
        {
            return "";
        } else
        {
            return Lookup.i18n(stepInstructionResource + suffix);
        }
    }
}
