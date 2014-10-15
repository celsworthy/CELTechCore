package celtech.coreUI.controllers.panels;

import celtech.services.calibration.NozzleOpeningCalibrationState;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

/**
 *
 * @author Ian
 */
public class CalibrationNozzleBInsetPanelController implements Initializable, CalibrationBStateListener
{

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setNozzleOpeningState(NozzleOpeningCalibrationState state)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    private Stenographer steno = StenographerFactory.getStenographer(CalibrationNozzleBInsetPanelController.class.getName());
//
//    private CalibrationNozzleBHelper calibrationHelper = new CalibrationNozzleBHelper();
//
//    private ResourceBundle i18nBundle = null;
//
//    @FXML
//    private VBox container;
//
//    @FXML
//    private Text calibrationInstruction;
//
//    @FXML
//    private Button startCalibrationButton;
//
//    @FXML
//    private Button cancelCalibrationButton;
//
//    @FXML
//    private Button saveSettingsButton;
//
//    @FXML
//    private Text calibrationStatus;
//
//    @FXML
//    private Button yesButton;
//
//    @FXML
//    private Button noButton;
//
//    @FXML
//    void yesButtonAction(ActionEvent event)
//    {
//        calibrationHelper.yesButtonAction();
//    }
//
//    @FXML
//    void noButtonAction(ActionEvent event)
//    {
//        calibrationHelper.noButtonAction();
//    }
//
//    @FXML
//    void startCalibration(ActionEvent event)
//    {
//        calibrationHelper.setState(NozzleBCalibrationState.INITIALISING);
//    }
//
//    @FXML
//    void cancelCalibration(ActionEvent event)
//    {
//        cancelCalibrationAction();
//    }
//
//    @FXML
//    void saveSettings(ActionEvent event)
//    {
//        calibrationHelper.saveSettings();
//        calibrationHelper.setState(NozzleBCalibrationState.IDLE);
//        ApplicationStatus.getInstance().returnToLastMode();
//    }
//
//    /**
//     *
//     */
//    public void cancelCalibrationAction()
//    {
//        calibrationHelper.cancelCalibrationAction();
//        calibrationHelper.setState(NozzleBCalibrationState.IDLE);
//        ApplicationStatus.getInstance().returnToLastMode();
//    }
//
//    @Override
//    public void initialize(URL location, ResourceBundle resources)
//    {
//        i18nBundle = DisplayManager.getLanguageBundle();
//
//        StatusScreenState statusScreenState = StatusScreenState.getInstance();
//
//        calibrationHelper.setPrinterToUse(statusScreenState.currentlySelectedPrinterProperty().get());
//
//        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
//        {
//            @Override
//            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
//            {
//                calibrationHelper.setPrinterToUse(newValue);
//            }
//        });
//
//        calibrationHelper.addStateListener(this);
//        calibrationHelper.setState(NozzleBCalibrationState.IDLE);
//    }
//
//    @Override
//    public void setState(NozzleBCalibrationState state)
//    {
//        switch (state)
//        {
//            case IDLE:
//                startCalibrationButton.setVisible(true);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(false);
//                noButton.setVisible(false);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case INITIALISING:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(false);
//                noButton.setVisible(false);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case HEATING:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(false);
//                noButton.setVisible(false);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case PRIMING:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(false);
//                noButton.setVisible(false);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case NO_MATERIAL_CHECK:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(true);
//                noButton.setVisible(true);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case MATERIAL_EXTRUDING_CHECK:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(true);
//                noButton.setVisible(true);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
//                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
//                break;
//            case HEAD_CLEAN_CHECK:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(true);
//                noButton.setVisible(false);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case PRE_CALIBRATION_PRIMING:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(false);
//                noButton.setVisible(false);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case CALIBRATE_NOZZLE:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(true);
//                noButton.setVisible(true);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
//                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
//                break;
//            case HEAD_CLEAN_CHECK_POST_CALIBRATION:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(true);
//                noButton.setVisible(false);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case POST_CALIBRATION_PRIMING:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(false);
//                noButton.setVisible(false);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case CONFIRM_NO_MATERIAL:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(true);
//                noButton.setVisible(true);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case CONFIRM_MATERIAL_EXTRUDING:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(true);
//                noButton.setVisible(true);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
//                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(calibrationHelper.getCurrentNozzleNumber())));
//                break;
//            case FINISHED:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(false);
//                noButton.setVisible(false);
//                saveSettingsButton.setVisible(true);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//            case FAILED:
//                startCalibrationButton.setVisible(false);
//                cancelCalibrationButton.setVisible(true);
//                yesButton.setVisible(false);
//                noButton.setVisible(false);
//                saveSettingsButton.setVisible(false);
//                calibrationStatus.setText(state.getStepTitle());
//                calibrationInstruction.setText(state.getStepInstruction());
//                break;
//        }
//    }
}
