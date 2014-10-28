package celtech.services.calibration;

import celtech.coreUI.DisplayManager;

/**
 *
 * @author Ian
 */
public enum CalibrationXAndYState
{

    IDLE("calibrationPanel.xAndYIntroduction"),
    
//    HEATING("calibrationPanel.heating"),
    
    PRINT_PATTERN("calibrationPanel.xAndYPrintPattern"),
    
    GET_Y_OFFSET("calibrationPanel.xAndYGetOffsets"),
    
    PRINT_CIRCLE("calibrationPanel.xAndYPrintingCircle"),
    
    PRINT_CIRCLE_CHECK("calibrationPanel.xAndYPrintCircleCheck"),
    
    FINISHED("calibrationPanel.calibrationSucceededMessage"),
    
    CANCELLED(""),
    
    DONE(""),

    FAILED("calibrationPanel.nozzleCalibrationFailed");

    private String stepTitleResource;

    private CalibrationXAndYState(String stepTitleResource)
    {
        this.stepTitleResource = stepTitleResource;
    }

    /**
     *
     * @return
     */
    public CalibrationXAndYState getNextState()
    {
        CalibrationXAndYState returnState = null;

        CalibrationXAndYState[] values = CalibrationXAndYState.values();

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
}
