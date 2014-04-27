/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.calibration;

/**
 *
 * @author Ian
 */
public enum CalibrationState
{

    IDLE, INITIALISING, HEATING, PRIMING, NO_MATERIAL_CHECK, MATERIAL_EXTRUDING_CHECK, HEAD_CLEAN_CHECK, PRE_CALIBRATION_PRIMING, CALIBRATE_NOZZLE, CONFIRM_NO_MATERIAL, CONFIRM_MATERIAL_EXTRUDING, FINISHED, FAILED;

    public CalibrationState getNextState()
    {
        CalibrationState returnState = null;
        
        CalibrationState[] values = CalibrationState.values();
        
        if (this != FINISHED && this != FAILED)
        {
            for (int i = 0; i < values.length; i++)
            {
                if (values[i] == this)
                {
                    returnState = values[i+1];
                }
            }
        }

        return returnState;
    }
}
