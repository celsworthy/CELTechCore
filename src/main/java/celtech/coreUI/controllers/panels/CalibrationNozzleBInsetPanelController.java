package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.coreUI.components.calibration.CalibrationMenu;
import celtech.coreUI.components.calibration.CalibrationProgress;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.services.calibration.NozzleBCalibrationState;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
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
public class CalibrationNozzleBInsetPanelController implements Initializable,
    CalibrationBStateListener
{

    private Stenographer steno = StenographerFactory.getStenographer(
        CalibrationNozzleBInsetPanelController.class.getName());

    private CalibrationNozzleBHelper calibrationHelper = new CalibrationNozzleBHelper();

    @FXML
    private CalibrationMenu calibrationMenu;

    @FXML
    private StackPane calibrateBottomMenu;

    @FXML
    private VBox calibrationBottomArea;

    @FXML
    CalibrationProgress calibrationProgress;

    @FXML
    private Text calibrationInstruction;
    
    @FXML
    private Text stepNumber;    

    @FXML
    private Button startCalibrationButton;

    @FXML
    private Button cancelCalibrationButton;

    @FXML
    private Button saveSettingsButton;

    @FXML
    private Label calibrationStatus;

    @FXML
    private Button yesButton;

    @FXML
    private Button noButton;

    private Printer currentPrinter;
    private int targetTemperature;
    private double currentExtruderTemperature;

    @FXML
    void yesButtonAction(ActionEvent event)
    {
        calibrationHelper.yesButtonAction();
    }

    @FXML
    void noButtonAction(ActionEvent event)
    {
        calibrationHelper.noButtonAction();
    }

    @FXML
    void startCalibration(ActionEvent event)
    {
        calibrationHelper.setState(NozzleBCalibrationState.HEATING);
    }

    @FXML
    void cancelCalibration(ActionEvent event)
    {
        cancelCalibrationAction();
    }

    @FXML
    void saveSettings(ActionEvent event)
    {
        calibrationHelper.saveSettings();
        calibrationHelper.setState(NozzleBCalibrationState.IDLE);
        ApplicationStatus.getInstance().returnToLastMode();
    }

    /**
     *
     */
    public void cancelCalibrationAction()
    {
        calibrationHelper.cancelCalibrationAction();
        calibrationHelper.setState(NozzleBCalibrationState.IDLE);
        ApplicationStatus.getInstance().returnToLastMode();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        StatusScreenState statusScreenState = StatusScreenState.getInstance();

        Printer printerToUse = statusScreenState.currentlySelectedPrinterProperty().get();
        setupChildComponents(printerToUse);

        statusScreenState.currentlySelectedPrinterProperty().addListener(
            new ChangeListener<Printer>()
            {
                @Override
                public void changed(ObservableValue<? extends Printer> observable, Printer oldValue,
                    Printer newValue)
                {
                    setupChildComponents(newValue);
                }
            });

        calibrationHelper.addStateListener(this);
        calibrationHelper.setState(NozzleBCalibrationState.IDLE);

        calibrationMenu.addItem("Nozzle Opening", (Callable) () ->
                            {
                                System.out.println("Called NO");
                                return null;
        });
        calibrationMenu.addItem("Nozzle Height", (Callable) () ->
                            {
                                System.out.println("Called NH");
                                return null;
        });
        calibrationMenu.addItem("X And Y Offset", (Callable) () ->
                            {
                                System.out.println("Called XY");
                                return null;
        });
        calibrationMenu.addItem("Gantry Level", (Callable) () ->
                            {
                                System.out.println("Called GL");
                                return null;
        });
    }

    private void setupChildComponents(Printer printerToUse)
    {
        calibrationHelper.setPrinterToUse(printerToUse);
        setupTemperatureProgressListeners(printerToUse);
        currentPrinter = printerToUse;
    }

    @Override
    public void setState(NozzleBCalibrationState state)
    {
        switch (state)
        {
            case IDLE:
                setCalibrationProgressVisible(false);
                calibrationProgress.setVisible(false);
                startCalibrationButton.setVisible(true);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 1));
                break;
            case HEATING:
                setCalibrationProgressVisible(true);
                calibrationProgress.setVisible(true);
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 2));
                break;
            case NO_MATERIAL_CHECK:
                setCalibrationProgressVisible(false);
                calibrationProgress.setVisible(false);
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 3));
                break;
            case MATERIAL_EXTRUDING_CHECK_FINE_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 4));
                break;
            case HEAD_CLEAN_CHECK_FINE_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                stepNumber.setText(String.format("Step %s of 10", 5));
                break;
            case MATERIAL_EXTRUDING_CHECK_FILL_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                stepNumber.setText(String.format("Step %s of 10", 6));
                break;
            case HEAD_CLEAN_CHECK_FILL_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                stepNumber.setText(String.format("Step %s of 10", 7));
                break;                
            case PRE_CALIBRATION_PRIMING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                stepNumber.setText(String.format("Step %s of 10", 6));
                break;
            case CALIBRATE_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle(String.valueOf(
                    calibrationHelper.getCurrentNozzleNumber())));
                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(
                    calibrationHelper.getCurrentNozzleNumber())));
                stepNumber.setText(String.format("Step %s of 10", 7));
                break;
            case HEAD_CLEAN_CHECK_POST_CALIBRATION:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                stepNumber.setText(String.format("Step %s of 10", 8));
                break;
            case POST_CALIBRATION_PRIMING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                stepNumber.setText(String.format("Step %s of 10", 9));
                break;
            case CONFIRM_NO_MATERIAL:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                stepNumber.setText(String.format("Step %s of 10", 10));
                break;
            case CONFIRM_MATERIAL_EXTRUDING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle(String.valueOf(
                    calibrationHelper.getCurrentNozzleNumber())));
                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(
                    calibrationHelper.getCurrentNozzleNumber())));
                stepNumber.setText(String.format("Step %s of 10", 11));
                break;
            case PARKING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                stepNumber.setText(String.format("Step %s of 10", 12));
                break;
            case FINISHED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case FAILED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
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
            String targetTempStr = targetTemperature + Lookup.i18("misc.degreesC");
            String currentTempStr = ((int) currentExtruderTemperature) + Lookup.i18("misc.degreesC");
            calibrationProgress.setCurrentValue(currentTempStr);
            calibrationProgress.setTargetValue(targetTempStr);
            calibrationProgress.setProgress(currentExtruderTemperature / targetTemperature);
        }
    }

    private void setCalibrationProgressVisible(boolean visible)
    {
        calibrationBottomArea.getChildren().clear();
        if (visible)
        {
            calibrationBottomArea.getChildren().add(calibrationProgress);
        }
        calibrationBottomArea.getChildren().add(calibrateBottomMenu);
    }
}
