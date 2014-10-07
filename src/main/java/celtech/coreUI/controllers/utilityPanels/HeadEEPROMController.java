/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.utilityPanels;

import celtech.configuration.EEPROMState;
import celtech.configuration.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.RestrictedTextField;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.utils.PrinterUtils;
import java.net.URL;
import java.text.ParseException;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class HeadEEPROMController implements Initializable
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
    private VBox headFullContainer;

    @FXML
    private Button writeOffsetsButton;

//    private BooleanProperty fastUpdates = new SimpleBooleanProperty(false);
//    private Head temporaryHead = null;
    private ObservableList<Printer> printerStatusList = null;
    //We'll only deal with the first printer we find.
    private Printer connectedPrinter = null;

    private ChangeListener<EEPROMState> headAttachListener = null;
    private ChangeListener<Boolean> headDataChangeListener = null;

    private ModalDialog eepromCommsError = null;

    private Float nozzle1ZOffsetCalculated;
    private Float nozzle2ZOffsetCalculated;

    private final BooleanProperty offsetFieldsDirty = new SimpleBooleanProperty();

    @FXML
    void resetToDefaults(ActionEvent event)
    {
        String headId = headTypeCode.getText();
        connectedPrinter.headProperty().get().repair(headId);
    }

    @FXML
    /**
     * Write the values from the text fields onto the actual head. If the unique id is already stored on the head then do not overwrite it.
     */
    void writeHeadConfig(ActionEvent event)
    {
        try
        {
            HeadEEPROMDataResponse headDataResponse = connectedPrinter.readHeadEEPROM();
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
            connectedPrinter.transmitWriteHeadEEPROM(
                headTypeCodeText, uniqueId, headMaxTemperatureVal, headThermistorBetaVal,
                headThermistorTCalVal, nozzle1XOffsetVal, nozzle1YOffsetVal,
                nozzle1ZOffsetCalculated, nozzle1BOffsetVal,
                nozzle2XOffsetVal, nozzle2YOffsetVal,
                nozzle2ZOffsetCalculated, nozzle2BOffsetVal,
                lastFilamentTemperatureVal, headHourCounterVal);
            offsetFieldsDirty.set(false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error writing reel EEPROM");
            eepromCommsError.setMessage(DisplayManager.getLanguageBundle().getString(
                "eeprom.headWriteError"));
            eepromCommsError.show();
        } catch (ParseException ex)
        {
            steno.info("Parse error getting head data");
        }
        readHeadConfig(event);
    }

    void readHeadConfig(ActionEvent event)
    {
        try
        {
            connectedPrinter.readHeadEEPROM();
        } catch (RoboxCommsException ex)
        {
            steno.error("Error reading head EEPROM");
            eepromCommsError.setMessage(DisplayManager.getLanguageBundle().getString(
                "eeprom.headReadError"));
            eepromCommsError.show();
        }
    }

    void readPrinterID(ActionEvent event)
    {
        try
        {
            connectedPrinter.transmitReadPrinterID();
        } catch (RoboxCommsException ex)
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

            printerStatusList = RoboxCommsManager.getInstance().getPrintStatusList();

            printerStatusList.addListener(new ListChangeListener<Printer>()
            {
                @Override
                public void onChanged(ListChangeListener.Change<? extends Printer> change)
                {
                    while (change.next())
                    {
                        if (change.wasAdded())
                        {
                            for (Printer additem : change.getAddedSubList())
                            {
                                bindToPrinter(additem);
                                readPrinterID(null);
                                readHeadConfig(null);
                                try
                                {
                                    additem.transmitResetErrors();
                                } catch (RoboxCommsException ex)
                                {
                                    steno.error("Failure whilst resetting firmware errors");
                                }
                            }
                        } else if (change.wasRemoved())
                        {
                            for (Printer additem : change.getRemoved())
                            {
                                if (additem == connectedPrinter)
                                {
                                    unbindFromPrinter(additem);
                                }
                            }
                        } else if (change.wasReplaced())
                        {
                        } else if (change.wasUpdated())
                        {
                        }
                    }
                }
            });

            headAttachListener = new ChangeListener<EEPROMState>()
            {
                @Override
                public void changed(ObservableValue<? extends EEPROMState> ov, EEPROMState t,
                    EEPROMState t1)
                {
                    if (t1 == EEPROMState.PROGRAMMED)
                    {
                        readHeadConfig(null);
                    }
                }
            };

            headDataChangeListener = new ChangeListener<Boolean>()
            {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
                {
                    updateFieldsFromAttachedHead();
                    if (headUniqueID.getText().length() == 0)
                    {
                        headUniqueID.setDisable(false);
                    } else
                    {
                        headUniqueID.setDisable(true);
                    }
                }
            };

//        //TODO - make this switch off again - need to hook into slider code?
//        fastUpdates.bind(inputsTab.selectedProperty());
//
//        fastUpdates.addListener(new ChangeListener<Boolean>()
//        {
//
//            @Override
//            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
//            {
//                if (t1.booleanValue() == true)
//                {
//                    commsManager.setSleepBetweenStatusChecks(connectedPrinter, 100);
//                } else
//                {
//                    commsManager.setSleepBetweenStatusChecks(connectedPrinter, 1000);
//                }
//            }
//        });
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

                        nozzle1ZOffsetCalculated = PrinterUtils.deriveNozzle1ZOffsetsFromOverrun(nozzle1OverrunValue, nozzle2OverrunValue);
                        nozzle2ZOffsetCalculated = PrinterUtils.deriveNozzle2ZOffsetsFromOverrun(nozzle1OverrunValue, nozzle2OverrunValue);
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
                        nozzle1ZOffsetCalculated = PrinterUtils.deriveNozzle1ZOffsetsFromOverrun(nozzle1OverrunValue, nozzle2OverrunValue);
                        nozzle2ZOffsetCalculated = PrinterUtils.deriveNozzle2ZOffsetsFromOverrun(nozzle1OverrunValue, nozzle2OverrunValue);
//                    nozzle1ZOffset.setText(String.format("%.2f", temporaryHead.getNozzle1ZOffset()));
//                    nozzle2ZOffset.setText(String.format("%.2f", temporaryHead.getNozzle2ZOffset()));
                    } catch (NumberFormatException ex)
                    {
                        steno.error("Error parsing nozzle overrun string value");
                    }
                }
            });

            setUpWriteEnabledAfterEdits();

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    private void setUpWriteEnabledAfterEdits()
    {
        ChangeListener offsetsChangedListener = new ChangeListener<String>()
        {

            @Override
            public void changed(
                ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                offsetFieldsDirty.set(true);
            }

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

    private void unbindFromPrinter(Printer printer)
    {
        if (connectedPrinter != null)
        {

//            connectedPrinter.getHeadDataChangedToggle().removeListener(headDataChangeListener);

            headFullContainer.disableProperty().unbind();

            connectedPrinter.headProperty().get().headEEPROMStatusProperty().removeListener(headAttachListener);

            connectedPrinter = null;

        }

    }

    private void bindToPrinter(Printer printer)
    {
        if (connectedPrinter == null)
        {
            connectedPrinter = printer;
//            connectedPrinter.getHeadDataChangedToggle().addListener(headDataChangeListener);

            headFullContainer.disableProperty().bind(Bindings.not(
                connectedPrinter.headProperty().get().headEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED)));

            connectedPrinter.headProperty().get().headEEPROMStatusProperty().addListener(headAttachListener);

        }
    }
    
    private void updateFieldsFromAttachedHead()
    {
        headTypeCode.setText(connectedPrinter.headProperty().get().typeCodeProperty().get().trim());
        headType.setText(connectedPrinter.headProperty().get().nameProperty().get().trim());
        headUniqueID.setText(connectedPrinter.headProperty().get().uniqueIDProperty().get().trim());
        //TODO modify to work with multiple heaters
        lastFilamentTemperature.setText(String.format("%.0f",
                                                      connectedPrinter.headProperty().get().getNozzleHeaters().get(0).lastFilamentTemperatureProperty()));
        headHourCounter.setText(String.format("%.2f", connectedPrinter.headProperty().get().headHoursProperty().get()));
        //TODO modify to work with multiple heaters
        headMaxTemperature.setText(String.format("%.0f",
                                                 connectedPrinter.headProperty().get().getNozzleHeaters().get(0).maximumTemperatureProperty().get()));
        //TODO modify to work with multiple heaters
        headThermistorBeta.setText(String.format("%.2f",
                                                 connectedPrinter.headProperty().get().getNozzleHeaters().get(0).betaProperty().get()));
        //TODO modify to work with multiple heaters
        headThermistorTCal.setText(String.format("%.2f",
                                                 connectedPrinter.headProperty().get().getNozzleHeaters().get(0).tCalProperty().get()));
        nozzle1BOffset.setText(String.format("%.2f", connectedPrinter.headProperty().get().getNozzles().get(0).bOffsetProperty().get()));
        nozzle1XOffset.setText(String.format("%.2f", connectedPrinter.headProperty().get().getNozzles().get(0).xOffsetProperty().get()));
        nozzle1YOffset.setText(String.format("%.2f", connectedPrinter.headProperty().get().getNozzles().get(0).yOffsetProperty().get()));
        nozzle1ZOffsetCalculated = connectedPrinter.headProperty().get().getNozzles().get(0).zOffsetProperty().get();
        nozzle2BOffset.setText(String.format("%.2f", connectedPrinter.headProperty().get().getNozzles().get(1).bOffsetProperty().get()));
        nozzle2XOffset.setText(String.format("%.2f", connectedPrinter.headProperty().get().getNozzles().get(1).xOffsetProperty().get()));
        nozzle2YOffset.setText(String.format("%.2f", connectedPrinter.headProperty().get().getNozzles().get(1).yOffsetProperty().get()));
        nozzle2ZOffsetCalculated = connectedPrinter.headProperty().get().getNozzles().get(1).zOffsetProperty().get();

        //TODO modify to deal with variable numbers of nozzle
        float nozzle1Offset = connectedPrinter.headProperty().get().getNozzles().get(0).bOffsetProperty().get();
        float nozzle2Offset = connectedPrinter.headProperty().get().getNozzles().get(2).bOffsetProperty().get();
        float nozzle1ZOverrunValue = PrinterUtils.deriveNozzle1OverrunFromOffsets(nozzle1Offset, nozzle2Offset);
        float nozzle2ZOverrunValue = PrinterUtils.deriveNozzle1OverrunFromOffsets(nozzle1Offset, nozzle2Offset);

        nozzle1ZOverrun.setText(String.format("%.2f", nozzle1ZOverrunValue));
        nozzle2ZOverrun.setText(String.format("%.2f", nozzle2ZOverrunValue));
        offsetFieldsDirty.set(false);
    }
}
