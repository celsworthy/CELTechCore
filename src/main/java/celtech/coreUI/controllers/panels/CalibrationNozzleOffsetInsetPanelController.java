package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationStatus;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.NudgeControlVertical;
import celtech.coreUI.controllers.StatusScreenState;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.HardwarePrinter;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationNozzleOffsetInsetPanelController implements Initializable, CalibrationNozzleOffsetStateListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(CalibrationNozzleOffsetInsetPanelController.class.getName());

    private ResourceBundle i18nBundle = null;

    private final CalibrationNozzleOffsetHelper calibrationHelper = new CalibrationNozzleOffsetHelper();

    private FloatProperty nozzle1Overrun = new SimpleFloatProperty(0);
    private FloatProperty nozzle2Overrun = new SimpleFloatProperty(0);

    @FXML
    private VBox container;

    @FXML
    private Button tooTightButton;

    @FXML
    private VBox nudgeOrRecalibrateVBox;

    @FXML
    private VBox nudgeControlVBox;

    @FXML
    private Text calibrationInstruction;

    @FXML
    private ImageView headImage;

    @FXML
    private Button cancelCalibrationButton;

    @FXML
    private Button justRightButton;

    @FXML
    private NudgeControlVertical fineNozzleNudge;

    @FXML
    private NudgeControlVertical coarseNozzleNudge;

    @FXML
    private Button noButton;

    @FXML
    private HBox manualAdjustmentControls;

    @FXML
    private Button saveSettingsButton;

    @FXML
    private Button startCalibrationButton;

    @FXML
    private Text calibrationStatus;

    @FXML
    private Button yesButton;

    @FXML
    private Button tooLooseButton;

    @FXML
    void adjustOffsets(ActionEvent event)
    {
        calibrationHelper.setState(NozzleOffsetCalibrationState.NUDGE_MODE);

//        if (nudgeHead != null)
//        {
//            fineNozzleNudge.getValueProperty().unbindBidirectional(nudgeHead.getNozzles().get(0).getNozzle1_Z_overrunProperty());
//            coarseNozzleNudge.getValueProperty().unbindBidirectional(nudgeHead.getNozzle2_Z_overrunProperty());
//        }
//
//        nudgeHead = new Head(calibrationHelper.getSavedHeadData());
//        nudgeHead.deriveZOverrunFromOffsets();

        fineNozzleNudge.getValueProperty().bindBidirectional(nozzle1Overrun);
        coarseNozzleNudge.getValueProperty().bindBidirectional(nozzle2Overrun);
    }

    @FXML
    void recalibrate(ActionEvent event)
    {
        calibrationHelper.setState(NozzleOffsetCalibrationState.IDLE);
    }

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
        calibrationHelper.cancelCalibrationAction();
        closeAndReset();
    }

    @FXML
    void saveSettings(ActionEvent event)
    {
        if (calibrationHelper.getState() == NozzleOffsetCalibrationState.NUDGE_MODE)
        {
//            nudgeHead.deriveZOffsetsFromOverrun();
//            calibrationHelper.saveNozzleOffsets(nudgeHead.getNozzle1ZOffset(), nudgeHead.getNozzle2ZOffset());
        }
        closeAndReset();
    }

    private void closeAndReset()
    {
        calibrationHelper.setState(NozzleOffsetCalibrationState.CHOOSE_MODE);
        ApplicationStatus.getInstance().returnToLastMode();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        i18nBundle = DisplayManager.getLanguageBundle();

        StatusScreenState statusScreenState = StatusScreenState.getInstance();
        statusScreenState.currentlySelectedPrinterProperty().addListener((ObservableValue<? extends HardwarePrinter> observable, HardwarePrinter oldValue, HardwarePrinter state) ->
        {
            calibrationHelper.setPrinterToUse(state);
        });

        calibrationHelper.setPrinterToUse(statusScreenState.getCurrentlySelectedPrinter());

        calibrationHelper.addStateListener(this);
        calibrationHelper.setState(NozzleOffsetCalibrationState.CHOOSE_MODE);

        fineNozzleNudge.setDeltaValue(0.05);
        coarseNozzleNudge.setDeltaValue(0.05);

//        fineNozzleNudge.setMaxValue(Head.normalZ1OverrunMax);
//        fineNozzleNudge.setMinValue(Head.normalZ1OverrunMin);
//        coarseNozzleNudge.setMaxValue(Head.normalZ2OverrunMax);
//        coarseNozzleNudge.setMinValue(Head.normalZ2OverrunMin);
    }

    @Override
    public void setState(NozzleOffsetCalibrationState state)
    {
        switch (state)
        {
            case CHOOSE_MODE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                nudgeOrRecalibrateVBox.setVisible(true);
                nudgeControlVBox.setVisible(false);
                headImage.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case IDLE:
                startCalibrationButton.setVisible(true);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                nudgeOrRecalibrateVBox.setVisible(false);
                nudgeControlVBox.setVisible(false);
                headImage.setVisible(false);
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
                saveSettingsButton.setVisible(false);
                nudgeOrRecalibrateVBox.setVisible(false);
                nudgeControlVBox.setVisible(false);
                headImage.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case HEAD_CLEAN_CHECK:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(false);
                saveSettingsButton.setVisible(false);
                nudgeOrRecalibrateVBox.setVisible(false);
                nudgeControlVBox.setVisible(false);
                headImage.setVisible(false);
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
                saveSettingsButton.setVisible(false);
                nudgeOrRecalibrateVBox.setVisible(false);
                nudgeControlVBox.setVisible(false);
                headImage.setVisible(false);
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
                saveSettingsButton.setVisible(false);
                nudgeOrRecalibrateVBox.setVisible(false);
                nudgeControlVBox.setVisible(false);
                headImage.setVisible(false);
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
                saveSettingsButton.setVisible(false);
                nudgeOrRecalibrateVBox.setVisible(false);
                nudgeControlVBox.setVisible(false);
                headImage.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case FINISHED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                saveSettingsButton.setVisible(true);
                nudgeOrRecalibrateVBox.setVisible(false);
                nudgeControlVBox.setVisible(false);
                headImage.setVisible(false);
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
                saveSettingsButton.setVisible(false);
                nudgeOrRecalibrateVBox.setVisible(false);
                nudgeControlVBox.setVisible(false);
                headImage.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
            case NUDGE_MODE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                saveSettingsButton.setVisible(true);
                nudgeOrRecalibrateVBox.setVisible(false);
                nudgeControlVBox.setVisible(true);
                headImage.setVisible(true);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());
                break;
        }
    }
}
