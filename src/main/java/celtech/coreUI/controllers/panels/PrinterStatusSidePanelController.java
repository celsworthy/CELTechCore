package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.coreUI.components.material.MaterialComponent;
import celtech.coreUI.components.printerstatus.PrinterGridComponent;
import celtech.printerControl.model.Extruder;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.NozzleHeater;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
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
    private VBox materialContainer;

    @FXML
    private HBox temperatureChartXLabels;

    @FXML
    private GridPane legendContainer;

    @FXML
    protected LineChart<Number, Number> temperatureChart;

    @FXML
    private NumberAxis temperatureAxis;
    @FXML
    private NumberAxis timeAxis;

    @FXML
    private Label legendNozzleS;

    @FXML
    private Label legendNozzleT;

    @FXML
    private Label legendBed;

    @FXML
    private Label legendAmbient;

    @FXML
    private PrinterGridComponent printerGridComponent;

    private Printer previousSelectedPrinter = null;
    private ObjectProperty<Printer> selectedPrinter = new SimpleObjectProperty<>();

    private final int MAX_DATA_POINTS = 210;

    private LineChart.Series<Number, Number> currentAmbientTemperatureHistory = null;

    private ChartManager chartManager;

    private final ListChangeListener<XYChart.Data<Number, Number>> graphDataPointChangeListener
        = (ListChangeListener.Change<? extends XYChart.Data<Number, Number>> change) ->
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
        };

    /**
     * Initialises the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        chartManager = new ChartManager(temperatureChart);

        selectedPrinter.bind(printerGridComponent.getSelectedPrinter());
        selectedPrinter.addListener(
            (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
            {
                whenPrinterSelected(newValue);
            });

        initialiseTemperatureChart();
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

    /**
     * When a printer is selected bind to it and show temperature chart etc if necessary.
     *
     * @param printer
     */
    private void whenPrinterSelected(Printer printer)
    {
        if (previousSelectedPrinter != null)
        {
            unbindPrinter(previousSelectedPrinter);
            if (previousSelectedPrinter.headProperty().get() != null)
            {
                unbindHeadProperties(previousSelectedPrinter.headProperty().get());
            }
        }

        if (printer != null)
        {
            previousSelectedPrinter = printer;
            Lookup.setCurrentlySelectedPrinter(printer);
            bindDetails(printer);
            if (printer.headProperty().get() != null)
            {
                bindHeadProperties(printer.headProperty().get());
            }
        }
        controlDetailsVisibility();
    }

    private void bindDetails(Printer printer)
    {
        if (selectedPrinter.get() != null)
        {
            unbindPrinter(selectedPrinter.get());
        }

        if (printer != null)
        {
            bindPrinter(printer);
        }
    }

    private void bindPrinter(Printer printer)
    {
        currentAmbientTemperatureHistory = printer.getPrinterAncillarySystems().getAmbientTemperatureHistory();
        chartManager.setLegendLabels(legendNozzleS, legendNozzleT, legendBed, legendAmbient);
        chartManager.bindPrinter(printer);

        bindMaterialContainer(printer);
    }

    private void bindMaterialContainer(Printer printer)
    {
        materialContainer.getChildren().clear();
        for (int extruderNumber = 0; extruderNumber < 2; extruderNumber++)
        {
            Extruder extruder = printer.extrudersProperty().get(extruderNumber);
            if (extruder.isFittedProperty().get())
            {
                MaterialComponent materialComponent
                    = new MaterialComponent(MaterialComponent.Mode.STATUS, printer, extruderNumber);
                materialContainer.getChildren().add(materialComponent);
            }
        }
    }

    private void unbindMaterialContainer()
    {
        materialContainer.getChildren().clear();
    }

    private void unbindPrinter(Printer printer)
    {
        if (printer.headProperty().get() != null)
        {
            unbindHeadProperties(printer.headProperty().get());
        }

        currentAmbientTemperatureHistory = null;
        chartManager.unbindPrinter();

        unbindMaterialContainer();
    }

    private void unbindHeadProperties(Head head)
    {
        head.getNozzleHeaters().get(0).getNozzleTemperatureHistory().getData().removeListener(
            graphDataPointChangeListener);
        chartManager.removeAllNozzles();
    }

    private void bindHeadProperties(Head head)
    {
        head.getNozzleHeaters().get(0).getNozzleTemperatureHistory().getData().addListener(
            graphDataPointChangeListener);

        for (int i = 0; i < head.getNozzleHeaters().size(); i++)
        {
            NozzleHeater nozzleHeater = head.getNozzleHeaters().get(i);
            chartManager.addNozzle(i,
                                   nozzleHeater.getNozzleTemperatureHistory(),
                                   nozzleHeater.heaterModeProperty(),
                                   nozzleHeater.nozzleTargetTemperatureProperty(),
                                   nozzleHeater.nozzleFirstLayerTargetTemperatureProperty(),
                                   nozzleHeater.nozzleTemperatureProperty());

        }
    }

    private void controlDetailsVisibility()
    {
        boolean visible = selectedPrinter.get() != null;

        temperatureChart.setVisible(visible);
        temperatureChartXLabels.setVisible(visible);
        legendContainer.setVisible(visible);
        materialContainer.setVisible(visible);
    }

    @Override
    public void configure(Initializable slideOutController)
    {
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
        controlDetailsVisibility();
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        controlDetailsVisibility();
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        if (printer == selectedPrinter.get())
        {
            Head head = printer.headProperty().get();
            bindHeadProperties(head);
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        if (printer == selectedPrinter.get())
        {
            unbindHeadProperties(head);
        }
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelNumber)
    {
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
    }

    @Override
    public void whenExtruderAdded(Printer printer, int extruderIndex)
    {
        if (printer == selectedPrinter.get())
        {
            bindMaterialContainer(printer);
        }
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {
        if (printer == selectedPrinter.get())
        {
            bindMaterialContainer(printer);
        }
    }

}
