package celtech.configuration;

import celtech.Lookup;

/**
 *
 * @author Ian
 */
public enum BusyStatus
{

    /**
     *
     */
    NOT_BUSY(0, null),

    /**
     *
     */
    BUSY(1, null),

    /**
     *
     */
    LOADING_FILAMENT(2, "printerStatus.loadingFilament"),

    /**
     *
     */
    UNLOADING_FILAMENT(3, "printerStatus.ejectingFilament");

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
    private final String i18nString;

    private BusyStatus(int value, String i18nString)
    {
        this.value = value;
        this.i18nString = i18nString;
    }
    
    /**
     *
     * @return
     */
    public int getValue()
    {
        return value;
    }
    
        /**
     *
     * @return
     */
    public String getI18nString()
    {
        return Lookup.i18n(i18nString);
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return getI18nString();
    }
}
