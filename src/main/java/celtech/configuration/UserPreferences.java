package celtech.configuration;

import celtech.Lookup;
import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.configuration.fileRepresentation.UserPreferenceFile;
import celtech.configuration.units.CurrencySymbol;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import libertysystems.stenographer.LogLevel;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class UserPreferences
{

    private final ObjectProperty<SlicerType> slicerType = new SimpleObjectProperty<>(SlicerType.Cura);
    private final BooleanProperty safetyFeaturesOn = new SimpleBooleanProperty(true);
    private String languageTag = "";
    private final BooleanProperty showTooltips = new SimpleBooleanProperty(true);
    private LogLevel loggingLevel = LogLevel.INFO;
    private final BooleanProperty advancedMode = new SimpleBooleanProperty(false);
    private final BooleanProperty firstUse = new SimpleBooleanProperty(true);
    private final BooleanProperty detectLoadedFilament = new SimpleBooleanProperty(true);
    private final BooleanProperty showDiagnostics = new SimpleBooleanProperty(true);
    private final BooleanProperty showGCode = new SimpleBooleanProperty(true);
    private final BooleanProperty showAdjustments = new SimpleBooleanProperty(true);
    private final ObjectProperty<CurrencySymbol> currencySymbol = new SimpleObjectProperty<>(CurrencySymbol.POUND);
    private final FloatProperty currencyGBPToLocalMultiplier = new SimpleFloatProperty(1);
    private final BooleanProperty showMetricUnits = new SimpleBooleanProperty(true);
    private final BooleanProperty goProTriggerEnabled = new SimpleBooleanProperty(false);
    private final StringProperty goProWifiPassword = new SimpleStringProperty("");
    private final StringProperty goProXMove = new SimpleStringProperty("");
    private final StringProperty goProYMove = new SimpleStringProperty("");
    private final IntegerProperty goProDelay = new SimpleIntegerProperty(0);
    private final IntegerProperty goProDelayBeforeCapture = new SimpleIntegerProperty(0);

    private final ChangeListener<String> stringChangeListener = (ObservableValue<? extends String> observable, String oldValue, String newValue) ->
    {
        saveSettings();
    };
    private final ChangeListener<Boolean> booleanChangeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        saveSettings();
    };
    private final ChangeListener<Number> numberChangeListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        saveSettings();
    };
    private boolean suppressAdvancedModeListenerCheck = false;
    private final ChangeListener<Boolean> advancedModeChangeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        if (!suppressAdvancedModeListenerCheck)
        {
            confirmAdvancedModeChange(newValue);
        }
        saveSettings();
    };

    public UserPreferences(UserPreferenceFile userPreferenceFile)
    {
        this.slicerType.set(userPreferenceFile.getSlicerType());
        safetyFeaturesOn.set(userPreferenceFile.isSafetyFeaturesOn());
        languageTag = userPreferenceFile.getLanguageTag();
        loggingLevel = userPreferenceFile.getLoggingLevel();
        advancedMode.set(userPreferenceFile.isAdvancedMode());
        firstUse.set(userPreferenceFile.isFirstUse());
        detectLoadedFilament.set(userPreferenceFile.isDetectLoadedFilament());
        showDiagnostics.set(userPreferenceFile.isShowDiagnostics());
        showGCode.set(userPreferenceFile.isShowGCode());
        showAdjustments.set(userPreferenceFile.isShowAdjustments());
        this.currencySymbol.set(userPreferenceFile.getCurrencySymbol());
        this.currencyGBPToLocalMultiplier.set(userPreferenceFile.getCurrencyGBPToLocalMultiplier());
        this.showMetricUnits.set(userPreferenceFile.isShowMetricUnits());
        this.goProTriggerEnabled.set(userPreferenceFile.isGoProTriggerEnabled());
        this.goProWifiPassword.set(userPreferenceFile.getGoProWifiPassword());
        this.goProXMove.set(userPreferenceFile.getGoProXMove());
        this.goProYMove.set(userPreferenceFile.getGoProYMove());
        this.goProDelay.set(userPreferenceFile.getGoProDelay());
        this.goProDelayBeforeCapture.set(userPreferenceFile.getGoProDelayBeforeCapture());

        safetyFeaturesOn.addListener(booleanChangeListener);
        advancedMode.addListener(advancedModeChangeListener);
        firstUse.addListener(booleanChangeListener);
        detectLoadedFilament.addListener(booleanChangeListener);
        showDiagnostics.addListener(booleanChangeListener);
        showGCode.addListener(booleanChangeListener);
        showAdjustments.addListener(booleanChangeListener);
        currencyGBPToLocalMultiplier.addListener(numberChangeListener);
        showMetricUnits.addListener(booleanChangeListener);
        goProTriggerEnabled.addListener(booleanChangeListener);
        goProWifiPassword.addListener(stringChangeListener);
        goProXMove.addListener(stringChangeListener);
        goProYMove.addListener(stringChangeListener);
        goProDelay.addListener(numberChangeListener);
        goProDelayBeforeCapture.addListener(numberChangeListener);
    }

    public String getLanguageTag()
    {
        return languageTag;
    }

    public void setLanguageTag(String language)
    {
        this.languageTag = language;
        saveSettings();
    }

    public SlicerType getSlicerType()
    {
        return slicerType.get();
    }

    public ObjectProperty<SlicerType> getSlicerTypeProperty()
    {
        return slicerType;
    }

    public void setSlicerType(SlicerType slicerType)
    {
        this.slicerType.set(slicerType);
        saveSettings();
    }

    public boolean isSafetyFeaturesOn()
    {
        return safetyFeaturesOn.get();
    }

    public void setSafetyFeaturesOn(boolean value)
    {
        this.safetyFeaturesOn.set(value);
    }

    public BooleanProperty safetyFeaturesOnProperty()
    {
        return safetyFeaturesOn;
    }

    public boolean isShowTooltips()
    {
        return showTooltips.get();
    }

    public void setShowTooltips(boolean value)
    {
        this.showTooltips.set(value);
    }

    public BooleanProperty showTooltipsProperty()
    {
        return showTooltips;
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
        return advancedMode.get();
    }

    public void setAdvancedMode(boolean advancedMode)
    {
        this.advancedMode.set(advancedMode);
    }

    public BooleanProperty advancedModeProperty()
    {
        return advancedMode;
    }

    public boolean isFirstUse()
    {
        return firstUse.get();
    }

    public void setFirstUse(boolean firstUse)
    {
        this.firstUse.set(firstUse);
    }

    public BooleanProperty firstUseProperty()
    {
        return firstUse;
    }

    public boolean getDetectLoadedFilament()
    {
        return detectLoadedFilament.get();
    }

    public void setDetectLoadedFilament(boolean firstUse)
    {
        this.detectLoadedFilament.set(firstUse);
    }

    public BooleanProperty detectLoadedFilamentProperty()
    {
        return detectLoadedFilament;
    }

    public ObjectProperty<CurrencySymbol> currencySymbolProperty()
    {
        return currencySymbol;
    }

    public CurrencySymbol getCurrencySymbol()
    {
        return currencySymbol.get();
    }

    public void setCurrencySymbol(CurrencySymbol currencySymbol)
    {
        this.currencySymbol.set(currencySymbol);
    }

    public FloatProperty currencyGBPToLocalMultiplierProperty()
    {
        return currencyGBPToLocalMultiplier;
    }

    public float getcurrencyGBPToLocalMultiplier()
    {
        return currencyGBPToLocalMultiplier.get();
    }

    public void setcurrencyGBPToLocalMultiplier(float value)
    {
        this.currencyGBPToLocalMultiplier.set(value);
    }

    public BooleanProperty getGoProTriggerEnabledProperty()
    {
        return goProTriggerEnabled;
    }

    public void setGoProTriggerEnabled(boolean goProTriggerEnabled)
    {
        this.goProTriggerEnabled.set(goProTriggerEnabled);
    }

    public boolean isGoProTriggerEnabled()
    {
        return goProTriggerEnabled.get();
    }

    public StringProperty getGoProWifiProperty()
    {
        return goProWifiPassword;
    }

    public void setGoProWifiPassword(String goProWifiPassword)
    {
        this.goProWifiPassword.set(goProWifiPassword);
    }

    public String getGoProWifiPassword()
    {
        return goProWifiPassword.get();
    }

    private void saveSettings()
    {
        UserPreferenceContainer.savePreferences(this);
    }

    private void confirmAdvancedModeChange(boolean advancedMode)
    {
        suppressAdvancedModeListenerCheck = true;

        if (advancedMode)
        {
            // Ask the user whether they really want to do this..
            boolean goToAdvancedMode = Lookup.getSystemNotificationHandler().confirmAdvancedMode();
            this.advancedMode.set(goToAdvancedMode);
        } else
        {
            this.advancedMode.set(advancedMode);
        }

        suppressAdvancedModeListenerCheck = false;
    }

    public BooleanProperty showDiagnosticsProperty()
    {
        return showDiagnostics;
    }

    public boolean getShowDiagnostics()
    {
        return showDiagnostics.get();
    }

    public void setShowDiagnostics(boolean showDiagnostics)
    {
        this.showDiagnostics.set(showDiagnostics);
    }

    public BooleanProperty showGCodeProperty()
    {
        return showGCode;
    }

    public boolean getShowGCode()
    {
        return showDiagnostics.get();
    }

    public void setShowGCode(boolean showGCode)
    {
        this.showGCode.set(showGCode);
    }

    public BooleanProperty showAdjustmentsProperty()
    {
        return showAdjustments;
    }

    public boolean getShowAdjustments()
    {
        return showAdjustments.get();
    }

    public void setShowAdjustments(boolean showAdjustments)
    {
        this.showAdjustments.set(showAdjustments);
    }

    public void setShowMetricUnits(boolean value)
    {
        showMetricUnits.set(value);
    }

    public boolean isShowMetricUnits()
    {
        return showMetricUnits.get();
    }

    public BooleanProperty showMetricUnitsProperty()
    {
        return showMetricUnits;
    }

    public String getGoProXMove()
    {
        return goProXMove.get();
    }

    public void setGoProXMove(String value)
    {
        goProXMove.set(value);
    }

    public StringProperty getGoProXMoveProperty()
    {
        return goProXMove;
    }

    public String getGoProYMove()
    {
        return goProYMove.get();
    }

    public void setGoProYMove(String value)
    {
        goProYMove.set(value);
    }

    public StringProperty getGoProYMoveProperty()
    {
        return goProYMove;
    }

    public int getGoProDelay()
    {
        return goProDelay.get();
    }

    public void setGoProDelay(int value)
    {
        goProDelay.set(value);
    }

    public IntegerProperty getGoProDelayProperty()
    {
        return goProDelay;
    }

    public int getGoProDelayBeforeCapture()
    {
        return goProDelayBeforeCapture.get();
    }

    public void setGoProDelayBeforeCapture(int value)
    {
        goProDelayBeforeCapture.set(value);
    }

    public IntegerProperty getGoProDelayBeforeCaptureProperty()
    {
        return goProDelayBeforeCapture;
    }
}
