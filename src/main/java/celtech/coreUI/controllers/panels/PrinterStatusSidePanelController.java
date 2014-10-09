package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.configuration.HeaterMode;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.PrinterStatusListCell;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.model.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Reel;
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
    private PurgeInsetPanelController purgePanelController = null;

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
    private Label filamentStatusValueLabel;

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
    private String reelNotFormattedString;
    private String connectedString;
    private String notConnectedString;
    private String headNotAttachedString;
    private String headNotFormattedString;
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

    private final ChangeListener<Head> headChangeListener = (ObservableValue<? extends Head> observable, Head oldHead, Head newHead) ->
    {
        if (newHead != null)
        {
            bindHeadProperties(newHead);
        } else if (oldHead != null)
        {
            unbindHeadProperties(oldHead);
        }
    };

    private final ListChangeListener<Reel> reelChangeListener = new ListChangeListener<Reel>()
    {
        @Override
        public void onChanged(ListChangeListener.Change<? extends Reel> change)
        {
            while (change.next())
            {
                if (change.wasAdded())
                {
                    for (Reel changedReel : change.getAddedSubList())
                    {
                        bindReelProperties(changedReel);
                    }
                } else if (change.wasRemoved())
                {
                    for (Reel changedReel : change.getRemoved())
                    {
                        unbindReelProperties();
                    }
                } else if (change.wasReplaced())
                {
                } else if (change.wasUpdated())
                {
                }
            }
        }
    };

    private Printer lastSelectedPrinter = null;

    private DisplayManager displayManager = null;

    private final int MAX_DATA_POINTS = 180;

    private LineChart.Series<Number, Number> currentAmbientTemperatureHistory = null;

    @FXML
    void setNozzleFirstLayerTargetTemp(ActionEvent event)
    {
        if (lastSelectedPrinter != null)
        {
            try
            {
                lastSelectedPrinter.setNozzleFirstLayerTargetTemperature(Integer.valueOf(nozzleFirstLayerTargetTemperature.getText()));
            } catch (NumberFormatException ex)
            {
                steno.error("Couldn't translate value to float");
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
                lastSelectedPrinter.setNozzleTargetTemperature(Integer.valueOf(nozzleTargetTemperature.getText()));
            } catch (NumberFormatException ex)
            {
                steno.error("Couldn't translate value to float");
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
                lastSelectedPrinter.setAmbientTemperature(Integer.valueOf(ambientTargetTemperature.getText()));
            } catch (NumberFormatException ex)
            {
                steno.error("Couldn't translate value to float");
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
                lastSelectedPrinter.setBedFirstLayerTargetTemperature(Integer.valueOf(bedFirstLayerTargetTemperature.getText()));
            } catch (NumberFormatException ex)
            {
                steno.error("Couldn't translate value to integer");
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
                lastSelectedPrinter.setBedTargetTemperature(Integer.valueOf(bedTargetTemperature.getText()));
            } catch (NumberFormatException ex)
            {
                steno.error("Couldn't translate value to integer");
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
        displayManager = DisplayManager.getInstance();
        RoboxCommsManager commsManager = RoboxCommsManager.getInstance();
        printerStatusList = commsManager.getPrintStatusList();
        statusScreenState = StatusScreenState.getInstance();
        printerUtils = PrinterUtils.getInstance();

        purgePanelController = displayManager.getPurgeInsetPanelController();

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
        reelNotFormattedString = languageBundle.getString("smartReelProgrammer.reelNotFormatted");
        connectedString = languageBundle.getString("sidePanel_printerStatus.connected");
        notConnectedString = languageBundle.getString("sidePanel_printerStatus.notConnected");
        headNotAttachedString = languageBundle.getString("sidePanel_printerStatus.headNotAttached");
        headNotFormattedString = languageBundle.getString("smartheadProgrammer.headNotFormatted");
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
                        printerIDDialog.setChosenDisplayColour(colourMap.printerToDisplayColour(printerToEdit.getPrinterIdentity().printerColourProperty().get()));
                        printerIDDialog.setChosenPrinterName(printerToEdit.getPrinterIdentity().printerFriendlyNameProperty().get());

                        Color currentColour = printerToEdit.getPrinterIdentity().printerColourProperty().get();

                        boolean okPressed = printerIDDialog.show();

                        if (okPressed)
                        {
                            try
                            {
                                printerToEdit.updatePrinterName(printerIDDialog.getChosenPrinterName());
                                printerToEdit.updatePrinterDisplayColour(colourMap.displayToPrinterColour(
                                    printerIDDialog.getChosenDisplayColour()));
                            } catch (PrinterException ex)
                            {
                                steno.error("Printer exception whilst setting name and colour " + ex.getMessage());
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
            public void changed(ObservableValue<? extends Boolean> ov, Boolean lastValue, Boolean newValue)
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
            public void changed(ObservableValue<? extends Boolean> ov, Boolean oldHeaterOnDemand, Boolean heaterOnDemand)
            {
                if (heaterOnDemand == true)
                {
                    if (lastSelectedPrinter != null)
                    {
                        if (lastSelectedPrinter.getPrinterAncillarySystems().bedHeaterModeProperty().get() == HeaterMode.OFF)
                        {
                            lastSelectedPrinter.goToTargetBedTemperature();
                        }
                    }
                } else
                {
                    if (lastSelectedPrinter != null)
                    {
                        if (lastSelectedPrinter.getPrinterAncillarySystems().bedHeaterModeProperty().get() != HeaterMode.OFF)
                        {
                            lastSelectedPrinter.switchBedHeaterOff();
                        }
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
                    if (lastSelectedPrinter != null)
                    {
                        //TODO modify to work with multiple heaters
                        if (lastSelectedPrinter.headProperty().get().getNozzleHeaters().get(0).heaterModeProperty().get() == HeaterMode.OFF)
                        {
                            boolean purgeConsent = printerUtils.offerPurgeIfNecessary(lastSelectedPrinter);
                            if (purgeConsent)
                            {
                                purgePanelController.purge(lastSelectedPrinter);
                            }
                            lastSelectedPrinter.goToTargetNozzleTemperature();
                        }
                    }

                } else
                {
                    if (lastSelectedPrinter != null)
                    {
                        if (lastSelectedPrinter.headProperty().get().getNozzleHeaters().get(0).heaterModeProperty().get() != HeaterMode.OFF)
                        {
                            lastSelectedPrinter.switchNozzleHeaterOff(0);
                        }
                    }
                }
            }
        };

        nozzleFirstLayerTargetTemperature.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) ->
        {
            if (t1 == false)
            {
                setNozzleFirstLayerTargetTemp(null);
            }
        });

        nozzleTargetTemperature.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) ->
        {
            if (t1 == false)
            {
                setNozzleTargetTemp(null);
            }
        });

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
        filamentStatusLabel.textProperty().unbind();

        if (lastSelectedPrinter != null)
        {
            if (lastSelectedPrinter.headProperty().get() != null)
            {
                unbindHeadProperties(lastSelectedPrinter.headProperty().get());
                lastSelectedPrinter.headProperty().removeListener(headChangeListener);
            }

            lastSelectedPrinter.reelsProperty().removeListener(reelChangeListener);

            temperatureChart.getData().remove(lastSelectedPrinter.getPrinterAncillarySystems().getAmbientTemperatureHistory());
            currentAmbientTemperatureHistory = null;
            temperatureChart.getData().remove(lastSelectedPrinter.getPrinterAncillarySystems().getBedTemperatureHistory());

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
            nozzleHeaterCheckBox.disableProperty().unbind();

            lastSelectedPrinter.getPrinterAncillarySystems().bedHeaterModeProperty().removeListener(bedHeaterStatusListener);
        }

        if (selectedPrinter != null)
        {
            if (selectedPrinter.headProperty().get() != null)
            {
                bindHeadProperties(selectedPrinter.headProperty().get());
            }

            selectedPrinter.headProperty().addListener(headChangeListener);

            // Temperatures / Heaters / Fans
            bedTemperatureLabel.textProperty().bind(Bindings
                .when(selectedPrinter.getPrinterAncillarySystems().bedTemperatureProperty()
                    .greaterThan(ApplicationConfiguration.maxTempToDisplayOnGraph))
                .then(tempOutOfRangeHighString)
                .otherwise(Bindings.when(selectedPrinter.getPrinterAncillarySystems().bedTemperatureProperty()
                        .lessThan(ApplicationConfiguration.minTempToDisplayOnGraph))
                    .then(tempOutOfRangeLowString)
                    .otherwise(selectedPrinter.getPrinterAncillarySystems().bedTemperatureProperty().asString("%d°C"))));
            bedFirstLayerTargetTemperature.setText(String.format("%d", selectedPrinter.getPrinterAncillarySystems().bedFirstLayerTargetTemperatureProperty().get()));
            selectedPrinter.getPrinterAncillarySystems().bedFirstLayerTargetTemperatureProperty().addListener(targetBedFirstLayerTempListener);
            bedTargetTemperature.setText(String.format("%d", selectedPrinter.getPrinterAncillarySystems().bedTargetTemperatureProperty().get()));
            selectedPrinter.getPrinterAncillarySystems().bedTargetTemperatureProperty().addListener(targetBedTempListener);
            bedFirstLayerTargetTemperature.visibleProperty().bind(selectedPrinter.getPrinterAncillarySystems().bedHeaterModeProperty().isEqualTo(HeaterMode.FIRST_LAYER));
            bedTargetTemperature.visibleProperty().bind(selectedPrinter.getPrinterAncillarySystems().bedHeaterModeProperty().isEqualTo(HeaterMode.NORMAL));
            bedTemperaturePlaceholder.visibleProperty().bind(selectedPrinter.getPrinterAncillarySystems().bedHeaterModeProperty().isEqualTo(HeaterMode.OFF));

            ambientTemperatureLabel.textProperty().bind(Bindings.when(selectedPrinter.getPrinterAncillarySystems().ambientTemperatureProperty().
                greaterThan(ApplicationConfiguration.maxTempToDisplayOnGraph)).then(tempOutOfRangeHighString).otherwise(selectedPrinter.getPrinterAncillarySystems().ambientTemperatureProperty().
                    asString("%d°C")));
            ambientTargetTemperature.setText(String.format("%d", selectedPrinter.getPrinterAncillarySystems().ambientTargetTemperatureProperty().get()));
            selectedPrinter.getPrinterAncillarySystems().ambientTargetTemperatureProperty().addListener(targetAmbientTempListener);
            /*
             * Door
             */
//            doorStatusLabel.textProperty().bind(Bindings.when(selectedPrinter.LidOpenProperty()).then(openString).otherwise(closedString));

            if (selectedPrinter.reelsProperty().isEmpty() == false)
            {
                //TODO modify for multiple reels
                bindReelProperties(selectedPrinter.reelsProperty().get(0));
            }

            selectedPrinter.reelsProperty().addListener(reelChangeListener);

            selectedPrinter.getPrinterAncillarySystems().getAmbientTemperatureHistory().setName(Lookup.i18n("printerStatus.temperatureGraphAmbientLabel"));
            selectedPrinter.getPrinterAncillarySystems().getBedTemperatureHistory().setName(Lookup.i18n("printerStatus.temperatureGraphBedLabel"));

            currentAmbientTemperatureHistory = selectedPrinter.getPrinterAncillarySystems().getAmbientTemperatureHistory();
            temperatureChart.getData().add(selectedPrinter.getPrinterAncillarySystems().getAmbientTemperatureHistory());
            temperatureChart.getData().add(selectedPrinter.getPrinterAncillarySystems().getBedTemperatureHistory());

            selectedPrinter.getPrinterAncillarySystems().bedHeaterModeProperty().addListener(bedHeaterStatusListener);

            bedHeaterCheckBox.selectedProperty()
                .addListener(bedHeaterCheckBoxListener);
            nozzleHeaterCheckBox.selectedProperty()
                .addListener(nozzleHeaterCheckBoxListener);
            nozzleHeaterCheckBox.disableProperty().bind(selectedPrinter.headProperty().isNull());

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

    /**
     *
     * @param slideOutController
     */
    @Override
    public void configure(Initializable slideOutController)
    {
        this.slideOutController = (PrinterStatusSlideOutPanelController) slideOutController;
    }

    private void unbindHeadProperties(Head head)
    {
        printHeadLabel.textProperty().unbind();
        printHeadLabel.setText(headNotAttachedString);

        //TODO modify for multiple heaters
        temperatureChart.getData().remove(head.getNozzleHeaters().get(0).getNozzleTemperatureHistory());

        //TODO modify to support multiple heaters
        head.getNozzleHeaters().get(0).heaterModeProperty().removeListener(nozzleHeaterStatusListener);
    }

    private void bindHeadProperties(Head head)
    {
        //TODO modify for multiple heaters
        nozzleTemperatureLabel.textProperty().bind(Bindings.when(head.getNozzleHeaters().get(0).nozzleTemperatureProperty()
            .greaterThan(ApplicationConfiguration.maxTempToDisplayOnGraph))
            .then(tempOutOfRangeHighString)
            .otherwise(Bindings.when(head.getNozzleHeaters().get(0).nozzleTemperatureProperty()
                    .lessThan(ApplicationConfiguration.minTempToDisplayOnGraph)).then(tempOutOfRangeLowString)
                .otherwise(head.getNozzleHeaters().get(0).nozzleTemperatureProperty().asString("%d°C"))));
        //TODO modify to support multiple heaters
        nozzleFirstLayerTargetTemperature.setText(String.format("%d", head.getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty().get()));
        //TODO modify to support multiple nozzles
        head.getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty().addListener(targetNozzleFirstLayerTempListener);
        //TODO modify to support multiple heaters
        nozzleTargetTemperature.setText(String.format("%d", head.getNozzleHeaters().get(0).nozzleTargetTemperatureProperty().get()));
        head.getNozzleHeaters().get(0).nozzleTargetTemperatureProperty().addListener(targetNozzleTempListener);
        //TODO modify to support multiple heaters
        nozzleFirstLayerTargetTemperature.visibleProperty().bind(head.getNozzleHeaters().get(0).heaterModeProperty().isEqualTo(HeaterMode.FIRST_LAYER));
        //TODO modify to support multiple heaters
        nozzleTargetTemperature.visibleProperty().bind(head.getNozzleHeaters().get(0).heaterModeProperty().isEqualTo(HeaterMode.NORMAL));
        //TODO modify to support multiple heaters
        nozzleTemperaturePlaceholder.visibleProperty().bind(head.getNozzleHeaters().get(0).heaterModeProperty().isEqualTo(HeaterMode.OFF));

        /*
         * Head
         */
        printHeadLabel.textProperty().bind(head.typeCodeProperty());

        //TODO modify for multiple heaters
        head.getNozzleHeaters().get(0).getNozzleTemperatureHistory().getData().addListener(graphDataPointChangeListener);

        //TODO modify for multiple heaters
        head.getNozzleHeaters().get(0).getNozzleTemperatureHistory().setName(Lookup.i18n("printerStatus.temperatureGraphNozzleLabel"));

        //TODO modify to support multiple heaters
        temperatureChart.getData().add(head.getNozzleHeaters().get(0).getNozzleTemperatureHistory());

        //TODO modify to work with multiple heaters
        head.getNozzleHeaters().get(0).heaterModeProperty().addListener(nozzleHeaterStatusListener);
    }

    private void unbindReelProperties()
    {
        filamentStatusLabel.textProperty().unbind();
    }

    private void bindReelProperties(Reel reel)
    {
        /*
         * Reel
         */
        //TODO modify to work with multiple reels
        filamentStatusLabel.textProperty().bind(reel.friendlyFilamentNameProperty());
    }
}
