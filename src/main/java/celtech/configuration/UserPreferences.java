package celtech.configuration;

import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.configuration.fileRepresentation.UserPreferenceFile;
import libertysystems.stenographer.LogLevel;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class UserPreferences
{

    private SlicerType slicerType = SlicerType.Cura;
    private boolean safetyFeaturesOn = true;
    private String languageTag = "";
    private boolean showTooltips = false;
    private LogLevel loggingLevel = LogLevel.INFO;
    private boolean advancedMode = false;

    public String getLanguageTag()
    {
        return languageTag;
    }

    public void setLanguageTag(String language)
    {
        this.languageTag = language;
        saveSettings();
    }

    public UserPreferences(UserPreferenceFile userPreferenceFile)
    {
        this.slicerType = userPreferenceFile.getSlicerType();
        safetyFeaturesOn = userPreferenceFile.isSafetyFeaturesOn();
        this.languageTag = userPreferenceFile.getLanguageTag();
        this.loggingLevel = userPreferenceFile.getLoggingLevel();
        this.advancedMode = userPreferenceFile.isAdvancedMode();
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

    public boolean isSafetyFeaturesOn()
    {
        return safetyFeaturesOn;
    }

    public void setSafetyFeaturesOn(boolean value)
    {
        this.safetyFeaturesOn = value;
        saveSettings();
    }

    public boolean isShowTooltips()
    {
        return showTooltips;
    }

    public void setShowTooltips(boolean value)
    {
        this.showTooltips = value;
        saveSettings();
    }

    public LogLevel getLoggingLevel()
    {
        return loggingLevel;
    }

    public void setLoggingLevel(LogLevel loggingLevel)
    {
        StenographerFactory.changeAllLogLevels(loggingLevel);
        this.loggingLevel = loggingLevel;
        saveSettings();
    }

    public boolean isAdvancedMode()
    {
        return advancedMode;
    }

    public void setAdvancedMode(boolean advancedMode)
    {
        this.advancedMode = advancedMode;
        saveSettings();
    }

    private void saveSettings()
    {
        UserPreferenceContainer.savePreferences(this);
    }
}
