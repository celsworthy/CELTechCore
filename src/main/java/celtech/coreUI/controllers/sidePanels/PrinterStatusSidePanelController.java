/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.sidePanels;

import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.configuration.HeaterMode;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.PrinterStatusListCell;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.utils.PrinterUtils;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusSidePanelController implements Initializable, SidePanelManager
{

    private final Stenographer steno = StenographerFactory.getStenographer(PrinterStatusSidePanelController.class.getName());
    private ApplicationStatus applicationStatus = null;
    private PrinterUtils printerUtils = null;

    @FXML
    private HBox myContainer;

    @FXML
    private HBox slideout;

    @FXML
    private PrinterStatusSlideOutPanelController slideoutController;

    @FXML
    private ListView printerStatusTable;

    @FXML
    private Label ambientTemperatureLabel;

    @FXML
    private TextField ambientTargetTemperature;

    @FXML
    private Label bedTemperatureLabel;

    @FXML
    private TextField bedTargetTemperature;

    @FXML
    private TextField bedFirstLayerTargetTemperature;

    @FXML
    private Label bedTemperaturePlaceholder;

    @FXML
    private Label filamentStatusLabel;

    @FXML
    private Label filamentStatusValueLabel;

    @FXML
    private Label nozzleTemperatureLabel;

    @FXML
    private TextField nozzleTargetTemperature;

    @FXML
    private TextField nozzleFirstLayerTargetTemperature;

    @FXML
    private Label nozzleTemperaturePlaceholder;

    @FXML
    private Label printHeadLabel;

    @FXML
    private Label printHeadValueLabel;

    @FXML
    private HBox printerStatusHBox;

    @FXML
    private VBox temperatureVBox;

    @FXML
    private Label targetTempLabel;
    @FXML
    private HBox targetNozzleTempHBox;
    @FXML
    private HBox targetBedTempHBox;
    @FXML
    private Label heaterStatusLabel;

    @FXML
    private CheckBox nozzleHeaterCheckBox;
    @FXML
    private CheckBox bedHeaterCheckBox;

    @FXML
    private HBox targetAmbientTempHBox;

    @FXML
    private LineChart<Number, Number> temperatureChart;

    @FXML
    private NumberAxis temperatureAxis;
    private NumberAxis timeAxis;

    private ChangeListener<Number> graphStartListener = null;
    private ChangeListener<Number> graphEndListener = null;

    private TableColumn printerNameColumn = new TableColumn();
    private SelectionModel printerStatusTableSelectionModel = null;

    private ObservableList<Printer> printerStatusList = null;
    private StatusScreenState statusScreenState = null;

    private String offString;
    private String onString;
    private String openString;
    private String closedString;
    private String filamentLoadedString;
    private String filamentNotLoadedString;
    private String connectedString;
    private String notConnectedString;
    private String headNotAttachedString;
    private String tempOutOfRangeHighString;
    private String tempOutOfRangeLowString;

    private PrinterStatusSlideOutPanelController slideOutController = null;

    private PrinterIDDialog printerIDDialog = null;

    private ChangeListener<Number> targetNozzleFirstLayerTempListener = null;
    private ChangeListener<Number> targetNozzleTempListener = null;
    private ChangeListener<Number> targetBedFirstLayerTempListener = null;
    private ChangeListener<Number> targetBedTempListener = null;
    private ChangeListener<Number> targetAmbientTempListener = null;

    private ChangeListener<Boolean> nozzleHeaterCheckBoxListener = null;
    private ChangeListener<Boolean> bedHeaterCheckBoxListener = null;
    private ChangeListener<HeaterMode> bedHeaterStatusListener = null;
    private ChangeListener<HeaterMode> nozzleHeaterStatusListener = null;

    private Printer lastSelectedPrinter = null;

    private DisplayManager displayManager = null;

    private final int MAX_DATA_POINTS = 180;

    private LineChart.Series<Number, Number> currentAmbientTemperatureHistory = null;

    private ListChangeListener<XYChart.Data<Number, Number>> graphDataPointChangeListener = new ListChangeListener<XYChart.Data<Number, Number>>()
    {
        @Override
        public void onChanged(ListChangeListener.Change<? extends XYChart.Data<Number, Number>> change)
        {
            while (change.next())
            {
                if (change.wasAdded() || change.wasRemoved())
                {
                    timeAxis.setLowerBound(currentAmbientTemperatureHistory.getData().size() - MAX_DATA_POINTS);
                    timeAxis.setUpperBound(currentAmbientTemperatureHistory.getData().size());
                } else if (change.wasReplaced())
                {
                } else if (change.wasUpdated())
                {
                }
            }
        }
    };

    @FXML
    void setNozzleFirstLayerTargetTemp(ActionEvent event)
    {
        if (lastSelectedPrinter != null)
        {
            try
            {
                lastSelectedPrinter.transmitDirectGCode(GCodeConstants.setFirstLayerNozzleTemperatureTarget + Integer.valueOf(nozzleFirstLayerTargetTemperature.getText()), false);
            } catch (NumberFormatException ex)
            {
                steno.error("Couldn't translate value to float");
            } catch (RoboxCommsException ex)
            {
                steno.error("Error whilst sending new temperature to printer");
            }
        }
    }

    @FXML
    void setNozzleTargetTemp(ActionEvent event)
    {
        if (lastSelectedPrinter != null)
        {
            try
            {
                lastSelectedPrinter.transmitDirectGCode(GCodeConstants.setNozzleTemperatureTarget + Integer.valueOf(nozzleTargetTemperature.getText()), false);
            } catch (NumberFormatException ex)
            {
                steno.error("Couldn't translate value to float");
            } catch (RoboxCommsException ex)
            {
                steno.error("Error whilst sending new temperature to printer");
            }
        }
    }

    @FXML
    void setAmbientTargetTemp(ActionEvent event)
    {
        if (lastSelectedPrinter != null)
        {
            try
            {
                lastSelectedPrinter.transmitDirectGCode(GCodeConstants.setAmbientTemperature + Integer.valueOf(ambientTargetTemperature.getText()), false);
            } catch (NumberFormatException ex)
            {
                steno.error("Couldn't translate value to float");
            } catch (RoboxCommsException ex)
            {
                steno.error("Error whilst sending new temperature to printer");
            }
        }
    }

    @FXML
    void setBedFirstLayerTargetTemp(ActionEvent event)
    {
        if (lastSelectedPrinter != null)
        {
            try
            {
                lastSelectedPrinter.transmitDirectGCode(GCodeConstants.setFirstLayerBedTemperatureTarget + Float.valueOf(bedFirstLayerTargetTemperature.getText()), false);
            } catch (NumberFormatException ex)
            {
                steno.error("Couldn't translate value to float");
            } catch (RoboxCommsException ex)
            {
                steno.error("Error whilst sending new temperature to printer");
            }
        }
    }

    @FXML
    void setBedTargetTemp(ActionEvent event)
    {
        if (lastSelectedPrinter != null)
        {
            try
            {
                lastSelectedPrinter.transmitDirectGCode(GCodeConstants.setBedTemperatureTarget + Float.valueOf(bedTargetTemperature.getText()), false);
            } catch (NumberFormatException ex)
            {
                steno.error("Couldn't translate value to float");
            } catch (RoboxCommsException ex)
            {
                steno.error("Error whilst sending new temperature to printer");
            }
        }
    }

    private PrinterColourMap colourMap = PrinterColourMap.getInstance();

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        applicationStatus = ApplicationStatus.getInstance();
        displayManager = DisplayManager.getInstance();
        RoboxCommsManager commsManager = RoboxCommsManager.getInstance();
        printerStatusList = commsManager.getPrintStatusList();
        statusScreenState = StatusScreenState.getInstance();
        printerUtils = PrinterUtils.getInstance();

        timeAxis = new NumberAxis(0, MAX_DATA_POINTS, 30);
        timeAxis.setForceZeroInRange(false);
        timeAxis.setAutoRanging(true);

        temperatureAxis = new NumberAxis();
        temperatureAxis.setAutoRanging(false);

        temperatureChart.setAnimated(false);
        temperatureChart.setLegendVisible(false);
        temperatureChart.setLegendSide(Side.RIGHT);

        temperatureChart.setVisible(false);

        printerIDDialog = new PrinterIDDialog();

        ResourceBundle languageBundle = DisplayManager.getLanguageBundle();
        offString = languageBundle.getString("genericFirstLetterCapitalised.Off");
        onString = languageBundle.getString("genericFirstLetterCapitalised.On");
        openString = languageBundle.getString("genericFirstLetterCapitalised.Open");
        closedString = languageBundle.getString("genericFirstLetterCapitalised.Closed");
        filamentLoadedString = languageBundle.getString("sidePanel_printerStatus.filamentLoaded");
        filamentNotLoadedString = languageBundle.getString("smartReelProgrammer.noReelLoaded");
        connectedString = languageBundle.getString("sidePanel_printerStatus.connected");
        notConnectedString = languageBundle.getString("sidePanel_printerStatus.notConnected");
        headNotAttachedString = languageBundle.getString("sidePanel_printerStatus.headNotAttached");
        tempOutOfRangeHighString = languageBundle.getString("printerStatus.tempOutOfRangeHigh");
        tempOutOfRangeLowString = languageBundle.getString("printerStatus.tempOutOfRangeLow");

        printerNameColumn.setText(languageBundle.getString("sidePanel_printerStatus.printerNameColumn"));
        printerNameColumn.setPrefWidth(300);
        printerNameColumn.setCellValueFactory(new PropertyValueFactory<Printer, String>("printerFriendlyName"));

//        printerStatusTable.getColumns().addAll(printerNameColumn);
//        printerStatusTable.setEditable(false);
//        printerStatusTable.getSortOrder().add(printerNameColumn);
        printerStatusTableSelectionModel = printerStatusTable.getSelectionModel();
        printerStatusTable.setItems(printerStatusList);
        printerStatusTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        printerStatusTable.setCellFactory(new Callback<ListView<Printer>, ListCell<Printer>>()
        {
            @Override
            public ListCell<Printer> call(ListView<Printer> list)
            {
                return new PrinterStatusListCell();
            }
        }
        );

        printerStatusTable.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent event)
            {
                if (event.getClickCount() > 1)
                {
                    Printer printerToEdit = (Printer) printerStatusTableSelectionModel.getSelectedItem();
                    if (printerToEdit != null)
                    {
                        printerIDDialog.setPrinterToUse(printerToEdit);
                        printerIDDialog.setChosenDisplayColour(colourMap.printerToDisplayColour(printerToEdit.getPrinterColour()));
                        printerIDDialog.setChosenPrinterName(printerToEdit.getPrinterFriendlyName());
                        boolean okPressed = printerIDDialog.show();

                        if (okPressed)
                        {
                            try
                            {
                                printerToEdit.transmitWritePrinterID(printerToEdit.getPrintermodel().get(), printerToEdit.getPrinteredition().get(),
                                                                     printerToEdit.getPrinterweekOfManufacture().get(), printerToEdit.getPrinteryearOfManufacture().get(),
                                                                     printerToEdit.getPrinterpoNumber().get(), printerToEdit.getPrinterUniqueID(),
                                                                     printerToEdit.getPrintercheckByte().get(), printerIDDialog.getChosenPrinterName(), colourMap.displayToPrinterColour(printerIDDialog.getChosenDisplayColour()));
                                printerToEdit.transmitReadPrinterID();
                            } catch (RoboxCommsException ex)
                            {
                                steno.error("Error writing printer ID");
                            }
                        }
                    }
                }
            }
        });

        Label noPrinterLabel = new Label();
        noPrinterLabel.setText(languageBundle.getString("sidePanel_printerStatus.noPrinterPlaceholder"));

        printerStatusTable.setPlaceholder(noPrinterLabel);

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
                            printerStatusTableSelectionModel.select(additem);
                        }
                    } else if (change.wasRemoved())
                    {
                        for (Printer additem : change.getRemoved())
                        {
                        }
                    } else if (change.wasReplaced())
                    {
                    } else if (change.wasUpdated())
                    {
                    }
                }
            }
        });

        controlDetailsVisibility();

        printerStatusTableSelectionModel.selectedItemProperty().addListener(new ChangeListener<Printer>()
        {

            @Override
            public void changed(ObservableValue<? extends Printer> ov, Printer t, Printer latestSelection)
            {
                if (latestSelection != null
                        || printerStatusList.size() > 0)
                {
                    statusScreenState.setCurrentlySelectedPrinter(latestSelection);
                    bindDetails(latestSelection);
                } else
                {
                    statusScreenState.setCurrentlySelectedPrinter(latestSelection);
                }

                controlDetailsVisibility();
            }
        });

