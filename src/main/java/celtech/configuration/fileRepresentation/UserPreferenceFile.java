package celtech.configuration.fileRepresentation;

import celtech.configuration.SlicerType;
import celtech.configuration.UserPreferences;

/**
 *
 * @author Ian
 */
public class UserPreferenceFile
{
    private SlicerType slicerType = null;
    private boolean overrideSafeties = false;
    private String languageTag = "";
    private boolean showTooltips = false;

    public String getLanguageTag()
    {
        return languageTag;
    }

    public void setLanguageTag(String languageTag)
    {
        this.languageTag = languageTag;
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

    public boolean isShowTooltips()
    {
        return showTooltips;
    }

    public void setShowTooltips(boolean showTooltips)
    {
        this.showTooltips = showTooltips;
    }

    public void populateFromSettings(UserPreferences userPreferences)
    {
        setSlicerType(userPreferences.getSlicerType());
        setOverrideSafeties(userPreferences.isOverrideSafeties());
        setLanguageTag(userPreferences.getLanguageTag());
        setShowTooltips(userPreferences.isShowTooltips());
    }
}
