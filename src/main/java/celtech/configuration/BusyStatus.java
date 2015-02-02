package celtech.configuration;

/**
 *
 * @author Ian
 */
public enum BusyStatus
{

    /**
     *
     */
    NOT_BUSY(0),

    /**
     *
     */
    BUSY(1),

    /**
     *
     */
    LOADING_FILAMENT(2),

    /**
     *
     */
    UNLOADING_FILAMENT(3);

    /**
     *
     * @param valueOf
     * @return
     */
    public static BusyStatus modeFromValue(Integer valueOf)
    {
        BusyStatus returnedMode = null;
        
        for (BusyStatus mode : BusyStatus.values())
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

    private BusyStatus(int value)
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
