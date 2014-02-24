/*
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
    DRAFT("Draft", PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName)),
    NORMAL("Normal", PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.normalSettingsProfileName)),
    FINE("Fine", PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.fineSettingsProfileName)),
    CUSTOM("Custom", PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.customSettingsProfileName));
    
    private final String friendlyName;
    private final SlicerSettings settings;

    private PrintQualityEnumeration(String friendlyName, SlicerSettings settings)
    {
        this.friendlyName = friendlyName;
        this.settings = settings;
    }
    
    public String getFriendlyName()
    {
        return friendlyName;
    }
    
    public SlicerSettings getSettings()
    {
        return settings;
    }
    
    @Override
    public String toString()
    {
        return friendlyName;
    }
}
