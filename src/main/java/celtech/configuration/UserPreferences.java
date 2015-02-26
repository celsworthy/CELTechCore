package celtech.configuration;

import celtech.configuration.datafileaccessors.UserPreferenceContainer;
import celtech.configuration.fileRepresentation.UserPreferenceFile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author Ian
 */
public class UserPreferences
{

    private SlicerType slicerType = SlicerType.Cura;
    private final BooleanProperty overrideSafeties = new SimpleBooleanProperty(false);
    private String languageTag = "";
    private final BooleanProperty showTooltips = new SimpleBooleanProperty(true);

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
        overrideSafeties.set(userPreferenceFile.isOverrideSafeties());
        this.languageTag = userPreferenceFile.getLanguageTag();
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

    public boolean isOverrideSafeties()
    {
        return overrideSafeties.get();
    }

    public void setOverrideSafeties(boolean overrideSafeties)
    {
        this.overrideSafeties.set(overrideSafeties);
        saveSettings();
    }

    public ReadOnlyBooleanProperty overrideSafetiesProperty()
    {
        return overrideSafeties;
    }

    public boolean isShowTooltips()
    {
        return showTooltips.get();
    }

    public void setShowTooltips(boolean overrideSafeties)
    {
        this.showTooltips.set(overrideSafeties);
        saveSettings();
    }

    public ReadOnlyBooleanProperty showTooltipsProperty()
    {
        return showTooltips;
    }

    private void saveSettings()
    {
        UserPreferenceContainer.savePreferences(this);
    }
}
