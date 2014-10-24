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
     */
    IDLE("calibrationPanel.readyToBeginNozzleOffsetCalibration"),
    /**
     *
     */
    INITIALISING("calibrationPanel.initialisingOffset"),    
    /**
     *
     */
    HEATING("calibrationPanel.heating"),
    /**
     *
     */
    HEAD_CLEAN_CHECK("calibrationPanel.headCleanCheck"),
    /**
     *
     */
    MEASURE_Z_DIFFERENCE("calibrationPanel.measuringZOffset"),
    /**
     *
     */
    INSERT_PAPER("calibrationPanel.insertPieceOfPaper"),
    /**
     *
     */
    PROBING("calibrationPanel.moveThePaperMessage"),
    /**
     *
     */
    INCREMENT_Z(""),
    
    DECREMENT_Z(""),
    
    
    LIFT_HEAD(""),
    /**
     *
     */    
    REPLACE_PEI_BED("calibrationPanel.replacePEIBed"),
    /**
     *
     */    
    FINISHED("calibrationPanel.calibrationSucceededMessage"),
    /**
     *
     */
    PREFAILED(""),
    
    FAILED("calibrationPanel.nozzleCalibrationFailed"),
    /**
     *
     */
    NUDGE_MODE(null);

    private String stepTitleResource = null;

    private NozzleOffsetCalibrationState(String stepTitleResource)
    {
        this.stepTitleResource = stepTitleResource;
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

 

}
