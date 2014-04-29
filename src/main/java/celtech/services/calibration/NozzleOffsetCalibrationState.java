
package celtech.services.calibration;

/**
 *
 * @author Ian
 */
public enum NozzleOffsetCalibrationState
{

    IDLE, INITIALISING, MEASURE_Z_DIFFERENCE, INSERT_PAPER, PROBING, FINISHED, FAILED;

    public NozzleOffsetCalibrationState getNextState()
    {
        NozzleOffsetCalibrationState returnState = null;
        
        NozzleOffsetCalibrationState[] values = NozzleOffsetCalibrationState.values();
        
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
