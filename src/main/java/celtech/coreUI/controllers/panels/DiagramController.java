/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 *
 * @author tony
 */
class DiagramController
{
    private final CalibrationInsetPanelController parentController;
    
    public DiagramController(CalibrationInsetPanelController parentController) {
        this.parentController = parentController;
        if (calibrationTextField != null) {
            calibrationTextField.setText("0.00");
        }
    }
    
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
    
    protected void setCalibrationTextField(String textFieldData) {
        if (calibrationTextField != null) {
            calibrationTextField.setText(textFieldData);
        }    
    }
    
}