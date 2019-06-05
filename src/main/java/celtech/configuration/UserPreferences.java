package celtech.configuration;

import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.configuration.fileRepresentation.UserPreferenceFile;
import celtech.configuration.units.CurrencySymbol;
import celtech.roboxbase.ApplicationFeature;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.comms.RoboxCommsManager;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.hardwarevariants.PrinterType;
import celtech.roboxbase.licence.Licence;
import celtech.roboxbase.licence.LicenceType;
import celtech.roboxbase.licensing.LicenceManager;
import celtech.roboxbase.licensing.LicenceManager.LicenceChangeListener;
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
    private final BooleanProperty timelapseTriggerEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty timelapseTurnOffHeadLights = new SimpleBooleanProperty(true);
    private final BooleanProperty timelapseTurnOffLED = new SimpleBooleanProperty(false);
    private final StringProperty goProWifiPassword = new SimpleStringProperty("");
    private final BooleanProperty timelapseMoveBeforeCapture = new SimpleBooleanProperty(true);
    private final IntegerProperty timelapseXMove = new SimpleIntegerProperty(0);
    private final IntegerProperty timelapseYMove = new SimpleIntegerProperty(0);
    private final IntegerProperty timelapseDelay = new SimpleIntegerProperty(0);
    private final IntegerProperty timelapseDelayBeforeCapture = new SimpleIntegerProperty(0);
    private final BooleanProperty loosePartSplitOnLoad = new SimpleBooleanProperty(false);
    private final BooleanProperty autoGCodePreview = new SimpleBooleanProperty(true);
    private final BooleanProperty customPrinterEnabled = new SimpleBooleanProperty(false);
    private final ObjectProperty<PrinterType> customPrinterType = new SimpleObjectProperty<>(PrinterType.ROBOX);
    private final StringProperty customPrinterHead = new SimpleStringProperty(HeadContainer.defaultHeadID);
    
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
    
    private final ChangeListener<Boolean> enableCustomPrinterChangeListener = (observable, oldValue, newValue) -> {
        if(newValue) 
        {
            if(BaseConfiguration.isApplicationFeatureEnabled(ApplicationFeature.OFFLINE_PRINTER)) 
            {
                RoboxCommsManager.getInstance().addDummyPrinter(true);
            } else 
            {
                BaseLookup.getSystemNotificationHandler().showPurchaseLicenseDialog();
                customPrinterEnabled.set(false);
            }
        } else
        {
            RoboxCommsManager.getInstance().removeAllDummyPrinters();
        }
        saveSettings();
    };

    private final ChangeListener<Boolean> enableAutoGCodePreviewChangeListener = (observable, oldValue, newValue) -> {
        if (newValue && !BaseConfiguration.isApplicationFeatureEnabled(ApplicationFeature.GCODE_VISUALISATION)) {
            BaseLookup.getSystemNotificationHandler().showPurchaseLicenseDialog();
            autoGCodePreview.set(false);
        }
        saveSettings();
    };
    
    private final LicenceChangeListener autoGCodePreviewLicenceChangeListener = licenceOption -> {
        if (licenceOption.map(Licence::getLicenceType).orElse(LicenceType.AUTOMAKER_FREE) == LicenceType.AUTOMAKER_FREE) 
        {
            autoGCodePreview.set(false);
        }
        saveSettings();
    };

    public UserPreferences(UserPreferenceFile userPreferenceFile)
    {
        this.slicerType.set(userPreferenceFile.getSlicerType());
        safetyFeaturesOn.set(userPreferenceFile.isSafetyFeaturesOn());
        languageTag = userPreferenceFile.getLanguageTag();
        showTooltips.set(userPreferenceFile.isShowTooltips());
        loggingLevel = userPreferenceFile.getLoggingLevel();
        advancedMode.set(userPreferenceFile.isAdvancedMode());
        firstUse.set(userPreferenceFile.isFirstUse());
        detectLoadedFilament.set(userPreferenceFile.isDetectLoadedFilament());
        showDiagnostics.set(userPreferenceFile.isShowDiagnostics());
        showGCode.set(userPreferenceFile.isShowGCode());
        showAdjustments.set(userPreferenceFile.isShowAdjustments());
        autoGCodePreview.set(userPreferenceFile.isAutoGCodePreview());
        currencySymbol.set(userPreferenceFile.getCurrencySymbol());
        currencyGBPToLocalMultiplier.set(userPreferenceFile.getCurrencyGBPToLocalMultiplier());
        showMetricUnits.set(userPreferenceFile.isShowMetricUnits());
        timelapseTriggerEnabled.set(userPreferenceFile.isTimelapseTriggerEnabled());
        timelapseTurnOffHeadLights.set(userPreferenceFile.isTimelapseTurnOffHeadLights());
        timelapseTurnOffLED.set(userPreferenceFile.isTimelapseTurnOffLED());
        goProWifiPassword.set(userPreferenceFile.getGoProWifiPassword());
        timelapseMoveBeforeCapture.set(userPreferenceFile.isTimelapseMoveBeforeCapture());
        timelapseXMove.set(userPreferenceFile.getTimelapseXMove());
        timelapseYMove.set(userPreferenceFile.getTimelapseYMove());
        timelapseDelay.set(userPreferenceFile.getTimelapseDelay());
        timelapseDelayBeforeCapture.set(userPreferenceFile.getTimelapseDelayBeforeCapture());
        loosePartSplitOnLoad.set(userPreferenceFile.isLoosePartSplitOnLoad());
        customPrinterEnabled.set(userPreferenceFile.isCustomPrinterEnabled());
        customPrinterType.set(userPreferenceFile.getCustomPrinterType());
        customPrinterHead.set(userPreferenceFile.getCustomPrinterHead());

        safetyFeaturesOn.addListener(booleanChangeListener);
        showTooltips.addListener(booleanChangeListener);
        advancedMode.addListener(advancedModeChangeListener);
        firstUse.addListener(booleanChangeListener);
        detectLoadedFilament.addListener(booleanChangeListener);
        showDiagnostics.addListener(booleanChangeListener);
        showGCode.addListener(booleanChangeListener);
        showAdjustments.addListener(booleanChangeListener);
        autoGCodePreview.addListener(enableAutoGCodePreviewChangeListener);
        currencyGBPToLocalMultiplier.addListener(numberChangeListener);
        showMetricUnits.addListener(booleanChangeListener);
        timelapseTriggerEnabled.addListener(booleanChangeListener);
        timelapseTurnOffHeadLights.addListener(booleanChangeListener);
        timelapseTurnOffLED.addListener(booleanChangeListener);
        goProWifiPassword.addListener(stringChangeListener);
        timelapseMoveBeforeCapture.addListener(booleanChangeListener);
        timelapseXMove.addListener(numberChangeListener);
        timelapseYMove.addListener(numberChangeListener);
        timelapseDelay.addListener(numberChangeListener);
        timelapseDelayBeforeCapture.addListener(numberChangeListener);
        loosePartSplitOnLoad.addListener(booleanChangeListener);
        customPrinterEnabled.addListener(enableCustomPrinterChangeListener);
        
        LicenceManager.addLicenceChangeListener(autoGCodePreviewLicenceChangeListener);
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

    public BooleanProperty getTimelapseTriggerEnabledProperty()
    {
        return timelapseTriggerEnabled;
    }

    public void setTimelapseTriggerEnabled(boolean timelapseTriggerEnabled)
    {
        this.timelapseTriggerEnabled.set(timelapseTriggerEnabled);
    }

    public boolean isTimelapseTriggerEnabled()
    {
        return timelapseTriggerEnabled.get();
    }
    
    public BooleanProperty getTimelapseTurnOffHeadLightsProperty()
    {
        return timelapseTurnOffHeadLights;
    }

    public void setTimelapseTurnOffHeadLightsEnabled(boolean timelapseTurnOffHeadLights)
    {
        this.timelapseTurnOffHeadLights.set(timelapseTurnOffHeadLights);
    }

    public boolean isTimelapseTurnOffHeadLights()
    {
        return timelapseTurnOffHeadLights.get();
    }
    
    public BooleanProperty getTimelapseTurnOffLEDProperty()
    {
        return timelapseTurnOffLED;
    }

    public void setTimelapseTurnOffLED(boolean timelapseTurnOffLED)
    {
        this.timelapseTurnOffLED.set(timelapseTurnOffLED);
    }

    public boolean isTimelapseTurnOffLED()
    {
        return timelapseTurnOffLED.get();
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
            boolean goToAdvancedMode = BaseLookup.getSystemNotificationHandler().confirmAdvancedMode();
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
        return showGCode.get();
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

    public BooleanProperty getTimelapseMoveBeforeCaptureProperty()
    {
        return timelapseMoveBeforeCapture;
    }
    
    public boolean isTimelapseMoveBeforeCapture()
    {
        return timelapseMoveBeforeCapture.get();
    }

    public void setTimelapseMoveBeforeCapture(boolean value)
    {
        timelapseMoveBeforeCapture.set(value);
    }
    
    public int getTimelapseXMove()
    {
        return timelapseXMove.get();
    }

    public void setTimelapseXMove(int value)
    {
        timelapseXMove.set(value);
    }

    public IntegerProperty getTimelapseXMoveProperty()
    {
        return timelapseXMove;
    }

    public int getTimelapseYMove()
    {
        return timelapseYMove.get();
    }

    public void setTimelapseYMove(int value)
    {
        timelapseYMove.set(value);
    }

    public IntegerProperty getTimelapseYMoveProperty()
    {
        return timelapseYMove;
    }

    public int getTimelapseDelay()
    {
        return timelapseDelay.get();
    }

    public void setTimelapseDelay(int value)
    {
        timelapseDelay.set(value);
    }

    public IntegerProperty getTimelapseDelayProperty()
    {
        return timelapseDelay;
    }

    public int getTimelapseDelayBeforeCapture()
    {
        return timelapseDelayBeforeCapture.get();
    }

    public void setTimelapseDelayBeforeCapture(int value)
    {
        timelapseDelayBeforeCapture.set(value);
    }

    public IntegerProperty getTimelapseDelayBeforeCaptureProperty()
    {
        return timelapseDelayBeforeCapture;
    }

    public boolean isLoosePartSplitOnLoad()
    {
        return loosePartSplitOnLoad.get();
    }

    public void setLoosePartSplitOnLoad(boolean value)
    {
        loosePartSplitOnLoad.set(value);
    }

    public BooleanProperty loosePartSplitOnLoadProperty()
    {
        return loosePartSplitOnLoad;
    }
    
    public BooleanProperty autoGCodePreviewProperty()
    {
        return autoGCodePreview;
    }

    public boolean isAutoGCodePreview()
    {
        return autoGCodePreview.get();
    }

    public void setAutoGCodePreview(boolean autoGCodePreview)
    {
        this.autoGCodePreview.set(autoGCodePreview);
    }

    public boolean isCustomPrinterEnabled()
    {
        return customPrinterEnabled.get();
    }
    
    public void setCustomPrinterEnabled(boolean value) 
    {
        customPrinterEnabled.set(value);
    }
    
    public BooleanProperty customPrinterEnabledProperty() 
    {
        return customPrinterEnabled;
    }
    
    public ObjectProperty<PrinterType> customPrinterTypeProperty()
    {
        return customPrinterType;
    }
    
    public PrinterType getCustomPrinterType() 
    {
        return customPrinterType.get();
    }
    
    public void setCustomPrinterType(PrinterType customPrinterType) 
    {
        this.customPrinterType.set(customPrinterType);
    }
    
    public StringProperty customPrinterHeadProperty()
    {
        return customPrinterHead;
    }
    
    public String getCustomPrinterHead() 
    {
        return customPrinterHead.get();
    }
    
    public void setCustomPrinterHead(String customPrinterHead) 
    {
        this.customPrinterHead.set(customPrinterHead);
    }
}
