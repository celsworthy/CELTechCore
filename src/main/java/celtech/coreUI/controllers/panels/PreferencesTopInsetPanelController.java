/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.SlicerType;
import celtech.configuration.UserPreferences;
import celtech.coreUI.components.VerticalMenu;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
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

        preferencesMenu.addItem(Lookup.i18n("preferences.printing"), this::showPrintingPreferences);
        preferencesMenu.addItem(Lookup.i18n("preferences.environment"),
                                this::showEnvironmentPreferences);
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

        Preference slicerTypePref = new Preference()
        {
            private final ComboBox<SlicerType> control;

            
            {
                control = new ComboBox<>();
                control.setItems(FXCollections.observableArrayList(SlicerType.values()));
                control.setPrefWidth(150);
                control.setMinWidth(control.getPrefWidth());
                control.valueProperty().addListener(
                    (ObservableValue<? extends SlicerType> observable, SlicerType oldValue, SlicerType newValue) ->
                    {
                        updateValueFromControl();
                    });
            }

            @Override
            public void updateValueFromControl()
            {
                SlicerType slicerType = control.getValue();
                userPreferences.setSlicerType(slicerType);
            }

            @Override
            public void populateControlWithCurrentValue()
            {
                control.setValue(userPreferences.getSlicerType());
            }

            @Override
            public Control getControl()
            {
                return control;
            }

            @Override
            public String getDescription()
            {
                return Lookup.i18n("preferences.slicerType");
            }
        };

        Preference overrideSafetyPref = new Preference()
        {
            private final CheckBox control;

            
            {
                control = new CheckBox();
                control.setPrefWidth(150);
                control.setMinWidth(control.getPrefWidth());
                control.selectedProperty().addListener(
                    (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
                    {
                        updateValueFromControl();
                    });
            }

            @Override
            public void updateValueFromControl()
            {
                boolean overrideSafety = control.isSelected();
                userPreferences.setOverrideSafeties(overrideSafety);
            }

            @Override
            public void populateControlWithCurrentValue()
            {
                control.setSelected(userPreferences.isOverrideSafeties());
            }

            @Override
            public Control getControl()
            {
                return control;
            }

            @Override
            public String getDescription()
            {
                return Lookup.i18n("preferences.overrideSafety");
            }
        };

        preferences.add(slicerTypePref);
        preferences.add(overrideSafetyPref);

        return preferences;
    }

    private List<Preference> createEnvironmentPreferences()
    {
        List<Preference> preferences = new ArrayList<>();

        Preference languagePref = new Preference()
        {
            private final ComboBox<Locale> control;
            {
                control = new ComboBox<>();

                setupCellFactory(control);

                List<Locale> localesList = new ArrayList<>();
                localesList.addAll(Lookup.getLanguages().getLocales());
                localesList.sort((Locale o1, Locale o2) ->
                    o1.getDisplayName().compareTo(o2.getDisplayName()));
                control.setItems(FXCollections.observableArrayList(localesList));
                control.setPrefWidth(200);
                control.setMinWidth(control.getPrefWidth());
                control.valueProperty().addListener(
                    (ObservableValue<? extends Locale> observable, Locale oldValue, Locale newValue) ->
                    {
                        updateValueFromControl();
                    });
            }

            @Override
            public void updateValueFromControl()
            {
                userPreferences.setLanguageTag(control.getValue().toLanguageTag());
            }

            @Override
            public void populateControlWithCurrentValue()
            {
                Locale preferredLocale = Locale.forLanguageTag(userPreferences.getLanguageTag());
                if (preferredLocale == null)
                {
                    preferredLocale = Locale.getDefault();
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

            private void setupCellFactory(ComboBox<Locale> control)
            {
                
                Callback<ListView<Locale>, ListCell<Locale>> cellFactory = new Callback<ListView<Locale>, ListCell<Locale>>()
                {
                    @Override
                    public ListCell<Locale> call(ListView<Locale> p)
                    {
                        return new ListCell<Locale>()
                        {
                            @Override
                            protected void updateItem(Locale item, boolean empty)
                            {
                                super.updateItem(item, empty);
                                if (item != null && !empty)
                                {
                                    setText(item.getDisplayName());
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
