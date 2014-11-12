package celtech.configuration;

import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.configuration.fileRepresentation.UserPreferenceFile;

/**
 *
 * @author Ian
 */
public class UserPreferences
{
    private SlicerType slicerType = SlicerType.Cura;
    private boolean overrideSafeties = false;
    private String languageTag = "";

    public String getLanguageTag()
    {
        return languageTag;
    }

    public void setLanguageTag(String language)
    {
        this.languageTag = language;
    }

    public UserPreferences(UserPreferenceFile userPreferenceFile)
    {
        this.slicerType = userPreferenceFile.getSlicerType();
        this.overrideSafeties = userPreferenceFile.isOverrideSafeties();
    }

    public SlicerType getSlicerType()
    {
        return slicerType;
    }

    public void setSlicerType(SlicerType slicerType)
    {
        this.slicerType = slicerType;
        saveSettings();
    }

    public boolean isOverrideSafeties()
    {
        return overrideSafeties;
    }

    public void setOverrideSafeties(boolean overrideSafeties)
    {
        this.overrideSafeties = overrideSafeties;
        saveSettings();
    }

    private void saveSettings()
    {
        UserPreferenceContainer.savePreferences(this);
    }
}
