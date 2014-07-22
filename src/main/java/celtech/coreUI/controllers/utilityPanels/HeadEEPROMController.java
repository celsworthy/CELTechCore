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
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
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
    private ComboBox headTypeCombo;

    @FXML
    private TextField headHourCounter;

    @FXML
    private TextField headMaxTemperature;

    @FXML
    private RestrictedTextField lastFilamentTemperature;

    @FXML
    private TextField headThermistorBeta;

    @FXML
    private TextField headThermistorTCal;

    @FXML
    private TextField headTypeCode;

    @FXML
    private TextField headUniqueID;

    @FXML
    private TextField nozzle1BOffset;

    @FXML
    private TextField nozzle1XOffset;

    @FXML
    private TextField nozzle1YOffset;

    @FXML
    private TextField nozzle1ZOverrun;

    @FXML
    private TextField nozzle2BOffset;

    @FXML
    private TextField nozzle2XOffset;

    @FXML
    private TextField nozzle2YOffset;

    @FXML
    private TextField nozzle2ZOverrun;

    @FXML
    private GridPane headEEPROMControls;


//    private BooleanProperty fastUpdates = new SimpleBooleanProperty(false);
    private Head temporaryHead = null;
    
    private Float nozzle1ZOffset;
    private Float nozzle2ZOffset;

    @FXML
    void writeHeadConfig(ActionEvent event)
    {
        try
        {
            connectedPrinter.transmitWriteHeadEEPROM(
                headTypeCode.getText(), headUniqueID.getText(), Float.valueOf(
                    headMaxTemperature.getText()), Float.valueOf(headThermistorBeta.getText()),
                Float.valueOf(headThermistorTCal.getText()), Float.valueOf(nozzle1XOffset.getText()),
                Float.valueOf(nozzle1YOffset.getText()), nozzle1ZOffset,
                Float.valueOf(nozzle1BOffset.getText()),
                Float.valueOf(nozzle2XOffset.getText()), Float.valueOf(nozzle2YOffset.getText()),
                nozzle2ZOffset, Float.valueOf(nozzle2BOffset.getText()),
                Float.valueOf(lastFilamentTemperature.getText()),
                Float.valueOf(headHourCounter.getText()));
        } catch (RoboxCommsException ex)
        {
            steno.error("Error writing reel EEPROM");
            eepromCommsError.setMessage(DisplayManager.getLanguageBundle().getString(
                "eeprom.headWriteError"));
            eepromCommsError.show();
        }
        readHeadConfig(event);
    }

    @FXML
    void resetHeadConfig(ActionEvent event)
    {
        updateHeadFieldsFromSelectedHead();
        try
        {
            connectedPrinter.transmitWriteHeadEEPROM(
                headTypeCode.getText(), headUniqueID.getText(),
                Float.valueOf(headMaxTemperature.getText()),
                Float.valueOf(headThermistorBeta.getText()),
                Float.valueOf(headThermistorTCal.getText()),
                Float.valueOf(nozzle1XOffset.getText()),
                Float.valueOf(nozzle1YOffset.getText()),
                nozzle1ZOffset,
                Float.valueOf(nozzle1BOffset.getText()),
                Float.valueOf(nozzle2XOffset.getText()),
                Float.valueOf(nozzle2YOffset.getText()),
                nozzle2ZOffset,
                Float.valueOf(nozzle2BOffset.getText()),
                Float.valueOf(lastFilamentTemperature.getText()),
                Float.valueOf(headHourCounter.getText()));
        } catch (RoboxCommsException ex)
        {
            steno.error("Error writing head EEPROM");
            eepromCommsError.setMessage(DisplayManager.getLanguageBundle().getString(
                "eeprom.headWriteError"));
            eepromCommsError.show();
        }
        readHeadConfig(event);
    }

    @FXML
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

    @FXML
    void formatHeadEEPROM(ActionEvent event)
    {
        try
        {
            connectedPrinter.transmitFormatHeadEEPROM();
        } catch (RoboxCommsException ex)
        {
            steno.error("Error formatting head EEPROM");
            eepromCommsError.setMessage(DisplayManager.getLanguageBundle().getString(
                "eeprom.headWriteError"));
            eepromCommsError.show();
        }
        readHeadConfig(null);
    }

    @FXML
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

    private ObservableList<Printer> printerStatusList = null;
    //We'll only deal with the first printer we find.
    private Printer connectedPrinter = null;

    private ChangeListener<EEPROMState> headAttachListener = null;
    private ChangeListener<Boolean> headDataChangeListener = null;
    private ChangeListener<Boolean> printerIDDataChangeListener = null;

    private StatusScreenState statusScreenState = null;
    private ModalDialog eepromCommsError = null;

    private RoboxCommsManager commsManager = null;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        temporaryHead = HeadContainer.getCompleteHeadList().get(0);
        statusScreenState = StatusScreenState.getInstance();
        commsManager = RoboxCommsManager.getInstance();

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

