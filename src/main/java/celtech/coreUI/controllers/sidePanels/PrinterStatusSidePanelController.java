/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.sidePanels;

import celtech.appManager.ApplicationStatus;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.PrinterStatusListCell;
import celtech.coreUI.controllers.StatusScreenMode;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusSidePanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(PrinterStatusSidePanelController.class.getName());
    private ApplicationStatus applicationStatus = null;

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

//    @FXML
//    private Label doorStatusLabel;

    @FXML
    private Label filamentStatusLabel;

    @FXML
    private Label nozzleTemperatureLabel;

    @FXML
    private TextField nozzleTargetTemperature;

    @FXML
    private Label printHeadLabel;

    @FXML
    private Label printerStatusLabel;

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
    private HBox nozzleHeaterHBox;
    @FXML
    private HBox bedHeaterHBox;
    @FXML
    private ToggleGroup bedHeaterControlGroup;
    @FXML
    private Toggle bedHeaterOn;
    @FXML
    private Toggle bedHeaterOff;
    @FXML
    private ToggleGroup nozzleHeaterControlGroup;
    @FXML
    private Toggle nozzleHeaterOn;
    @FXML
    private Toggle nozzleHeaterOff;
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
    private String tempOutOfRangeString;

    private PrinterIDDialog printerIDDialog = null;

    private ChangeListener<Number> targetExtruderTempListener = null;
    private ChangeListener<Number> targetBedTempListener = null;
    private ChangeListener<Number> targetAmbientTempListener = null;

    private ChangeListener<Toggle> nozzleHeaterToggleListener = null;
    private ChangeListener<Toggle> bedHeaterToggleListener = null;
    private ChangeListener<Boolean> bedHeaterStatusListener = null;
    private ChangeListener<Boolean> nozzleHeaterStatusListener = null;

    private Printer lastSelectedPrinter = null;

    private DisplayManager displayManager = null;

    private final float maxTempToDisplay = 350;

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
        filamentNotLoadedString = languageBundle.getString("sidePanel_printerStatus.filamentNotLoaded");
        connectedString = languageBundle.getString("sidePanel_printerStatus.connected");
        notConnectedString = languageBundle.getString("sidePanel_printerStatus.notConnected");
        headNotAttachedString = languageBundle.getString("sidePanel_printerStatus.headNotAttached");
        tempOutOfRangeString = languageBundle.getString("printerStatus.tempOutOfRange");

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
                        printerIDDialog.setChosenColour(printerToEdit.getPrinterColour());
                        printerIDDialog.setChosenPrinterID(printerToEdit.getPrinterserialNumber().get());
                        printerIDDialog.setChosenPrinterName(printerToEdit.getPrinterFriendlyName());
                        printerIDDialog.show();

                        try
                        {
                            printerToEdit.transmitWritePrinterID(printerToEdit.getPrintermodel().get(), printerToEdit.getPrinteredition().get(),
                                    printerToEdit.getPrinterweekOfManufacture().get(), printerToEdit.getPrinteryearOfManufacture().get(),
                                    printerToEdit.getPrinterpoNumber().get(), printerIDDialog.getChosenPrinterID(),
                                    "0", printerIDDialog.getChosenPrinterName(), printerIDDialog.getColour());
                            printerToEdit.transmitReadPrinterID();
                        } catch (RoboxCommsException ex)
                        {
                            steno.error("Error writing printer ID");
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
                            steno.info("Added: " + additem.toString());
                            printerStatusTableSelectionModel.select(additem);
                        }
                    } else if (change.wasRemoved())
                    {
                        for (Printer additem : change.getRemoved())
                        {
                            steno.info("Removed: " + additem.toString());
                        }
                    } else if (change.wasReplaced())
                    {
                        steno.info("Replaced: ");
                    } else if (change.wasUpdated())
                    {
                        steno.info("Updated: ");
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

        targetExtruderTempListener = new ChangeListener<Number>()
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

//        targetTempLabel.visibleProperty().bind(advancedControlsToggle.selectedProperty());
//        heaterStatusLabel.visibleProperty().bind(advancedControlsToggle.selectedProperty());
//
//        targetAmbientTempHBox.visibleProperty().bind(advancedControlsToggle.selectedProperty());

        ambientTargetTemperature.setOnKeyPressed(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent t)
            {
                if (t.getCode() == KeyCode.ENTER && lastSelectedPrinter != null)
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
        });

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

        bedTargetTemperature.setOnKeyPressed(new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent t)
            {
                if (t.getCode() == KeyCode.ENTER && lastSelectedPrinter != null)
                {
                    try
                    {
                        lastSelectedPrinter.transmitDirectGCode(GCodeConstants.setBedTemperature + Float.valueOf(bedTargetTemperature.getText()), false);
                    } catch (NumberFormatException ex)
                    {
                        steno.error("Couldn't translate value to float");
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error whilst sending new temperature to printer");
                    }
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

        bedHeaterToggleListener = new ChangeListener<Toggle>()
        {
            @Override
            public void changed(ObservableValue<? extends Toggle> ov, Toggle t, Toggle t1)
            {

                if ((t1 == null))
                {
                    Platform.runLater(new Runnable()
                    {

                        public void run()
                        {
                            bedHeaterControlGroup.selectToggle(t);
                        }
                    });
                } else if (t1 == bedHeaterOn)
                {
                    try
                    {
                        if (lastSelectedPrinter != null)
                        {
                            lastSelectedPrinter.transmitDirectGCode(GCodeConstants.setBedTemperatureToTarget, true);
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
                            lastSelectedPrinter.transmitDirectGCode(GCodeConstants.switchBedHeaterOff, true);
                        }
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error whilst setting bed target temperature");
                    }
                }

            }
        };

        nozzleHeaterToggleListener = new ChangeListener<Toggle>()
        {
            @Override
            public void changed(ObservableValue<? extends Toggle> ov, Toggle t, Toggle t1)
            {
                if ((t1 == null))
                {
                    Platform.runLater(new Runnable()
                    {
                        public void run()
                        {
                            nozzleHeaterControlGroup.selectToggle(t);
                        }
                    });
                } else if (t1 == nozzleHeaterOn)
                {
                    try
                    {
                        if (lastSelectedPrinter != null)
                        {
                            lastSelectedPrinter.transmitDirectGCode(GCodeConstants.setNozzleTemperatureToTarget, true);
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
                            lastSelectedPrinter.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, true);
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

        nozzleTargetTemperature.setOnKeyPressed(
                new EventHandler<KeyEvent>()
                {
                    @Override
                    public void handle(KeyEvent t
                    )
                    {
                        if (t.getCode() == KeyCode.ENTER && lastSelectedPrinter != null)
                        {
                            try
                            {
                                lastSelectedPrinter.transmitDirectGCode(GCodeConstants.setNozzleTemperature + Integer.valueOf(nozzleTargetTemperature.getText()), false);
                            } catch (NumberFormatException ex)
                            {
                                steno.error("Couldn't translate value to float");
                            } catch (RoboxCommsException ex)
                            {
                                steno.error("Error whilst sending new temperature to printer");
                            }
                        }
                    }
                }
        );
        nozzleTargetTemperature.focusedProperty()
                .addListener(new ChangeListener<Boolean>()
                        {

                            @Override
                            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1
                            )
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

        bedHeaterStatusListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == true && t1.booleanValue() == false)
                {
                    bedHeaterControlGroup.selectToggle(bedHeaterOn);
                } else if (t1.booleanValue() == false && t1.booleanValue() == true)
                {
                    bedHeaterControlGroup.selectToggle(bedHeaterOff);
                }
            }
        };

        nozzleHeaterStatusListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == true && t1.booleanValue() == false)
                {
                    nozzleHeaterControlGroup.selectToggle(nozzleHeaterOn);
                } else if (t1.booleanValue() == false && t1.booleanValue() == true)
                {
                    nozzleHeaterControlGroup.selectToggle(nozzleHeaterOff);
                }
            }
        };

        bedHeaterControlGroup.selectedToggleProperty()
                .addListener(bedHeaterToggleListener);
        nozzleHeaterControlGroup.selectedToggleProperty()
                .addListener(nozzleHeaterToggleListener);

    }

    private void bindDetails(Printer selectedPrinter)
    {

        printerStatusLabel.textProperty().unbind();
        printerStatusLabel.setText("-");

        nozzleTemperatureLabel.textProperty().unbind();
        bedTemperatureLabel.textProperty().unbind();
        ambientTemperatureLabel.textProperty().unbind();
//        doorStatusLabel.textProperty().unbind();
        filamentStatusLabel.textProperty().unbind();

        if (lastSelectedPrinter != null)
        {
            lastSelectedPrinter.extruderTargetTemperatureProperty().unbind();
            lastSelectedPrinter.bedTargetTemperatureProperty().unbind();
            lastSelectedPrinter.ambientTargetTemperatureProperty().unbind();

            temperatureChart.getData().remove(lastSelectedPrinter.ambientTemperatureHistory());
            currentAmbientTemperatureHistory = null;
            temperatureChart.getData().remove(lastSelectedPrinter.bedTemperatureHistory());
            temperatureChart.getData().remove(lastSelectedPrinter.nozzleTemperatureHistory());
            temperatureChart.getData().remove(lastSelectedPrinter.ambientTargetTemperatureHistory());
            temperatureChart.getData().remove(lastSelectedPrinter.bedTargetTemperatureHistory());
            temperatureChart.getData().remove(lastSelectedPrinter.nozzleTargetTemperatureHistory());

            lastSelectedPrinter.bedHeaterOnProperty().removeListener(bedHeaterStatusListener);
            lastSelectedPrinter.nozzleHeaterOnProperty().removeListener(nozzleHeaterStatusListener);
        }

        if (selectedPrinter != null)
        {

            printerStatusLabel.textProperty().bind(Bindings.when(selectedPrinter.printerConnectedProperty()).then(selectedPrinter.printerStatusProperty().asString()).otherwise(notConnectedString));
            // Temperatures / Heaters / Fans
            nozzleTemperatureLabel.textProperty().bind(Bindings.when(selectedPrinter.printerConnectedProperty().not()).then("-").otherwise(Bindings.when(selectedPrinter.extruderTemperatureProperty().greaterThan(maxTempToDisplay)).then(tempOutOfRangeString).otherwise(selectedPrinter.extruderTemperatureProperty().asString("%d°C"))));
            nozzleTargetTemperature.setText(String.format("%d", selectedPrinter.getNozzleTargetTemperature()));
            selectedPrinter.extruderTargetTemperatureProperty().addListener(targetExtruderTempListener);

            bedTemperatureLabel.textProperty().bind(Bindings.when(selectedPrinter.printerConnectedProperty().not()).then("-").otherwise(Bindings.when(selectedPrinter.bedTemperatureProperty().greaterThan(maxTempToDisplay)).then(tempOutOfRangeString).otherwise(selectedPrinter.bedTemperatureProperty().asString("%d°C"))));
            bedTargetTemperature.setText(String.format("%d", selectedPrinter.getBedTargetTemperature()));
            selectedPrinter.bedTargetTemperatureProperty().addListener(targetBedTempListener);

            ambientTemperatureLabel.textProperty().bind(Bindings.when(selectedPrinter.printerConnectedProperty().not()).then("-").otherwise(Bindings.when(selectedPrinter.ambientTemperatureProperty().greaterThan(maxTempToDisplay)).then(tempOutOfRangeString).otherwise(selectedPrinter.ambientTemperatureProperty().asString("%d°C"))));
            ambientTargetTemperature.setText(String.format("%d", selectedPrinter.getAmbientTargetTemperature()));
            selectedPrinter.ambientTargetTemperatureProperty().addListener(targetAmbientTempListener);
            /*
             * Door
             */
//            doorStatusLabel.textProperty().bind(Bindings.when(selectedPrinter.LidOpenProperty()).then(openString).otherwise(closedString));

            /*
             * Reel
             */
            filamentStatusLabel.textProperty().bind(Bindings.when(selectedPrinter.reelAttachedProperty()).then(selectedPrinter.reelFriendlyNameProperty()).otherwise(filamentNotLoadedString));

            /*
             * Head
             */
            printHeadLabel.textProperty().bind(Bindings.when(selectedPrinter.headAttachedProperty()).then(selectedPrinter.getHeadType()).otherwise(headNotAttachedString));

            currentAmbientTemperatureHistory = selectedPrinter.ambientTemperatureHistory();
            temperatureChart.getData().add(selectedPrinter.ambientTemperatureHistory());
            selectedPrinter.ambientTemperatureHistory().getData().addListener(graphDataPointChangeListener);
            temperatureChart.getData().add(selectedPrinter.bedTemperatureHistory());
            temperatureChart.getData().add(selectedPrinter.nozzleTemperatureHistory());
            temperatureChart.getData().add(selectedPrinter.ambientTargetTemperatureHistory());
            temperatureChart.getData().add(selectedPrinter.bedTargetTemperatureHistory());
            temperatureChart.getData().add(selectedPrinter.nozzleTargetTemperatureHistory());

            if (selectedPrinter.bedHeaterOnProperty().get() == true)
            {
                if (bedHeaterControlGroup.getSelectedToggle() != bedHeaterOn)
                {
                    bedHeaterControlGroup.selectToggle(bedHeaterOn);
                }
            } else
            {
                if (bedHeaterControlGroup.getSelectedToggle() != bedHeaterOff)
                {
                    bedHeaterControlGroup.selectToggle(bedHeaterOff);
                }
            }

            if (selectedPrinter.nozzleHeaterOnProperty().get() == true)
            {
                if (nozzleHeaterControlGroup.getSelectedToggle() != nozzleHeaterOn)
                {
                    nozzleHeaterControlGroup.selectToggle(nozzleHeaterOn);
                }
            } else
            {
                if (nozzleHeaterControlGroup.getSelectedToggle() != nozzleHeaterOff)
                {
                    nozzleHeaterControlGroup.selectToggle(nozzleHeaterOff);
                }
            }

            selectedPrinter.bedHeaterOnProperty().addListener(bedHeaterStatusListener);
            selectedPrinter.nozzleHeaterOnProperty().addListener(nozzleHeaterStatusListener);

            lastSelectedPrinter = selectedPrinter;
        }
    }

    private void controlDetailsVisibility()
    {
        boolean visible = printerStatusList.size() > 0 && !printerStatusTableSelectionModel.isEmpty();
        temperatureVBox.setVisible(visible);
        temperatureChart.setVisible(visible);
    }
}
