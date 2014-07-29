/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.configuration;

/**
 *
 * @author Ian
 */
public enum PauseStatus
{

    /**
     *
     */
    NOT_PAUSED(0),

    /**
     *
     */
    PAUSE_PENDING(1),

    /**
     *
     */
    PAUSED(2),

    /**
     *
     */
    RESUME_PENDING(3);

    /**
     *
     * @param valueOf
     * @return
     */
    public static PauseStatus modeFromValue(Integer valueOf)
    {
        PauseStatus returnedMode = null;
        
        for (PauseStatus mode : PauseStatus.values())
        {
            if (mode.getValue() == valueOf)
            {
                returnedMode = mode;
                break;
            }
        }
        
        return returnedMode;
    }
    
    private int value;

    private PauseStatus(int value)
    {
        this.value = value;
    }
    
    /**
     *
     * @return
     */
    public int getValue()
    {
        return value;
    }
}
