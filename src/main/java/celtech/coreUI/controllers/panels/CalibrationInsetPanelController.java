package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.components.VerticalMenu;
import celtech.coreUI.components.LargeProgress;
import celtech.coreUI.components.Spinner;
import static celtech.coreUI.controllers.panels.CalibrationMenuConfiguration.configureCalibrationMenu;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.NozzleHeater;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import celtech.printerControl.model.calibration.StateTransitionManager;
import celtech.printerControl.model.calibration.XAndYStateTransitionManager;
import celtech.services.calibration.CalibrationXAndYState;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import celtech.services.calibration.NozzleOpeningCalibrationState;
import celtech.utils.PrinterListChangesListener;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationInsetPanelController implements Initializable,
    CalibrationBStateListener, CalibrationNozzleOffsetStateListener,
    CalibrationXAndYStateListener, PrinterListChangesListener
{
    
    CalibrationXAndYGUI calibrationXAndYGUI;
    XAndYStateTransitionManager stateManager;

    private ResourceBundle resources;

    private void resizeTopBorderPane()
    {
        topBorderPane.setPrefWidth(topPane.getWidth());
        topBorderPane.setPrefHeight(topPane.getHeight());
    }

    void whenZCoChanged(double zco)
    {
        if (diagramController != null)
        {
            diagramController.setCalibrationTextField(String.format("%1.2f", zco));
        }
    }

    protected static enum ProgressVisibility
    {

        TEMP, PRINT, NONE;
    };

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationInsetPanelController.class.getName());

    private CalibrationHelper calibrationHelper;
    private CalibrationNozzleOffsetGUIStateHandler calibrationNozzleOffsetGUIStateHandler;
    private CalibrationNozzleBGUIStateHandler calibrationNozzleBGUIStateHandler;
    private CalibrationXAndYGUIStateHandler calibrationXAndYGUIStateHandler;

    @FXML
    protected VerticalMenu calibrationMenu;

    @FXML
    protected StackPane calibrateBottomMenu;

    @FXML
    protected Pane calibrationBottomArea;

    @FXML
    protected Pane altButtonContainer;

    @FXML
    protected LargeProgress calibrationProgressTemp;

    @FXML
    protected LargeProgress calibrationProgressPrint;

    @FXML
    protected Text stepNumber;

    @FXML
    protected Button buttonA;

    @FXML
    protected Button buttonB;

    @FXML
    protected Button nextButton;

    @FXML
    protected Button retryPrintButton;

    @FXML
    protected Button backToStatus;

    @FXML
    protected Button startCalibrationButton;

    @FXML
    protected Button cancelCalibrationButton;

    @FXML
    protected Label calibrationStatus;

    @FXML
    private BorderPane informationCentre;

    @FXML
    private BorderPane topBorderPane;

    @FXML
    private Pane topPane;

    private Printer currentPrinter;
    private int targetTemperature;
    private double currentExtruderTemperature;
    private int targetETC;
    private double printPercent;
    private Spinner spinner;
    private Node diagramNode;
    DiagramController diagramController;
    private final Map<String, Node> nameToNodeCache = new HashMap<>();

    @FXML
    void buttonAAction()
    {
        calibrationHelper.buttonAAction();
    }

    @FXML
    void buttonBAction()
    {
        calibrationHelper.buttonBAction();
    }

    @FXML
    void nextButtonAction(ActionEvent event)
    {
//        calibrationHelper.nextButtonAction();
        stateManager.followTransition(StateTransitionManager.GUIName.NEXT);
    }

    @FXML
    void backToStatusAction(ActionEvent event)
    {
        stateManager.followTransition(StateTransitionManager.GUIName.BACK);
        ApplicationStatus.getInstance().returnToLastMode();
        setCalibrationMode(CalibrationMode.CHOICE);
    }

    @FXML
    void startCalibration(ActionEvent event)
    {
//        calibrationHelper.nextButtonAction();
        stateManager.followTransition(StateTransitionManager.GUIName.START);
    }

    @FXML
    void cancelCalibration(ActionEvent event)
    {
        stateManager.followTransition(StateTransitionManager.GUIName.CANCEL);
        cancelCalibrationAction();
    }

    @FXML
    void retryCalibration(ActionEvent event)
    {
//        calibrationHelper.retryAction();
        stateManager.followTransition(StateTransitionManager.GUIName.RETRY);
    }

    /**
     *
     */
    public void cancelCalibrationAction()
    {
        ApplicationStatus.getInstance().returnToLastMode();
        if (calibrationHelper != null) {
            calibrationHelper.cancelCalibrationAction();
        }
        setCalibrationMode(CalibrationMode.CHOICE);
    }

    protected void hideAllInputControlsExceptStepNumber()
    {
        backToStatus.setVisible(false);
        setCalibrationProgressVisible(CalibrationInsetPanelController.ProgressVisibility.NONE);
        retryPrintButton.setVisible(false);
        startCalibrationButton.setVisible(false);
        cancelCalibrationButton.setVisible(false);
        nextButton.setVisible(false);
        buttonB.setVisible(false);
        buttonA.setVisible(false);
        stepNumber.setVisible(true);
        hideSpinner();
        if (diagramNode != null)
        {
            diagramNode.setVisible(false);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        this.resources = resources;

        setupProgressBars();
        setupSpinner(informationCentre);

        setCalibrationMode(CalibrationMode.CHOICE);

        Lookup.getPrinterListChangesNotifier().addListener(this);

        configureCalibrationMenu(calibrationMenu, this);

        addDiagramMoveScaleListeners();

    }

    private void addDiagramMoveScaleListeners()
    {
        topPane.widthProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                resizeDiagram();
                resizeTopBorderPane();
            });

        topPane.heightProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                resizeDiagram();
                resizeTopBorderPane();
            });

        calibrationStatus.widthProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                resizeDiagram();
            });

        calibrationStatus.heightProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                resizeDiagram();
            });

    }

    private void resizeDiagram()
    {
        Platform.runLater(this::resizeDiagramLater);
    }

    private void resizeDiagramLater()
    {
        if (diagramNode == null)
        {
            return;
        }

        double diagramWidth = diagramNode.getBoundsInLocal().getWidth();
        double diagramHeight = diagramNode.getBoundsInLocal().getHeight();

        Bounds statusBounds = calibrationStatus.localToScene(calibrationStatus.getBoundsInLocal());
        Bounds ancestorStatusBounds = topPane.sceneToLocal(statusBounds);
        double upperBoundaryInAncestorCoords = ancestorStatusBounds.getMaxY();

        Bounds bottomAreaBounds = buttonA.localToScene(buttonA.getBoundsInLocal());
        Bounds ancestorBottomBounds = topPane.sceneToLocal(bottomAreaBounds);
        double lowerBoundaryInAncestorCoords = ancestorBottomBounds.getMinY();
        double availableHeight = lowerBoundaryInAncestorCoords - upperBoundaryInAncestorCoords;
        double availableWidth = ancestorStatusBounds.getWidth();

        double requiredScaleHeight = availableHeight / diagramHeight * 0.95;
        double requiredScaleWidth = availableWidth / diagramWidth * 0.95;
        double requiredScale = Math.min(requiredScaleHeight, requiredScaleWidth);
        requiredScale = Math.min(requiredScale, 1.3d);
        steno.info("Setting scale to " + requiredScale);

        diagramController.setScale(requiredScale, diagramNode);

        double scaledDiagramWidth = diagramNode.getBoundsInLocal().getWidth();
        double scaledDiagramHeight = diagramNode.getBoundsInLocal().getHeight();

        double xTranslate = -scaledDiagramWidth / 2;
        double yTranslate = -scaledDiagramHeight / 2;

        xTranslate += ancestorStatusBounds.getMinX() + (ancestorStatusBounds.getWidth() / 2.0d);
        yTranslate += upperBoundaryInAncestorCoords + availableHeight / 2.0;

        xTranslate += 10; // Fudge factor

        diagramNode.setTranslateX(xTranslate);
        diagramNode.setTranslateY(yTranslate);

    }

    /**
     * If this diagramNode has already been loaded then return that, else create it and return it.
     *
     * @param diagramName
     * @return
     */
    private Node getDiagramNode(String section, String diagramName)
    {
        Pane loadedDiagramNode = null;
        if (!nameToNodeCache.containsKey(diagramName))
        {
            URL fxmlFileName = getClass().getResource(ApplicationConfiguration.fxmlResourcePath
                + "diagrams/" + section + "/" + diagramName);
            try
            {
                FXMLLoader loader = new FXMLLoader(fxmlFileName, resources);
                diagramController = new DiagramController(this);
                loader.setController(diagramController);
                loadedDiagramNode = loader.load();
                nameToNodeCache.put(diagramName, loadedDiagramNode);
            } catch (IOException ex)
            {
                ex.printStackTrace();
                steno.error("Could not load diagram: " + diagramName);
            }
        }
        return nameToNodeCache.get(diagramName);
    }

    protected void showDiagram(String section, String diagramName)
    {
        showDiagram(section, diagramName, true);
    }

    protected void showDiagram(String section, String diagramName, boolean transparent)
    {
        diagramNode = getDiagramNode(section, diagramName);
        if (diagramNode == null)
        {
            return;
        }
        if (topPane.getChildren().size() == 1)
        {
            topPane.getChildren().add(diagramNode);
        } else
        {
            Node firstChild = topPane.getChildren().get(0);
            topPane.getChildren().clear();
            topPane.getChildren().addAll(firstChild, diagramNode);
            diagramNode.setMouseTransparent(transparent);
        }

        resizeDiagram();
        diagramNode.setVisible(true);
    }

    @Override
    public void setNozzleOpeningState(NozzleOpeningCalibrationState state)
    {
        calibrationNozzleBGUIStateHandler.setNozzleOpeningState(state);
    }

    @Override
    public void setNozzleHeightState(NozzleOffsetCalibrationState state)
    {
        calibrationNozzleOffsetGUIStateHandler.setNozzleHeightState(state);
    }

    @Override
    public void setXAndYState(CalibrationXAndYState state)
    {
        calibrationXAndYGUIStateHandler.setXAndYState(state);
    }

    private final ChangeListener<Number> targetTemperatureListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        targetTemperature = newValue.intValue();
        updateCalibrationProgressTemp();
    };

    private final ChangeListener<Number> extruderTemperatureListener
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            currentExtruderTemperature = newValue.doubleValue();
            updateCalibrationProgressTemp();
        };

    private void updateCalibrationProgressTemp()
    {
        if (targetTemperature != 0 && calibrationProgressTemp.isVisible())
        {
            String targetTempStr = targetTemperature + Lookup.i18n("misc.degreesC");
            String currentTempStr = ((int) currentExtruderTemperature)
                + Lookup.i18n("misc.degreesC");
            calibrationProgressTemp.setCurrentValue(currentTempStr);
            calibrationProgressTemp.setTargetValue(targetTempStr);
            calibrationProgressTemp.setProgress(currentExtruderTemperature / targetTemperature);
        }
    }

    private final ChangeListener<Number> targetETCListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        targetETC = newValue.intValue();
        updateCalibrationProgressPrint();
    };

    private final ChangeListener<Number> printPercentListener
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            printPercent = newValue.doubleValue();
            updateCalibrationProgressPrint();
        };

    private void updateCalibrationProgressPrint()
    {
        targetETC = currentPrinter.getPrintEngine().progressETCProperty().get();
        if (calibrationProgressPrint.isVisible())
        {
            String targetETCStr = targetETC + "s";
            String currentPrintPercentStr = ((int) (printPercent * 100)) + "%";
            calibrationProgressPrint.setCurrentValue(currentPrintPercentStr);
            calibrationProgressPrint.setTargetValue(targetETCStr);
            calibrationProgressPrint.setProgress(printPercent);
        }
    }

    protected void setCalibrationProgressVisible(ProgressVisibility visibility)
    {
        calibrationProgressTemp.setVisible(false);
        calibrationProgressPrint.setVisible(false);
        calibrationBottomArea.getChildren().clear();
        if (visibility != ProgressVisibility.NONE)
        {
            if (visibility == ProgressVisibility.TEMP)
            {
                calibrationProgressTemp.setVisible(true);
                calibrationBottomArea.getChildren().add(calibrationProgressTemp);
            }
            if (visibility == ProgressVisibility.PRINT)
            {
                calibrationProgressPrint.setVisible(true);
                calibrationBottomArea.getChildren().add(calibrationProgressPrint);
            }
        }
        calibrationBottomArea.getChildren().add(calibrateBottomMenu);
    }

    private void switchToPrinter(Printer printer)
    {
        if (currentPrinter != null)
        {
            unbindPrinter(currentPrinter);
        }
        if (printer != null)
        {
            bindPrinter(printer);
        }
        currentPrinter = printer;
    }

    private void unbindPrinter(Printer printer)
    {
        removeHeadListeners(printer);
        removePrintProgressListeners(printer);
    }

    private void bindPrinter(Printer printer)
    {
        calibrationProgressTemp.setProgress(0);
        Head newHead = printer.headProperty().get();
        if (newHead != null)
        {
            NozzleHeater nozzleHeater = newHead.getNozzleHeaters().get(0);
            targetTemperature = nozzleHeater.nozzleTargetTemperatureProperty().get();
            nozzleHeater.nozzleTargetTemperatureProperty().addListener(targetTemperatureListener);
            nozzleHeater.nozzleTemperatureProperty().addListener(extruderTemperatureListener);
        }
        setupPrintProgressListeners(printer);
    }

    private void removePrintProgressListeners(Printer printer)
    {
        printer.getPrintEngine().progressETCProperty().removeListener(targetETCListener);
        printer.getPrintEngine().progressProperty().removeListener(printPercentListener);
    }

    private void setupPrintProgressListeners(Printer printer)
    {
        printer.getPrintEngine().progressProperty().addListener(printPercentListener);
        printer.getPrintEngine().progressETCProperty().addListener(targetETCListener);
    }

    private void removeHeadListeners(Printer printer)
    {
        if (printer.headProperty().get() != null)
        {
            NozzleHeater nozzleHeater = printer.headProperty().get().getNozzleHeaters().get(0);
            nozzleHeater.nozzleTargetTemperatureProperty().removeListener(targetTemperatureListener);
            nozzleHeater.nozzleTemperatureProperty().removeListener(extruderTemperatureListener);
        }
    }

    public void setCalibrationMode(CalibrationMode calibrationMode)
    {
        switchToPrinter(Lookup.getCurrentlySelectedPrinter());
        switch (calibrationMode)
        {
            case NOZZLE_OPENING:
                calibrationHelper = new CalibrationNozzleBHelper();
                calibrationNozzleBGUIStateHandler
                    = new CalibrationNozzleBGUIStateHandler(this, calibrationHelper);
                ((CalibrationNozzleBHelper) calibrationHelper).addStateListener(this);
                calibrationHelper.goToIdleState();
                calibrationHelper.setPrinterToUse(currentPrinter);
                setNozzleOpeningState(NozzleOpeningCalibrationState.IDLE);
                break;
            case NOZZLE_HEIGHT:
                calibrationHelper = new CalibrationNozzleOffsetHelper(this);
                calibrationNozzleOffsetGUIStateHandler
                    = new CalibrationNozzleOffsetGUIStateHandler(this, calibrationHelper);
                ((CalibrationNozzleOffsetHelper) calibrationHelper).addStateListener(this);
                calibrationHelper.goToIdleState();
                calibrationHelper.setPrinterToUse(currentPrinter);
                setNozzleHeightState(NozzleOffsetCalibrationState.IDLE);
                break;
            case X_AND_Y_OFFSET:
                calibrationHelper = null;
                calibrationXAndYGUIStateHandler = null;
                
                stateManager = currentPrinter.startCalibrateXAndY();
                calibrationXAndYGUI = new CalibrationXAndYGUI(this, stateManager);
                calibrationXAndYGUI.setState(CalibrationXAndYState.IDLE);
                break;
            case CHOICE:
                calibrationHelper = null;
                setupChoice();
        }
    }

    protected void setXOffset(String xOffset)
    {
//        calibrationHelper.setXOffset(xOffset);
        stateManager.setXOffset(xOffset);
    }

    protected void setYOffset(Integer yOffset)
    {
//        calibrationHelper.setYOffset(yOffset);
        stateManager.setYOffset(yOffset);
    }

    private void setupChoice()
    {
        calibrationStatus.setText(Lookup.i18n("calibrationPanel.chooseCalibration"));
        calibrationMenu.reset();
        hideAllInputControlsExceptStepNumber();
        stepNumber.setVisible(false);
        backToStatus.setVisible(false);
        cancelCalibrationButton.setVisible(true);
    }

    private void setupProgressBars()
    {
        calibrationProgressPrint.setTargetLegend("Approximate Build Time Remaining:");
        calibrationProgressPrint.setProgressDescription("PRINTING...");
        calibrationProgressPrint.setTargetValue("0");
    }

    protected void showSpinner()
    {
        spinner.startSpinning();
    }
    
    protected void hideSpinner()
    {
        spinner.stopSpinning();
    }    

    /**
     * Initialise the spinner and make it remain centred on the given parent.
     */
    private void setupSpinner(Pane parent)
    {
        spinner = new Spinner();

//        parent.getChildren().add(spinner);

        parent.widthProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                recentreSpinner(parent);
            });

        parent.heightProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                recentreSpinner(parent);
            });

        recentreSpinner(parent);
        spinner.stopSpinning();

    }

    private void recentreSpinner(Pane parent)
    {
        spinner.recentre(parent);
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        if (printer == currentPrinter)
        {
            cancelCalibrationAction();
        }
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        if (printer == currentPrinter)
        {
            bindPrinter(printer);
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        if (printer == currentPrinter)
        {
            cancelCalibrationAction();
        }
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel)
    {
    }

}
