package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.RestrictedTextField;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import celtech.utils.PrinterUtils;
import java.net.URL;
import java.text.ParseException;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class HeadEEPROMController implements Initializable, PrinterListChangesListener
{

    Stenographer steno = StenographerFactory.getStenographer(HeadEEPROMController.class.getName());

    @FXML
    private RestrictedTextField nozzle2ZOverrun;

    @FXML
    private RestrictedTextField headThermistorTCal;

    @FXML
    private RestrictedTextField headThermistorBeta;

    @FXML
    private RestrictedTextField lastFilamentTemperature;

    @FXML
    private RestrictedTextField nozzle1ZOverrun;

    @FXML
    private RestrictedTextField headMaxTemperature;

    @FXML
    private RestrictedTextField nozzle2XOffset;

    @FXML
    private RestrictedTextField nozzle1YOffset;

    @FXML
    private RestrictedTextField nozzle2YOffset;

    @FXML
    private RestrictedTextField nozzle1XOffset;

    @FXML
    private RestrictedTextField headHourCounter;

    @FXML
    private RestrictedTextField nozzle2BOffset;

    @FXML
    private RestrictedTextField headUniqueID;

    @FXML
    private RestrictedTextField headTypeCode;

    @FXML
    private RestrictedTextField nozzle1BOffset;

    @FXML
    private RestrictedTextField headType;

    @FXML
    private Button writeOffsetsButton;

    private ModalDialog eepromCommsError = null;

    private Float nozzle1ZOffsetCalculated;
    private Float nozzle2ZOffsetCalculated;

    private final BooleanProperty offsetFieldsDirty = new SimpleBooleanProperty();

    private Printer selectedPrinter;

    @FXML
    void resetToDefaults(ActionEvent event)
    {
        String headId = headTypeCode.getText();
        selectedPrinter.headProperty().get().repair(headId);
    }

    @FXML
    /**
     * Write the values from the text fields onto the actual head. If the unique id is already
     * stored on the head then do not overwrite it.
     */
    void writeHeadConfig(ActionEvent event)
    {
        try
        {
            HeadEEPROMDataResponse headDataResponse = selectedPrinter.readHeadEEPROM();
            String uniqueId = headDataResponse.getUniqueID();
            if (uniqueId.length() == 0)
            {
                uniqueId = headUniqueID.getText();
            }
            String headTypeCodeText = headTypeCode.getText();
            Float headMaxTemperatureVal = headMaxTemperature.getFloatValue();
            Float headThermistorBetaVal = headThermistorBeta.getFloatValue();
            Float headThermistorTCalVal = headThermistorTCal.getFloatValue();
            Float nozzle1XOffsetVal = nozzle1XOffset.getFloatValue();
            Float nozzle1YOffsetVal = nozzle1YOffset.getFloatValue();
            Float nozzle1BOffsetVal = nozzle1BOffset.getFloatValue();
            Float nozzle2XOffsetVal = nozzle2XOffset.getFloatValue();
            Float nozzle2YOffsetVal = nozzle2YOffset.getFloatValue();
            Float nozzle2BOffsetVal = nozzle2BOffset.getFloatValue();
            Float lastFilamentTemperatureVal = lastFilamentTemperature.getFloatValue();
            Float headHourCounterVal = headHourCounter.getFloatValue();
            selectedPrinter.transmitWriteHeadEEPROM(
                headTypeCodeText, uniqueId, headMaxTemperatureVal, headThermistorBetaVal,
                headThermistorTCalVal, nozzle1XOffsetVal, nozzle1YOffsetVal,
                nozzle1ZOffsetCalculated, nozzle1BOffsetVal,
                nozzle2XOffsetVal, nozzle2YOffsetVal,
                nozzle2ZOffsetCalculated, nozzle2BOffsetVal,
                lastFilamentTemperatureVal, headHourCounterVal);
            offsetFieldsDirty.set(false);
            selectedPrinter.readHeadEEPROM();
        } catch (RoboxCommsException ex)
        {
            steno.error("Error writing head EEPROM");
            eepromCommsError.setMessage(DisplayManager.getLanguageBundle().getString(
                "eeprom.headWriteError"));
            eepromCommsError.show();
        } catch (ParseException ex)
        {
            steno.info("Parse error getting head data");
        }
    }

    void readPrinterID(ActionEvent event)
    {
        try
        {
            selectedPrinter.readPrinterID();
        } catch (PrinterException ex)
        {
            steno.error("Error reading printer ID");
        }
    }

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            eepromCommsError = new ModalDialog();
            eepromCommsError.setTitle(DisplayManager.getLanguageBundle().getString("eeprom.error"));
            eepromCommsError.addButton(DisplayManager.getLanguageBundle().getString("dialogs.OK"));

            setupNozzleOverrunListeners();
            setUpWriteEnabledAfterEdits();

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

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    private void setupNozzleOverrunListeners()
    {
        nozzle1ZOverrun.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> ov, String t, String t1)
            {
                try
                {
                    if (nozzle2ZOverrun.getText().isEmpty())
                    {
                        return;
                    }
                    float nozzle1OverrunValue = Float.valueOf(t1);
                    float nozzle2OverrunValue = Float.valueOf(nozzle2ZOverrun.getText());

                    nozzle1ZOffsetCalculated = PrinterUtils.deriveNozzle1ZOffsetsFromOverrun(
                        nozzle1OverrunValue, nozzle2OverrunValue);
                    nozzle2ZOffsetCalculated = PrinterUtils.deriveNozzle2ZOffsetsFromOverrun(
                        nozzle1OverrunValue, nozzle2OverrunValue);
//                    nozzle1ZOffset.setText(String.format("%.2f", temporaryHead.getNozzle1ZOffset()));
//                    nozzle2ZOffset.setText(String.format("%.2f", temporaryHead.getNozzle2ZOffset()));
                } catch (NumberFormatException ex)
                {
                    steno.error("Error parsing nozzle overrun string value");
                }
            }
        });

        nozzle2ZOverrun.textProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> ov, String t, String t1)
            {
                try
                {
                    if (nozzle1ZOverrun.getText().isEmpty())
                    {
                        return;
                    }
                    float nozzle1OverrunValue = Float.valueOf(nozzle1ZOverrun.getText());
                    float nozzle2OverrunValue = Float.valueOf(t1);
                    nozzle1ZOffsetCalculated = PrinterUtils.deriveNozzle1ZOffsetsFromOverrun(
                        nozzle1OverrunValue, nozzle2OverrunValue);
                    nozzle2ZOffsetCalculated = PrinterUtils.deriveNozzle2ZOffsetsFromOverrun(
                        nozzle1OverrunValue, nozzle2OverrunValue);
//                    nozzle1ZOffset.setText(String.format("%.2f", temporaryHead.getNozzle1ZOffset()));
//                    nozzle2ZOffset.setText(String.format("%.2f", temporaryHead.getNozzle2ZOffset()));
                } catch (NumberFormatException ex)
                {
                    steno.error("Error parsing nozzle overrun string value");
                }
            }
        });
    }

    private void setUpWriteEnabledAfterEdits()
    {
        ChangeListener offsetsChangedListener = (ChangeListener<String>) (ObservableValue<? extends String> observable, String oldValue, String newValue) ->
        {
            offsetFieldsDirty.set(true);
        };

        nozzle1BOffset.textProperty().addListener(offsetsChangedListener);
        nozzle1XOffset.textProperty().addListener(offsetsChangedListener);
        nozzle1YOffset.textProperty().addListener(offsetsChangedListener);
        nozzle1ZOverrun.textProperty().addListener(offsetsChangedListener);
        nozzle2BOffset.textProperty().addListener(offsetsChangedListener);
        nozzle2XOffset.textProperty().addListener(offsetsChangedListener);
        nozzle2YOffset.textProperty().addListener(offsetsChangedListener);
        nozzle2ZOverrun.textProperty().addListener(offsetsChangedListener);

        writeOffsetsButton.disableProperty().bind(Bindings.not(offsetFieldsDirty));

    }

    private void updateFieldsFromAttachedHead(Head head)
    {
        headTypeCode.setText(head.typeCodeProperty().get().trim());
        headType.setText(head.nameProperty().get().trim());
        headUniqueID.setText(head.uniqueIDProperty().get().trim());
        //TODO modify to work with multiple heaters
        lastFilamentTemperature.setText(String.format("%.0f",
                                                      head.getNozzleHeaters().get(0).lastFilamentTemperatureProperty().get()));
        headHourCounter.setText(String.format("%.2f", head.headHoursProperty().get()));
        //TODO modify to work with multiple heaters
        headMaxTemperature.setText(String.format("%.0f",
                                                 head.getNozzleHeaters().get(0).maximumTemperatureProperty().get()));
        //TODO modify to work with multiple heaters
        headThermistorBeta.setText(String.format("%.2f",
                                                 head.getNozzleHeaters().get(0).betaProperty().get()));
        //TODO modify to work with multiple heaters
        headThermistorTCal.setText(String.format("%.2f",
                                                 head.getNozzleHeaters().get(0).tCalProperty().get()));
        nozzle1BOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(0).bOffsetProperty().get()));
        nozzle1XOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(0).xOffsetProperty().get()));
        nozzle1YOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(0).yOffsetProperty().get()));
        nozzle1ZOffsetCalculated = head.getNozzles().get(0).zOffsetProperty().get();
        nozzle2BOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(1).bOffsetProperty().get()));
        nozzle2XOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(1).xOffsetProperty().get()));
        nozzle2YOffset.setText(String.format("%.2f",
                                             head.getNozzles().get(1).yOffsetProperty().get()));
        nozzle2ZOffsetCalculated = head.getNozzles().get(1).zOffsetProperty().get();

        //TODO modify to deal with variable numbers of nozzle
        float nozzle1Offset = head.getNozzles().get(0).bOffsetProperty().get();
        float nozzle2Offset = head.getNozzles().get(1).bOffsetProperty().get();
        float nozzle1ZOverrunValue = PrinterUtils.deriveNozzle1OverrunFromOffsets(nozzle1Offset,
                                                                                  nozzle2Offset);
        float nozzle2ZOverrunValue = PrinterUtils.deriveNozzle1OverrunFromOffsets(nozzle1Offset,
                                                                                  nozzle2Offset);

        nozzle1ZOverrun.setText(String.format("%.2f", nozzle1ZOverrunValue));
        nozzle2ZOverrun.setText(String.format("%.2f", nozzle2ZOverrunValue));
        offsetFieldsDirty.set(false);
    }

    private void updateFieldsForNoHead()
    {
        headTypeCode.setText("");
        headType.setText("");
        headUniqueID.setText("");
        //TODO modify to work with multiple heaters
        lastFilamentTemperature.setText("");
        headHourCounter.setText("");
        //TODO modify to work with multiple heaters
        headMaxTemperature.setText("");
        //TODO modify to work with multiple heaters
        headThermistorBeta.setText("");
        //TODO modify to work with multiple heaters
        headThermistorTCal.setText("");
        nozzle1BOffset.setText("");
        nozzle1XOffset.setText("");
        nozzle1YOffset.setText("");
        nozzle2BOffset.setText("");
        nozzle2XOffset.setText("");
        nozzle2YOffset.setText("");

        nozzle1ZOverrun.setText("");
        nozzle2ZOverrun.setText("");
        offsetFieldsDirty.set(false);
    }

    private void setSelectedPrinter(Printer printer)
    {
        updateFieldsForNoHead();
        selectedPrinter = printer;
        if (printer != null && printer.headProperty().get() != null)
        {
            Head head = printer.headProperty().get();
            updateFieldsFromAttachedHead(head);
            updateHeadUniqueId();
        }
    }

    private void updateHeadUniqueId()
    {
        if (headUniqueID.getText().length() == 0)
        {
            headUniqueID.setDisable(false);
        } else
        {
            headUniqueID.setDisable(true);
        }
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
        if (printer == selectedPrinter)
        {
            Head head = printer.headProperty().get();
            updateFieldsFromAttachedHead(head);
            updateHeadUniqueId();
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        if (printer == selectedPrinter)
        {
            updateFieldsForNoHead();
            updateHeadUniqueId();
        }
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel)
    {
    }

}
