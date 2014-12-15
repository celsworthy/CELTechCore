package celtech.services.slicer;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;

/**
 *
 * @author ianhudson
 */
public enum PrintQualityEnumeration
{

    /**
     *
     */
    DRAFT("Draft", SlicerParametersContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName), 0),

    /**
     *
     */
    NORMAL("Normal", SlicerParametersContainer.getSettingsByProfileName(ApplicationConfiguration.normalSettingsProfileName), 1),

    /**
     *
     */
    FINE("Fine", SlicerParametersContainer.getSettingsByProfileName(ApplicationConfiguration.fineSettingsProfileName), 2),

    /**
     *
     */
    CUSTOM("Custom", SlicerParametersContainer.getSettingsByProfileName(ApplicationConfiguration.customSettingsProfileName), 3);

    private final String friendlyName;
    private final SlicerParametersFile settings;
    private final int enumPosition;

    private PrintQualityEnumeration(String friendlyName, SlicerParametersFile settings, int enumPosition)
    {
        this.friendlyName = friendlyName;
        this.settings = settings;
        this.enumPosition = enumPosition;
    }

    /**
     *
     * @return
     */
    public String getFriendlyName()
    {
        return friendlyName;
    }

    /**
     *
     * @return
     */
    public SlicerParametersFile getSettings()
    {
        return settings;
    }
    
    /**
     *
     * @return
     */
    public int getEnumPosition()
    {
        return enumPosition;
    }

    /**
     *
     * @param enumPosition
     * @return
     */
    public static PrintQualityEnumeration fromEnumPosition(int enumPosition)
    {
        PrintQualityEnumeration returnVal = null;
        
        for (PrintQualityEnumeration value:values())
        {
            if (value.getEnumPosition() == enumPosition)
            {
                returnVal = value;
                break;
            }
        }

        return returnVal;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return friendlyName;
    }
}
