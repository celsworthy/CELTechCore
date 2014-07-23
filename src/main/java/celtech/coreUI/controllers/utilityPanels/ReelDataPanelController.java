/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.utilityPanels;

import celtech.configuration.EEPROMState;
import celtech.coreUI.components.RestrictedTextField;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class ReelDataPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        ReelDataPanelController.class.getName());
    private Printer connectedPrinter = null;
    private StatusScreenState statusScreenState = null;

    @FXML
    Pane reelContainer;
    
    @FXML
    private RestrictedTextField reelFilamentName;

    @FXML
    private RestrictedTextField reelFirstLayerBedTemperature;

    @FXML
    private RestrictedTextField reelAmbientTemperature;

    @FXML
    private RestrictedTextField reelFilamentDiameter;

    @FXML
    private RestrictedTextField reelRemainingFilament;

    @FXML
    private TextField filamentID;

    @FXML
    private RestrictedTextField reelNozzleTemperature;

    @FXML
    private RestrictedTextField reelFeedRateMultiplier;

    @FXML
    private RestrictedTextField reelFirstLayerNozzleTemperature;

    @FXML
    private RestrictedTextField reelFilamentMultiplier;

    @FXML
    private RestrictedTextField reelBedTemperature;

    @FXML
    private Button reelWriteConfig;

    @FXML
    private Button saveFilamentAs;

    @FXML
    void filamentSaveAs(ActionEvent event)
    {

    }

    @FXML
    void writeReelConfig(ActionEvent event)
    {
        try
        {
            float remainingFilament = 0;

            try
            {
                remainingFilament = Float.valueOf(reelRemainingFilament.getText());
            } catch (NumberFormatException ex)
            {
                steno.error("Error parsing filament parameters");
            }

            connectedPrinter.transmitWriteReelEEPROM(
                filamentID.getText(), Float.valueOf(reelFirstLayerNozzleTemperature.getText()),
                Float.valueOf(reelNozzleTemperature.getText()),
                Float.valueOf(reelFirstLayerBedTemperature.getText()), Float.valueOf(
                    reelBedTemperature.getText()), Float.valueOf(reelAmbientTemperature.getText()),
                Float.valueOf(reelFilamentDiameter.getText()),
                Float.valueOf(reelFilamentMultiplier.getText()), Float.valueOf(
                    reelFeedRateMultiplier.getText()), remainingFilament,
                    reelFilamentName.getText(), "XYZ", 0);

        } catch (RoboxCommsException ex)
        {
            steno.error("Error writing reel EEPROM");
        }
        readReelConfig(event);
    }

    void readReelConfig(ActionEvent event)
    {
        try
        {
            connectedPrinter.transmitReadReelEEPROM();
        } catch (RoboxCommsException ex)
        {
            steno.error("Error reading reel EEPROM");
        }
    }

    void formatReelEEPROM(ActionEvent event)
    {
        try
        {
            connectedPrinter.transmitFormatReelEEPROM();
        } catch (RoboxCommsException ex)
        {
            steno.error("Error formatting reel EEPROM");
        }
        readReelConfig(null);
    }

    private ChangeListener<Boolean> reelDataChangeListener = new ChangeListener<Boolean>()
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
            Boolean newValue)
        {

            if (connectedPrinter.getReelFilamentIsMutable().get())
            {
                reelContainer.disableProperty().set(false);
                reelWriteConfig.disableProperty().set(false);
            } else
            {
                reelContainer.disableProperty().set(true);
                reelWriteConfig.disableProperty().set(true);
            }

            filamentID.setText(connectedPrinter.getReelFilamentID().get());
            reelAmbientTemperature.setText(String.format("%d",
                                                         connectedPrinter.getReelAmbientTemperature().get()));
            reelFirstLayerBedTemperature.setText(String.format("%d",
                                                               connectedPrinter.getReelFirstLayerBedTemperature().get()));
            reelBedTemperature.setText(String.format("%d",
                                                     connectedPrinter.getReelBedTemperature().get()));
            reelFirstLayerNozzleTemperature.setText(String.format("%d",
                                                                  connectedPrinter.getReelFirstLayerNozzleTemperature().get()));
            reelNozzleTemperature.setText(String.format("%d",
                                                        connectedPrinter.getReelNozzleTemperature().get()));
            reelFilamentMultiplier.setText(String.format("%.2f",
                                                         connectedPrinter.getReelFilamentMultiplier().get()));
            reelFeedRateMultiplier.setText(String.format("%.2f",
                                                         connectedPrinter.getReelFeedRateMultiplier().get()));
            reelRemainingFilament.setText(String.format("%.0f",
                                                        connectedPrinter.getReelRemainingFilament().get()));
            reelFilamentDiameter.setText(String.format("%.2f",
                                                       connectedPrinter.getReelFilamentDiameter().get()));
            reelFilamentName.setText(connectedPrinter.getReelFriendlyName());
        }
    };
    /*
     * Initializes the controller class.
     */

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        statusScreenState = StatusScreenState.getInstance();

        statusScreenState.currentlySelectedPrinterProperty().addListener(
            new ChangeListener<Printer>()
            {

                @Override
                public void changed(ObservableValue<? extends Printer> observable, Printer oldValue,
                    Printer newValue)
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
            reelContainer.visibleProperty().unbind();

            connectedPrinter.reelDataChangedProperty().removeListener(reelDataChangeListener);

            reelWriteConfig.visibleProperty().unbind();
            saveFilamentAs.visibleProperty().unbind();

            connectedPrinter = null;
        }
    }

    private void bindToPrinter(Printer printer)
    {
        if (connectedPrinter == null)
        {
            connectedPrinter = printer;

            reelContainer.visibleProperty().bind(
                connectedPrinter.reelEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED));

            connectedPrinter.reelDataChangedProperty().addListener(reelDataChangeListener);

            reelWriteConfig.visibleProperty().bind(
                connectedPrinter.reelEEPROMStatusProperty().isNotEqualTo(EEPROMState.NOT_PRESENT));
            saveFilamentAs.visibleProperty().bind(
                connectedPrinter.reelEEPROMStatusProperty().isNotEqualTo(EEPROMState.NOT_PRESENT));
        }
    }
}
