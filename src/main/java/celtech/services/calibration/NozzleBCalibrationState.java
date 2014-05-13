package celtech.services.calibration;

import celtech.coreUI.DisplayManager;

/**
 *
 * @author Ian
 */
public enum NozzleBCalibrationState
{

    IDLE("calibrationPanel.readyToBeginNozzleOpeningCalibration", null),
    INITIALISING("calibrationPanel.calibrationInitialising", null),
    HEATING("calibrationPanel.heating", null),
    PRIMING("calibrationPanel.primingNozzle", null),
    NO_MATERIAL_CHECK("calibrationPanel.valvesClosedNoMaterial", "calibrationPanel.isMaterialExtrudingEitherNozzle"),
    MATERIAL_EXTRUDING_CHECK("calibrationPanel.valvesOpenMaterialExtruding", "calibrationPanel.isMaterialExtrudingNozzle"),
    HEAD_CLEAN_CHECK("calibrationPanel.ensureHeadIsCleanBMessage", "calibrationPanel.ensureHeadIsCleanInstruction"),
    PRE_CALIBRATION_PRIMING("calibrationPanel.primingNozzle", null),
    CALIBRATE_NOZZLE("calibrationPanel.calibrationCommencedMessage", null),
    HEAD_CLEAN_CHECK_POST_CALIBRATION("calibrationPanel.ensureHeadIsCleanMessage", null),
    POST_CALIBRATION_PRIMING("calibrationPanel.primingNozzle", null),
    CONFIRM_NO_MATERIAL("calibrationPanel.valvesClosedNoMaterial", "calibrationPanel.isMaterialExtrudingEitherNozzle"),
    CONFIRM_MATERIAL_EXTRUDING("calibrationPanel.valvesOpenMaterialExtruding", "calibrationPanel.isMaterialExtrudingNozzle"),
    FINISHED("calibrationPanel.calibrationSucceededBMessage", null),
    FAILED("calibrationPanel.nozzleCalibrationFailed", null);

    private String stepTitleResource = null;
    private String stepInstructionResource = null;

    private NozzleBCalibrationState(String stepTitleResource, String stepInstructionResource)
    {
        this.stepTitleResource = stepTitleResource;
        this.stepInstructionResource = stepInstructionResource;
    }

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
