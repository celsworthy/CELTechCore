/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.SlicerType;
import celtech.configuration.UserPreferences;
import celtech.coreUI.components.VerticalMenu;
import celtech.coreUI.controllers.panels.userpreferences.LogLevelPreference;
import celtech.coreUI.controllers.panels.userpreferences.SafetyFeaturesOnPreference;
import celtech.coreUI.controllers.panels.userpreferences.ShowTooltipPreference;
import celtech.coreUI.controllers.panels.userpreferences.SlicerTypePreference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.util.Callback;

/**
 *
 * @author tony
 */
public class PreferencesTopInsetPanelController implements Initializable
{

    private static final int ROW_HEIGHT = 60;

    private static final String SYSTEM_DEFAULT = "System Default";

    public interface Preference
    {

        public void updateValueFromControl();

        public void populateControlWithCurrentValue();

        public Control getControl();

        public String getDescription();
    }

    @FXML
    void backwardPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
    }

    @FXML
    private GridPane preferencesGridPane;

    @FXML
    private VerticalMenu preferencesMenu;

    private UserPreferences userPreferences;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        userPreferences = Lookup.getUserPreferences();

        preferencesMenu.setTitle(Lookup.i18n("preferences.preferences"));

        preferencesMenu.addItem(Lookup.i18n("preferences.printing"), this::showPrintingPreferences,
                                null);
        preferencesMenu.addItem(Lookup.i18n("preferences.environment"),
                                this::showEnvironmentPreferences, null);
        preferencesMenu.selectFirstItem();

    }

    private Object showEnvironmentPreferences()
    {
        List<Preference> preferences = createEnvironmentPreferences();

        displayPreferences(preferences);

        return null;
    }

    private void displayPreferences(List<Preference> preferences)
    {
        preferencesGridPane.getChildren().clear();
        int rowNo = 0;
        for (Preference preference : preferences)
        {
            preference.populateControlWithCurrentValue();
            addPreferenceToContainer(preference, rowNo);
            rowNo++;
        }
    }

    private Object showPrintingPreferences()
    {
        List<Preference> preferences = createPrintingPreferences();

        displayPreferences(preferences);

        return null;
    }

    private List<Preference> createPrintingPreferences()
    {
        List<Preference> preferences = new ArrayList<>();

        Preference slicerTypePref = new SlicerTypePreference(userPreferences);

        Preference safetyFeaturesOnPref = new SafetyFeaturesOnPreference(userPreferences);

        preferences.add(slicerTypePref);
        preferences.add(safetyFeaturesOnPref);

        return preferences;
    }

    private List<Preference> createEnvironmentPreferences()
    {
        List<Preference> preferences = new ArrayList<>();

        Preference languagePref = new Preference()
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

        Preference showTooltipsPref = new ShowTooltipPreference(userPreferences);
        Preference logLevelPref = new LogLevelPreference(userPreferences);

        preferences.add(languagePref);
//        preferences.add(showTooltipsPref);
        preferences.add(logLevelPref);

        return preferences;
    }

    private void addPreferenceToContainer(Preference preference, int rowNo)
    {
        Label description = getPreferenceDescriptionLabel(preference);
        Control editor = getPreferenceEditorControl(preference);
        preferencesGridPane.addRow(rowNo, description, editor);
        RowConstraints rowConstraints = preferencesGridPane.getRowConstraints().get(rowNo);
        rowConstraints.setPrefHeight(ROW_HEIGHT);
        rowConstraints.setMinHeight(ROW_HEIGHT);
        rowConstraints.setMaxHeight(ROW_HEIGHT);
    }

    private Label getPreferenceDescriptionLabel(Preference preference)
    {
        Label descriptionLabel = new Label(preference.getDescription() + ":");
        descriptionLabel.getStyleClass().add("preferenceLabel");
        return descriptionLabel;
    }

    private Control getPreferenceEditorControl(Preference preference)
    {
        Control control = preference.getControl();
        control.getStyleClass().add("preferenceControl");
        return control;
    }

}
