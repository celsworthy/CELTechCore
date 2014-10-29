/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.services.calibration.old;

import celtech.services.calibration.CalibrationXAndYState;

/**
 *
 * @author Ian
 */
public class CalibrationXAndYStepResult
{
    private CalibrationXAndYState completedState = null;
    private boolean success = false;

    /**
     *
     * @param completedState
     * @param success
     */
    public CalibrationXAndYStepResult(CalibrationXAndYState completedState, boolean success)
    {
        this.completedState = completedState;
        this.success = success;
    }
    
    /**
     *
     * @return
     */
    public CalibrationXAndYState getCompletedState()
    {
        return completedState;
    }
    
    /**
     *
     * @return
     */
    public boolean isSuccess()
    {
        return success;
    }
}
