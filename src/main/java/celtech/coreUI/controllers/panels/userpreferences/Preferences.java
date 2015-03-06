/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels.userpreferences;

import celtech.Lookup;
import celtech.configuration.UserPreferences;
import celtech.coreUI.controllers.panels.PreferencesInnerPanelController;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;

/**
 * Preferences creates collections of the Preference class.
 * @author tony
 */
public class Preferences
{

    private static final String SYSTEM_DEFAULT = "System Default";

    public static List<PreferencesInnerPanelController.Preference> createPrintingPreferences(
        UserPreferences userPreferences)
    {
        List<PreferencesInnerPanelController.Preference> preferences = new ArrayList<>();

        PreferencesInnerPanelController.Preference slicerTypePref = new SlicerTypePreference(
            userPreferences);

        PreferencesInnerPanelController.Preference safetyFeaturesOnPref = new SafetyFeaturesOnPreference(
            userPreferences);

        PreferencesInnerPanelController.Preference showTooltipsPref = new ShowTooltipPreference(
            userPreferences);

        preferences.add(slicerTypePref);
        preferences.add(safetyFeaturesOnPref);
        preferences.add(showTooltipsPref);

        return preferences;
    }

    public static List<PreferencesInnerPanelController.Preference> createEnvironmentPreferences(
        UserPreferences userPreferences)
    {
        List<PreferencesInnerPanelController.Preference> preferences = new ArrayList<>();

        PreferencesInnerPanelController.Preference languagePref = new PreferencesInnerPanelController.Preference()
        {
            private final ComboBox<Object> control;

            
            {
                control = new ComboBox<>();

                setupCellFactory(control);

                List<Object> localesList = new ArrayList<>();
                localesList.add(SYSTEM_DEFAULT);
                localesList.addAll(Lookup.getLanguages().getLocales());
                localesList.sort((Object o1, Object o2) ->
                {
                    // Make "System Default" come at the top of the combo
                    if (o1 instanceof String)
                    {
                        return -1;
                    } else if (o2 instanceof String)
                    {
                        return 1;
                    }
                    // o1 and o2 are both Locales
                    return ((Locale) o1).getDisplayName().compareTo(((Locale) o2).getDisplayName());
                });
                control.setItems(FXCollections.observableArrayList(localesList));
                control.setPrefWidth(300);
                control.setMinWidth(control.getPrefWidth());
                control.valueProperty().addListener(
                    (ObservableValue<? extends Object> observable, Object oldValue, Object newValue) ->
                    {
                        updateValueFromControl();
                    });
            }

            @Override
            public void updateValueFromControl()
            {
                if (control.getValue() instanceof Locale)
                {
                    Locale localeToUse = ((Locale) control.getValue());
                    if (localeToUse.getVariant().length() > 0)
                    {
                        userPreferences.setLanguageTag(localeToUse.getLanguage() + "-"
                            + localeToUse.getCountry() + "-" + localeToUse.getVariant());
                    } else if (localeToUse.getCountry().length() > 0)
                    {
                        userPreferences.setLanguageTag(localeToUse.getLanguage() + "-"
                            + localeToUse.getCountry());
                    } else
                    {
                        userPreferences.setLanguageTag(localeToUse.getLanguage());
                    }
                } else
                {
                    userPreferences.setLanguageTag("");
                }
            }

            @Override
            public void populateControlWithCurrentValue()
            {
                Object preferredLocale;
                String userPrefLanguageTag = userPreferences.getLanguageTag();

                if (userPrefLanguageTag == null || userPrefLanguageTag.equals(""))
                {
                    preferredLocale = SYSTEM_DEFAULT;
                } else
                {
                    preferredLocale = Locale.forLanguageTag(userPrefLanguageTag);
                }
                control.setValue(preferredLocale);
            }

            @Override
            public Control getControl()
            {
                return control;
            }

            @Override
            public String getDescription()
            {
                return Lookup.i18n("preferences.language");
            }

            private void setupCellFactory(ComboBox<Object> control)
            {

                Callback<ListView<Object>, ListCell<Object>> cellFactory = new Callback<ListView<Object>, ListCell<Object>>()
                {
                    @Override
                    public ListCell<Object> call(ListView<Object> p)
                    {
                        return new ListCell<Object>()
                        {
                            @Override
                            protected void updateItem(Object item, boolean empty)
                            {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                {
                                    if (item instanceof Locale)
                                    {
                                        setText(((Locale) item).getDisplayName());
                                    } else
                                    {
                                        setText((String) item);
                                    }
                                }
                            }
                        };
                    }
                };

                control.setButtonCell(cellFactory.call(null));
                control.setCellFactory(cellFactory);
            }
        };

        preferences.add(languagePref);

        return preferences;
    }

}
