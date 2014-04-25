/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileContainer;

/**
 *
 * @author ianhudson
 */
public enum PrintQualityEnumeration
{

    DRAFT("Draft", PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName), 0),
    NORMAL("Normal", PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.normalSettingsProfileName), 1),
    FINE("Fine", PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.fineSettingsProfileName), 2),
    CUSTOM("Custom", PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.customSettingsProfileName), 3);

    private final String friendlyName;
    private final RoboxProfile settings;
    private final int enumPosition;

    private PrintQualityEnumeration(String friendlyName, RoboxProfile settings, int enumPosition)
    {
        this.friendlyName = friendlyName;
        this.settings = settings;
        this.enumPosition = enumPosition;
    }

    public String getFriendlyName()
    {
        return friendlyName;
    }

    public RoboxProfile getSettings()
    {
        return settings;
    }
    
    public int getEnumPosition()
    {
        return enumPosition;
    }

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

    @Override
    public String toString()
    {
        return friendlyName;
    }
}
