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

    @FXML
    private CalibrationMenu calibrationMenu;

    @FXML
    private StackPane calibrateBottomMenu;

    @FXML
    private VBox calibrationBottomArea;

    @FXML
    CalibrationProgress calibrationProgress;

    @FXML
    private Text stepNumber;

    @FXML
    private Button buttonA;

    @FXML
    private Button buttonB;

    @FXML
    private Button buttonAAlt;

    @FXML
    private Button buttonBAlt;

    @FXML
    private Button nextButton;

    @FXML
    private Button backToStatus;
    
    @FXML
    private Button startCalibrationButton;

    @FXML
    private Button cancelCalibrationButton;

    @FXML
    private Label calibrationStatus;

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
//        calibrationHelper.setState(NozzleOpeningCalibrationState.IDLE);
        ApplicationStatus.getInstance().returnToLastMode();
    }

    /**
     *
     */
    public void cancelCalibrationAction()
    {
        calibrationMenu.enableNonSelectedItems();
        calibrationHelper.cancelCalibrationAction();
        ApplicationStatus.getInstance().returnToLastMode();
         switch (calibrationMode)
        {
            case NOZZLE_OPENING:
                ((CalibrationNozzleBHelper)calibrationHelper).setState(NozzleOpeningCalibrationState.IDLE);
                break;
            case NOZZLE_HEIGHT:
                ((CalibrationNozzleOffsetHelper)calibrationHelper).setState(NozzleOffsetCalibrationState.IDLE);
                break;
        }
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
        switch (state)
        {
            case IDLE:
                backToStatus.setVisible(false);
                buttonAAlt.setVisible(false);
                buttonBAlt.setVisible(false);
                stepNumber.setVisible(true);
                setCalibrationProgressVisible(false);
                startCalibrationButton.setVisible(true);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(false);
                buttonB.setVisible(false);
                nextButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 1));
                break;
            case HEATING:
                calibrationMenu.disableNonSelectedItems();
                setCalibrationProgressVisible(true);
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(false);
                buttonB.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 2));
                break;
            case NO_MATERIAL_CHECK:
                setCalibrationProgressVisible(false);
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(true);
                buttonB.setVisible(true);
                buttonA.setText("Yes");
                buttonB.setText("No");
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 3));
                break;
            case PRE_CALIBRATION_PRIMING_FINE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(false);
                buttonB.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                break;
            case CALIBRATE_FINE_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(true);
                buttonB.setVisible(true);
                buttonA.setText("Flowing");
                buttonB.setText("Not flowing");
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 4));
                break;
            case HEAD_CLEAN_CHECK_FINE_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(false);
                buttonB.setVisible(false);
                nextButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 5));
                break;
            case CALIBRATE_FILL_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(true);
                buttonB.setVisible(true);
                nextButton.setVisible(false);
                buttonA.setText("Flowing");
                buttonB.setText("Not flowing");
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 6));
                break;
            case HEAD_CLEAN_CHECK_FILL_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(false);
                buttonB.setVisible(false);
                nextButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 7));
                break;
            case CONFIRM_NO_MATERIAL:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(true);
                buttonB.setVisible(true);
                nextButton.setVisible(false);
                buttonA.setText("No");
                buttonB.setText("Yes");
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 8));
                break;
            case CONFIRM_MATERIAL_EXTRUDING_FINE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(true);
                buttonB.setVisible(true);
                nextButton.setVisible(false);
                buttonA.setText("Yes");
                buttonB.setText("No");
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 9));
                break;
            case FINISHED:
                backToStatus.setVisible(true);
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(false);
                buttonB.setVisible(false);
                nextButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 10));
                setCalibrationMode(CalibrationMode.CHOICE);
                break;
            case FAILED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(false);
                buttonB.setVisible(false);
                nextButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText("");
                setCalibrationMode(CalibrationMode.CHOICE);
                break;
        }
    }

    @Override
    public void setNozzleHeightState(NozzleOffsetCalibrationState state)
    {
        switch (state)
        {
            case IDLE:
                backToStatus.setVisible(false);
                setCalibrationProgressVisible(false);
                stepNumber.setVisible(true);
                buttonAAlt.setVisible(false);
                buttonBAlt.setVisible(false);
                startCalibrationButton.setVisible(true);
                cancelCalibrationButton.setVisible(true);
                nextButton.setVisible(false);
                buttonB.setVisible(false);
                buttonA.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 1));
                break;
            case INITIALISING:
                calibrationMenu.disableNonSelectedItems();
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                nextButton.setVisible(true);
                buttonB.setVisible(false);
                buttonA.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 2));
                break;
            case HEATING:
                setCalibrationProgressVisible(true);
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                nextButton.setVisible(false);
                buttonB.setVisible(false);
                buttonA.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 3));
                break;
            case HEAD_CLEAN_CHECK:
                setCalibrationProgressVisible(false);
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonA.setVisible(false);
                buttonB.setVisible(false);
                nextButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 4));
                break;
            case MEASURE_Z_DIFFERENCE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonB.setVisible(false);
                buttonA.setVisible(false);
                nextButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 5));
                break;
            case INSERT_PAPER:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonB.setVisible(false);
                buttonA.setVisible(false);
                nextButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 6));
                break;
            case PROBING:
                buttonAAlt.disableProperty().bind(
                    ((CalibrationNozzleOffsetHelper) calibrationHelper).showDownButton.not());
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonAAlt.setVisible(true);
                buttonBAlt.setVisible(true);
                buttonB.setVisible(false);
                buttonA.setVisible(false);
                nextButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 7));
                break;
            case LIFT_HEAD:
                break;
            case REPLACE_PEI_BED:    
                buttonAAlt.disableProperty().unbind();
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonB.setVisible(false);
                buttonA.setVisible(false);
                buttonAAlt.setVisible(false);
                buttonBAlt.setVisible(false);
                nextButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 8));
                break;
            case FINISHED:
                backToStatus.setVisible(true);
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                buttonB.setVisible(false);
                buttonA.setVisible(false);
                buttonAAlt.setVisible(false);
                buttonBAlt.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 9));
                setCalibrationMode(CalibrationMode.CHOICE);
                break;
            case FAILED:
                buttonAAlt.disableProperty().unbind();
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(false);
                buttonB.setVisible(false);
                buttonA.setVisible(false);
                buttonAAlt.setVisible(false);
                buttonBAlt.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText("");
                setCalibrationMode(CalibrationMode.CHOICE);
                break;
        }
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

    private void setCalibrationProgressVisible(boolean visible)
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
                ((CalibrationNozzleBHelper) calibrationHelper).addStateListener(this);
                calibrationHelper.goToIdleState();
                calibrationHelper.setPrinterToUse(currentPrinter);
                setNozzleOpeningState(NozzleOpeningCalibrationState.IDLE);
                break;

            case NOZZLE_HEIGHT:
                calibrationHelper = new CalibrationNozzleOffsetHelper();
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
        calibrationMenu.deselectSelectedItem();
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
