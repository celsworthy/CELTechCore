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
    
}
