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
    private boolean showDiagnostics = false;
    private boolean showGCode = true;
    private boolean showAdjustments = true;
    private boolean showMetricUnits = true;
    private boolean timelapseTriggerEnabled = false;
    private String goProWifiPassword = "";
    private boolean timelapseMoveBeforeCapture = true;
    private int timelapseXMove = 0;
    private int timelapseYMove = 150;
    private int timelapseDelay = 2;
    private int timelapseDelayBeforeCapture = 2;
    private boolean loosePartSplitOnLoad = true;

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

    public boolean isTimelapseTriggerEnabled()
    {
        return timelapseTriggerEnabled;
    }

    public void setTimelapseTriggerEnabled(boolean timelapseTriggerEnabled)
    {
        this.timelapseTriggerEnabled = timelapseTriggerEnabled;
    }

    public String getGoProWifiPassword()
    {
        return goProWifiPassword;
    }

    public void setGoProWifiPassword(String goProWifiPassword)
    {
        this.goProWifiPassword = goProWifiPassword;
    }

    public boolean isTimelapseMoveBeforeCapture()
    {
        return timelapseMoveBeforeCapture;
    }

    public void setTimelapseMoveBeforeCapture(boolean timelapseMoveBeforeCapture)
    {
        this.timelapseMoveBeforeCapture = timelapseMoveBeforeCapture;
    }

    public void setTimelapseXMove(int timelapseXMove)
    {
        this.timelapseXMove = timelapseXMove;
    }

    public int getTimelapseXMove()
    {
        return timelapseXMove;
    }

    public void setTimelapseYMove(int timelapseYMove)
    {
        this.timelapseYMove = timelapseYMove;
    }

    public int getTimelapseYMove()
    {
        return timelapseYMove;
    }

    public void setTimelapseDelay(int timelapseDelay)
    {
        this.timelapseDelay = timelapseDelay;
    }

    public int getTimelapseDelay()
    {
        return timelapseDelay;
    }

    public void setTimelapseDelayBeforeCapture(int timelapseDelayBeforeCapture)
    {
        this.timelapseDelayBeforeCapture = timelapseDelayBeforeCapture;
    }

    public int getTimelapseDelayBeforeCapture()
    {
        return timelapseDelayBeforeCapture;
    }

    public boolean isLoosePartSplitOnLoad()
    {
        return loosePartSplitOnLoad;
    }

    public void setLoosePartSplitOnLoad(boolean loosePartSplitOnLoad)
    {
        this.loosePartSplitOnLoad = loosePartSplitOnLoad;
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
        setTimelapseTriggerEnabled(userPreferences.isTimelapseTriggerEnabled());
        setGoProWifiPassword(userPreferences.getGoProWifiPassword());
        setTimelapseMoveBeforeCapture(userPreferences.isTimelapseMoveBeforeCapture());
        setTimelapseXMove(userPreferences.getTimelapseXMove());
        setTimelapseYMove(userPreferences.getTimelapseYMove());
        setTimelapseDelay(userPreferences.getTimelapseDelay());
        setTimelapseDelayBeforeCapture(userPreferences.getTimelapseDelayBeforeCapture());
        setLoosePartSplitOnLoad(userPreferences.isLoosePartSplitOnLoad());
    }
}
