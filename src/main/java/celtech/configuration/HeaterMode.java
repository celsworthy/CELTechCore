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
public enum HeaterMode
{

    /**
     *
     */
    OFF(0),

    /**
     *
     */
    NORMAL(1),

    /**
     *
     */
    FIRST_LAYER(2);

    /**
     *
     * @param valueOf
     * @return
     */
    public static HeaterMode modeFromValue(Integer valueOf)
    {
        HeaterMode returnedMode = null;
        
        for (HeaterMode mode : HeaterMode.values())
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

    private HeaterMode(int value)
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