//        reelDataChangeListener = new ChangeListener<Boolean>()
//        {
//            @Override
//            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
//            {
//                materialTypeCombo.getSelectionModel().clearSelection();
//                updateFilamentList();
//                updateReelFieldsFromLoadedReel();
//            }
//        };
        headDataChangeListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                updateHeadFieldsFromAttachedHead();
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
                    nozzle1ZOffset = temporaryHead.getNozzle1ZOffset();
                    nozzle2ZOffset = temporaryHead.getNozzle2ZOffset();
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
                    nozzle1ZOffset = temporaryHead.getNozzle1ZOffset();
                    nozzle2ZOffset = temporaryHead.getNozzle2ZOffset();
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
                updateHeadFieldsFromSelectedHead();
            }
        });

    }

    private void unbindFromPrinter(Printer printer)
    {
        if (connectedPrinter != null)
        {

            connectedPrinter.getHeadDataChangedToggle().removeListener(headDataChangeListener);
            connectedPrinter.getPrinterIDDataChangedToggle().removeListener(
                printerIDDataChangeListener);

            headEEPROMControls.visibleProperty().unbind();

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
            connectedPrinter.getPrinterIDDataChangedToggle().addListener(printerIDDataChangeListener);

            headEEPROMControls.visibleProperty().bind(
                connectedPrinter.headEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED));

            connectedPrinter.headEEPROMStatusProperty().addListener(headAttachListener);

//            configurationPage.setVisible(true);
        }
    }

    private void updateHeadFieldsFromSelectedHead()
    {
        Head selectedHead = (Head) (headTypeCombo.getSelectionModel().selectedItemProperty().get());
        if (selectedHead != null)
        {
            headTypeCode.setText(selectedHead.getTypeCode());
            headMaxTemperature.setText(String.format("%.0f", selectedHead.getMaximumTemperature()));
            headThermistorBeta.setText(String.format("%.2f", selectedHead.getBeta()));
            headThermistorTCal.setText(String.format("%.2f", selectedHead.getTCal()));
            nozzle1BOffset.setText(String.format("%.2f", selectedHead.getNozzle1BOffset()));
            nozzle1XOffset.setText(String.format("%.2f", selectedHead.getNozzle1XOffset()));
            nozzle1YOffset.setText(String.format("%.2f", selectedHead.getNozzle1YOffset()));
            nozzle1ZOffset = selectedHead.getNozzle1ZOffset();
//            nozzle1ZOffset.setText(String.format("%.2f", selectedHead.getNozzle1ZOffset()));
            nozzle2BOffset.setText(String.format("%.2f", selectedHead.getNozzle2BOffset()));
            nozzle2XOffset.setText(String.format("%.2f", selectedHead.getNozzle2XOffset()));
            nozzle2YOffset.setText(String.format("%.2f", selectedHead.getNozzle2YOffset()));
            nozzle2ZOffset = selectedHead.getNozzle2ZOffset();
//            nozzle2ZOffset.setText(String.format("%.2f", selectedHead.getNozzle2ZOffset()));
            lastFilamentTemperature.setText(String.format("%.0f",
                                                          selectedHead.getLastFilamentTemperature()));
            headHourCounter.setText(String.format("%.2f", selectedHead.getHeadHours()));
            selectedHead.deriveZOverrunFromOffsets();
            nozzle1ZOverrun.setText(String.format("%.2f", selectedHead.getNozzle1ZOverrun()));
            nozzle2ZOverrun.setText(String.format("%.2f", selectedHead.getNozzle2ZOverrun()));
        }
    }

    private void updateHeadFieldsFromAttachedHead()
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
        nozzle1ZOffset = connectedPrinter.getHeadNozzle1ZOffset().get();
//        nozzle1ZOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle1ZOffset().get()));
        nozzle2BOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle2BOffset().get()));
        nozzle2XOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle2XOffset().get()));
        nozzle2YOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle2YOffset().get()));
        nozzle2ZOffset = connectedPrinter.getHeadNozzle2ZOffset().get();
//        nozzle2ZOffset.setText(String.format("%.2f", connectedPrinter.getHeadNozzle2ZOffset().get()));

        connectedPrinter.attachedHeadProperty().get().deriveZOverrunFromOffsets();
        nozzle1ZOverrun.setText(String.format("%.2f",
                                              connectedPrinter.attachedHeadProperty().get().getNozzle1ZOverrun()));
        nozzle2ZOverrun.setText(String.format("%.2f",
                                              connectedPrinter.attachedHeadProperty().get().getNozzle2ZOverrun()));
    }
}
