/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.printerControl.model.Printer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

/**
 *
 * @author Ian
 */
public class SystemInformationPanelController implements Initializable
{

    private SettingsScreenState settingsScreenState = null;
    private Printer currentPrinter = null;
    
    @FXML
    private Label roboxSerialNumber;

    @FXML
    private Label headSerialNumber;

    @FXML
    private Label version;
    
    @FXML
    private void okPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.STATUS);
    }

    @FXML
    private void systemInformationPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.ABOUT);        
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        settingsScreenState = SettingsScreenState.getInstance();
        
        settingsScreenState.selectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue,
                Printer newValue)
            {
                if (currentPrinter != null)
                {
                    headSerialNumber.textProperty().unbind();
                }

                if (newValue != null)
                {
                    currentPrinter = newValue;
                    headSerialNumber.textProperty().bind(currentPrinter.getPrinterIdentity().printerUniqueIDProperty());
                }
            }
        });
        
        version.setText(ApplicationConfiguration.getApplicationVersion());
    }

}
