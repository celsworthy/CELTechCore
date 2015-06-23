package celtech.configuration.fileRepresentation;

import celtech.configuration.SlicerType;
import celtech.configuration.UserPreferences;
import libertysystems.stenographer.LogLevel;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 * @author Ian
 */
@JsonIgnoreProperties(ignoreUnknown = true)

public class UserPreferenceFile
{
    private SlicerType slicerType = null;
    private boolean safetyFeaturesOn = true;
    private String languageTag = "";
    private boolean showTooltips = false;
    private LogLevel loggingLevel = LogLevel.DEBUG;
    private boolean advancedMode = false;
    private boolean firstUse = true;
    private boolean detectLoadedFilament = true;

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

    public boolean isSafetyFeaturesOn()
    {
        return safetyFeaturesOn;
    }

    public void setSafetyFeaturesOn(boolean value)
    {
        this.safetyFeaturesOn = value;
    }

    public boolean isShowTooltips()
    {
        return showTooltips;
    }

    public void setShowTooltips(boolean showTooltips)
    {
        this.showTooltips = showTooltips;
    }

    public LogLevel getLoggingLevel()
    {
        return loggingLevel;
    }

    public void setLoggingLevel(LogLevel loggingLevel)
    {
        this.loggingLevel = loggingLevel;
    }

    public boolean isAdvancedMode()
    {
        return advancedMode;
    }

    public void setAdvancedMode(boolean advancedMode)
    {
        this.advancedMode = advancedMode;
    }

    public boolean isFirstUse()
    {
        return firstUse;
    }
    
    public boolean isDetectLoadedFilament() {
        return detectLoadedFilament;
    }

    public void setFirstUse(boolean value)
    {
        this.firstUse = value;
    }
    
    public void setDetectLoadedFilament(boolean value)
    {
        this.detectLoadedFilament = value;
    }    

    public void populateFromSettings(UserPreferences userPreferences)
    {
        setSlicerType(userPreferences.getSlicerType());
        setSafetyFeaturesOn(userPreferences.isSafetyFeaturesOn());
        setLanguageTag(userPreferences.getLanguageTag());
        setShowTooltips(userPreferences.isShowTooltips());
        setLoggingLevel(userPreferences.getLoggingLevel());
        setAdvancedMode(userPreferences.isAdvancedMode());
        setFirstUse(userPreferences.isFirstUse());
        setDetectLoadedFilament(userPreferences.getDetectLoadedFilament());
    }
}
