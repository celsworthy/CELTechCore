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
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     *//**
     *
     */
    IDLE("calibrationPanel.readyToBeginNozzleOpeningCalibration", null),

    /**
     *
     */
    HEATING("calibrationPanel.heating", null),

    /**
     *
     */
    NO_MATERIAL_CHECK("calibrationPanel.valvesClosedNoMaterial", null),

    /**
     *
     */
    MATERIAL_EXTRUDING_CHECK_FINE_NOZZLE("calibrationPanel.valvesOpenMaterialExtrudingFine", null),

    /**
     *
     */
    HEAD_CLEAN_CHECK_FINE_NOZZLE("calibrationPanel.ensureHeadIsCleanBMessage", null),
    
    /**
     *
     */
    MATERIAL_EXTRUDING_CHECK_FILL_NOZZLE("calibrationPanel.valvesOpenMaterialExtrudingFill", null),

    /**
     *
     */
    HEAD_CLEAN_CHECK_FILL_NOZZLE("calibrationPanel.ensureHeadIsCleanBMessage", null),    

    /**
     *
     */
    PRE_CALIBRATION_PRIMING("calibrationPanel.primingNozzle", null),

    /**
     *
     */
    CALIBRATE_NOZZLE("calibrationPanel.calibrationCommencedMessage", null),

    /**
     *
     */
    HEAD_CLEAN_CHECK_POST_CALIBRATION("calibrationPanel.ensureHeadIsCleanBMessage", null),

    /**
     *
     */
    POST_CALIBRATION_PRIMING("calibrationPanel.primingNozzle", null),

    /**
     *
     */
    CONFIRM_NO_MATERIAL("calibrationPanel.valvesClosedNoMaterial", "calibrationPanel.isMaterialExtrudingEitherNozzle"),

    /**
     *
     */
    CONFIRM_MATERIAL_EXTRUDING("calibrationPanel.valvesOpenMaterialExtruding", "calibrationPanel.isMaterialExtrudingNozzle"),

    /**
     *
     */
    PARKING("calibrationPanel.calibrationParkingMessage", null),
    
    /**
     *
     */
    FINISHED("calibrationPanel.calibrationSucceededBMessage", null),

    /**
     *
     */
    FAILED("calibrationPanel.nozzleCalibrationFailed", null);

    private String stepTitleResource = null;
        private String stepInstructionResource = null;

    private NozzleBCalibrationState(String stepTitleResource, String stepInstructionResource)
    {
        this.stepTitleResource = stepTitleResource;
        this.stepInstructionResource = stepInstructionResource;
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
