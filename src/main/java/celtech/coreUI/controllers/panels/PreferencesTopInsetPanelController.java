/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

/**
 *
 * @author tony
 */
public class PreferencesTopInsetPanelController implements Initializable
{
    
        @FXML
    void cancelPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().modeProperty().set(ApplicationMode.STATUS);
    }

    @FXML
    void backwardPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().modeProperty().set(ApplicationMode.STATUS);
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {

    }    
    
}
