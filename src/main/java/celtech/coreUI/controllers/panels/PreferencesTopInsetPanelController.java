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
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 *
 * @author tony
 */
public class PreferencesTopInsetPanelController implements Initializable
{

    public interface Preference
    {

        public void updatePreference();

        public void getPreference();

        public Control getControl();

        public String getDescription();
    }

    @FXML
    void cancelPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
    }

    @FXML
    void backwardPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
    }

    @FXML
    private VBox preferencesListContainer;

    @FXML
    private VerticalMenu preferencesMenu;

    private UserPreferences userPreferences;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        userPreferences = Lookup.getUserPreferences();

        preferencesMenu.setTitle(Lookup.i18n("preferences.preferences"));

        preferencesMenu.addItem("Environment", this::showEnvironmentPreferences);
        preferencesMenu.addItem("Printing", this::showPrintingPreferences);
    }

    private Object showEnvironmentPreferences()
    {
        return null;
    }

    private Object showPrintingPreferences()
    {
        List<Preference> preferences = createPrintingPreferences();
        
        preferencesListContainer.getChildren().clear();
        for (Preference preference : preferences)
        {
            addPreferenceToContainer(preference);
        }

        return null;
    }

    private List<Preference> createPrintingPreferences()
    {
        List<Preference> preferences = new ArrayList<>();
        
        Preference slicerTypePref = new Preference()
        {
            private ComboBox<SlicerType> control;
            
            {
                control = new ComboBox<>();
                control.setItems(FXCollections.observableArrayList(SlicerType.values()));
                control.setPrefWidth(150);
                control.setMinWidth(control.getPrefWidth());
            }

            @Override
            public void updatePreference()
            {
                SlicerType slicerType = control.getValue();
                userPreferences.setSlicerType(slicerType);
            }

            @Override
            public void getPreference()
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
                return "Slicer Type";
            }
        };
        
        Preference overrideSafetyPref = new Preference()
        {
            private CheckBox control;
            
            {
                control = new CheckBox();
                control.setPrefWidth(150);
                control.setMinWidth(control.getPrefWidth());
            }

            @Override
            public void updatePreference()
            {
                boolean overrideSafety = control.isSelected();
                userPreferences.setOverrideSafeties(overrideSafety);
            }

            @Override
            public void getPreference()
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
                return "Override Safety";
            }
        };
        
        preferences.add(slicerTypePref);
        preferences.add(overrideSafetyPref);
        
        return preferences;
    }

    private void addPreferenceToContainer(Preference preference)
    {
        HBox row = getPreferenceRow(preference);
        preferencesListContainer.getChildren().add(row);
        preferencesListContainer.setMargin(row, new Insets(0, 0, 0, 60));
    }

    public HBox getPreferenceRow(Preference preference)
    {
        HBox rowHBox = new HBox();
        rowHBox.getStyleClass().add("preferenceRow");
        Label descriptionLabel = new Label(preference.getDescription() + ":");
        descriptionLabel.getStyleClass().add("preferenceLabel");
        rowHBox.getChildren().add(descriptionLabel);
        Control control = preference.getControl();
        control.getStyleClass().add("preferenceControl");
        rowHBox.getChildren().add(control);
        return rowHBox;
    }

}
