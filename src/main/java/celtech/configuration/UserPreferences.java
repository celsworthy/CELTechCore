package celtech.configuration;

import celtech.Lookup;
import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.configuration.fileRepresentation.UserPreferenceFile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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

    private SlicerType slicerType = SlicerType.Cura;
    private final BooleanProperty safetyFeaturesOn = new SimpleBooleanProperty(true);
    private String languageTag = "";
    private final BooleanProperty showTooltips = new SimpleBooleanProperty(true);
    private LogLevel loggingLevel = LogLevel.INFO;
    private final BooleanProperty advancedMode = new SimpleBooleanProperty(false);
    private final BooleanProperty firstUse = new SimpleBooleanProperty(true);
    private final ChangeListener<Boolean> booleanChangeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
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
        safetyFeaturesOn.set(userPreferenceFile.isSafetyFeaturesOn());
        this.languageTag = userPreferenceFile.getLanguageTag();
        this.loggingLevel = userPreferenceFile.getLoggingLevel();
        this.advancedMode.set(userPreferenceFile.isAdvancedMode());
        this.firstUse.set(userPreferenceFile.isFirstUse());

        safetyFeaturesOn.addListener(booleanChangeListener);
        advancedMode.addListener(advancedModeChangeListener);
        firstUse.addListener(booleanChangeListener);
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

    private void saveSettings()
    {
        UserPreferenceContainer.savePreferences(this);
    }

    private void confirmAdvancedModeChange(boolean advancedMode)
    {
        suppressAdvancedModeListenerCheck = true;

        if (advancedMode && !this.advancedMode.get())
        {
            // Ask the user whether they really want to do this..
            boolean goToAdvancedMode = Lookup.getSystemNotificationHandler().confirmAdvancedMode();
            if (!goToAdvancedMode)
            {
                this.advancedMode.set(true);
            }
            else
            {
                this.advancedMode.set(false);
            }
        } else
        {
            this.advancedMode.set(advancedMode);
        }
        
        suppressAdvancedModeListenerCheck = false;
    }
}
