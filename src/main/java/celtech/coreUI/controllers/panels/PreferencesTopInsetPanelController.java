/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.UserPreferences;
import celtech.coreUI.components.VerticalMenu;
import celtech.coreUI.controllers.panels.userpreferences.LanguagePreference;
import celtech.coreUI.controllers.panels.userpreferences.LogLevelPreference;
import celtech.coreUI.controllers.panels.userpreferences.SlicerTypePreference;
import celtech.coreUI.controllers.panels.userpreferences.TickBoxPreference;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

/**
 *
 * @author tony
 */
public class PreferencesTopInsetPanelController implements Initializable
{

    private static final int ROW_HEIGHT = 60;

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

        Preference safetyFeaturesOnPref = new TickBoxPreference(userPreferences.
            safetyFeaturesOnProperty(), "preferences.safetyFeaturesOn");

        preferences.add(slicerTypePref);
        preferences.add(safetyFeaturesOnPref);

        return preferences;
    }

    private List<Preference> createEnvironmentPreferences()
    {
        List<Preference> preferences = new ArrayList<>();

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
//        preferences.add(showTooltipsPref);
        preferences.add(logLevelPref);
        preferences.add(advancedModePref);

        return preferences;
    }

    private void addPreferenceToContainer(Preference preference, int rowNo)
    {
        Label description = getPreferenceDescriptionLabel(preference);
        Control editor = getPreferenceEditorControl(preference);
        preferencesGridPane.addRow(rowNo, description, editor);

        if (preferencesGridPane.getRowConstraints().size() < rowNo)
        {
            RowConstraints rowConstraints = preferencesGridPane.getRowConstraints().get(rowNo);
            rowConstraints.setPrefHeight(ROW_HEIGHT);
            rowConstraints.setMinHeight(ROW_HEIGHT);
            rowConstraints.setMaxHeight(ROW_HEIGHT);
        } else
        {
            preferencesGridPane.getRowConstraints().add(rowNo, new RowConstraints(ROW_HEIGHT, ROW_HEIGHT,
                                                                           ROW_HEIGHT));
        }
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
