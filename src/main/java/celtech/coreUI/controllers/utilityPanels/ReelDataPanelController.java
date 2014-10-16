
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.configuration.MaterialType;
import celtech.coreUI.components.RestrictedTextField;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.net.URL;
import java.text.ParseException;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class ReelDataPanelController implements Initializable, PrinterListChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        ReelDataPanelController.class.getName());

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
    private ComboBox<MaterialType> reelMaterialType;

    @FXML
    private ColorPicker reelDisplayColor;

    @FXML
    private Button reelWriteConfig;

    @FXML
    private Button saveFilamentAs;

    private Printer selectedPrinter;

    @FXML
    void filamentSaveAs(ActionEvent event)
    {
        Filament newFilament;
        try
        {
            newFilament = makeFilamentFromFields();
            newFilament.setFilamentID(null);
            String safeNewFilamentName = FilamentContainer.suggestNonDuplicateName(
                newFilament.getFriendlyFilamentName());
            newFilament.setFriendlyFilamentName(safeNewFilamentName);
            FilamentContainer.saveFilament(newFilament);
        } catch (ParseException ex)
        {
            steno.info("Parse error getting filament data");
        }

    }

    @FXML
    void writeReelConfig(ActionEvent event)
    {
        try
        {
            Filament newFilament = makeFilamentFromFields();
            try
            {
                FilamentContainer.saveFilament(newFilament);
                selectedPrinter.transmitWriteReelEEPROM(newFilament);

            } catch (RoboxCommsException ex)
            {
                steno.error("Error writing reel EEPROM");
            }

        } catch (ParseException ex)
        {
            steno.info("Parse error getting filament data");
        }
        readReelConfig(event);

    }

    private Filament makeFilamentFromFields() throws ParseException
    {
        float remainingFilament = 0;
        try
        {
            remainingFilament = Float.valueOf(reelRemainingFilament.getText());
        } catch (NumberFormatException ex)
        {
            steno.error("Error parsing filament parameters");
        }
        Filament newFilament = new Filament(
            reelFilamentName.getText(),
            reelMaterialType.getSelectionModel().getSelectedItem(),
            filamentID.getText(),
            reelFilamentDiameter.getFloatValue(),
            reelFilamentMultiplier.getFloatValue(),
            reelFeedRateMultiplier.getFloatValue(),
            Integer.valueOf(reelAmbientTemperature.getText()),
            Integer.valueOf(reelFirstLayerBedTemperature.getText()),
            Integer.valueOf(reelBedTemperature.getText()),
            Integer.valueOf(reelFirstLayerNozzleTemperature.getText()),
            Integer.valueOf(reelNozzleTemperature.getText()),
            reelDisplayColor.getValue(),
            true);
        newFilament.setRemainingFilament(remainingFilament);
        return newFilament;
    }

    void readReelConfig(ActionEvent event)
    {
        try
        {
            selectedPrinter.readReelEEPROM();
        } catch (RoboxCommsException ex)
        {
            steno.error("Error reading reel EEPROM");
        }
    }

    void formatReelEEPROM(ActionEvent event)
    {
        try
        {
            selectedPrinter.formatReelEEPROM();
        } catch (PrinterException ex)
        {
            steno.error("Error formatting reel EEPROM");
        }
        readReelConfig(null);
    }

    private void populateSelectedPrinter()
    {
        Reel reel = selectedPrinter.reelsProperty().get(0);
        if (reel.isUserFilament() == false)
        {
            reelContainer.disableProperty().set(false);
            reelWriteConfig.disableProperty().set(false);
        } else
        {
            reelContainer.disableProperty().set(true);
            reelWriteConfig.disableProperty().set(true);
        }

        filamentID.setText(reel.filamentIDProperty().get());
        reelAmbientTemperature.setText(String.format("%d",
                                                     reel.ambientTemperatureProperty().get()));
        reelFirstLayerBedTemperature.setText(String.format("%d",
                                                           reel.firstLayerBedTemperatureProperty().get()));
        reelBedTemperature.setText(String.format("%d",
                                                 reel.bedTemperatureProperty().get()));
        reelFirstLayerNozzleTemperature.setText(String.format("%d",
                                                              reel.firstLayerNozzleTemperatureProperty().get()));
        reelNozzleTemperature.setText(String.format("%d",
                                                    reel.nozzleTemperatureProperty().get()));
        reelFilamentMultiplier.setText(String.format("%.2f",
                                                     reel.filamentMultiplierProperty().get()));
        reelFeedRateMultiplier.setText(String.format("%.2f",
                                                     reel.feedRateMultiplierProperty().get()));
        reelRemainingFilament.setText(String.format("%.0f",
                                                    reel.remainingFilamentProperty().get()));
        reelFilamentDiameter.setText(String.format("%.2f",
                                                   reel.diameterProperty().get()));
        reelFilamentName.setText(
            reel.friendlyFilamentNameProperty().get());
        MaterialType reelMaterialTypeVal = reel.materialProperty().get();
        reelMaterialType.getSelectionModel().select(reelMaterialTypeVal);
        reelDisplayColor.setValue(reel.displayColourProperty().get());
    }
    /*
     * Initializes the controller class.
     */

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        for (MaterialType materialType : MaterialType.values())
        {
            reelMaterialType.getItems().add(materialType);
        }

        Lookup.currentlySelectedPrinterProperty().addListener(
            (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
            {
                if (newValue != oldValue)
                {
                    setSelectedPrinter(newValue);
                }
            });

        Lookup.getPrinterListChangesNotifier().addListener(this);

        if (Lookup.currentlySelectedPrinterProperty().get() != null)
        {
            setSelectedPrinter(
                Lookup.currentlySelectedPrinterProperty().get());
        }
    }

    private void setSelectedPrinter(Printer printer)
    {
        selectedPrinter = printer;

        if (!printer.reelsProperty().isEmpty())
        {
            populateSelectedPrinter();
            setFieldsVisible(true);
        } else
        {
            setFieldsVisible(false);
        }
    }

    private void setFieldsVisible(boolean visible)
    {
        reelContainer.setVisible(visible);
        reelWriteConfig.setVisible(visible);
        saveFilamentAs.setVisible(visible);
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
        if (printer == selectedPrinter)
        {
            populateSelectedPrinter();
            setFieldsVisible(true);
        }
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel)
    {
        if (printer == selectedPrinter)
        {
            setFieldsVisible(false);
        }
    }
}
