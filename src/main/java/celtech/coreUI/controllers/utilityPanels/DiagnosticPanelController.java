/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.utilityPanels;

import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class DiagnosticPanelController implements Initializable
{
    
    private Printer connectedPrinter = null;
    private StatusScreenState statusScreenState = null;
    
    @FXML
    private Label extruder1Loaded;
    
    @FXML
    private Label lidSwitch;
    
    @FXML
    private Label zPositiveLimitSwitch;
    
    @FXML
    private Label extruder2Loaded;
    
    @FXML
    private Label extruder2Index;
    
    @FXML
    private Label reelButtonSwitch;
    
    @FXML
    private Label printerID;
    
    @FXML
    private Label zLimitSwitch;
    
    @FXML
    private Label xLimitSwitch;
    
    @FXML
    private Label yLimitSwitch;
    
    @FXML
    private Label extruder1Index;
    
    @FXML
    private Label headID;
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        statusScreenState = StatusScreenState.getInstance();
        
        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
            {
                if (connectedPrinter != null)
                {
                    unbindFromPrinter(connectedPrinter);
                }
                
                if (newValue != null)
                {
                    bindToPrinter(newValue);
                }
            }
        });
    }
    
    private void unbindFromPrinter(Printer printer)
    {
        if (connectedPrinter != null)
        {
            xLimitSwitch.textProperty().unbind();
            xLimitSwitch.setText("");
            printerID.textProperty().unbind();
            printerID.setText("");
            headID.textProperty().unbind();
            headID.setText("");
            yLimitSwitch.textProperty().unbind();
            yLimitSwitch.setText("");
            zLimitSwitch.textProperty().unbind();
            zLimitSwitch.setText("");
            zPositiveLimitSwitch.textProperty().unbind();
            zPositiveLimitSwitch.setText("");
            lidSwitch.textProperty().unbind();
            lidSwitch.setText("");
            reelButtonSwitch.textProperty().unbind();
            reelButtonSwitch.setText("");
            extruder1Loaded.textProperty().unbind();
            extruder1Loaded.setText("");
            extruder2Loaded.textProperty().unbind();
            extruder2Loaded.setText("");
            extruder1Index.textProperty().unbind();
            extruder1Index.setText("");
            extruder2Index.textProperty().unbind();
            extruder2Index.setText("");
            
            connectedPrinter = null;
        }
    }
    
    private void bindToPrinter(Printer printer)
    {
        if (connectedPrinter == null)
        {
            connectedPrinter = printer;
            
            printerID.textProperty().bind(printer.getPrinterserialNumber());
            headID.textProperty().bind(printer.getHeadUniqueID());
            xLimitSwitch.textProperty().bind(printer.XStopSwitchProperty().asString());
            yLimitSwitch.textProperty().bind(printer.YStopSwitchProperty().asString());
            zLimitSwitch.textProperty().bind(printer.ZStopSwitchProperty().asString());
            zPositiveLimitSwitch.textProperty().bind(printer.ZTopStopSwitchProperty().asString());
            lidSwitch.textProperty().bind(printer.LidOpenProperty().asString());
            reelButtonSwitch.textProperty().bind(printer.reelButtonProperty().asString());
            extruder1Loaded.textProperty().bind(printer.Filament1LoadedProperty().asString());
            extruder1Index.textProperty().bind(printer.Filament1IndexProperty().asString());
            extruder2Loaded.textProperty().bind(printer.Filament2LoadedProperty().asString());
            extruder2Index.textProperty().bind(printer.Filament2IndexProperty().asString());
        }
    }
    
}
