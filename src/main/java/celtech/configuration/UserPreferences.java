package celtech.configuration;

import celtech.Lookup;
import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.configuration.fileRepresentation.UserPreferenceFile;
import celtech.configuration.units.CurrencySymbol;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
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
    private final ObjectProperty<CurrencySymbol> currencySymbol = new SimpleObjectProperty<>(CurrencySymbol.POUND);
    private final FloatProperty currencyGBPToLocalMultiplier = new SimpleFloatProperty(1);

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
        this.languageTag = userPreferenceFile.getLanguageTag();
        this.loggingLevel = userPreferenceFile.getLoggingLevel();
        this.advancedMode.set(userPreferenceFile.isAdvancedMode());
        this.firstUse.set(userPreferenceFile.isFirstUse());
        this.detectLoadedFilament.set(userPreferenceFile.isDetectLoadedFilament());
        this.currencySymbol.set(userPreferenceFile.getCurrencySymbol());
        this.currencyGBPToLocalMultiplier.set(userPreferenceFile.getCurrencyGBPToLocalMultiplier());

        safetyFeaturesOn.addListener(booleanChangeListener);
        advancedMode.addListener(advancedModeChangeListener);
        firstUse.addListener(booleanChangeListener);
        detectLoadedFilament.addListener(booleanChangeListener);
        currencyGBPToLocalMultiplier.addListener(numberChangeListener);
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
}
