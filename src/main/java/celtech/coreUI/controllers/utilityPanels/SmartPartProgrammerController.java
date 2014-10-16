package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.model.Printer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
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
            if (connectedPrinter.reelsProperty().size() == 1)
            {
                //TODO modify for multiple reels
                remainingFilament = connectedPrinter.reelsProperty().get(0).remainingFilamentProperty().get();
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

                connectedPrinter.readReelEEPROM();
            }
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
        materialSelector.setItems(FilamentContainer.getCompleteFilamentList());

        Lookup.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newPrinter)
            {
                if (connectedPrinter != null)
                {
                    //TODO modify to support multiple reels
//                    connectedPrinter.reelsProperty().get(0).reelEEPROMStatusProperty().removeListener(reelEEPROMStateChangeListener);
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
                    programReelButton.disableProperty().bind(Bindings.isEmpty(newPrinter.reelsProperty())
                        .or(materialSelector.getSelectionModel()
                            .selectedItemProperty().isNull()));
                    materialSelector.disableProperty().bind(Bindings.isEmpty(newPrinter.reelsProperty()));
//                    newPrinter.reelsProperty().get(0).reelEEPROMStatusProperty().addListener(reelEEPROMStateChangeListener);
                }

                connectedPrinter = newPrinter;
            }
        });
    }

}