//        statusScreenState.modeProperty().bind(Bindings.when(advancedControlsToggle.selectedProperty().and(printerStatusTableSelectionModel.selectedItemProperty().isNotNull())).then(StatusScreenMode.ADVANCED).otherwise(StatusScreenMode.NORMAL));
        targetAmbientTempListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                if (ambientTargetTemperature.isFocused() == false)
                {
                    ambientTargetTemperature.setText(String.format("%d", t1.intValue()));
                }
            }
        };

        targetNozzleFirstLayerTempListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                if (nozzleFirstLayerTargetTemperature.isFocused() == false)
                {
                    nozzleFirstLayerTargetTemperature.setText(String.format("%d", t1.intValue()));
                }
            }
        };
        targetNozzleTempListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                if (nozzleTargetTemperature.isFocused() == false)
                {
                    nozzleTargetTemperature.setText(String.format("%d", t1.intValue()));
                }
            }
        };
        targetBedTempListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                if (bedTargetTemperature.isFocused() == false)
                {
                    bedTargetTemperature.setText(String.format("%d", t1.intValue()));
                }
            }
        };

        targetBedFirstLayerTempListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                if (bedFirstLayerTargetTemperature.isFocused() == false)
                {
                    bedFirstLayerTargetTemperature.setText(String.format("%d", t1.intValue()));
                }
            }
        };

