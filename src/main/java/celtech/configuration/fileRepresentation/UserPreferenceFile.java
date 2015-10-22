package celtech.configuration.fileRepresentation;

import celtech.configuration.SlicerType;
import celtech.configuration.UserPreferences;
import celtech.configuration.units.CurrencySymbol;
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
    private LogLevel loggingLevel = LogLevel.INFO;
    private boolean advancedMode = false;
    private boolean firstUse = true;
    private boolean detectLoadedFilament = true;
    private CurrencySymbol currencySymbol = CurrencySymbol.POUND;
    private float currencyGBPToLocalMultiplier = 1;
    private boolean showDiagnostics = true;
    private boolean showGCode = true;
    private boolean showAdjustments = true;
    private boolean showMetricUnits = true;

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

    public boolean isDetectLoadedFilament()
    {
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

    public CurrencySymbol getCurrencySymbol()
    {
        return currencySymbol;
    }

    public void setCurrencySymbol(CurrencySymbol currencySymbol)
    {
        this.currencySymbol = currencySymbol;
    }

    public float getCurrencyGBPToLocalMultiplier()
    {
        return currencyGBPToLocalMultiplier;
    }

    public void setCurrencyGBPToLocalMultiplier(float currencyGBPToLocalMultiplier)
    {
        this.currencyGBPToLocalMultiplier = currencyGBPToLocalMultiplier;
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
        setCurrencySymbol(userPreferences.getCurrencySymbol());
        setCurrencyGBPToLocalMultiplier(userPreferences.getcurrencyGBPToLocalMultiplier());
        setShowDiagnostics(userPreferences.getShowDiagnostics());
        setShowGCode(userPreferences.getShowGCode());
        setShowAdjustments(userPreferences.getShowAdjustments());
        setShowMetricUnits(userPreferences.isShowMetricUnits());
    }

    public boolean isShowDiagnostics()
    {
        return showDiagnostics;
    }

    public boolean isShowGCode()
    {
        return showGCode;
    }

    public boolean isShowAdjustments()
    {
        return showAdjustments;
    }

    public void setShowDiagnostics(boolean showDiagnostics)
    {
        this.showDiagnostics = showDiagnostics;
    }

    public void setShowGCode(boolean showGCode)
    {
        this.showGCode = showGCode;
    }

    public void setShowAdjustments(boolean showAdjustments)
    {
        this.showAdjustments = showAdjustments;
    }

    public boolean isShowMetricUnits()
    {
        return showMetricUnits;
    }

    public void setShowMetricUnits(boolean showMetricUnits)
    {
        this.showMetricUnits = showMetricUnits;
    }
}
