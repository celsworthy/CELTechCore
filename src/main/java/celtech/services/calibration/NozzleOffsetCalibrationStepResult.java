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
public class NozzleOffsetCalibrationStepResult
{
    private NozzleOffsetCalibrationState completedState = null;
    private boolean success = false;
    private float floatValue = 0;

    public NozzleOffsetCalibrationStepResult(NozzleOffsetCalibrationState completedState, float floatValue, boolean success)
    {
        this.completedState = completedState;
        this.floatValue = floatValue;
        this.success = success;
    }
    
    public NozzleOffsetCalibrationState getCompletedState()
    {
        return completedState;
    }
    
    public boolean isSuccess()
    {
        return success;
    }
    
    public float getFloatValue()
    {
        return floatValue;
    }
}
