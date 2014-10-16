/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.material.MaterialComponent;
import celtech.coreUI.components.printerstatus.PrinterComponent;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.PrinterAncillarySystems;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.PrinterIdentity;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.net.URL;
import java.util.HashMap;
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
public class PrinterStatusSidePanelController implements Initializable, SidePanelManager,
    PrinterListChangesListener
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

    private StatusScreenState statusScreenState = null;

    private PrinterIDDialog printerIDDialog = null;

    private Printer selectedPrinter = null;
    private final Map<Printer, PrinterComponent> printerComponentsByPrinter = new HashMap<>();

    private final int MAX_DATA_POINTS = 210;

    private final ObservableList<Printer> connectedPrinters = Lookup.getConnectedPrinters();

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

    private final PrinterColourMap colourMap = PrinterColourMap.getInstance();

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
        statusScreenState = StatusScreenState.getInstance();

        printerIDDialog = new PrinterIDDialog();

        initialiseTemperatureChart();
        initialisePrinterStatusGrid();
        controlDetailsVisibility();

        Lookup.getPrinterListChangesNotifier().addListener(this);

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
        clearAndAddAllPrintersToGrid();
    }

    private void clearAndAddAllPrintersToGrid()
    {
        removeAllPrintersFromGrid();
        int row = 0;
        int column = 0;
        for (Printer printer : connectedPrinters)
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
        if (connectedPrinters.size() == 1)
        {
            printerStatusGrid.setPrefSize(260, 260);

        } else if (connectedPrinters.size() == 2)
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
        if (connectedPrinters.size() > 1)
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
        for (Printer printer : connectedPrinters)
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
        if (connectedPrinters.size() > 0)
        {
            selectPrinter(connectedPrinters.get(0));
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
            printerComponent.setSelected(false);
            unbindPrinter(selectedPrinter);
            if (printer.headProperty().get() != null) {
                unbindHeadProperties(printer.headProperty().get());
            }
            if (! printer.reelsProperty().isEmpty()) {
                unbindReelProperties(printer.reelsProperty().get(0));
            }
        }

        if (printer != null)
        {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
            printerComponent.setSelected(true);
            statusScreenState.setCurrentlySelectedPrinter(printer);
            bindDetails(printer);
            if (printer.headProperty().get() != null) {
                bindHeadProperties(printer.headProperty().get());
            }
            if (! printer.reelsProperty().isEmpty()) {
                bindReelProperties(printer.reelsProperty().get(0));
            }
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

    private void bindPrinter(Printer printer)
    {
        PrinterAncillarySystems ancillarySystems = printer.getPrinterAncillarySystems();
        currentAmbientTemperatureHistory = ancillarySystems.getAmbientTemperatureHistory();

        chartManager.setAmbientData(ancillarySystems.getAmbientTemperatureHistory());
        chartManager.setBedData(ancillarySystems.getBedTemperatureHistory());

        chartManager.setTargetAmbientTemperatureProperty(
            ancillarySystems.ambientTargetTemperatureProperty());
        chartManager.setTargetBedTemperatureProperty(ancillarySystems.bedTargetTemperatureProperty());
    }

    private void unbindPrinter(Printer printer)
    {
        if (printer.headProperty().get() != null)
        {
            unbindHeadProperties(printer.headProperty().get());
        }
        if (! printer.reelsProperty().isEmpty()) {
            unbindReelProperties(printer.reelsProperty().get(0));
        }
        updateReelMaterial(null);
        
        chartManager.clearAmbientData();
        chartManager.clearBedData();
        currentAmbientTemperatureHistory = null;
    }    

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
        chartManager.clearNozzleData();
    }

    private void bindHeadProperties(Head head)
    {
        head.getNozzleHeaters().get(0).getNozzleTemperatureHistory().getData().addListener(
            graphDataPointChangeListener);

        chartManager.setNozzleData(head.getNozzleHeaters().get(0).getNozzleTemperatureHistory());
        chartManager.setTargetNozzleTemperatureProperty(
            head.getNozzleHeaters().get(0).nozzleTargetTemperatureProperty());
    }

    /**
     * Update the material component with the appropriate details.
     */
    private void updateReelMaterial(Reel reel)
    {
        if (reel == null)
        {
            material1.showFilamentNotLoaded();
        } else
        {
            material1.setMaterial(1, reel.materialProperty().get(),
                                  reel.friendlyFilamentNameProperty().get(),
                                  reel.displayColourProperty().get(),
                                  reel.remainingFilamentProperty().get(),
                                  reel.diameterProperty().get());
        }
//            material1.showReelNotFormatted();
    }

    private void controlDetailsVisibility()
    {
        boolean visible = connectedPrinters.size() > 0;

        temperatureVBox.setVisible(visible);
        temperatureChart.setVisible(visible);
        temperatureChartXLabels.setVisible(visible);
        materialContainer.setVisible(visible);

        legendContainer.setVisible(visible);
    }

    @Override
    public void configure(Initializable slideOutController)
    {
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
        clearAndAddAllPrintersToGrid();
        selectPrinter(printer);
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        removePrinter(printer);
        clearAndAddAllPrintersToGrid();
        selectOnePrinter();
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        if (printer == selectedPrinter)
        {
            Head head = printer.headProperty().get();
            bindHeadProperties(head);
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        if (printer == selectedPrinter)
        {
            unbindHeadProperties(head);
        }
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
        if (printer == selectedPrinter)
        {
            Reel reel = printer.reelsProperty().get(0);
            bindReelProperties(reel);
            updateReelMaterial(reel);
        }
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel)
    {
        if (printer == selectedPrinter)
        {
            unbindReelProperties(reel);
            updateReelMaterial(null);
        }
    }

}
