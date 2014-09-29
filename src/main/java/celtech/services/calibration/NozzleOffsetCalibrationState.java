package celtech.services.calibration;

import celtech.coreUI.DisplayManager;

/**
 *
 * @author Ian
 */
public enum NozzleOffsetCalibrationState
{

    /**
     *
     *//**
     *
     */
    IDLE("calibrationPanel.readyToBeginNozzleOffsetCalibration", null),
    /**
     *
     */
    INITIALISING("calibrationPanel.initialisingOffset", null),    
    /**
     *
     */
    HEATING("calibrationPanel.heating", null),
    /**
     *
     */
    HEAD_CLEAN_CHECK("calibrationPanel.ensureHeadIsCleanOffsetMessage", null),
    /**
     *
     */
    MEASURE_Z_DIFFERENCE("calibrationPanel.measuringZOffset", null),
    /**
     *
     */
    INSERT_PAPER("calibrationPanel.insertPieceOfPaper", "calibrationPanel.isThePaperInPlace"),
    /**
     *
     */
    PROBING("calibrationPanel.moveThePaperMessage", null),
    /**
     *
     */
    FINISHED("calibrationPanel.calibrationSucceededOffsetMessage", null),
    /**
     *
     */
    FAILED("calibrationPanel.nozzleCalibrationFailed", null),
    /**
     *
     */
    NUDGE_MODE(null, null);

    private String stepTitleResource = null;
    private String stepInstructionResource = null;

    private NozzleOffsetCalibrationState(String stepTitleResource, String stepInstructionResource)
    {
        this.stepTitleResource = stepTitleResource;
        this.stepInstructionResource = stepInstructionResource;
    }

    /**
     *
     * @return
     */
    public NozzleOffsetCalibrationState getNextState()
    {
        NozzleOffsetCalibrationState returnState = null;

        NozzleOffsetCalibrationState[] values = NozzleOffsetCalibrationState.values();

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
            return DisplayManager.getLanguageBundle().getString(stepTitleResource);
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
            return DisplayManager.getLanguageBundle().getString(stepTitleResource + suffix);
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
            return DisplayManager.getLanguageBundle().getString(stepInstructionResource);
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
            return DisplayManager.getLanguageBundle().getString(stepInstructionResource + suffix);
        }
    }

}
