/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.utilityPanels;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.coreUI.DisplayManager;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class SmartReelProgrammerController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(SmartReelProgrammerController.class.getName());
    private StatusScreenState statusScreenState = null;
    private Printer connectedPrinter = null;
    private ChangeListener<Boolean> reelDataChangeListener = new ChangeListener<Boolean>()
    {

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            if (connectedPrinter != null)
            {
                setupSmartReelDisplay(connectedPrinter.getReelEEPROMStatus());
            }
        }
    };

    private ChangeListener<EEPROMState> reelEEPROMStateChangeListener = new ChangeListener<EEPROMState>()
    {
        @Override
        public void changed(ObservableValue<? extends EEPROMState> observable, EEPROMState oldValue, EEPROMState newValue)
        {
            setupSmartReelDisplay(newValue);
        }
    };

    private void setupSmartReelDisplay(EEPROMState newValue)
    {
        switch (newValue)
        {
            case NOT_PRESENT:
                currentReelTitle.setText(DisplayManager.getLanguageBundle().getString("smartReelProgrammer.noReelLoaded"));
                break;
            case NOT_PROGRAMMED:
                currentReelTitle.setText(DisplayManager.getLanguageBundle().getString("smartReelProgrammer.reelNotFormatted"));
                break;
            case PROGRAMMED:
                if (connectedPrinter.loadedFilamentProperty().get() != null)
                {
                    currentReelTitle.setText(connectedPrinter.loadedFilamentProperty().get().toString());
                } else
                {
                    currentReelTitle.setText("* " + connectedPrinter.reelFriendlyNameProperty().get());
                }
                break;
        }
    }

    @FXML
    private Label currentReelTitle;

    @FXML
    private ComboBox<Filament> materialSelector;

    @FXML
    private Button programReelButton;

    @FXML
    void programReel(ActionEvent event)
    {
        Filament selectedFilament = materialSelector.getSelectionModel().getSelectedItem();

        try
        {
            float remainingFilament = 0;

            if (connectedPrinter.reelEEPROMStatusProperty().get() == EEPROMState.NOT_PROGRAMMED)
            {
                connectedPrinter.transmitFormatReelEEPROM();
                remainingFilament = ApplicationConfiguration.mmOfFilamentOnAReel;
            } else
            {
                remainingFilament = connectedPrinter.getReelRemainingFilament().get();
            }

            connectedPrinter.transmitWriteReelEEPROM(selectedFilament.getReelID(),
                    selectedFilament.getUniqueID(),
                    selectedFilament.getFirstLayerNozzleTemperature(),
                    selectedFilament.getNozzleTemperature(),
                    selectedFilament.getFirstLayerBedTemperature(),
                    selectedFilament.getBedTemperature(),
                    selectedFilament.getAmbientTemperature(),
                    selectedFilament.getDiameter(),
                    selectedFilament.getFilamentMultiplier(),
                    selectedFilament.getFeedRateMultiplier(),
                    remainingFilament);
            
            connectedPrinter.transmitReadReelEEPROM();
        } catch (RoboxCommsException ex)
        {
            steno.error("Error writing reel EEPROM");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        statusScreenState = StatusScreenState.getInstance();

        materialSelector.setItems(FilamentContainer.getCompleteFilamentList());

        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newPrinter)
            {
                if (connectedPrinter != null)
                {
                    connectedPrinter.reelEEPROMStatusProperty().removeListener(reelEEPROMStateChangeListener);
                    connectedPrinter.reelDataChangedProperty().removeListener(reelDataChangeListener);
                }

                if (newPrinter == null)
                {
                    programReelButton.disableProperty().unbind();
                    programReelButton.setDisable(true);
                    materialSelector.getSelectionModel().clearSelection();
                    materialSelector.disableProperty().unbind();
                    materialSelector.setDisable(true);
                } else
                {
                    programReelButton.disableProperty().bind(newPrinter.reelEEPROMStatusProperty().isEqualTo(EEPROMState.NOT_PRESENT).or(materialSelector.getSelectionModel().selectedItemProperty().isNull()));
                    materialSelector.disableProperty().bind(newPrinter.reelEEPROMStatusProperty().isEqualTo(EEPROMState.NOT_PRESENT));
                    newPrinter.reelEEPROMStatusProperty().addListener(reelEEPROMStateChangeListener);
                    newPrinter.reelDataChangedProperty().addListener(reelDataChangeListener);
                }

                connectedPrinter = newPrinter;
            }
        });
    }

}
