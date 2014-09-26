package celtech.services.calibration;

import celtech.coreUI.DisplayManager;

/**
 *
 * @author Ian
 */
public enum NozzleBCalibrationState
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
    CONFIRM_MATERIAL_EXTRUDING_FINE("calibrationPanel.valvesOpenMaterialExtrudingFine"),
    /**
     *
     */
    CONFIRM_MATERIAL_EXTRUDING_FILL("calibrationPanel.valvesOpenMaterialExtrudingFill"),
    /**
     *
     */
    PARKING("calibrationPanel.calibrationParkingMessage"),
    /**
     *
     */
    FINISHED("calibrationPanel.calibrationSucceededBMessage"),
    /**
     *
     */
    FAILED("calibrationPanel.nozzleCalibrationFailed");

    private String stepTitleResource = null;

    private NozzleBCalibrationState(String stepTitleResource)
    {
        this.stepTitleResource = stepTitleResource;
    }

    /**
     *
     * @return
     */
    public NozzleBCalibrationState getNextState()
    {
        NozzleBCalibrationState returnState = null;

        NozzleBCalibrationState[] values = NozzleBCalibrationState.values();

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
