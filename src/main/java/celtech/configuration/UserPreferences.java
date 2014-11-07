package celtech.configuration;

import celtech.configuration.fileRepresentation.UserPreferenceFile;

/**
 *
 * @author Ian
 */
public class UserPreferences
{

    private SlicerType slicerType = SlicerType.Cura;
    private boolean overrideSafeties = false;
    private String language = "";

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
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
    }

    public boolean isOverrideSafeties()
    {
        return overrideSafeties;
    }

    public void setOverrideSafeties(boolean overrideSafeties)
    {
        this.overrideSafeties = overrideSafeties;
    }
}