//        targetTempLabel.visibleProperty().bind(advancedControlsToggle.selectedProperty());
//        heaterStatusLabel.visibleProperty().bind(advancedControlsToggle.selectedProperty());
//
//        targetAmbientTempHBox.visibleProperty().bind(advancedControlsToggle.selectedProperty());
        ambientTargetTemperature.focusedProperty().addListener(new ChangeListener<Boolean>()
        {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == false && lastSelectedPrinter != null)
                {
                    ambientTargetTemperature.setText(String.format("%d", lastSelectedPrinter.getAmbientTargetTemperature()));
                }
            }
        });

//        targetBedTempHBox.visibleProperty().bind(advancedControlsToggle.selectedProperty());
//        bedHeaterHBox.visibleProperty().bind(advancedControlsToggle.selectedProperty());
        bedFirstLayerTargetTemperature.focusedProperty().addListener(new ChangeListener<Boolean>()
        {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == false && lastSelectedPrinter != null)
                {
                    bedFirstLayerTargetTemperature.setText(String.format("%d", lastSelectedPrinter.getBedFirstLayerTargetTemperature()));
                }
            }
        });

        bedTargetTemperature.focusedProperty().addListener(new ChangeListener<Boolean>()
        {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == false && lastSelectedPrinter != null)
                {
                    bedTargetTemperature.setText(String.format("%d", lastSelectedPrinter.getBedTargetTemperature()));
                }
            }
        });

        bedHeaterCheckBoxListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldHeaterOnDemand, Boolean heaterOnDemand)
            {
                if (heaterOnDemand == true)
                {
                    try
                    {
                        if (lastSelectedPrinter != null)
                        {
                            if (lastSelectedPrinter.getBedHeaterMode() == HeaterMode.OFF)
                            {
                                lastSelectedPrinter.transmitDirectGCode(GCodeConstants.goToTargetBedTemperature, false);
                            }
                        }
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error whilst setting bed target temperature");
                    }
                } else
                {
                    try
                    {
                        if (lastSelectedPrinter != null)
                        {
                            if (lastSelectedPrinter.getBedHeaterMode() != HeaterMode.OFF)
                            {
                                lastSelectedPrinter.transmitDirectGCode(GCodeConstants.switchBedHeaterOff, false);
                            }
                        }
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error whilst setting bed target temperature");
                    }
                }
            }
        };

        nozzleHeaterCheckBoxListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1 == true)
                {
                    try
                    {
                        if (lastSelectedPrinter != null)
                        {
                            if (lastSelectedPrinter.getNozzleHeaterMode() == HeaterMode.OFF)
                            {
                                printerUtils.offerPurgeIfNecessary(lastSelectedPrinter);
                                lastSelectedPrinter.transmitDirectGCode(GCodeConstants.goToTargetNozzleTemperature, false);
                            }
                        }
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error whilst setting nozzle target temperature");
                    }
                } else
                {
                    try
                    {
                        if (lastSelectedPrinter != null)
                        {
                            if (lastSelectedPrinter.getNozzleHeaterMode() != HeaterMode.OFF)
                            {
                                lastSelectedPrinter.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
                            }
                        }
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error whilst setting nozzle target temperature");
                    }
                }
            }
        };
