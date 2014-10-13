/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

/**
 *
 * @author tony
 */
class DiagramController implements Initializable
{

    private final CalibrationInsetPanelController parentController;

    public DiagramController(CalibrationInsetPanelController parentController)
    {
        this.parentController = parentController;
        if (calibrationTextField != null)
        {
            calibrationTextField.setText("0.00");
        }
    }

    @FXML
    protected ComboBox cmbYOffset;

    @FXML
    protected ComboBox cmbXOffset;

    @FXML
    private TextField calibrationTextField;

    @FXML
    void buttonAAction(ActionEvent event)
    {
        parentController.buttonAAction();
    }

    @FXML
    void buttonBAction(ActionEvent event)
    {
        parentController.buttonBAction();
    }

    protected void setCalibrationTextField(String textFieldData)
    {
        if (calibrationTextField != null)
        {
            calibrationTextField.setText(textFieldData);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        setupOffsetCombos();
    }

    private void setupOffsetCombos()
    {
        if (cmbXOffset != null)
        {
            cmbXOffset.getItems().add("A");
            cmbXOffset.getItems().add("B");
            cmbXOffset.getItems().add("C");
            cmbXOffset.getItems().add("D");
            cmbXOffset.getItems().add("E");
            cmbXOffset.getItems().add("F");
            cmbXOffset.getItems().add("G");
            cmbXOffset.getItems().add("H");
            cmbXOffset.getItems().add("I");
            cmbXOffset.getItems().add("J");
            cmbXOffset.getItems().add("K");
            cmbYOffset.getItems().add("1");
            cmbYOffset.getItems().add("2");
            cmbYOffset.getItems().add("3");
            cmbYOffset.getItems().add("4");
            cmbYOffset.getItems().add("5");
            cmbYOffset.getItems().add("6");
            cmbYOffset.getItems().add("7");
            cmbYOffset.getItems().add("8");
            cmbYOffset.getItems().add("9");
            cmbYOffset.getItems().add("10");
            cmbYOffset.getItems().add("11");

            cmbXOffset.valueProperty().addListener(
                (ObservableValue observable, Object oldValue, Object newValue) ->
                {
                    parentController.setXOffset(newValue.toString());
                });

            cmbYOffset.valueProperty().addListener(
                (ObservableValue observable, Object oldValue, Object newValue) ->
                {
                    parentController.setYOffset(Integer.parseInt(newValue.toString()));
                });
        }
    }

}
