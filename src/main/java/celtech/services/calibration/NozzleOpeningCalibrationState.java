package celtech.services.calibration;

import celtech.coreUI.DisplayManager;

/**
 *
 * @author Ian
 */
public enum NozzleOpeningCalibrationState
{

    IDLE("calibrationPanel.readyToBeginNozzleOpeningCalibration"),

    HEATING("calibrationPanel.heating"),

    NO_MATERIAL_CHECK("calibrationPanel.valvesClosedNoMaterial"),
    
    T0_EXTRUDING("calibrationPanel.isMaterialExtrudingNozzle0"),
    
    T1_EXTRUDING("calibrationPanel.isMaterialExtrudingNozzle1"),
    
    HEAD_CLEAN_CHECK_AFTER_EXTRUDE("calibrationPanel.ensureHeadIsCleanBMessage"),

    PRE_CALIBRATION_PRIMING_FINE("calibrationPanel.primingNozzle"),

    CALIBRATE_FINE_NOZZLE("calibrationPanel.calibrationCommencedMessageFine"),
    
    INCREMENT_FINE_NOZZLE_POSITION(""),

    PRE_CALIBRATION_PRIMING_FILL("calibrationPanel.primingNozzle"),

    CALIBRATE_FILL_NOZZLE("calibrationPanel.calibrationCommencedMessageFill"),
    
    INCREMENT_FILL_NOZZLE_POSITION(""),

    HEAD_CLEAN_CHECK_FILL_NOZZLE("calibrationPanel.ensureHeadIsCleanBMessage"),

    CONFIRM_NO_MATERIAL("calibrationPanel.valvesClosedNoMaterialPostCalibration"),

//    CONFIRM_MATERIAL_EXTRUDING("calibrationPanel.valvesOpenMaterialExtruding"),

    FINISHED("calibrationPanel.calibrationSucceededMessage"),
    
    CANCELLED(""),
    
    DONE(""),

    FAILED("calibrationPanel.nozzleCalibrationFailed");

    private String stepTitleResource = null;

    private NozzleOpeningCalibrationState(String stepTitleResource)
    {
        this.stepTitleResource = stepTitleResource;
    }

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
