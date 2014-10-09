package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import static celtech.coreUI.DisplayManager.getLanguageBundle;
import celtech.coreUI.components.VerticalMenu;
import celtech.coreUI.components.LargeProgress;
import celtech.coreUI.controllers.StatusScreenState;
import static celtech.coreUI.controllers.panels.CalibrationMenuConfiguration.configureCalibrationMenu;
import celtech.printerControl.Printer;
import celtech.services.calibration.CalibrationXAndYState;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import celtech.services.calibration.NozzleOpeningCalibrationState;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
    CalibrationXAndYStateListener
{
    
    private void resizeTopBorderPane()
    {
        topBorderPane.setPrefWidth(topPane.getWidth());
        topBorderPane.setPrefHeight(topPane.getHeight());
    }

    void whenZCoChanged(double zco)
    {
        if (diagramController != null) {
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
    protected Pane offsetCombosContainer;
    
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
    protected ComboBox cmbYOffset;
    
    @FXML
    protected ComboBox cmbXOffset;
    
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
    private Node waitTimer;
    private Node diagramNode;
    DiagramController diagramController;
    private Map<String, Node> nameToNodeCache = new HashMap<>();
    
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
        calibrationHelper.nextButtonAction();
    }
    
    @FXML
    void backToStatusAction(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
        setCalibrationMode(CalibrationMode.CHOICE);
    }
    
    @FXML
    void startCalibration(ActionEvent event)
    {
        calibrationHelper.nextButtonAction();
    }
    
    @FXML
    void cancelCalibration(ActionEvent event)
    {
        cancelCalibrationAction();
    }
    
    @FXML
    void retryCalibration(ActionEvent event)
    {
        calibrationHelper.retryAction();
    }

    /**
     *
     */
    public void cancelCalibrationAction()
    {
        ApplicationStatus.getInstance().returnToLastMode();
        calibrationHelper.cancelCalibrationAction();
        setCalibrationMode(CalibrationMode.CHOICE);
    }
    
    protected void hideAllInputControlsExceptStepNumber()
    {
        backToStatus.setVisible(false);
        setCalibrationProgressVisible(CalibrationInsetPanelController.ProgressVisibility.NONE);
        offsetCombosContainer.setVisible(false);
        retryPrintButton.setVisible(false);
        startCalibrationButton.setVisible(false);
        cancelCalibrationButton.setVisible(false);
        nextButton.setVisible(false);
        buttonB.setVisible(false);
        buttonA.setVisible(false);
        stepNumber.setVisible(true);
        showWaitTimer(false);
        if (diagramNode != null)
        {
            diagramNode.setVisible(false);
        }
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        setupProgressBars();
        setupOffsetCombos();
        setupWaitTimer(informationCentre);
        
        setCalibrationMode(CalibrationMode.CHOICE);
        
        StatusScreenState statusScreenState = StatusScreenState.getInstance();
        
        Printer printerToUse = statusScreenState.currentlySelectedPrinterProperty().get();
        setupChildComponents(printerToUse);
        
        statusScreenState.currentlySelectedPrinterProperty().addListener(
            (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
            {
                setupChildComponents(newValue);
            });
        
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
                steno.info("STATUS WIDTH CHANGED");
                resizeDiagram();
            });
        
        calibrationStatus.heightProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                steno.info("STATUS HEIGHT CHANGED");
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
        
        diagramNode.setScaleX(0.7);
        diagramNode.setScaleY(0.7);
        
        double diagramWidth = diagramNode.getBoundsInLocal().getWidth();
        double diagramHeight = diagramNode.getBoundsInLocal().getHeight();
//        steno.info("DIAG HEIGHT " + diagramHeight);

        Bounds statusBounds = calibrationStatus.localToScene(calibrationStatus.getBoundsInLocal());
        Bounds ancestorStatusBounds = topPane.sceneToLocal(statusBounds);
        double upperBoundaryInAncestorCoords = ancestorStatusBounds.getMaxY();
//        steno.info("STATUS MAX Y IS " + upperBoundaryInAncestorCoords);
        Bounds bottomAreaBounds = calibrationBottomArea.localToScene(
            calibrationBottomArea.getBoundsInLocal());
        Bounds ancestorBottomBounds = topPane.sceneToLocal(bottomAreaBounds);
        double lowerBoundaryInAncestorCoords = ancestorBottomBounds.getMinY();
        double availableHeight = lowerBoundaryInAncestorCoords - upperBoundaryInAncestorCoords - 120;
//        steno.info("AVAIL HEIGHT " + availableHeight);

        double xTranslate = 0;
        double yTranslate = 0;
        
        xTranslate = -diagramWidth / 2;
        yTranslate = -diagramHeight / 2;
        
        xTranslate += ancestorStatusBounds.getMinX() + (ancestorStatusBounds.getWidth() / 2.0d);
        yTranslate += upperBoundaryInAncestorCoords + availableHeight / 2.0;
        
        diagramNode.setTranslateX(xTranslate);
        diagramNode.setTranslateY(yTranslate); //

        double requiredScale = availableHeight / diagramHeight * 0.8;
        steno.info("requiredScale " + requiredScale);
        
    }

    /**
     * If this diagramNode has already been loaded then return that, else create it and return it.
     *
     * @param diagramName
     * @return
     */
    private Node getDiagramNode(String section, String diagramName)
    {
        Pane diagramNode = null;
        if (!nameToNodeCache.containsKey(diagramName))
        {
            URL fxmlFileName = getClass().getResource(ApplicationConfiguration.fxmlResourcePath
                + "diagrams/" + section + "/" + diagramName);
            try
            {
                FXMLLoader loader = new FXMLLoader(fxmlFileName);
                diagramController = new DiagramController(this);
                loader.setController(diagramController);
                diagramNode = loader.load();
                nameToNodeCache.put(diagramName, diagramNode);
            } catch (IOException ex)
            {
                steno.error("Could not load diagram: " + diagramName);
            }
        }
        return nameToNodeCache.get(diagramName);
//        return new TextField("ABC");
    }
    
    protected void showDiagram(String section, String diagramName)
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
        }
        
        resizeDiagram();
        diagramNode.setVisible(true);
    }
    
    private void setupChildComponents(Printer printerToUse)
    {
        if (calibrationHelper != null)
        {
            calibrationHelper.setPrinterToUse(printerToUse);
        }
        setupTemperatureProgressListeners(printerToUse);
        setupPrintProgressListeners(printerToUse);
        currentPrinter = printerToUse;
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
    
    private void removeTemperatureProgressListeners(Printer printer)
    {
        printer.nozzleTargetTemperatureProperty().removeListener(targetTemperatureListener);
        printer.extruderTemperatureProperty().removeListener(extruderTemperatureListener);
    }
    
    private void setupTemperatureProgressListeners(Printer printer)
    {
        if (currentPrinter != null)
        {
            removeTemperatureProgressListeners(currentPrinter);
        }
        
        if (printer == null)
        {
            calibrationProgressTemp.setProgress(0);
        } else
        {
            printer.nozzleTargetTemperatureProperty().addListener(targetTemperatureListener);
            printer.extruderTemperatureProperty().addListener(extruderTemperatureListener);
        }
    }
    
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
    
    private void removePrintProgressListeners(Printer printer)
    {
        printer.getPrintQueue().progressETCProperty().removeListener(targetETCListener);
        printer.getPrintQueue().progressProperty().removeListener(printPercentListener);
    }
    
    private void setupPrintProgressListeners(Printer printer)
    {
        if (currentPrinter != null)
        {
            removePrintProgressListeners(currentPrinter);
        }
        
        if (printer == null)
        {
            calibrationProgressTemp.setProgress(0);
            calibrationProgressPrint.setProgress(0);
        } else
        {
            printer.getPrintQueue().progressProperty().addListener(printPercentListener);
            printer.getPrintQueue().progressETCProperty().addListener(targetETCListener);
        }
    }
    
    private void updateCalibrationProgressPrint()
    {
        targetETC = currentPrinter.getPrintQueue().progressETCProperty().get();
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
    
    public void setCalibrationMode(CalibrationMode calibrationMode)
    {
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
                calibrationHelper = new CalibrationXAndYHelper();
                calibrationXAndYGUIStateHandler
                    = new CalibrationXAndYGUIStateHandler(this, calibrationHelper);
                ((CalibrationXAndYHelper) calibrationHelper).addStateListener(this);
                calibrationHelper.goToIdleState();
                calibrationHelper.setPrinterToUse(currentPrinter);
                setXAndYState(CalibrationXAndYState.IDLE);
                break;
            case CHOICE:
                calibrationHelper = null;
                setupChoice();
        }
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
    
    private void setupOffsetCombos()
    {
        cmbXOffset.getItems().add("A");
        cmbXOffset.getItems().add("B");
        cmbXOffset.getItems().add("C");
        cmbXOffset.getItems().add("D");
        cmbXOffset.getItems().add("E");
        cmbXOffset.getItems().add("F");
        cmbXOffset.getItems().add("G");
        cmbXOffset.getItems().add("H");
        cmbXOffset.getItems().add("I");
        cmbXOffset.getItems().add("J");
        cmbXOffset.getItems().add("K");
        cmbYOffset.getItems().add("1");
        cmbYOffset.getItems().add("2");
        cmbYOffset.getItems().add("3");
        cmbYOffset.getItems().add("4");
        cmbYOffset.getItems().add("5");
        cmbYOffset.getItems().add("6");
        cmbYOffset.getItems().add("7");
        cmbYOffset.getItems().add("8");
        cmbYOffset.getItems().add("9");
        cmbYOffset.getItems().add("10");
        cmbYOffset.getItems().add("11");
        
        cmbXOffset.valueProperty().addListener(
            (ObservableValue observable, Object oldValue, Object newValue) ->
            {
                calibrationHelper.setXOffset(newValue.toString());
            });
        
        cmbYOffset.valueProperty().addListener(
            (ObservableValue observable, Object oldValue, Object newValue) ->
            {
                calibrationHelper.setYOffset(Integer.parseInt(newValue.toString()));
            });
    }
    
    protected void showWaitTimer(boolean show)
    {
        waitTimer.setVisible(show);
    }

    /**
     * Initialise the waitTimer and make it remain centred on the given parent.
     */
    private void setupWaitTimer(Pane parent)
    {
        try
        {
            URL fxmlFileName = getClass().getResource(ApplicationConfiguration.fxmlPanelResourcePath
                + "spinner.fxml");
            FXMLLoader waitTimerLoader = new FXMLLoader(fxmlFileName, getLanguageBundle());
            waitTimer = waitTimerLoader.load();
            waitTimer.scaleXProperty().set(0.5);
            waitTimer.scaleYProperty().set(0.5);
            
            parent.getChildren().add(waitTimer);
            
            parent.widthProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                {
                    relocateWaitTimer(parent);
                });
            
            parent.heightProperty().addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                {
                    relocateWaitTimer(parent);
                });
            
            relocateWaitTimer(parent);
            waitTimer.setVisible(false);
            
        } catch (IOException ex)
        {
            ex.printStackTrace();
            steno.error("Cannot load wait timer " + ex);
        }
    }
    
    private void relocateWaitTimer(Pane parent)
    {
        waitTimer.setTranslateX(parent.getWidth() / 2.0);
        waitTimer.setTranslateY(parent.getHeight() / 2.0);
    }
    
}
