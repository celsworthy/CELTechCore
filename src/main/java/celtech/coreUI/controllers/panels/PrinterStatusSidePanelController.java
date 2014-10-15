/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.configuration.PrinterColourMap;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.material.MaterialComponent;
import celtech.coreUI.components.printerstatus.PrinterComponent;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.model.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.PrinterAncillarySystems;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.PrinterIdentity;
import celtech.printerControl.model.Reel;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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

    @FXML
    private MaterialComponent material1;

    @FXML
    private HBox materialContainer;

    @FXML
    private HBox temperatureChartXLabels;

    @FXML
    private HBox legendContainer;

    @FXML
    private VBox temperatureVBox;

    @FXML
    protected LineChart<Number, Number> temperatureChart;

    @FXML
    private GridPane printerStatusGrid;

    @FXML
    private NumberAxis temperatureAxis;
    @FXML
    private NumberAxis timeAxis;

    private ObservableList<Printer> printerStatusList = null;
    private StatusScreenState statusScreenState = null;

    private PrinterIDDialog printerIDDialog = null;

    private Printer selectedPrinter = null;
    private final Map<Printer, PrinterComponent> printerComponentsByPrinter = new HashMap<>();

    private final int MAX_DATA_POINTS = 210;

    private final List<Printer> activePrinters = new ArrayList<>();

    private LineChart.Series<Number, Number> currentAmbientTemperatureHistory = null;
    
    private ChartManager chartManager;

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
        chartManager = new ChartManager(temperatureChart);
        RoboxCommsManager commsManager = RoboxCommsManager.getInstance();
        printerStatusList = commsManager.getPrintStatusList();
        statusScreenState = StatusScreenState.getInstance();

        printerIDDialog = new PrinterIDDialog();

        initialiseTemperatureChart();
        initialisePrinterStatusGrid();
        controlDetailsVisibility();
        
        
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

    private void initialisePrinterStatusGrid()
    {
        activePrinters.addAll(printerStatusList);
        clearAndAddAllPrintersToGrid();

        printerStatusList.addListener((ListChangeListener.Change<? extends Printer> change) ->
        {
            while (change.next())
            {
                if (change.wasAdded())
                {
                    for (Printer printer : change.getAddedSubList())
                    {
                        activePrinters.add(printer);
                        clearAndAddAllPrintersToGrid();
                        selectPrinter(printer);
                    }
                } else if (change.wasRemoved())
                {
                    for (Printer printer : change.getRemoved())
                    {
                        removePrinter(printer);
                        activePrinters.remove(printer);
                        clearAndAddAllPrintersToGrid();
                        selectOnePrinter();
                    }
                } else if (change.wasReplaced())
                {
                } else if (change.wasUpdated())
                {
                }
            }
        });

    }

    private void clearAndAddAllPrintersToGrid()
    {
        removeAllPrintersFromGrid();
        int row = 0;
        int column = 0;
        for (Printer printer : activePrinters)
        {
            PrinterComponent printerComponent = createPrinterComponentForPrinter(printer);
            addPrinterComponentToGrid(printerComponent, row, column);
            column += 1;
            if (column == 2)
            {
                column = 0;
                row += 1;
            }
        }
        // UGH shouldnt need this here but can't get PrinterComponent / Grid to negotiate size
        if (activePrinters.size() == 1)
        {
            printerStatusGrid.setPrefSize(260, 260);

        } else if (activePrinters.size() == 2)
        {
            printerStatusGrid.setPrefSize(260, 120);
        } else
        {
            printerStatusGrid.setPrefSize(260, 260);
        }
    }

    /**
     * Add the given printer component to the given grid coordinates.
     */
    private void addPrinterComponentToGrid(PrinterComponent printerComponent, int row,
        int column)
    {
        PrinterComponent.Size size;
        if (activePrinters.size() > 1)
        {
            size = PrinterComponent.Size.SIZE_MEDIUM;
        } else
        {
            size = PrinterComponent.Size.SIZE_LARGE;
        }
        printerComponent.setSize(size);
        printerStatusGrid.add(printerComponent, column, row);
    }

    private void removeAllPrintersFromGrid()
    {
        for (Printer printer : activePrinters)
        {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
            removePrinterComponentFromGrid(printerComponent);
        }
    }

    /**
     * Remove the given printer from the display. Update the selected printer to one of the
     * remaining printers.
     */
    private void removePrinter(Printer printer)
    {
        PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
        removePrinterComponentFromGrid(printerComponent);
    }

    /**
     * Select any one of the active printers.
     */
    private void selectOnePrinter()
    {
        if (activePrinters.size() > 0)
        {
            selectPrinter(activePrinters.get(0));
        } else
        {
            selectPrinter(null);
        }
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
//            statusScreenState.setCurrentlySelectedPrinter(null);
            printerComponent.setSelected(false);
        }

        if (printer != null)
        {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
            printerComponent.setSelected(true);
            statusScreenState.setCurrentlySelectedPrinter(printer);
            bindDetails(printer);
        }
        controlDetailsVisibility();

        selectedPrinter = printer;
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
            PrinterIdentity printerIdentity = printer.getPrinterIdentity();
            printerIDDialog.setChosenDisplayColour(colourMap.printerToDisplayColour(
                printerIdentity.printerColourProperty().get()));
            printerIDDialog.setChosenPrinterName(printerIdentity.printerFriendlyNameProperty().get());

            boolean okPressed = printerIDDialog.show();

            if (okPressed)
            {
                try
                {
                    printer.updatePrinterName(printerIDDialog.getChosenPrinterName());
                    printer.updatePrinterDisplayColour(colourMap.displayToPrinterColour(
                        printerIDDialog.getChosenDisplayColour()));
                } catch (PrinterException ex)
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
     * Remove the given printer from the grid.
     *
     * @param printerComponent
     */
    private void removePrinterComponentFromGrid(PrinterComponent printerComponent)
    {
        printerStatusGrid.getChildren().remove(printerComponent);
    }

    private void bindDetails(Printer printer)
    {
        if (selectedPrinter != null)
        {
            unbindPrinter(selectedPrinter);
        }

        if (printer != null)
        {
            bindPrinter(printer);
        }
    }

    private void unbindPrinter(Printer printer)
    {
        if (printer.headProperty().get() != null)
        {
            unbindHeadProperties(printer.headProperty().get());
            printer.headProperty().removeListener(headChangeListener);
        }
        
        printer.reelsProperty().removeListener(reelsChangedListener);

        chartManager.clearAmbientData();
        chartManager.clearBedData();
        currentAmbientTemperatureHistory = null;

//        temperatureChart.getData().remove(printer.ambientTargetTemperatureHistory());
//        temperatureChart.getData().remove(printer.bedTargetTemperatureHistory());
//        temperatureChart.getData().remove(printer.nozzleTargetTemperatureHistory());
    }

    private void bindPrinter(Printer printer)
    {
        if (printer.headProperty().get() != null)
        {
            bindHeadProperties(printer.headProperty().get());
        }
        printer.headProperty().addListener(headChangeListener);

        printer.reelsProperty().addListener(reelsChangedListener);
        if (!printer.reelsProperty().isEmpty())
        {
            bindReelProperties(printer.reelsProperty().get(0));
            updateReelMaterial(printer.reelsProperty().get(0));
        }

        PrinterAncillarySystems ancillarySystems = printer.getPrinterAncillarySystems();
        currentAmbientTemperatureHistory = ancillarySystems.getAmbientTemperatureHistory();
        
        chartManager.setAmbientData(ancillarySystems.getAmbientTemperatureHistory());
        chartManager.setBedData(ancillarySystems.getBedTemperatureHistory());
        
        chartManager.setTargetAmbientTemperatureProperty(ancillarySystems.ambientTargetTemperatureProperty());
        chartManager.setTargetBedTemperatureProperty(ancillarySystems.bedTargetTemperatureProperty());

//        temperatureChart.getData().add(printer.ambientTargetTemperatureProperty());
//        temperatureChart.getData().add(printer.bedTargetTemperatureHistory());
//        temperatureChart.getData().add(printer.nozzleTargetTemperatureHistory());
    }

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

    private final ListChangeListener<Reel> reelsChangedListener = new ListChangeListener<Reel>()
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
                        updateReelMaterial(changedReel);
                    }
                } else if (change.wasRemoved())
                {
                    for (Reel changedReel : change.getRemoved())
                    {
                        unbindReelProperties(changedReel);
                    }
                } else if (change.wasReplaced())
                {
                } else if (change.wasUpdated())
                {
                }
            }
        }
    };

    private ChangeListener<Object> reelListener;

    private void unbindReelProperties(Reel reel)
    {
        reel.friendlyFilamentNameProperty().removeListener(reelListener);
        reel.displayColourProperty().removeListener(reelListener);
        reel.remainingFilamentProperty().removeListener(reelListener);
        reel.diameterProperty().removeListener(reelListener);
        reel.materialProperty().removeListener(reelListener);
    }

    private void bindReelProperties(Reel reel)
    {
        reelListener = (ObservableValue<? extends Object> observable, Object oldValue, Object newValue) ->
        {
            updateReelMaterial(reel);
        };
        reel.friendlyFilamentNameProperty().addListener(reelListener);
        reel.displayColourProperty().addListener(reelListener);
        reel.remainingFilamentProperty().addListener(reelListener);
        reel.diameterProperty().addListener(reelListener);
        reel.materialProperty().addListener(reelListener);
    }

    private void unbindHeadProperties(Head head)
    {
//        printHeadLabel.textProperty().unbind();
//        printHeadLabel.setText(headNotAttachedString);

        //TODO modify for multiple heaters
        chartManager.clearNozzleData();

        //TODO modify to support multiple heaters
//        head.getNozzleHeaters().get(0).heaterModeProperty().removeListener(nozzleHeaterStatusListener);
    }

    private void bindHeadProperties(Head head)
    {
//        //TODO modify for multiple heaters
//        nozzleTemperatureLabel.textProperty().bind(Bindings.when(head.getNozzleHeaters().get(0).nozzleTemperatureProperty()
//            .greaterThan(ApplicationConfiguration.maxTempToDisplayOnGraph))
//            .then(tempOutOfRangeHighString)
//            .otherwise(Bindings.when(head.getNozzleHeaters().get(0).nozzleTemperatureProperty()
//                    .lessThan(ApplicationConfiguration.minTempToDisplayOnGraph)).then(tempOutOfRangeLowString)
//                .otherwise(head.getNozzleHeaters().get(0).nozzleTemperatureProperty().asString("%dÂ°C"))));
//        //TODO modify to support multiple heaters
//        nozzleFirstLayerTargetTemperature.setText(String.format("%d", head.getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty().get()));
//        //TODO modify to support multiple nozzles
//        head.getNozzleHeaters().get(0).nozzleFirstLayerTargetTemperatureProperty().addListener(targetNozzleFirstLayerTempListener);
//        //TODO modify to support multiple heaters
//        nozzleTargetTemperature.setText(String.format("%d", head.getNozzleHeaters().get(0).nozzleTargetTemperatureProperty().get()));
//        head.getNozzleHeaters().get(0).nozzleTargetTemperatureProperty().addListener(targetNozzleTempListener);
//        //TODO modify to support multiple heaters
//        nozzleFirstLayerTargetTemperature.visibleProperty().bind(head.getNozzleHeaters().get(0).heaterModeProperty().isEqualTo(HeaterMode.FIRST_LAYER));
//        //TODO modify to support multiple heaters
//        nozzleTargetTemperature.visibleProperty().bind(head.getNozzleHeaters().get(0).heaterModeProperty().isEqualTo(HeaterMode.NORMAL));
//        //TODO modify to support multiple heaters
//        nozzleTemperaturePlaceholder.visibleProperty().bind(head.getNozzleHeaters().get(0).heaterModeProperty().isEqualTo(HeaterMode.OFF));
//
//        /*
//         * Head
//         */
//        printHeadLabel.textProperty().bind(head.typeCodeProperty());

        //TODO modify for multiple heaters
        head.getNozzleHeaters().get(0).getNozzleTemperatureHistory().getData().addListener(
            graphDataPointChangeListener);

        //TODO modify for multiple heaters
//        head.getNozzleHeaters().get(0).getNozzleTemperatureHistory().setName(Lookup.i18n(
//            "printerStatus.temperatureGraphNozzleLabel"));
        //TODO modify to support multiple heaters
        chartManager.setNozzleData(head.getNozzleHeaters().get(0).getNozzleTemperatureHistory());
        chartManager.setTargetNozzleTemperatureProperty(head.getNozzleHeaters().get(0).nozzleTargetTemperatureProperty());

        //TODO modify to work with multiple heaters
//        head.getNozzleHeaters().get(0).heaterModeProperty().addListener(nozzleHeaterStatusListener);
    }

    /**
     * Update the material component with the appropriate details.
     *
     * @param printer
     */
    private void updateReelMaterial(Reel reel)
    {
        material1.setMaterial(1, reel.materialProperty().get(),
                              reel.friendlyFilamentNameProperty().get(),
                              reel.displayColourProperty().get(),
                              reel.remainingFilamentProperty().get(),
                              reel.diameterProperty().get());
//            material1.showReelNotFormatted();
//            material1.showFilamentNotLoaded();
    }

    private void controlDetailsVisibility()
    {
        boolean visible = printerStatusList.size() > 0;

        temperatureVBox.setVisible(visible);
        temperatureChart.setVisible(visible);
        temperatureChartXLabels.setVisible(visible);
        materialContainer.setVisible(visible);

        legendContainer.setVisible(visible);
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
