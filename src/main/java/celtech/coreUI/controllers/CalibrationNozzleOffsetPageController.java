package celtech.coreUI.controllers;

import celtech.appManager.TaskController;
import celtech.configuration.Head;
import celtech.configuration.HeadContainer;
import celtech.coreUI.DisplayManager;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.services.calibration.CalibrateNozzleOffsetTask;
import celtech.services.calibration.NozzleBCalibrationState;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import celtech.services.calibration.NozzleOffsetCalibrationStepResult;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationNozzleOffsetPageController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(CalibrationNozzleOffsetPageController.class.getName());

    private ResourceBundle i18nBundle = null;

    private CalibrationNozzleOffsetHelper calibrationHelper = new CalibrationNozzleOffsetHelper();

    @FXML
    private VBox container;

    @FXML
    private Text calibrationInstruction;

    @FXML
    private Button startCalibrationButton;

    @FXML
    private Button tooTightButton;

    @FXML
    private Button tooLooseButton;

    @FXML
    private Button justRightButton;

    @FXML
    private Button cancelCalibrationButton;

    @FXML
    private Text calibrationStatus;

    @FXML
    private Button yesButton;

    @FXML
    private Button noButton;

    @FXML
    void yesButtonAction(ActionEvent event)
    {
        calibrationHelper.yesButtonAction();
    }

    @FXML
    void noButtonAction(ActionEvent event)
    {
    }

    @FXML
    void tooLooseAction(ActionEvent event)
    {
        calibrationHelper.tooLooseAction();
        if (calibrationHelper.getZCo() == 0)
        {
            tooLooseButton.setVisible(false);
        } else
        {
            tooLooseButton.setVisible(true);
        }
    }

    @FXML
    void tooTightAction(ActionEvent event)
    {
        calibrationHelper.tooTightAction();
    }

    @FXML
    void justRightAction(ActionEvent event)
    {
        calibrationHelper.setState(NozzleOffsetCalibrationState.FINISHED);
    }

    @FXML
    void startCalibration(ActionEvent event)
    {
        calibrationHelper.setState(NozzleOffsetCalibrationState.INITIALISING);
    }

    @FXML
    void cancelCalibration(ActionEvent event)
    {
        cancelCalibrationAction();
    }

    /**
     *
     */
    public void cancelCalibrationAction()
    {
        calibrationHelper.cancelCalibrationAction();
        if (calibrationHelper.getState() == NozzleOffsetCalibrationState.IDLE)
        {
            Stage stage = (Stage) container.getScene().getWindow();
            stage.close();
        } else
        {
            calibrationHelper.setState(NozzleOffsetCalibrationState.IDLE);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        i18nBundle = DisplayManager.getLanguageBundle();

        StatusScreenState statusScreenState = StatusScreenState.getInstance();
        statusScreenState.currentlySelectedPrinterProperty().addListener((ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
        {
            calibrationHelper.setPrinterToUse(newValue);
        });

        calibrationHelper.setPrinterToUse(statusScreenState.getCurrentlySelectedPrinter());

        calibrationHelper.getStateProperty().addListener((ObservableValue<? extends NozzleOffsetCalibrationState> observable, NozzleOffsetCalibrationState oldValue, NozzleOffsetCalibrationState newValue) ->
        {
            switch (newValue)
            {
                case IDLE:
                    startCalibrationButton.setVisible(true);
                    cancelCalibrationButton.setVisible(true);
                    yesButton.setVisible(false);
                    noButton.setVisible(false);
                    tooLooseButton.setVisible(false);
                    tooTightButton.setVisible(false);
                    justRightButton.setVisible(false);
                    calibrationStatus.setText(newValue.getStepTitle());
                    calibrationInstruction.setText(newValue.getStepInstruction());
                    break;
                case INITIALISING:
                    startCalibrationButton.setVisible(false);
                    cancelCalibrationButton.setVisible(true);
                    yesButton.setVisible(false);
                    noButton.setVisible(false);
                    tooLooseButton.setVisible(false);
                    tooTightButton.setVisible(false);
                    justRightButton.setVisible(false);
                    calibrationStatus.setText(newValue.getStepTitle());
                    calibrationInstruction.setText(newValue.getStepInstruction());
                    break;
                case HEAD_CLEAN_CHECK:
                    startCalibrationButton.setVisible(false);
                    cancelCalibrationButton.setVisible(true);
                    yesButton.setVisible(true);
                    noButton.setVisible(false);
                    calibrationStatus.setText(newValue.getStepTitle());
                    calibrationInstruction.setText(newValue.getStepInstruction());
                    break;
                case MEASURE_Z_DIFFERENCE:
                    startCalibrationButton.setVisible(false);
                    cancelCalibrationButton.setVisible(true);
                    yesButton.setVisible(false);
                    noButton.setVisible(false);
                    tooLooseButton.setVisible(false);
                    tooTightButton.setVisible(false);
                    justRightButton.setVisible(false);
                    calibrationStatus.setText(newValue.getStepTitle());
                    calibrationInstruction.setText(newValue.getStepInstruction());
                    break;
                case INSERT_PAPER:
                    startCalibrationButton.setVisible(false);
                    cancelCalibrationButton.setVisible(true);
                    yesButton.setVisible(true);
                    noButton.setVisible(false);
                    tooLooseButton.setVisible(false);
                    tooTightButton.setVisible(false);
                    justRightButton.setVisible(false);
                    calibrationStatus.setText(newValue.getStepTitle());
                    calibrationInstruction.setText(newValue.getStepInstruction());
                    break;
                case PROBING:
                    startCalibrationButton.setVisible(false);
                    cancelCalibrationButton.setVisible(true);
                    yesButton.setVisible(false);
                    noButton.setVisible(false);
                    tooLooseButton.setVisible(false);
                    tooTightButton.setVisible(true);
                    justRightButton.setVisible(true);
                    calibrationStatus.setText(newValue.getStepTitle());
                    calibrationInstruction.setText(newValue.getStepInstruction());
                    break;
                case FINISHED:
                    startCalibrationButton.setVisible(false);
                    cancelCalibrationButton.setVisible(false);
                    yesButton.setVisible(false);
                    noButton.setVisible(false);
                    tooLooseButton.setVisible(false);
                    tooTightButton.setVisible(false);
                    justRightButton.setVisible(false);
                    calibrationStatus.setText(newValue.getStepTitle());
                    calibrationInstruction.setText(newValue.getStepInstruction());
                    break;
                case FAILED:
                    startCalibrationButton.setVisible(false);
                    cancelCalibrationButton.setVisible(true);
                    yesButton.setVisible(false);
                    noButton.setVisible(false);
                    tooLooseButton.setVisible(false);
                    tooTightButton.setVisible(false);
                    justRightButton.setVisible(false);
                    calibrationStatus.setText(newValue.getStepTitle());
                    calibrationInstruction.setText(newValue.getStepInstruction());
                    break;
            }
        });

        calibrationHelper.setState(NozzleOffsetCalibrationState.IDLE);
    }
}
