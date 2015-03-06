/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
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
public class PreferencesInnerPanelController implements Initializable, ExtrasMenuInnerPanel
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
    private GridPane preferencesGridPane;

    private final String menuTitle;
    private final List<Preference> preferences;

    public PreferencesInnerPanelController(String menuTitle,
        List<Preference> preferences)
    {
        this.menuTitle = menuTitle;
        this.preferences = preferences;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        displayPreferences(preferences);
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

    @Override
    public String getMenuTitle()
    {
        return menuTitle;
    }

    @Override
    public List<ExtrasMenuInnerPanel.OperationButton> getOperationButtons()
    {
        List<ExtrasMenuInnerPanel.OperationButton> operationButtons = new ArrayList<>();
        return operationButtons;
    }
}