//        targetNozzleTempHBox.visibleProperty()
//                .bind(advancedControlsToggle.selectedProperty());
//        nozzleHeaterHBox.visibleProperty()
//                .bind(advancedControlsToggle.selectedProperty());
        nozzleFirstLayerTargetTemperature.focusedProperty()
                .addListener(new ChangeListener<Boolean>()
                        {
                            @Override
                            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
                            {
                                if (t1.booleanValue() == false && lastSelectedPrinter != null)
                                {
                                    nozzleFirstLayerTargetTemperature.setText(String.format("%d", lastSelectedPrinter.getNozzleFirstLayerTargetTemperature()));
                                }
                            }
                }
                );
        nozzleTargetTemperature.focusedProperty()
                .addListener(new ChangeListener<Boolean>()
                        {

                            @Override
                            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
                            {
                                if (t1.booleanValue() == false && lastSelectedPrinter != null)
                                {
                                    nozzleTargetTemperature.setText(String.format("%d", lastSelectedPrinter.getNozzleTargetTemperature()));
                                }
                            }
                }
                );

//        advancedControlsToggle.selectedProperty()
//                .addListener(new ChangeListener<Boolean>()
//                        {
//
//                            @Override
//                            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1
//                            )
//                            {
//                                displayManager.showAdvancedStatusPanel(t1.booleanValue());
//                            }
//                }
//                );
        bedHeaterStatusListener = new ChangeListener<HeaterMode>()
        {
            @Override
            public void changed(ObservableValue<? extends HeaterMode> ov, HeaterMode oldValue, HeaterMode newValue)
            {
                bedHeaterCheckBox.setSelected(newValue != HeaterMode.OFF);
            }
        };

        nozzleHeaterStatusListener = new ChangeListener<HeaterMode>()
        {
            @Override
            public void changed(ObservableValue<? extends HeaterMode> ov, HeaterMode oldValue, HeaterMode newValue)
            {
                nozzleHeaterCheckBox.setSelected(newValue != HeaterMode.OFF);
            }
        };

    }

    private void bindDetails(Printer selectedPrinter)
    {
        nozzleTemperatureLabel.textProperty().unbind();
        bedTemperatureLabel.textProperty().unbind();
        ambientTemperatureLabel.textProperty().unbind();
//        doorStatusLabel.textProperty().unbind();
        filamentStatusLabel.textProperty().unbind();

        if (lastSelectedPrinter != null)
        {
            lastSelectedPrinter.nozzleFirstLayerTargetTemperatureProperty().unbind();
            lastSelectedPrinter.nozzleTargetTemperatureProperty().unbind();
            lastSelectedPrinter.bedFirstLayerTargetTemperatureProperty().unbind();
            lastSelectedPrinter.bedTargetTemperatureProperty().unbind();
            lastSelectedPrinter.ambientTargetTemperatureProperty().unbind();

            temperatureChart.getData().remove(lastSelectedPrinter.ambientTemperatureHistory());
            currentAmbientTemperatureHistory = null;
            temperatureChart.getData().remove(lastSelectedPrinter.bedTemperatureHistory());
            temperatureChart.getData().remove(lastSelectedPrinter.nozzleTemperatureHistory());
            temperatureChart.getData().remove(lastSelectedPrinter.ambientTargetTemperatureHistory());
            temperatureChart.getData().remove(lastSelectedPrinter.bedTargetTemperatureHistory());
            temperatureChart.getData().remove(lastSelectedPrinter.nozzleTargetTemperatureHistory());

            nozzleFirstLayerTargetTemperature.visibleProperty().unbind();
            nozzleTargetTemperature.visibleProperty().unbind();
            nozzleTemperaturePlaceholder.visibleProperty().unbind();
            bedFirstLayerTargetTemperature.visibleProperty().unbind();
            bedTargetTemperature.visibleProperty().unbind();
            bedTemperaturePlaceholder.visibleProperty().unbind();

            bedHeaterCheckBox.selectedProperty()
                    .removeListener(bedHeaterCheckBoxListener);
            nozzleHeaterCheckBox.selectedProperty()
                    .removeListener(nozzleHeaterCheckBoxListener);
            lastSelectedPrinter.getBedHeaterModeProperty().removeListener(bedHeaterStatusListener);
            lastSelectedPrinter.getNozzleHeaterModeProperty().removeListener(nozzleHeaterStatusListener);
        }

        if (selectedPrinter != null)
        {
            // Temperatures / Heaters / Fans
            nozzleTemperatureLabel.textProperty().bind(Bindings.when(selectedPrinter.printerConnectedProperty().not()).then("-").otherwise(Bindings.when(selectedPrinter.extruderTemperatureProperty().greaterThan(ApplicationConfiguration.maxTempToDisplayOnGraph)).then(tempOutOfRangeHighString)
                    .otherwise(Bindings.when(selectedPrinter.extruderTemperatureProperty().lessThan(ApplicationConfiguration.minTempToDisplayOnGraph)).then(tempOutOfRangeLowString).otherwise(selectedPrinter.extruderTemperatureProperty().asString("%d°C")))));
            nozzleFirstLayerTargetTemperature.setText(String.format("%d", selectedPrinter.getNozzleFirstLayerTargetTemperature()));
            selectedPrinter.nozzleFirstLayerTargetTemperatureProperty().addListener(targetNozzleFirstLayerTempListener);
            nozzleTargetTemperature.setText(String.format("%d", selectedPrinter.getNozzleTargetTemperature()));
            selectedPrinter.nozzleTargetTemperatureProperty().addListener(targetNozzleTempListener);
            nozzleFirstLayerTargetTemperature.visibleProperty().bind(selectedPrinter.getNozzleHeaterModeProperty().isEqualTo(HeaterMode.FIRST_LAYER));
            nozzleTargetTemperature.visibleProperty().bind(selectedPrinter.getNozzleHeaterModeProperty().isEqualTo(HeaterMode.NORMAL));
            nozzleTemperaturePlaceholder.visibleProperty().bind(selectedPrinter.getNozzleHeaterModeProperty().isEqualTo(HeaterMode.OFF));

            bedTemperatureLabel.textProperty().bind(Bindings.when(selectedPrinter.printerConnectedProperty().not()).then("-").otherwise(Bindings.when(selectedPrinter.bedTemperatureProperty().greaterThan(ApplicationConfiguration.maxTempToDisplayOnGraph)).then(tempOutOfRangeHighString)
                    .otherwise(Bindings.when(selectedPrinter.bedTemperatureProperty().lessThan(ApplicationConfiguration.minTempToDisplayOnGraph)).then(tempOutOfRangeLowString).otherwise(selectedPrinter.bedTemperatureProperty().asString("%d°C")))));
            bedFirstLayerTargetTemperature.setText(String.format("%d", selectedPrinter.getBedFirstLayerTargetTemperature()));
            selectedPrinter.bedFirstLayerTargetTemperatureProperty().addListener(targetBedFirstLayerTempListener);
            bedTargetTemperature.setText(String.format("%d", selectedPrinter.getBedTargetTemperature()));
            selectedPrinter.bedTargetTemperatureProperty().addListener(targetBedTempListener);
            bedFirstLayerTargetTemperature.visibleProperty().bind(selectedPrinter.getBedHeaterModeProperty().isEqualTo(HeaterMode.FIRST_LAYER));
            bedTargetTemperature.visibleProperty().bind(selectedPrinter.getBedHeaterModeProperty().isEqualTo(HeaterMode.NORMAL));
            bedTemperaturePlaceholder.visibleProperty().bind(selectedPrinter.getBedHeaterModeProperty().isEqualTo(HeaterMode.OFF));

            ambientTemperatureLabel.textProperty().bind(Bindings.when(selectedPrinter.printerConnectedProperty().not()).then("-").otherwise(Bindings.when(selectedPrinter.ambientTemperatureProperty().greaterThan(ApplicationConfiguration.maxTempToDisplayOnGraph)).then(tempOutOfRangeHighString).otherwise(selectedPrinter.ambientTemperatureProperty().asString("%d°C"))));
            ambientTargetTemperature.setText(String.format("%d", selectedPrinter.getAmbientTargetTemperature()));
            selectedPrinter.ambientTargetTemperatureProperty().addListener(targetAmbientTempListener);
            /*
             * Door
             */
//            doorStatusLabel.textProperty().bind(Bindings.when(selectedPrinter.LidOpenProperty()).then(openString).otherwise(closedString));

            /*
             * Reel
             */
            filamentStatusLabel.textProperty().bind(Bindings.when(selectedPrinter.reelEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED)).then(selectedPrinter.reelFriendlyNameProperty()).otherwise(filamentNotLoadedString));

            /*
             * Head
             */
            printHeadLabel.textProperty().bind(Bindings.when(selectedPrinter.headEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED)).then(selectedPrinter.getHeadType()).otherwise(headNotAttachedString));

            currentAmbientTemperatureHistory = selectedPrinter.ambientTemperatureHistory();
            temperatureChart.getData().add(selectedPrinter.ambientTemperatureHistory());
            selectedPrinter.ambientTemperatureHistory().getData().addListener(graphDataPointChangeListener);
            temperatureChart.getData().add(selectedPrinter.bedTemperatureHistory());
            temperatureChart.getData().add(selectedPrinter.nozzleTemperatureHistory());
            temperatureChart.getData().add(selectedPrinter.ambientTargetTemperatureHistory());
            temperatureChart.getData().add(selectedPrinter.bedTargetTemperatureHistory());
            temperatureChart.getData().add(selectedPrinter.nozzleTargetTemperatureHistory());

//            bedHeaterCheckBox.setSelected((selectedPrinter.getBedHeaterMode() == HeaterMode.OFF) ? false : true);
            selectedPrinter.getBedHeaterModeProperty().addListener(bedHeaterStatusListener);

//            nozzleHeaterCheckBox.setSelected((selectedPrinter.getNozzleHeaterMode() == HeaterMode.OFF) ? false : true);
            selectedPrinter.getNozzleHeaterModeProperty().addListener(nozzleHeaterStatusListener);
            bedHeaterCheckBox.selectedProperty()
                    .addListener(bedHeaterCheckBoxListener);
            nozzleHeaterCheckBox.selectedProperty()
                    .addListener(nozzleHeaterCheckBoxListener);

            lastSelectedPrinter = selectedPrinter;
        }
    }

    private void controlDetailsVisibility()
    {
        boolean visible = printerStatusList.size() > 0 && !printerStatusTableSelectionModel.isEmpty();

        temperatureVBox.setVisible(visible);
        temperatureChart.setVisible(visible);
        printerStatusHBox.setVisible(visible);
    }

    @Override
    public void configure(Initializable slideOutController)
    {
        this.slideOutController = (PrinterStatusSlideOutPanelController) slideOutController;
    }
}
