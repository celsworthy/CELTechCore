package celtech.services.calibration;

import celtech.coreUI.DisplayManager;

/**
 *
 * @author Ian
 */
public enum NozzleOpeningCalibrationState
{

    /**
     *
     */
    IDLE("calibrationPanel.readyToBeginNozzleOpeningCalibration"),
    /**
     *
     */
    HEATING("calibrationPanel.heating"),
    /**
     *
     */
    NO_MATERIAL_CHECK("calibrationPanel.valvesClosedNoMaterial"),
    /**
     *
     */
    PRE_CALIBRATION_PRIMING_FINE("calibrationPanel.primingNozzle"),
    /**
     *
     */
    CALIBRATE_FINE_NOZZLE("calibrationPanel.calibrationCommencedMessageFine"),
    
    INCREMENT_FINE_NOZZLE_POSITION(""),
    
    /**
     *
     */
    HEAD_CLEAN_CHECK_FINE_NOZZLE("calibrationPanel.ensureHeadIsCleanBMessage"),
    /**
     *
     */
    PRE_CALIBRATION_PRIMING_FILL("calibrationPanel.primingNozzle"),
    /**
     *
     */
    CALIBRATE_FILL_NOZZLE("calibrationPanel.calibrationCommencedMessageFill"),
    
    INCREMENT_FILL_NOZZLE_POSITION(""),
    /**
     *
     */
    HEAD_CLEAN_CHECK_FILL_NOZZLE("calibrationPanel.ensureHeadIsCleanBMessage"),
    /**
     *
     */
    CONFIRM_NO_MATERIAL("calibrationPanel.valvesClosedNoMaterialPostCalibration"),
    /**
     *
     */
    CONFIRM_MATERIAL_EXTRUDING("calibrationPanel.valvesOpenMaterialExtruding"),
    /**
     *
     */
    FINISHED("calibrationPanel.calibrationSucceededMessage"),
    /**
     *
     */
    FAILED("calibrationPanel.nozzleCalibrationFailed");

    private String stepTitleResource = null;

    private NozzleOpeningCalibrationState(String stepTitleResource)
    {
        this.stepTitleResource = stepTitleResource;
    }

    /**
     *
     * @return
     */
    public NozzleOpeningCalibrationState getNextState()
    {
        NozzleOpeningCalibrationState returnState = null;

        NozzleOpeningCalibrationState[] values = NozzleOpeningCalibrationState.values();

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
