/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.configuration.EEPROMState;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.material.MaterialComponent;
import celtech.coreUI.components.printerstatus.PrinterComponent;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
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
import javafx.geometry.Insets;
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
    private LineChart<Number, Number> temperatureChart;

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

    private ChangeListener reelDataChangedListener;

    private final List<Printer> activePrinters = new ArrayList<>();

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
        clearAndAddAllPrintersToGrid();

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
                            activePrinters.add(printer);
                            System.out.println("add printer " + printer);
                            clearAndAddAllPrintersToGrid();
                            selectPrinter(printer);
                        }
                    } else if (change.wasRemoved())
                    {
                        for (Printer printer : change.getRemoved())
                        {
                            System.out.println("remove printer " + printer);
                            removePrinter(printer);
                            activePrinters.remove(printer);
                            clearAndAddAllPrintersToGrid();
                            selectOnePrinter();
                        }
                    } else if (change.wasReplaced())
                    {
                        System.out.println("printer replaced");
                    } else if (change.wasUpdated())
                    {
                        System.out.println("printer updated");
                    }
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
        System.out.println("set size to " + size + " for printerC " + printerComponent);
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
        System.out.println("bind printer " + printer);
        updateReelMaterial(printer);
        reelDataChangedListener = (ChangeListener) (ObservableValue observable, Object oldValue, Object newValue) ->
        {
            updateReelMaterial(printer);
        };
        printer.reelDataChangedProperty().addListener(reelDataChangedListener);

        currentAmbientTemperatureHistory = printer.ambientTemperatureHistory();
        temperatureChart.getData().add(printer.ambientTemperatureHistory());
        printer.nozzleTemperatureHistory().getData().addListener(
            graphDataPointChangeListener);
        temperatureChart.getData().add(printer.bedTemperatureHistory());
        temperatureChart.getData().add(printer.nozzleTemperatureHistory());
        temperatureChart.getData().add(printer.ambientTargetTemperatureHistory());
        temperatureChart.getData().add(printer.bedTargetTemperatureHistory());
        temperatureChart.getData().add(printer.nozzleTargetTemperatureHistory());

    }

    /**
     * Update the material component with the appropriate details.
     *
     * @param printer
     */
    private void updateReelMaterial(Printer printer)
    {
        System.out.println("update material for printer " + printer);
        if (printer.reelEEPROMStatusProperty().get().equals(EEPROMState.PROGRAMMED))
        {
            material1.setMaterial(1, printer.getReelMaterialType(),
                                  printer.reelFriendlyNameProperty().get(),
                                  printer.getReelDisplayColour(),
                                  printer.getReelRemainingFilament().get(),
                                  printer.getReelFilamentDiameter().get());
        } else if (printer.reelEEPROMStatusProperty().get().equals(
            EEPROMState.NOT_PROGRAMMED))
        {
            material1.showReelNotFormatted();
        } else
        {
            material1.showFilamentNotLoaded();
        }
    }

    private void unbindPrinter(Printer printer)
    {
        if (reelDataChangedListener != null)
        {
            printer.reelDataChangedProperty().removeListener(reelDataChangedListener);
        }
        temperatureChart.getData().remove(printer.ambientTemperatureHistory());
        currentAmbientTemperatureHistory = null;
        temperatureChart.getData().remove(printer.bedTemperatureHistory());
        temperatureChart.getData().remove(printer.nozzleTemperatureHistory());
        temperatureChart.getData().remove(printer.ambientTargetTemperatureHistory());
        temperatureChart.getData().remove(printer.bedTargetTemperatureHistory());
        temperatureChart.getData().remove(printer.nozzleTargetTemperatureHistory());

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
