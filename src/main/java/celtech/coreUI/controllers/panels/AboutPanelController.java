/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.printerControl.Printer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

/**
 *
 * @author Ian
 */
public class AboutPanelController implements Initializable
{

    private SettingsScreenState settingsScreenState = null;
    private Printer currentPrinter = null;
    private final ChangeListener<Boolean> printerIDListener = new ChangeListener<Boolean>()
    {
        @Override
        public void changed(
            ObservableValue<? extends Boolean> observable, Boolean oldValue,
            Boolean newValue)
        {
            roboxSerialNumber.setText(currentPrinter.getPrinterUniqueID());
        }
    };

    @FXML
    private Label roboxSerialNumber;

    @FXML
    private Label headSerialNumber;

    @FXML
    private Label version;

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
                    currentPrinter.getPrinterIDDataChangedToggle().removeListener(printerIDListener);
                    headSerialNumber.textProperty().unbind();
                }

                if (newValue != null)
                {
                    currentPrinter = newValue;
                    currentPrinter.getPrinterIDDataChangedToggle().addListener(printerIDListener);
                    headSerialNumber.textProperty().bind(currentPrinter.getHeadUniqueID());
                }
            }
        });
        
        version.setText(ApplicationConfiguration.getApplicationVersion());
    }

}
