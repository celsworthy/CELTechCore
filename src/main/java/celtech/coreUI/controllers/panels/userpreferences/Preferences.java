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

        Preference currencySymbolPref = new CurrencySymbolPreference(userPreferences);
        Preference currencyGBPToLocalMultiplierPref = new FloatingPointPreference(userPreferences.currencyGBPToLocalMultiplierProperty(),
                2, 7, false, "preferences.currencyGBPToLocalMultiplier");

        Preference loosePartSplitPref = new TickBoxPreference(userPreferences.loosePartSplitOnLoadProperty(),
                "preferences.loosePartSplit");

        preferences.add(firstUsePref);
        preferences.add(languagePref);
        preferences.add(logLevelPref);
        preferences.add(advancedModePref);
        preferences.add(currencySymbolPref);
        preferences.add(currencyGBPToLocalMultiplierPref);
        preferences.add(loosePartSplitPref);

        return preferences;
    }

    public static List<PreferencesInnerPanelController.Preference> createInterfacePreferences(
            UserPreferences userPreferences)
    {
        List<PreferencesInnerPanelController.Preference> preferences = new ArrayList<>();

        Preference showDiagnosticsPref = new TickBoxPreference(userPreferences.showDiagnosticsProperty(),
                "preferences.showDiagnostics");

        Preference showGCodePref = new TickBoxPreference(userPreferences.showGCodeProperty(),
                "preferences.showGCode");
        Preference showAdjustmentsPref = new TickBoxPreference(userPreferences.showAdjustmentsProperty(),
                "preferences.showAdjustments");

        preferences.add(showDiagnosticsPref);
        preferences.add(showGCodePref);
        preferences.add(showAdjustmentsPref);

        return preferences;
    }

    public static List<PreferencesInnerPanelController.Preference> createTimelapsePreferences(
            UserPreferences userPreferences)
    {
        List<PreferencesInnerPanelController.Preference> preferences = new ArrayList<>();

        Preference timelapseTriggerEnabledPref = new TickBoxPreference(userPreferences.getTimelapseTriggerEnabledProperty(),
                "preferences.timelapseTriggerEnabled");

        Preference goProWifiPasswordPref = new PasswordPreference(userPreferences.getGoProWifiProperty(),
                "preferences.goProWifiPassword");

        Preference timelapseXMovePref = new IntegerPreference(userPreferences.getTimelapseXMoveProperty(),
                "preferences.timelapseXMove");

        Preference timelapseYMovePref = new IntegerPreference(userPreferences.getTimelapseYMoveProperty(),
                "preferences.timelapseYMove");

        Preference timelapseDelayPref = new IntegerPreference(userPreferences.getTimelapseDelayProperty(),
                "preferences.timelapseDelay");

        Preference timelapseDelayBeforeCapturePref = new IntegerPreference(userPreferences.getTimelapseDelayBeforeCaptureProperty(),
                "preferences.timelapseDelayBeforeCapture");

        preferences.add(timelapseTriggerEnabledPref);
        preferences.add(goProWifiPasswordPref);
        preferences.add(timelapseXMovePref);
        preferences.add(timelapseYMovePref);
        preferences.add(timelapseDelayPref);
        preferences.add(timelapseDelayBeforeCapturePref);

        return preferences;
    }
}
