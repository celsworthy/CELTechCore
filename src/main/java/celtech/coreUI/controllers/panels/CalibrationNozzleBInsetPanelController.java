package celtech.coreUI.controllers.panels;

import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.Printer;
import celtech.services.calibration.NozzleBCalibrationState;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
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
public class CalibrationNozzleBInsetPanelController implements Initializable, CalibrationBStateListener
{

    private Stenographer steno = StenographerFactory.getStenographer(CalibrationNozzleBInsetPanelController.class.getName());

    private CalibrationNozzleBHelper calibrationHelper = new CalibrationNozzleBHelper();

    private ResourceBundle i18nBundle = null;

    @FXML
    private VBox container;

    @FXML
    private Text calibrationInstruction;

    @FXML
    private Button startCalibrationButton;

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
        calibrationHelper.noButtonAction();
    }

    @FXML
    void startCalibration(ActionEvent event)
    {
        calibrationHelper.setState(NozzleBCalibrationState.INITIALISING);
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

        if (calibrationHelper.getState() == NozzleBCalibrationState.IDLE)
        {
            Stage stage = (Stage) container.getScene().getWindow();
            stage.close();
        } else
        {
            calibrationHelper.setState(NozzleBCalibrationState.IDLE);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        i18nBundle = DisplayManager.getLanguageBundle();

        StatusScreenState statusScreenState = StatusScreenState.getInstance();

        calibrationHelper.setPrinterToUse(statusScreenState.currentlySelectedPrinterProperty().get());

        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
            {
                calibrationHelper.setPrinterToUse(newValue);
            }
        });

        calibrationHelper.addStateListener(this);
        calibrationHelper.setState(NozzleBCalibrationState.IDLE);
    }

    @Override
    public void setState(NozzleBCalibrationState state)
    {
        switch (state)
        {
            case IDLE:
                startCalibrationButton.setVisible(true);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case INITIALISING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case HEATING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case PRIMING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case NO_MATERIAL_CHECK:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case MATERIAL_EXTRUDING_CHECK:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
                break;
            case HEAD_CLEAN_CHECK:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case PRE_CALIBRATION_PRIMING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case CALIBRATE_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
                break;
            case HEAD_CLEAN_CHECK_POST_CALIBRATION:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case POST_CALIBRATION_PRIMING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case CONFIRM_NO_MATERIAL:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case CONFIRM_MATERIAL_EXTRUDING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
                break;
            case FINISHED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(false);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case FAILED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
        }
    }
}
