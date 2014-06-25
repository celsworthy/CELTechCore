package celtech.coreUI.controllers;

import celtech.coreUI.DisplayManager;
import celtech.printerControl.Printer;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import java.net.URL;
import java.util.ResourceBundle;
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
public class CalibrationNozzleOffsetPageController implements Initializable, CalibrationNozzleOffsetStateListener
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
        if (calibrationHelper.getZCo() <= 0.0001)
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
        if (calibrationHelper.getZCo() <= 0.0001)
        {
            tooLooseButton.setVisible(false);
        } else
        {
            tooLooseButton.setVisible(true);
        }
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
        statusScreenState.currentlySelectedPrinterProperty().addListener((ObservableValue<? extends Printer> observable, Printer oldValue, Printer state) ->
        {
            calibrationHelper.setPrinterToUse(state);
        });

        calibrationHelper.setPrinterToUse(statusScreenState.getCurrentlySelectedPrinter());

        calibrationHelper.addStateListener(this);
        calibrationHelper.setState(NozzleOffsetCalibrationState.IDLE);
    }

    @Override
    public void setState(NozzleOffsetCalibrationState state)
    {
        switch (state)
        {
            case IDLE:
                startCalibrationButton.setVisible(true);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case INITIALISING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case HEAD_CLEAN_CHECK:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case MEASURE_Z_DIFFERENCE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case INSERT_PAPER:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case PROBING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(true);
                justRightButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case FINISHED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(false);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case FAILED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
        }
    }
}
