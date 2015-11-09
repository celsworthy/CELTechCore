package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.DisplayManager.DisplayScalingMode;
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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Group;
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

    private final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();

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

    @FXML
    private VBox headPanel;

    @FXML
    private Label headTitleBold;

    @FXML
    private Label headTitleLight;

    @FXML
    private Label headDescription;

    @FXML
    private Label headNozzles;

    @FXML
    private Label headFeeds;

    @FXML
    private Group singleMaterialHead;

    @FXML
    private Group dualMaterialHead;

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

    private ChangeListener<Boolean> filamentLoadedListener = new ChangeListener<Boolean>()
    {
        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
        {
            refreshMaterialContainer(previousSelectedPrinter);
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

        headPanel.setVisible(false);

        Lookup.getPrinterListChangesNotifier().addListener(this);
        DisplayManager.getInstance().getDisplayScalingModeProperty().addListener((ObservableValue<? extends DisplayManager.DisplayScalingMode> observable, DisplayManager.DisplayScalingMode oldValue, DisplayManager.DisplayScalingMode newValue) ->
        {
            updateForDisplayScaling(newValue);
        });

        updateForDisplayScaling(DisplayManager.getInstance().getDisplayScalingModeProperty().get());
    }

    private void updateForDisplayScaling(DisplayManager.DisplayScalingMode displayScalingMode)
    {
//        switch (displayScalingMode)
//        {
//            case NORMAL:
//                temperatureChart.setMaxHeight(180);
//                break;
//            case SHORT:
//                temperatureChart.setMaxHeight(160);
//                break;
//            case VERY_SHORT:
//                temperatureChart.setMaxHeight(140);
//                break;
//        }
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
     * When a printer is selected bind to it and show temperature chart etc if
     * necessary.
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
            Lookup.setSelectedPrinter(printer);
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

        printer.extrudersProperty().forEach(extruder ->
        {
            extruder.filamentLoadedProperty().addListener(filamentLoadedListener);
        });

        refreshMaterialContainer(printer);
    }

    private void refreshMaterialContainer(Printer printer)
    {
        materialContainer.getChildren().clear();
        for (int extruderNumber = 0; extruderNumber < 2; extruderNumber++)
        {
            Extruder extruder = printer.extrudersProperty().get(extruderNumber);
            if (extruder.filamentLoadedProperty().get())
            {
                MaterialComponent materialComponent
                        = new MaterialComponent(MaterialComponent.Mode.SETTINGS, printer, extruderNumber);
                materialContainer.getChildren().add(materialComponent);
                if (printer.extrudersProperty().size() > 1)
                {
                    materialComponent.setMaxHeight(110);
                } else
                {
                    materialComponent.setMaxHeight(120);
                }
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

        printer.extrudersProperty().forEach(extruder ->
        {
            extruder.filamentLoadedProperty().removeListener(filamentLoadedListener);
        });

        unbindMaterialContainer();
    }

    private void unbindHeadProperties(Head head)
    {
        head.getNozzleHeaters().get(0).getNozzleTemperatureHistory().getData().removeListener(
                graphDataPointChangeListener);
        chartManager.removeAllNozzles();
        headPanel.setPrefHeight(0);
        headPanel.setMinHeight(0);
        headPanel.setVisible(false);
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
        headPanel.setPrefHeight(-1);
        headPanel.setVisible(true);
        headTitleBold.setText(Lookup.i18n("headPanel." + head.typeCodeProperty().get() + ".titleBold"));
        headTitleLight.setText(Lookup.i18n("headPanel." + head.typeCodeProperty().get() + ".titleLight"));
        headDescription.setText(Lookup.i18n("headPanel." + head.typeCodeProperty().get() + ".description"));
        headNozzles.setText(Lookup.i18n("headPanel." + head.typeCodeProperty().get() + ".nozzles"));
        headFeeds.setText(Lookup.i18n("headPanel." + head.typeCodeProperty().get() + ".feeds"));

        if (head.headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
        {
            dualMaterialHead.setVisible(true);
            singleMaterialHead.setVisible(false);
        } else
        {
            dualMaterialHead.setVisible(false);
            singleMaterialHead.setVisible(true);
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
            refreshMaterialContainer(printer);
        }
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {
        if (printer == selectedPrinter.get())
        {
            refreshMaterialContainer(printer);
        }
    }

}
