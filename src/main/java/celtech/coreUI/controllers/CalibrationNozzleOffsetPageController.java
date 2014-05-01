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

    private NozzleOffsetCalibrationState state = NozzleOffsetCalibrationState.IDLE;

    private String readyToBeginMessage = null;
    private String initialisingMessage = null;
    private String insertPieceOfPaperMessage = null;
    private String isThePaperInPlaceInstruction = null;
    private String moveThePieceOfPaperMessage = null;
    private String moveThePieceOfPaperInstruction = null;
    private String calibrationSucceededMessage = null;
    private String nozzleCalibrationFailedMessage = null;
    private String measuringZOffsetMessage = null;
    private ResourceBundle i18nBundle = null;
    private Printer printerToUse = null;

    private double zco = 0;
    private double zDifference = 0;

    private HeadEEPROMDataResponse savedHeadData = null;

    private CalibrateNozzleOffsetTask calibrationTask = null;

    private EventHandler<WorkerStateEvent> failedTaskHandler = new EventHandler<WorkerStateEvent>()
    {
        @Override
        public void handle(WorkerStateEvent event)
        {
            cancelCalibrationAction();
        }
    };

    private EventHandler<WorkerStateEvent> succeededTaskHandler = new EventHandler<WorkerStateEvent>()
    {
        @Override
        public void handle(WorkerStateEvent event)
        {
            if (state == NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE)
            {
                NozzleOffsetCalibrationStepResult result = (NozzleOffsetCalibrationStepResult) event.getSource().getValue();
                if (result.isSuccess())
                {
                    zDifference = result.getFloatValue();
                    setState(state.getNextState());
                } else
                {
                    setState(NozzleOffsetCalibrationState.FAILED);
                }
            } else
            {
                setState(state.getNextState());
            }
        }
    };

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
        switch (state)
        {
            case INSERT_PAPER:
                try
                {
                    printerToUse.transmitDirectGCode("G28 Z", false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + state.name());
                }
                setState(NozzleOffsetCalibrationState.PROBING);
                break;
        }
    }

    @FXML
    void noButtonAction(ActionEvent event)
    {
    }

    @FXML
    void tooLooseAction(ActionEvent event)
    {
        zco += 0.1;
    }

    @FXML
    void tooTightAction(ActionEvent event)
    {
        zco -= 0.1;
    }

    @FXML
    void justRightAction(ActionEvent event)
    {
        steno.info("Zco calculated as " + zco);
        setState(NozzleOffsetCalibrationState.FINISHED);
    }

    @FXML
    void startCalibration(ActionEvent event)
    {
        setState(NozzleOffsetCalibrationState.INITIALISING);
    }

    @FXML
    void cancelCalibration(ActionEvent event)
    {
        cancelCalibrationAction();
    }

    public void cancelCalibrationAction()
    {
        if (calibrationTask != null)
        {
            if (calibrationTask.isRunning())
            {
                calibrationTask.cancel();
            }
        }

        try
        {
            if (savedHeadData != null && state != NozzleOffsetCalibrationState.FINISHED)
            {
                printerToUse.transmitWriteHeadEEPROM(savedHeadData.getHeadTypeCode(),
                                                     savedHeadData.getUniqueID(),
                                                     savedHeadData.getMaximumTemperature(),
                                                     savedHeadData.getThermistorBeta(),
                                                     savedHeadData.getThermistorTCal(),
                                                     savedHeadData.getNozzle1XOffset(),
                                                     savedHeadData.getNozzle1YOffset(),
                                                     savedHeadData.getNozzle1ZOffset(),
                                                     savedHeadData.getNozzle1BOffset(),
                                                     savedHeadData.getNozzle2XOffset(),
                                                     savedHeadData.getNozzle2YOffset(),
                                                     savedHeadData.getNozzle2ZOffset(),
                                                     savedHeadData.getNozzle2BOffset(),
                                                     savedHeadData.getLastFilamentTemperature(),
                                                     savedHeadData.getHoursUsed());
            }

            printerToUse.transmitDirectGCode("G0 Z25", false);
            printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error in nozzle offset calibration - mode=" + state.name());
        }

        if (state == NozzleOffsetCalibrationState.IDLE)
        {
            Stage stage = (Stage) container.getScene().getWindow();
            stage.close();
        } else
        {
            setState(NozzleOffsetCalibrationState.IDLE);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        i18nBundle = DisplayManager.getLanguageBundle();

        readyToBeginMessage = i18nBundle.getString("calibrationPanel.readyToBeginTest");
        initialisingMessage = i18nBundle.getString("calibrationPanel.BCalibrationInitialising");
        insertPieceOfPaperMessage = i18nBundle.getString("calibrationPanel.insertPieceOfPaper");
        isThePaperInPlaceInstruction = i18nBundle.getString("calibrationPanel.isThePaperInPlace");
        moveThePieceOfPaperMessage = i18nBundle.getString("calibrationPanel.moveThePaperMessage");
        moveThePieceOfPaperInstruction = i18nBundle.getString("calibrationPanel.moveThePaperInstruction");
        calibrationSucceededMessage = i18nBundle.getString("calibrationPanel.calibrationSucceededMessage");
        nozzleCalibrationFailedMessage = i18nBundle.getString("calibrationPanel.nozzleCalibrationFailed");
        measuringZOffsetMessage = i18nBundle.getString("calibrationPanel.measuringZOffset");

        StatusScreenState statusScreenState = StatusScreenState.getInstance();
        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
            {
                printerToUse = newValue;
            }
        });

        printerToUse = statusScreenState.getCurrentlySelectedPrinter();

        statusScreenState.currentlySelectedPrinterProperty().addListener(new ChangeListener<Printer>()
        {
            @Override
            public void changed(ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue)
            {
                printerToUse = newValue;
            }
        });

        setState(NozzleOffsetCalibrationState.IDLE);
    }

    private void setState(NozzleOffsetCalibrationState state)
    {
        this.state = state;
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
                calibrationInstruction.setText("");
                calibrationStatus.setText(readyToBeginMessage);
                break;
            case INITIALISING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(initialisingMessage);
                calibrationInstruction.setText("");

                try
                {
                    savedHeadData = printerToUse.transmitReadHeadEEPROM();

                    zco = 0.5 * (savedHeadData.getNozzle1ZOffset() + savedHeadData.getNozzle2ZOffset());
                    zDifference = savedHeadData.getNozzle2ZOffset() - savedHeadData.getNozzle1ZOffset();

                    Head defaultHead = HeadContainer.getCompleteHeadList().get(0);
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getHeadTypeCode(),
                                                         savedHeadData.getUniqueID(),
                                                         savedHeadData.getMaximumTemperature(),
                                                         savedHeadData.getThermistorBeta(),
                                                         savedHeadData.getThermistorTCal(),
                                                         defaultHead.getNozzle1_X_offset(),
                                                         defaultHead.getNozzle1_Y_offset(),
                                                         0,
                                                         savedHeadData.getNozzle1ZOffset(),
                                                         defaultHead.getNozzle2_X_offset(),
                                                         defaultHead.getNozzle2_Y_offset(),
                                                         0,
                                                         savedHeadData.getNozzle2ZOffset(),
                                                         savedHeadData.getLastFilamentTemperature(),
                                                         savedHeadData.getHoursUsed());
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + state.name());
                }

                calibrationTask = new CalibrateNozzleOffsetTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread initialiseTaskThread = new Thread(calibrationTask);
                initialiseTaskThread.setName("Calibration N - initialising");
                initialiseTaskThread.start();
                break;
            case MEASURE_Z_DIFFERENCE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(measuringZOffsetMessage);
                calibrationInstruction.setText("");

                calibrationTask = new CalibrateNozzleOffsetTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread measureTaskThread = new Thread(calibrationTask);
                measureTaskThread.setName("Calibration N - measuring");
                measureTaskThread.start();
                break;
            case INSERT_PAPER:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(insertPieceOfPaperMessage);
                calibrationInstruction.setText(isThePaperInPlaceInstruction);
                break;
            case PROBING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(true);
                tooTightButton.setVisible(true);
                justRightButton.setVisible(true);
                calibrationStatus.setText(moveThePieceOfPaperMessage);
                calibrationInstruction.setText(moveThePieceOfPaperInstruction);
                break;
            case FINISHED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(false);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(calibrationSucceededMessage);
                calibrationInstruction.setText("");

                try
                {
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getHeadTypeCode(),
                                                         savedHeadData.getUniqueID(),
                                                         savedHeadData.getMaximumTemperature(),
                                                         savedHeadData.getThermistorBeta(),
                                                         savedHeadData.getThermistorTCal(),
                                                         savedHeadData.getNozzle1XOffset(),
                                                         savedHeadData.getNozzle1YOffset(),
                                                         savedHeadData.getNozzle1ZOffset(),
                                                         (float) (zco - (0.5 * zDifference)),
                                                         savedHeadData.getNozzle2XOffset(),
                                                         savedHeadData.getNozzle2YOffset(),
                                                         savedHeadData.getNozzle2ZOffset(),
                                                         (float) (zco + (0.5 * zDifference)),
                                                         savedHeadData.getLastFilamentTemperature(),
                                                         savedHeadData.getHoursUsed());
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + state.name());
                }
                break;
            case FAILED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                tooLooseButton.setVisible(false);
                tooTightButton.setVisible(false);
                justRightButton.setVisible(false);
                calibrationStatus.setText(nozzleCalibrationFailedMessage);
                calibrationInstruction.setText("");
                break;
        }
    }

}
