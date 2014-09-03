/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.configuration.HeaterMode;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.printerstatus.PrinterComponent;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.utils.PrinterUtils;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterStatusSidePanelController implements Initializable, SidePanelManager
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PrinterStatusSidePanelController.class.getName());
    private ApplicationStatus applicationStatus = null;
    private PrinterUtils printerUtils = null;

    @FXML
    private Label ambientTemperatureLabel;
    
    @FXML
    private GridPane targetTemperatureColumn;

    @FXML
    private RestrictedNumberField ambientTargetTemperature;

    @FXML
    private Label bedTemperatureLabel;

    @FXML
    private RestrictedNumberField bedTargetTemperature;

    @FXML
    private RestrictedNumberField bedFirstLayerTargetTemperature;

    @FXML
    private Label bedTemperaturePlaceholder;

    @FXML
    private Label filamentStatusLabel;

    @FXML
    private Label nozzleTemperatureLabel;

    @FXML
    private RestrictedNumberField nozzleTargetTemperature;

    @FXML
    private RestrictedNumberField nozzleFirstLayerTargetTemperature;

    @FXML
    private Label nozzleTemperaturePlaceholder;

    @FXML
    private Label printHeadLabel;

    @FXML
    private HBox printerStatusHBox;

    @FXML
    private VBox temperatureVBox;

    @FXML
    private CheckBox nozzleHeaterCheckBox;
    @FXML
    private CheckBox bedHeaterCheckBox;

    @FXML
    private LineChart<Number, Number> temperatureChart;

    @FXML
    private GridPane printerStatusGrid;

    @FXML
    private NumberAxis temperatureAxis;
    private NumberAxis timeAxis;

    private final TableColumn printerNameColumn = new TableColumn();

    private ObservableList<Printer> printerStatusList = null;
    private StatusScreenState statusScreenState = null;

    private String filamentNotLoadedString;
    private String reelNotFormattedString;
    private String headNotAttachedString;
    private String headNotFormattedString;
    private String tempOutOfRangeHighString;
    private String tempOutOfRangeLowString;

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

    private Printer selectedPrinter = null;
    private final Map<Printer, PrinterComponent> printerComponentsByPrinter = new HashMap<>();

    private final int MAX_DATA_POINTS = 180;

    private LineChart.Series<Number, Number> currentAmbientTemperatureHistory = null;

    private final ListChangeListener<XYChart.Data<Number, Number>> graphDataPointChangeListener = new ListChangeListener<XYChart.Data<Number, Number>>()
    {
        @Override
        public void onChanged(
            ListChangeListener.Change<? extends XYChart.Data<Number, Number>> change)
        {
            while (change.next())
            {
                if (change.wasAdded() || change.wasRemoved())
                {
                    timeAxis.setLowerBound(currentAmbientTemperatureHistory.getData().size()
                        - MAX_DATA_POINTS);
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
        if (selectedPrinter != null)
        {
            try
            {
                selectedPrinter.transmitDirectGCode(
                    GCodeConstants.setFirstLayerNozzleTemperatureTarget + Integer.valueOf(
                        nozzleFirstLayerTargetTemperature.getText()), false);
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
        if (selectedPrinter != null)
        {
            try
            {
                selectedPrinter.transmitDirectGCode(GCodeConstants.setNozzleTemperatureTarget
                    + Integer.valueOf(nozzleTargetTemperature.getText()), false);
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
        if (selectedPrinter != null)
        {
            try
            {
                selectedPrinter.transmitDirectGCode(GCodeConstants.setAmbientTemperature
                    + Integer.valueOf(ambientTargetTemperature.getText()), false);
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
        if (selectedPrinter != null)
        {
            try
            {
                selectedPrinter.transmitDirectGCode(
                    GCodeConstants.setFirstLayerBedTemperatureTarget + Float.valueOf(
                        bedFirstLayerTargetTemperature.getText()), false);
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
        if (selectedPrinter != null)
        {
            try
            {
                selectedPrinter.transmitDirectGCode(GCodeConstants.setBedTemperatureTarget
                    + Float.valueOf(bedTargetTemperature.getText()), false);
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
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        applicationStatus = ApplicationStatus.getInstance();
        RoboxCommsManager commsManager = RoboxCommsManager.getInstance();
        printerStatusList = commsManager.getPrintStatusList();
        statusScreenState = StatusScreenState.getInstance();
        printerUtils = PrinterUtils.getInstance();

        printerIDDialog = new PrinterIDDialog();

        ResourceBundle languageBundle = DisplayManager.getLanguageBundle();
        initialiseMessages(languageBundle);

        printerNameColumn.setText(languageBundle.getString(
            "sidePanel_printerStatus.printerNameColumn"));
        printerNameColumn.setPrefWidth(300);
        printerNameColumn.setCellValueFactory(new PropertyValueFactory<Printer, String>(
            "printerFriendlyName"));

        targetTemperatureColumn.setVisible(false);
        
        initialiseTemperatureChart();
        initialisePrinterStatusGrid();
        initialiseTemperatureAndHeaterListeners();
        controlDetailsVisibility();
    }

    private void initialiseMessages(ResourceBundle languageBundle)
    {
        filamentNotLoadedString = languageBundle.getString("smartReelProgrammer.noReelLoaded");
        reelNotFormattedString = languageBundle.getString("smartReelProgrammer.reelNotFormatted");
        headNotAttachedString = languageBundle.getString("sidePanel_printerStatus.headNotAttached");
        headNotFormattedString = languageBundle.getString("smartheadProgrammer.headNotFormatted");
        tempOutOfRangeHighString = languageBundle.getString("printerStatus.tempOutOfRangeHigh");
        tempOutOfRangeLowString = languageBundle.getString("printerStatus.tempOutOfRangeLow");
    }

    private void initialiseTemperatureChart()
    {
        timeAxis = new NumberAxis(0, MAX_DATA_POINTS, 30);
        timeAxis.setForceZeroInRange(false);
        timeAxis.setAutoRanging(true);
        
        temperatureAxis = new NumberAxis();
        temperatureAxis.setAutoRanging(false);
        
        temperatureChart.setAnimated(false);
        temperatureChart.setLegendVisible(false);
        temperatureChart.setLegendSide(Side.RIGHT);
        
        temperatureChart.setVisible(false);
    }

    private void initialiseTemperatureAndHeaterListeners()
    {
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

        ambientTargetTemperature.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == false)
                {
                    setAmbientTargetTemp(null);
                }
            }
        });

        bedFirstLayerTargetTemperature.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean lastValue,
                Boolean newValue)
            {
                if (newValue.booleanValue() == false)
                {
                    setBedFirstLayerTargetTemp(null);
                }
            }
        });

        bedTargetTemperature.focusedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == false)
                {
                    setBedTargetTemp(null);
                }
            }
        });

        bedHeaterCheckBoxListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldHeaterOnDemand,
                Boolean heaterOnDemand)
            {
                if (heaterOnDemand == true)
                {
                    try
                    {
                        if (selectedPrinter != null)
                        {
                            if (selectedPrinter.getBedHeaterMode() == HeaterMode.OFF)
                            {
                                selectedPrinter.transmitDirectGCode(
                                    GCodeConstants.goToTargetBedTemperature, false);
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
                        if (selectedPrinter != null)
                        {
                            if (selectedPrinter.getBedHeaterMode() != HeaterMode.OFF)
                            {
                                selectedPrinter.transmitDirectGCode(
                                    GCodeConstants.switchBedHeaterOff, false);
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
                        if (selectedPrinter != null)
                        {
                            if (selectedPrinter.getNozzleHeaterMode() == HeaterMode.OFF)
                            {
                                boolean purgeConsent = printerUtils.offerPurgeIfNecessary(
                                    selectedPrinter);
                                if (purgeConsent)
                                {
                                    applicationStatus.setMode(ApplicationMode.STATUS);
                                    PrinterUtils.runPurge(selectedPrinter);
                                }
                                selectedPrinter.transmitDirectGCode(
                                    GCodeConstants.goToTargetNozzleTemperature, false);
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
                        if (selectedPrinter != null)
                        {
                            if (selectedPrinter.getNozzleHeaterMode() != HeaterMode.OFF)
                            {
                                selectedPrinter.transmitDirectGCode(
                                    GCodeConstants.switchNozzleHeaterOff, false);
                            }
                        }
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error whilst setting nozzle target temperature");
                    }
                }
            }
        };

        nozzleFirstLayerTargetTemperature.focusedProperty().addListener(
            new ChangeListener<Boolean>()
            {
                @Override
                public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
                {
                    if (t1.booleanValue() == false)
                    {
                        setNozzleFirstLayerTargetTemp(null);
                    }
                }
            });

        nozzleTargetTemperature.focusedProperty().addListener(new ChangeListener<Boolean>()
        {

            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue() == false)
                {
                    setNozzleTargetTemp(null);
                }
            }
        }
        );

        bedHeaterStatusListener = new ChangeListener<HeaterMode>()
        {
            @Override
            public void changed(ObservableValue<? extends HeaterMode> ov, HeaterMode oldValue,
                HeaterMode newValue)
            {
                bedHeaterCheckBox.setSelected(newValue != HeaterMode.OFF);
            }
        };

        nozzleHeaterStatusListener = new ChangeListener<HeaterMode>()
        {
            @Override
            public void changed(ObservableValue<? extends HeaterMode> ov, HeaterMode oldValue,
                HeaterMode newValue)
            {
                nozzleHeaterCheckBox.setSelected(newValue != HeaterMode.OFF);
            }
        };
    }

    private void initialisePrinterStatusGrid()
    {
        int row = 0;
        int column = 0;
        for (Printer printer : printerStatusList)
        {
            PrinterComponent printerComponent = createPrinterComponentForPrinter(printer);
            addPrinterComponentToGrid(printerComponent, row, column);
        }

        printerStatusList.addListener(new ListChangeListener<Printer>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends Printer> change)
            {
                while (change.next())
                {
                    if (change.wasAdded())
                    {
                        for (Printer printer : change.getAddedSubList())
                        {
                            addPrinterToGridAndLayout(printer);
                            selectPrinter(printer);
                        }
                    } else if (change.wasRemoved())
                    {
                        for (Printer printer : change.getRemoved())
                        {
                            removePrinter(printer);
                        }
                    } else if (change.wasReplaced())
                    {
                    } else if (change.wasUpdated())
                    {
                    }
                }
            }

        });

    }

    /**
     * Remove the current printer from the display. Update the selected printer to one of
     * the remaining printers.
     */
    private void removePrinter(Printer printer)
    {
        PrinterComponent printerComponent = printerComponentsByPrinter.get(selectedPrinter);
        removePrinterComponentFromGrid(printerComponent);
        selectPrinter(null);
    }

    /**
     * Make the given printer the selected printer.
     *
     * @param printer
     */
    private void selectPrinter(Printer printer)
    {
        if (selectedPrinter != null)
        {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(selectedPrinter);
            printerComponent.setSelected(true);
        }
       
        selectedPrinter = printer;
         if (printer != null) {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
            printerComponent.setSelected(true);
         }   
        statusScreenState.setCurrentlySelectedPrinter(printer);
        bindDetails(printer);
        controlDetailsVisibility();
        
    }

    /**
     * This is called when the user clicks on the printer component for the given printer, and
     * handles click (select printer) and double-click (go to edit printer details).
     *
     * @param event
     */
    private void handlePrinterClicked(MouseEvent event, Printer printer)
    {
        if (event.getClickCount() == 1)
        {
            selectPrinter(printer);
        }
        if (event.getClickCount() > 1)
        {
            showEditPrinterDetails(printer);
        }
    }

    /**
     * Show the printerIDDialog for the given printer.
     */
    private void showEditPrinterDetails(Printer printer)
    {
        if (printer != null)
        {
            printerIDDialog.setPrinterToUse(printer);
            printerIDDialog.setChosenDisplayColour(colourMap.printerToDisplayColour(
                printer.getPrinterColour()));
            printerIDDialog.setChosenPrinterName(printer.getPrinterFriendlyName());

            boolean okPressed = printerIDDialog.show();

            if (okPressed)
            {
                try
                {
                    printer.transmitWritePrinterID(
                        printer.getPrintermodel().get(),
                        printer.getPrinteredition().get(),
                        printer.getPrinterweekOfManufacture().get(),
                        printer.getPrinteryearOfManufacture().get(),
                        printer.getPrinterpoNumber().get(),
                        printer.getPrinterserialNumber().get(),
                        printer.getPrintercheckByte().get(),
                        printerIDDialog.getChosenPrinterName(),
                        colourMap.displayToPrinterColour(
                            printerIDDialog.getChosenDisplayColour()));

                    printer.transmitReadPrinterID();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error writing printer ID");
                }
            }
        }
    }

    /**
     * Create the PrinterComponent for the given printer and set up any listeners on component
     * events.
     */
    private PrinterComponent createPrinterComponentForPrinter(Printer printer)
    {
        PrinterComponent printerComponent = new PrinterComponent(printer);
        printerComponent.setOnMouseClicked((MouseEvent event) ->
        {
            handlePrinterClicked(event, printer);
        });
        printerComponentsByPrinter.put(printer, printerComponent);
        return printerComponent;
    }

    /**
     * Add the given printer to the grid in an appropriate place.
     */
    private void addPrinterToGridAndLayout(Printer printer)
    {
        // TODO: make work for more than one printer
        PrinterComponent printerComponent = createPrinterComponentForPrinter(printer);
        addPrinterComponentToGrid(printerComponent, 0, 0);
    }
    
    /**
     * Add the given printer component to the given grid coordinates.
     */
    private void addPrinterComponentToGrid(PrinterComponent printerComponent, int row,
        int column)
    {
        printerComponent.setSize(PrinterComponent.Size.SIZE_LARGE);
        printerStatusGrid.add(printerComponent, row, column);
    }    
    
    /**
     * Remove the given printer from the grid.
     * @param printerComponent 
     */
    private void removePrinterComponentFromGrid(PrinterComponent printerComponent) {
        printerStatusGrid.getChildren().remove(printerComponent);
    }    

    private void bindDetails(Printer printer)
    {
        nozzleTemperatureLabel.textProperty().unbind();
        bedTemperatureLabel.textProperty().unbind();
        ambientTemperatureLabel.textProperty().unbind();
        filamentStatusLabel.textProperty().unbind();

        if (selectedPrinter != null)
        {
            selectedPrinter.nozzleFirstLayerTargetTemperatureProperty().unbind();
            selectedPrinter.nozzleTargetTemperatureProperty().unbind();
            selectedPrinter.bedFirstLayerTargetTemperatureProperty().unbind();
            selectedPrinter.bedTargetTemperatureProperty().unbind();
            selectedPrinter.ambientTargetTemperatureProperty().unbind();

            temperatureChart.getData().remove(selectedPrinter.ambientTemperatureHistory());
            currentAmbientTemperatureHistory = null;
            temperatureChart.getData().remove(selectedPrinter.bedTemperatureHistory());
            temperatureChart.getData().remove(selectedPrinter.nozzleTemperatureHistory());
            temperatureChart.getData().remove(selectedPrinter.ambientTargetTemperatureHistory());
            temperatureChart.getData().remove(selectedPrinter.bedTargetTemperatureHistory());
            temperatureChart.getData().remove(selectedPrinter.nozzleTargetTemperatureHistory());

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
            selectedPrinter.getBedHeaterModeProperty().removeListener(bedHeaterStatusListener);
            selectedPrinter.getNozzleHeaterModeProperty().removeListener(
                nozzleHeaterStatusListener);
        }

        if (printer != null)
        {
            // Temperatures / Heaters / Fans
            nozzleTemperatureLabel.textProperty().bind(Bindings.when(
                printer.printerConnectedProperty().not()).then("-").otherwise(Bindings.when(
                        printer.extruderTemperatureProperty().greaterThan(
                            ApplicationConfiguration.maxTempToDisplayOnGraph)).then(
                        tempOutOfRangeHighString)
                    .otherwise(Bindings.when(printer.extruderTemperatureProperty().lessThan(
                                ApplicationConfiguration.minTempToDisplayOnGraph)).then(
                            tempOutOfRangeLowString).otherwise(
                            printer.extruderTemperatureProperty().asString("%d°C")))));
            nozzleFirstLayerTargetTemperature.setText(String.format("%d",
                                                                    printer.getNozzleFirstLayerTargetTemperature()));
            printer.nozzleFirstLayerTargetTemperatureProperty().addListener(
                targetNozzleFirstLayerTempListener);
            nozzleTargetTemperature.setText(String.format("%d",
                                                          printer.getNozzleTargetTemperature()));
            printer.nozzleTargetTemperatureProperty().addListener(targetNozzleTempListener);
            nozzleFirstLayerTargetTemperature.visibleProperty().bind(
                printer.getNozzleHeaterModeProperty().isEqualTo(HeaterMode.FIRST_LAYER));
            nozzleTargetTemperature.visibleProperty().bind(
                printer.getNozzleHeaterModeProperty().isEqualTo(HeaterMode.NORMAL));
            nozzleTemperaturePlaceholder.visibleProperty().bind(
                printer.getNozzleHeaterModeProperty().isEqualTo(HeaterMode.OFF));

            bedTemperatureLabel.textProperty().bind(Bindings.when(
                printer.printerConnectedProperty().not()).then("-").otherwise(Bindings.when(
                        printer.bedTemperatureProperty().greaterThan(
                            ApplicationConfiguration.maxTempToDisplayOnGraph)).then(
                        tempOutOfRangeHighString)
                    .otherwise(Bindings.when(printer.bedTemperatureProperty().lessThan(
                                ApplicationConfiguration.minTempToDisplayOnGraph)).then(
                            tempOutOfRangeLowString).otherwise(
                            printer.bedTemperatureProperty().asString("%d°C")))));
            bedFirstLayerTargetTemperature.setText(String.format("%d",
                                                                 printer.getBedFirstLayerTargetTemperature()));
            printer.bedFirstLayerTargetTemperatureProperty().addListener(
                targetBedFirstLayerTempListener);
            bedTargetTemperature.setText(String.format("%d",
                                                       printer.getBedTargetTemperature()));
            printer.bedTargetTemperatureProperty().addListener(targetBedTempListener);
            bedFirstLayerTargetTemperature.visibleProperty().bind(
                printer.getBedHeaterModeProperty().isEqualTo(HeaterMode.FIRST_LAYER));
            bedTargetTemperature.visibleProperty().bind(
                printer.getBedHeaterModeProperty().isEqualTo(HeaterMode.NORMAL));
            bedTemperaturePlaceholder.visibleProperty().bind(
                printer.getBedHeaterModeProperty().isEqualTo(HeaterMode.OFF));

            ambientTemperatureLabel.textProperty().bind(Bindings.when(
                printer.printerConnectedProperty().not()).then("-").otherwise(Bindings.when(
                        printer.ambientTemperatureProperty().greaterThan(
                            ApplicationConfiguration.maxTempToDisplayOnGraph)).then(
                        tempOutOfRangeHighString).otherwise(
                        printer.ambientTemperatureProperty().asString("%d°C"))));
            ambientTargetTemperature.setText(String.format("%d",
                                                           printer.getAmbientTargetTemperature()));
            printer.ambientTargetTemperatureProperty().addListener(targetAmbientTempListener);
            /*
             * Door
             */
//            doorStatusLabel.textProperty().bind(Bindings.when(selectedPrinter.LidOpenProperty()).then(openString).otherwise(closedString));

            /*
             * Reel
             */
            filamentStatusLabel.textProperty().bind(Bindings.when(
                printer.reelEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED)).then(
                    printer.reelFriendlyNameProperty()).otherwise(Bindings.when(
                        printer.reelEEPROMStatusProperty().isEqualTo(
                            EEPROMState.NOT_PROGRAMMED)).then(reelNotFormattedString).otherwise(
                        filamentNotLoadedString)));

            /*
             * Head
             */
            printHeadLabel.textProperty().bind(Bindings.when(
                printer.headEEPROMStatusProperty().isEqualTo(EEPROMState.PROGRAMMED)).then(
                    printer.getHeadType()).otherwise(Bindings.when(
                        printer.headEEPROMStatusProperty().isEqualTo(
                            EEPROMState.NOT_PROGRAMMED)).then(headNotFormattedString).otherwise(
                        headNotAttachedString)));

            currentAmbientTemperatureHistory = printer.ambientTemperatureHistory();
            temperatureChart.getData().add(printer.ambientTemperatureHistory());
            printer.nozzleTemperatureHistory().getData().addListener(
                graphDataPointChangeListener);
            temperatureChart.getData().add(printer.bedTemperatureHistory());
            temperatureChart.getData().add(printer.nozzleTemperatureHistory());
            temperatureChart.getData().add(printer.ambientTargetTemperatureHistory());
            temperatureChart.getData().add(printer.bedTargetTemperatureHistory());
            temperatureChart.getData().add(printer.nozzleTargetTemperatureHistory());

            printer.getBedHeaterModeProperty().addListener(bedHeaterStatusListener);

            printer.getNozzleHeaterModeProperty().addListener(nozzleHeaterStatusListener);
            bedHeaterCheckBox.selectedProperty()
                .addListener(bedHeaterCheckBoxListener);
            nozzleHeaterCheckBox.selectedProperty()
                .addListener(nozzleHeaterCheckBoxListener);
        }
    }

    private void controlDetailsVisibility()
    {
        boolean visible = printerStatusList.size() > 0;

        temperatureVBox.setVisible(visible);
        temperatureChart.setVisible(visible);
        printerStatusHBox.setVisible(visible);
    }

    /**
     *
     * @param slideOutController
     */
    @Override
    public void configure(Initializable slideOutController)
    {
    }

}
