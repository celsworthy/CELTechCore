/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels.userpreferences;

import celtech.configuration.UserPreferences;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController.Preference;
import java.util.ArrayList;
import java.util.List;

/**
 * Preferences creates collections of the Preference class.
 *
 * @author tony
 */
public class Preferences
{

    public static List<PreferencesInnerPanelController.Preference> createPrintingPreferences(
        UserPreferences userPreferences)
    {
        List<PreferencesInnerPanelController.Preference> preferences = new ArrayList<>();

        Preference slicerTypePref = new SlicerTypePreference(userPreferences);

        Preference safetyFeaturesOnPref = new TickBoxPreference(userPreferences.
            safetyFeaturesOnProperty(), "preferences.safetyFeaturesOn");
        
        Preference detectFilamentLoadedPref = new TickBoxPreference(userPreferences.
            detectLoadedFilamentProperty(), "preferences.detectLoadedFilament");

        preferences.add(slicerTypePref);
        preferences.add(safetyFeaturesOnPref);
        preferences.add(detectFilamentLoadedPref);

        return preferences;
    }

    public static List<PreferencesInnerPanelController.Preference> createEnvironmentPreferences(
        UserPreferences userPreferences)
    {
        List<PreferencesInnerPanelController.Preference> preferences = new ArrayList<>();

        Preference languagePref = new LanguagePreference(userPreferences);
        Preference showTooltipsPref = new TickBoxPreference(userPreferences.showTooltipsProperty(),
                                                            "preferences.showTooltips");
        Preference logLevelPref = new LogLevelPreference(userPreferences);
        Preference advancedModePref = new TickBoxPreference(userPreferences.advancedModeProperty(),
                                                            "preferences.advancedMode");
        Preference firstUsePref = new TickBoxPreference(userPreferences.firstUseProperty(),
                                                        "preferences.firstUse");

        preferences.add(firstUsePref);
        preferences.add(languagePref);
        preferences.add(logLevelPref);
        preferences.add(advancedModePref);

        return preferences;
    }

}
