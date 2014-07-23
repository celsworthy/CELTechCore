/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.utilityPanels;

import celtech.configuration.EEPROMState;
import celtech.configuration.Head;
import celtech.configuration.HeadContainer;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.RestrictedTextField;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
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
    private ComboBox headTypeCombo;

    @FXML
    private VBox headFullContainer;
    

//    private BooleanProperty fastUpdates = new SimpleBooleanProperty(false);
    private Head temporaryHead = null;

    private ObservableList<Printer> printerStatusList = null;
    //We'll only deal with the first printer we find.
    private Printer connectedPrinter = null;

    private ChangeListener<EEPROMState> headAttachListener = null;
    private ChangeListener<Boolean> headDataChangeListener = null;

    private ModalDialog eepromCommsError = null;

    private Float nozzle1ZOffsetCalculated;
    private Float nozzle2ZOffsetCalculated;

    @FXML
    void resetToDefaults(ActionEvent event)
    {
        String headId = headTypeCode.getText();
        Head currentStandardHead = HeadContainer.getHeadByID(headId);
        updateOffsetFieldsForHead(currentStandardHead);
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
            HeadEEPROMDataResponse headDataResponse = connectedPrinter.transmitReadHeadEEPROM();
            String uniqueId = headDataResponse.getUniqueID();
            if (uniqueId.length() == 0)
            {
                uniqueId = headUniqueID.getText();
            }
            String headTypeCodeText = headTypeCode.getText();
            Float headMaxTemperatureVal = Float.valueOf(headMaxTemperature.getText());
            Float headThermistorBetaVal = Float.valueOf(headThermistorBeta.getText());
            Float headThermistorTCalVal = Float.valueOf(headThermistorTCal.getText());
            Float nozzle1XOffsetVal = Float.valueOf(nozzle1XOffset.getText());
            Float nozzle1YOffsetVal = Float.valueOf(nozzle1YOffset.getText());
            Float nozzle1BOffsetVal = Float.valueOf(nozzle1BOffset.getText());
            Float nozzle2XOffsetVal = Float.valueOf(nozzle2XOffset.getText());
            Float nozzle2YOffsetVal = Float.valueOf(nozzle2YOffset.getText());
            Float nozzle2BOffsetVal = Float.valueOf(nozzle2BOffset.getText());
            Float lastFilamentTemperatureVal = Float.valueOf(lastFilamentTemperature.getText());
            Float headHourCounterVal = Float.valueOf(headHourCounter.getText());
            connectedPrinter.transmitWriteHeadEEPROM(
                headTypeCodeText, uniqueId, headMaxTemperatureVal, headThermistorBetaVal,
                headThermistorTCalVal, nozzle1XOffsetVal, nozzle1YOffsetVal,
                nozzle1ZOffsetCalculated, nozzle1BOffsetVal,
                nozzle2XOffsetVal, nozzle2YOffsetVal,
                nozzle2ZOffsetCalculated, nozzle2BOffsetVal,
                lastFilamentTemperatureVal, headHourCounterVal);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error writing reel EEPROM");
            eepromCommsError.setMessage(DisplayManager.getLanguageBundle().getString(
                "eeprom.headWriteError"));
            eepromCommsError.show();
        }
        readHeadConfig(event);
    }

    void readHeadConfig(ActionEvent event)
    {
        try
        {
            connectedPrinter.transmitReadHeadEEPROM();
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
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try {
        temporaryHead = HeadContainer.getCompleteHeadList().get(0);

        eepromCommsError = new ModalDialog();
        eepromCommsError.setTitle(DisplayManager.getLanguageBundle().getString("eeprom.error"));
        eepromCommsError.addButton(DisplayManager.getLanguageBundle().getString("dialogs.ok"));

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
                    float nozzle1OverrunValue = Float.valueOf(t1);
                    float nozzle2OverrunValue = Float.valueOf(nozzle2ZOverrun.getText());
                    temporaryHead.setNozzle1_Z_overrun(nozzle1OverrunValue);
                    temporaryHead.setNozzle2_Z_overrun(nozzle2OverrunValue);
                    temporaryHead.deriveZOffsetsFromOverrun();
                    nozzle1ZOffsetCalculated = temporaryHead.getNozzle1ZOffset();
                    nozzle2ZOffsetCalculated = temporaryHead.getNozzle2ZOffset();
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
                    float nozzle1OverrunValue = Float.valueOf(nozzle1ZOverrun.getText());
                    float nozzle2OverrunValue = Float.valueOf(t1);
                    temporaryHead.setNozzle1_Z_overrun(nozzle1OverrunValue);
                    temporaryHead.setNozzle2_Z_overrun(nozzle2OverrunValue);
                    temporaryHead.deriveZOffsetsFromOverrun();
                    nozzle1ZOffsetCalculated = temporaryHead.getNozzle1ZOffset();
                    nozzle2ZOffsetCalculated = temporaryHead.getNozzle2ZOffset();
//                    nozzle1ZOffset.setText(String.format("%.2f", temporaryHead.getNozzle1ZOffset()));
//                    nozzle2ZOffset.setText(String.format("%.2f", temporaryHead.getNozzle2ZOffset()));
                } catch (NumberFormatException ex)
                {
                    steno.error("Error parsing nozzle overrun string value");
                }
            }
        });

        headTypeCombo.setItems(HeadContainer.getCompleteHeadList());
        headTypeCombo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener()
        {
            @Override
            public void changed(ObservableValue ov, Object t, Object t1)
            {
                updateFieldsFromSelectedHead();
            }
        });
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void unbindFromPrinter(Printer printer)
    {
        if (connectedPrinter != null)
        {

            connectedPrinter.getHeadDataChangedToggle().removeListener(headDataChangeListener);

            headFullContainer.visibleProperty().unbind();

            connectedPrinter.headEEPROMStatusProperty().removeListener(headAttachListener);

            connectedPrinter = null;

//            configurationPage.setVisible(false);
        }

    }

    private void bindToPrinter(Printer printer)
    {
        if (connectedPrinter == null)
        {
            connectedPrinter = printer;
            connectedPrinter.getHeadDataChangedToggle().addListener(headDataChangeListener);

            headFullContainer.visibleProperty().bind(
                connectedPrinter.headEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED));

            connectedPrinter.headEEPROMStatusProperty().addListener(headAttachListener);

//            configurationPage.setVisible(true);
        }
    }

    private void updateFieldsFromSelectedHead()
    {
        Head selectedHead = getSelectedHead();
        if (selectedHead != null)
        {
            headTypeCode.setText(selectedHead.getTypeCode());
            headMaxTemperature.setText(String.format("%.0f", selectedHead.getMaximumTemperature()));
            headThermistorBeta.setText(String.format("%.2f", selectedHead.getBeta()));
            headThermistorTCal.setText(String.format("%.2f", selectedHead.getTCal()));
            lastFilamentTemperature.setText(String.format("%.0f",
                                                          selectedHead.getLastFilamentTemperature()));
            headHourCounter.setText(String.format("%.2f", selectedHead.getHeadHours()));
            updateOffsetFieldsForHead(selectedHead);
        }
    }

    private void updateOffsetFieldsForHead(Head selectedHead)
    {
        nozzle1BOffset.setText(String.format("%.2f", selectedHead.getNozzle1BOffset()));
        nozzle1XOffset.setText(String.format("%.2f", selectedHead.getNozzle1XOffset()));
        nozzle1YOffset.setText(String.format("%.2f", selectedHead.getNozzle1YOffset()));
        nozzle1ZOffsetCalculated = selectedHead.getNozzle1ZOffset();
//            nozzle1ZOffset.setText(String.format("%.2f", selectedHead.getNozzle1ZOffset()));
        nozzle2BOffset.setText(String.format("%.2f", selectedHead.getNozzle2BOffset()));
        nozzle2XOffset.setText(String.format("%.2f", selectedHead.getNozzle2XOffset()));
        nozzle2YOffset.setText(String.format("%.2f", selectedHead.getNozzle2YOffset()));
        nozzle2ZOffsetCalculated = selectedHead.getNozzle2ZOffset();
//            nozzle2ZOffset.setText(String.format("%.2f", selectedHead.getNozzle2ZOffset()));
        selectedHead.deriveZOverrunFromOffsets();
        nozzle1ZOverrun.setText(String.format("%.2f", selectedHead.getNozzle1ZOverrun()));
        nozzle2ZOverrun.setText(String.format("%.2f", selectedHead.getNozzle2ZOverrun()));
    }

    /**
     * Get the standard head details for the selected head type.
     */
    private Head getSelectedHead()
    {
        Head selectedHead = (Head) (headTypeCombo.getSelectionModel().selectedItemProperty().get());
        return selectedHead;
    }

    private void updateFieldsFromAttachedHead()
    {
        headTypeCode.setText(connectedPrinter.getHeadTypeCode().get().trim());
        headTypeCombo.getSelectionModel().select(connectedPrinter.attachedHeadProperty().get());
        headUniqueID.setText(connectedPrinter.getHeadUniqueID().get().trim());
        lastFilamentTemperature.setText(String.format("%.0f",
                                                      connectedPrinter.getLastFilamentTemperature().get()));
        headHourCounter.setText(String.format("%.2f", connectedPrinter.getHeadHoursCounter().get()));
        headMaxTemperature.setText(String.format("%.0f",
                                                 connectedPrinter.getHeadMaximumTemperature().get()));
        headThermistorBeta.setText(String.format("%.2f",
                                                 connectedPrinter.getHeadThermistorBeta().get()));
        headThermistorTCal.setText(String.format("%.2f",
                                                 connectedPrinter.getHeadThermistorTCal().get()));
        nozzle1BOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle1BOffset().get()));
        nozzle1XOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle1XOffset().get()));
        nozzle1YOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle1YOffset().get()));
        nozzle1ZOffsetCalculated = connectedPrinter.getHeadNozzle1ZOffset().get();
//        nozzle1ZOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle1ZOffset().get()));
        nozzle2BOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle2BOffset().get()));
        nozzle2XOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle2XOffset().get()));
        nozzle2YOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle2YOffset().get()));
        nozzle2ZOffsetCalculated = connectedPrinter.getHeadNozzle2ZOffset().get();
//        nozzle2ZOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle2ZOffset().get()));

        connectedPrinter.attachedHeadProperty().get().deriveZOverrunFromOffsets();
        nozzle1ZOverrun.setText(String.format("%.2f",
                                              connectedPrinter.attachedHeadProperty().get().getNozzle1ZOverrun()));
        nozzle2ZOverrun.setText(String.format("%.2f",
                                              connectedPrinter.attachedHeadProperty().get().getNozzle2ZOverrun()));
    }
}
