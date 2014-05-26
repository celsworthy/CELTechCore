package celtech.coreUI.controllers;

import celtech.appManager.TaskController;
import celtech.coreUI.DisplayManager;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.services.calibration.CalibrateBTask;
import celtech.services.calibration.NozzleBCalibrationState;
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
public class CalibrationNozzleBPageController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(CalibrationNozzleBPageController.class.getName());

    private NozzleBCalibrationState state = NozzleBCalibrationState.IDLE;

    private ResourceBundle i18nBundle = null;
    private Printer printerToUse = null;
    private final float bOffsetStartingValue = 0.8f;
    private int currentNozzleNumber = 0;
    private float nozzlePosition = 0;
    private float nozzle0BOffset = 0;
    private float nozzle1BOffset = 0;

    private boolean parkRequired = false;

    private HeadEEPROMDataResponse savedHeadData = null;

    private CalibrateBTask calibrationTask = null;

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
            setState(state.getNextState());
        }
    };

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
        switch (state)
        {
            case NO_MATERIAL_CHECK:
                setState(NozzleBCalibrationState.FAILED);
                break;
            case MATERIAL_EXTRUDING_CHECK:
                try
                {
                    printerToUse.transmitDirectGCode("G0 B0", false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
                if (currentNozzleNumber == 0)
                {
                    currentNozzleNumber++;
                    setState(NozzleBCalibrationState.MATERIAL_EXTRUDING_CHECK);
                } else
                {
                    currentNozzleNumber = 0;
                    setState(NozzleBCalibrationState.HEAD_CLEAN_CHECK);
                }
                break;
            case HEAD_CLEAN_CHECK:
                currentNozzleNumber = 0;
                setState(NozzleBCalibrationState.PRE_CALIBRATION_PRIMING);
                break;
            case CALIBRATE_NOZZLE:
                try
                {
                    printerToUse.transmitDirectGCode("G0 B0", false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
                if (currentNozzleNumber == 0)
                {
                    nozzle0BOffset = bOffsetStartingValue - 0.1f + nozzlePosition;
                    currentNozzleNumber = 1;
                    setState(NozzleBCalibrationState.PRE_CALIBRATION_PRIMING);
                } else
                {
                    nozzle1BOffset = -bOffsetStartingValue + 0.1f - nozzlePosition;
                    try
                    {
                        printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                                             savedHeadData.getUniqueID(),
                                                             savedHeadData.getMaximumTemperature(),
                                                             savedHeadData.getBeta(),
                                                             savedHeadData.getTCal(),
                                                             0,
                                                             0,
                                                             0,
                                                             nozzle0BOffset,
                                                             0,
                                                             0,
                                                             0,
                                                             nozzle1BOffset,
                                                             savedHeadData.getLastFilamentTemperature(),
                                                             savedHeadData.getHeadHours());

                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error in needle valve calibration - mode=" + state.name());
                    }
                    setState(NozzleBCalibrationState.HEAD_CLEAN_CHECK_POST_CALIBRATION);
                }
                break;
            case HEAD_CLEAN_CHECK_POST_CALIBRATION:
                currentNozzleNumber = 0;
                setState(NozzleBCalibrationState.POST_CALIBRATION_PRIMING);
                break;
            case CONFIRM_NO_MATERIAL:
                setState(NozzleBCalibrationState.FAILED);
                break;
            case CONFIRM_MATERIAL_EXTRUDING:
                if (currentNozzleNumber == 0)
                {
                    currentNozzleNumber++;
                    setState(NozzleBCalibrationState.CONFIRM_MATERIAL_EXTRUDING);
                } else
                {
                    setState(NozzleBCalibrationState.FINISHED);
                }
                break;
        }
    }

    @FXML
    void noButtonAction(ActionEvent event)
    {
        switch (state)
        {
            case NO_MATERIAL_CHECK:
                currentNozzleNumber = 0;
                setState(NozzleBCalibrationState.MATERIAL_EXTRUDING_CHECK);
                break;
            case MATERIAL_EXTRUDING_CHECK:
                setState(NozzleBCalibrationState.FAILED);
                break;
            case CALIBRATE_NOZZLE:
                nozzlePosition += 0.1;
                if (nozzlePosition <= 2f)
                {
                    setState(NozzleBCalibrationState.CALIBRATE_NOZZLE);
                } else
                {
                    setState(NozzleBCalibrationState.FAILED);
                }
                break;
            case CONFIRM_NO_MATERIAL:
                currentNozzleNumber = 0;
                setState(NozzleBCalibrationState.CONFIRM_MATERIAL_EXTRUDING);
                break;
            case CONFIRM_MATERIAL_EXTRUDING:
                setState(NozzleBCalibrationState.FAILED);
                break;
        }
    }

    @FXML
    void startCalibration(ActionEvent event)
    {
        setState(NozzleBCalibrationState.INITIALISING);
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
        if (calibrationTask != null)
        {
            if (calibrationTask.isRunning())
            {
                calibrationTask.cancel();
            }
        }

        try
        {
            if (savedHeadData != null && state != NozzleBCalibrationState.FINISHED)
            {
                printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                                     savedHeadData.getUniqueID(),
                                                     savedHeadData.getMaximumTemperature(),
                                                     savedHeadData.getBeta(),
                                                     savedHeadData.getTCal(),
                                                     savedHeadData.getNozzle1XOffset(),
                                                     savedHeadData.getNozzle1YOffset(),
                                                     savedHeadData.getNozzle1ZOffset(),
                                                     savedHeadData.getNozzle1BOffset(),
                                                     savedHeadData.getNozzle2XOffset(),
                                                     savedHeadData.getNozzle2YOffset(),
                                                     savedHeadData.getNozzle2ZOffset(),
                                                     savedHeadData.getNozzle2BOffset(),
                                                     savedHeadData.getLastFilamentTemperature(),
                                                     savedHeadData.getHeadHours());
            }

            printerToUse.transmitDirectGCode("G0 B0", false);
            printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
            printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);

            if (parkRequired)
            {
                parkRequired = false;
                printerToUse.transmitStoredGCode("Park");
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error in needle valve calibration - mode=" + state.name());
        }

        if (state == NozzleBCalibrationState.IDLE)
        {
            Stage stage = (Stage) container.getScene().getWindow();
            stage.close();
        } else
        {
            setState(NozzleBCalibrationState.IDLE);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        i18nBundle = DisplayManager.getLanguageBundle();

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

        setState(NozzleBCalibrationState.IDLE);
    }

    private void setState(NozzleBCalibrationState state)
    {
        this.state = state;
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
                currentNozzleNumber = 0;
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());

                try
                {
                    savedHeadData = printerToUse.transmitReadHeadEEPROM();
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                                         savedHeadData.getUniqueID(),
                                                         savedHeadData.getMaximumTemperature(),
                                                         savedHeadData.getBeta(),
                                                         savedHeadData.getTCal(),
                                                         0,
                                                         0,
                                                         0,
                                                         bOffsetStartingValue,
                                                         0,
                                                         0,
                                                         0,
                                                         -bOffsetStartingValue,
                                                         savedHeadData.getLastFilamentTemperature(),
                                                         savedHeadData.getHeadHours());

                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }

                calibrationTask = new CalibrateBTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread initialiseTaskThread = new Thread(calibrationTask);
                initialiseTaskThread.setName("Calibration - initialiser");
                initialiseTaskThread.start();
                break;
            case HEATING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());

                calibrationTask = new CalibrateBTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread heatingTaskThread = new Thread(calibrationTask);
                heatingTaskThread.setName("Calibration - heating");
                heatingTaskThread.start();

                break;

            case PRIMING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());

                calibrationTask = new CalibrateBTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread primingTaskThread = new Thread(calibrationTask);
                primingTaskThread.setName("Calibration - priming");
                primingTaskThread.start();

                parkRequired = true;
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
                calibrationStatus.setText(state.getStepTitle(String.valueOf(currentNozzleNumber)));
                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(currentNozzleNumber)));

                calibrationTask = new CalibrateBTask(state, currentNozzleNumber);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread materialExtrudingTaskThread = new Thread(calibrationTask);
                materialExtrudingTaskThread.setName("Calibration - extrusion check");
                materialExtrudingTaskThread.start();
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

                nozzlePosition = 0;

                try
                {
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                                         savedHeadData.getUniqueID(),
                                                         savedHeadData.getMaximumTemperature(),
                                                         savedHeadData.getBeta(),
                                                         savedHeadData.getTCal(),
                                                         0,
                                                         0,
                                                         0,
                                                         bOffsetStartingValue,
                                                         0,
                                                         0,
                                                         0,
                                                         -bOffsetStartingValue,
                                                         savedHeadData.getLastFilamentTemperature(),
                                                         savedHeadData.getHeadHours());
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }

                calibrationTask = new CalibrateBTask(state, currentNozzleNumber);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread preCalibrationPrimingTaskThread = new Thread(calibrationTask);
                preCalibrationPrimingTaskThread.setName("Calibration - pre calibration priming");
                preCalibrationPrimingTaskThread.start();
                break;
            case CALIBRATE_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(state.getStepTitle(String.valueOf(currentNozzleNumber)));
                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(currentNozzleNumber)));
                try
                {
                    printerToUse.transmitDirectGCode("G0 B" + nozzlePosition, false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
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

                nozzlePosition = 0;
                calibrationTask = new CalibrateBTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread postCalibrationPrimingTaskThread = new Thread(calibrationTask);
                postCalibrationPrimingTaskThread.setName("Calibration - post calibration priming");
                postCalibrationPrimingTaskThread.start();
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
                calibrationStatus.setText(state.getStepTitle(String.valueOf(currentNozzleNumber)));
                calibrationInstruction.setText(state.getStepInstruction(String.valueOf(currentNozzleNumber)));

                calibrationTask = new CalibrateBTask(state, currentNozzleNumber);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread confirmMaterialExtrudingTaskThread = new Thread(calibrationTask);
                confirmMaterialExtrudingTaskThread.setName("Calibration - extruding");
                confirmMaterialExtrudingTaskThread.start();
                break;
            case FINISHED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(false);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());

                try
                {
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                                         savedHeadData.getUniqueID(),
                                                         savedHeadData.getMaximumTemperature(),
                                                         savedHeadData.getBeta(),
                                                         savedHeadData.getTCal(),
                                                         savedHeadData.getNozzle1XOffset(),
                                                         savedHeadData.getNozzle1YOffset(),
                                                         savedHeadData.getNozzle1ZOffset(),
                                                         nozzle0BOffset,
                                                         savedHeadData.getNozzle2XOffset(),
                                                         savedHeadData.getNozzle2YOffset(),
                                                         savedHeadData.getNozzle2ZOffset(),
                                                         nozzle1BOffset,
                                                         savedHeadData.getLastFilamentTemperature(),
                                                         savedHeadData.getHeadHours());

                    printerToUse.transmitDirectGCode("G0 B0", false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
                    printerToUse.transmitStoredGCode("Park");
                    parkRequired = false;
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
                break;
            case FAILED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(state.getStepTitle());
                calibrationInstruction.setText(state.getStepInstruction());

                try
                {
                    printerToUse.transmitDirectGCode("G0 B0", false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);

                    if (parkRequired)
                    {
                        printerToUse.transmitStoredGCode("Park");
                        parkRequired = false;
                    }
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error clearing up after failed calibration");
                }
                break;
        }
    }
}
