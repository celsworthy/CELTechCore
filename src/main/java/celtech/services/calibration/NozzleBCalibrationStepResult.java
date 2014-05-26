/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.services.calibration;

/**
 *
 * @author Ian
 */
public class NozzleBCalibrationStepResult
{
    private NozzleBCalibrationState completedState = null;
    private boolean success = false;

    /**
     *
     * @param completedState
     * @param success
     */
    public NozzleBCalibrationStepResult(NozzleBCalibrationState completedState, boolean success)
    {
        this.completedState = completedState;
        this.success = success;
    }
    
    /**
     *
     * @return
     */
    public NozzleBCalibrationState getCompletedState()
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
