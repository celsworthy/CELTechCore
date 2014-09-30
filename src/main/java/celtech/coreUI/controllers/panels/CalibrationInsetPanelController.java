package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.coreUI.components.calibration.CalibrationMenu;
import celtech.coreUI.components.calibration.CalibrationProgress;
import celtech.coreUI.controllers.StatusScreenState;
import static celtech.coreUI.controllers.panels.CalibrationMenuConfiguration.configureCalibrationMenu;
import celtech.printerControl.Printer;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import celtech.services.calibration.NozzleOpeningCalibrationState;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationInsetPanelController implements Initializable,
    CalibrationBStateListener, CalibrationNozzleOffsetStateListener
{

    private CalibrationMode calibrationMode;

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationInsetPanelController.class.getName());

    private CalibrationHelper calibrationHelper;
    private CalibrationNozzleOffsetGUIStateHandler calibrationNozzleOffsetGUIStateHandler;
    private CalibrationNozzleBGUIStateHandler calibrationNozzleBGUIStateHandler;

    @FXML
    protected CalibrationMenu calibrationMenu;

    @FXML
    protected StackPane calibrateBottomMenu;

    @FXML
    protected VBox calibrationBottomArea;

    @FXML
    protected CalibrationProgress calibrationProgress;

    @FXML
    protected Text stepNumber;

    @FXML
    protected Button buttonA;

    @FXML
    protected Button buttonB;

    @FXML
    protected Button buttonAAlt;

    @FXML
    protected Button buttonBAlt;

    @FXML
    protected Button nextButton;

    @FXML
    protected Button backToStatus;

    @FXML
    protected Button startCalibrationButton;

    @FXML
    protected Button cancelCalibrationButton;

    @FXML
    protected Label calibrationStatus;

    private Printer currentPrinter;
    private int targetTemperature;
    private double currentExtruderTemperature;

    @FXML
    void buttonAAction(ActionEvent event)
    {
        calibrationHelper.buttonAAction();
    }

    @FXML
    void buttonBAction(ActionEvent event)
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
    }

    @FXML
    void startCalibration(ActionEvent event)
    {
        switch (calibrationMode)
        {
            case NOZZLE_OPENING:
                ((CalibrationNozzleBHelper) calibrationHelper).setState(
                    NozzleOpeningCalibrationState.HEATING);
                break;
            case NOZZLE_HEIGHT:
                ((CalibrationNozzleOffsetHelper) calibrationHelper).setState(
                    NozzleOffsetCalibrationState.INITIALISING);
                break;
        }

    }

    @FXML
    void cancelCalibration(ActionEvent event)
    {
        cancelCalibrationAction();
    }

    void saveSettings(ActionEvent event)
    {
        calibrationHelper.saveSettings();
        ApplicationStatus.getInstance().returnToLastMode();
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

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
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
    }

    private void setupChildComponents(Printer printerToUse)
    {
        if (calibrationHelper != null)
        {
            calibrationHelper.setPrinterToUse(printerToUse);
        }
        setupTemperatureProgressListeners(printerToUse);
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

    private final ChangeListener<Number> targetTemperatureListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        targetTemperature = newValue.intValue();
        updateCalibrationProgress();
    };

    private final ChangeListener<Number> extruderTemperatureListener
        = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            currentExtruderTemperature = newValue.doubleValue();
            updateCalibrationProgress();
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
            calibrationProgress.setProgress(0);
        } else
        {
            printer.nozzleTargetTemperatureProperty().addListener(targetTemperatureListener);
            printer.extruderTemperatureProperty().addListener(extruderTemperatureListener);
        }
    }

    private void updateCalibrationProgress()
    {
        if (targetTemperature != 0 && calibrationProgress.isVisible())
        {
            String targetTempStr = targetTemperature + Lookup.i18n("misc.degreesC");
            String currentTempStr = ((int) currentExtruderTemperature)
                + Lookup.i18n("misc.degreesC");
            calibrationProgress.setCurrentValue(currentTempStr);
            calibrationProgress.setTargetValue(targetTempStr);
            calibrationProgress.setProgress(currentExtruderTemperature / targetTemperature);
        }
    }

    protected void setCalibrationProgressVisible(boolean visible)
    {
        calibrationProgress.setVisible(visible);
        calibrationBottomArea.getChildren().clear();
        if (visible)
        {
            calibrationBottomArea.getChildren().add(calibrationProgress);
        }
        calibrationBottomArea.getChildren().add(calibrateBottomMenu);
    }

    public void setCalibrationMode(CalibrationMode calibrationMode)
    {
        this.calibrationMode = calibrationMode;
        switch (calibrationMode)
        {
            case NOZZLE_OPENING:
                calibrationHelper = new CalibrationNozzleBHelper();
                calibrationNozzleOffsetGUIStateHandler = 
                    new CalibrationNozzleOffsetGUIStateHandler(this, calibrationHelper);
                ((CalibrationNozzleBHelper) calibrationHelper).addStateListener(this);
                calibrationHelper.goToIdleState();
                calibrationHelper.setPrinterToUse(currentPrinter);
                setNozzleOpeningState(NozzleOpeningCalibrationState.IDLE);
                break;
            case NOZZLE_HEIGHT:
                calibrationHelper = new CalibrationNozzleOffsetHelper();
                calibrationNozzleBGUIStateHandler = 
                    new CalibrationNozzleBGUIStateHandler(this, calibrationHelper);
                ((CalibrationNozzleOffsetHelper) calibrationHelper).addStateListener(this);
                calibrationHelper.goToIdleState();
                calibrationHelper.setPrinterToUse(currentPrinter);
                setNozzleHeightState(NozzleOffsetCalibrationState.IDLE);
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
        setCalibrationProgressVisible(false);
        backToStatus.setVisible(false);
        stepNumber.setVisible(false);
        nextButton.setVisible(false);
        startCalibrationButton.setVisible(false);
        cancelCalibrationButton.setVisible(false);
        buttonA.setVisible(false);
        buttonB.setVisible(false);
        buttonAAlt.setVisible(false);
        buttonBAlt.setVisible(false);
    }

}
