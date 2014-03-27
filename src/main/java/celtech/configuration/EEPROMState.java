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
public enum EEPROMState
{
    NOT_PRESENT(0), NOT_PROGRAMMED(1), PROGRAMMED(2);

    public static EEPROMState modeFromValue(Integer valueOf)
    {
        EEPROMState returnedMode = null;
        
        for (EEPROMState mode : EEPROMState.values())
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

    private EEPROMState(int value)
    {
        this.value = value;
    }
    
    public int getValue()
    {
        return value;
    }
}
