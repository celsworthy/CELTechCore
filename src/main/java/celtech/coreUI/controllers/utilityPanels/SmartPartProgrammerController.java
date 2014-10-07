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
import celtech.printerControl.model.Head;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.model.Printer;
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
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class SmartPartProgrammerController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(SmartPartProgrammerController.class.getName());
    private StatusScreenState statusScreenState = null;
    private Printer connectedPrinter = null;
    private ChangeListener<Boolean> reelDataChangeListener = new ChangeListener<Boolean>()
    {

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            if (connectedPrinter != null)
            {
//                setupSmartReelDisplay(connectedPrinter.getReelEEPROMStatus());
            }
        }
    };

    private ChangeListener<EEPROMState> reelEEPROMStateChangeListener = new ChangeListener<EEPROMState>()
    {
        @Override
        public void changed(ObservableValue<? extends EEPROMState> observable, EEPROMState oldValue, EEPROMState newValue)
        {
//            setupSmartReelDisplay(newValue);
        }
    };

//    private void setupSmartReelDisplay(EEPROMState newValue)
//    {
//        switch (newValue)
//        {
//            case NOT_PRESENT:
//                currentReelTitle.setText(DisplayManager.getLanguageBundle().getString("smartReelProgrammer.noReelLoaded"));
//                break;
//            case NOT_PROGRAMMED:
//                currentReelTitle.setText(DisplayManager.getLanguageBundle().getString("smartReelProgrammer.reelNotFormatted"));
//                break;
//            case PROGRAMMED:
//                if (connectedPrinter.loadedFilamentProperty().get() != null)
//                {
//                    currentReelTitle.setText(connectedPrinter.loadedFilamentProperty().get().toString());
//                } else
//                {
//                    currentReelTitle.setText("* " + connectedPrinter.reelFriendlyNameProperty().get());
//                }
//                break;
//        }
//    }

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

            //TODO modify for multiple reels
            if (connectedPrinter.reelsProperty().get(0).getReelEEPROMStatusProperty().get() == EEPROMState.NOT_PROGRAMMED)
            {
                connectedPrinter.transmitFormatReelEEPROM();
                remainingFilament = ApplicationConfiguration.mmOfFilamentOnAReel;
            } else
            {
                //TODO modify for multiple reels
                remainingFilament = connectedPrinter.reelsProperty().get(0).getRemainingFilamentProperty().get();
            }

            connectedPrinter.transmitWriteReelEEPROM(selectedFilament.getFilamentID(),
                                                     selectedFilament.getFirstLayerNozzleTemperature(),
                                                     selectedFilament.getNozzleTemperature(),
                                                     selectedFilament.getFirstLayerBedTemperature(),
                                                     selectedFilament.getBedTemperature(),
                                                     selectedFilament.getAmbientTemperature(),
                                                     selectedFilament.getDiameter(),
                                                     selectedFilament.getFilamentMultiplier(),
                                                     selectedFilament.getFeedRateMultiplier(),
                                                     remainingFilament,
                                                     selectedFilament.getFriendlyFilamentName(),
                                                     selectedFilament.getMaterial(),
                                                     selectedFilament.getDisplayColour());

            connectedPrinter.transmitReadReelEEPROM();
        } catch (RoboxCommsException ex)
        {
            steno.error("Error writing reel EEPROM");
        }
    }

    @FXML
    void resetHeadToDefaults(ActionEvent event)
    {
        if (connectedPrinter != null)
        {
            connectedPrinter.forceHeadReset();
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
                    //TODO modify to support multiple reels
                    connectedPrinter.reelsProperty().get(0).getReelEEPROMStatusProperty().removeListener(reelEEPROMStateChangeListener);
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
                    //TODO modify to support multiple reels
                    programReelButton.disableProperty().bind(newPrinter.reelsProperty().get(0).getReelEEPROMStatusProperty().isEqualTo(EEPROMState.NOT_PRESENT).or(materialSelector.getSelectionModel().selectedItemProperty().isNull()));
                    materialSelector.disableProperty().bind(newPrinter.reelsProperty().get(0).getReelEEPROMStatusProperty().isEqualTo(EEPROMState.NOT_PRESENT));
                    newPrinter.reelsProperty().get(0).getReelEEPROMStatusProperty().addListener(reelEEPROMStateChangeListener);
                }

                connectedPrinter = newPrinter;
            }
        });
    }

}
