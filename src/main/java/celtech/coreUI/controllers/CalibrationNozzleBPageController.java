
package celtech.coreUI.controllers;

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

    private String initialisingMessage = null;
    private String heatingMessage = null;
    private String readyToBeginMessage = null;
    private String primingNozzleMessage = null;
    private String noMaterialShouldBePresentMessage = null;
    private String materialShouldBePresentMessage = null;
    private String isMaterialExtrudingInstruction = null;
    private String isMaterialExtrudingEitherNozzle = null;
    private String nozzleCalibrationFailedMessage = null;
    private String ensureHeadIsCleanMessage = null;
    private String ensureHeadIsCleanInstruction = null;
    private String calibrationCommencedMessage = null;
    private String calibrationSucceededMessage = null;
    private String pressAKeyMessage = null;
    private String pressAKeyToContinueMessage = null;
    private String preparingExtruderMessage = null;
    private ResourceBundle i18nBundle = null;
    private Printer printerToUse = null;
    private final float bOffsetStartingValue = 0.9f;
    private int currentNozzleNumber = 0;
    private float nozzlePosition = 0;
    private float nozzle0BOffset = 0;
    private float nozzle1BOffset = 0;

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
                        printerToUse.transmitWriteHeadEEPROM(savedHeadData.getHeadTypeCode(),
                                savedHeadData.getUniqueID(),
                                savedHeadData.getMaximumTemperature(),
                                savedHeadData.getThermistorBeta(),
                                savedHeadData.getThermistorTCal(),
                                0,
                                0,
                                0,
                                nozzle0BOffset,
                                0,
                                0,
                                0,
                                nozzle1BOffset,
                                savedHeadData.getLastFilamentTemperature(),
                                savedHeadData.getHoursUsed());

                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error in needle valve calibration - mode=" + state.name());
                    }
                    setState(NozzleBCalibrationState.CONFIRM_NO_MATERIAL);
                }
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

            printerToUse.transmitDirectGCode("G0 B0", false);
            printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
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

        initialisingMessage = i18nBundle.getString("calibrationPanel.BCalibrationInitialising");
        heatingMessage = i18nBundle.getString("calibrationPanel.BCalibrationHeating");
        pressAKeyMessage = i18nBundle.getString("calibrationPanel.pressAKey");
        preparingExtruderMessage = i18nBundle.getString("calibrationPanel.preparingExtruder");
        pressAKeyToContinueMessage = i18nBundle.getString("calibrationPanel.pressAKeyToContinue");
        readyToBeginMessage = i18nBundle.getString("calibrationPanel.readyToBeginTest");
        primingNozzleMessage = i18nBundle.getString("calibrationPanel.primingNozzle");
        isMaterialExtrudingInstruction = i18nBundle.getString("calibrationPanel.isMaterialExtruding");
        noMaterialShouldBePresentMessage = i18nBundle.getString("calibrationPanel.valvesClosedNoMaterial");
        materialShouldBePresentMessage = i18nBundle.getString("calibrationPanel.valvesOpenMaterialExtruding");
        nozzleCalibrationFailedMessage = i18nBundle.getString("calibrationPanel.nozzleCalibrationFailed");
        ensureHeadIsCleanMessage = i18nBundle.getString("calibrationPanel.ensureHeadIsCleanMessage");
        ensureHeadIsCleanInstruction = i18nBundle.getString("calibrationPanel.ensureHeadIsCleanInstruction");
        calibrationCommencedMessage = i18nBundle.getString("calibrationPanel.calibrationCommencedMessage");
        calibrationSucceededMessage = i18nBundle.getString("calibrationPanel.calibrationSucceededMessage");
        isMaterialExtrudingEitherNozzle = i18nBundle.getString("calibrationPanel.isMaterialExtrudingEitherNozzle");

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
                calibrationInstruction.setText("");
                calibrationStatus.setText(readyToBeginMessage);
                break;
            case INITIALISING:
                currentNozzleNumber = 0;
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(initialisingMessage);
                calibrationInstruction.setText("");

                try
                {
                    savedHeadData = printerToUse.transmitReadHeadEEPROM();
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getHeadTypeCode(),
                            savedHeadData.getUniqueID(),
                            savedHeadData.getMaximumTemperature(),
                            savedHeadData.getThermistorBeta(),
                            savedHeadData.getThermistorTCal(),
                            0,
                            0,
                            0,
                            0.9f,
                            0,
                            0,
                            0,
                            -0.9f,
                            savedHeadData.getLastFilamentTemperature(),
                            savedHeadData.getHoursUsed());

                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }

                calibrationTask = new CalibrateBTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                Thread initialiseTaskThread = new Thread(calibrationTask);
                initialiseTaskThread.start();
                break;
            case HEATING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(heatingMessage);
                calibrationInstruction.setText("");

                calibrationTask = new CalibrateBTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                Thread heatingTaskThread = new Thread(calibrationTask);
                heatingTaskThread.start();

                break;

            case PRIMING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(primingNozzleMessage);
                calibrationInstruction.setText("");

                calibrationTask = new CalibrateBTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                Thread primingTaskThread = new Thread(calibrationTask);
                primingTaskThread.start();
                break;

            case NO_MATERIAL_CHECK:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(noMaterialShouldBePresentMessage);
                calibrationInstruction.setText(isMaterialExtrudingEitherNozzle);
                break;
            case MATERIAL_EXTRUDING_CHECK:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(materialShouldBePresentMessage);
                calibrationInstruction.setText(isMaterialExtrudingInstruction + " " + currentNozzleNumber + "?");

                calibrationTask = new CalibrateBTask(state, currentNozzleNumber);
                calibrationTask.setOnFailed(failedTaskHandler);
                Thread materialExtrudingTaskThread = new Thread(calibrationTask);
                materialExtrudingTaskThread.start();
                break;
            case HEAD_CLEAN_CHECK:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(false);
                calibrationStatus.setText(ensureHeadIsCleanMessage);
                calibrationInstruction.setText(ensureHeadIsCleanInstruction);
                break;
            case PRE_CALIBRATION_PRIMING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(false);
                noButton.setVisible(false);
                calibrationStatus.setText(primingNozzleMessage);
                calibrationInstruction.setText("");

                nozzlePosition = 0;

                try
                {
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getHeadTypeCode(),
                            savedHeadData.getUniqueID(),
                            savedHeadData.getMaximumTemperature(),
                            savedHeadData.getThermistorBeta(),
                            savedHeadData.getThermistorTCal(),
                            0,
                            0,
                            0,
                            0.9f,
                            0,
                            0,
                            0,
                            -0.9f,
                            savedHeadData.getLastFilamentTemperature(),
                            savedHeadData.getHoursUsed());
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }

                calibrationTask = new CalibrateBTask(state, currentNozzleNumber);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                Thread preCalibrationPrimingTaskThread = new Thread(calibrationTask);
                preCalibrationPrimingTaskThread.start();
                break;
            case CALIBRATE_NOZZLE:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(calibrationCommencedMessage + " " + currentNozzleNumber);
                calibrationInstruction.setText(isMaterialExtrudingInstruction + " " + currentNozzleNumber + "?");
                try
                {
                    printerToUse.transmitDirectGCode("G0 B" + nozzlePosition, false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
                break;
            case CONFIRM_NO_MATERIAL:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(noMaterialShouldBePresentMessage);
                calibrationInstruction.setText(isMaterialExtrudingEitherNozzle);
                break;
            case CONFIRM_MATERIAL_EXTRUDING:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(true);
                yesButton.setVisible(true);
                noButton.setVisible(true);
                calibrationStatus.setText(materialShouldBePresentMessage);
                calibrationInstruction.setText(isMaterialExtrudingInstruction + " " + currentNozzleNumber + "?");

                calibrationTask = new CalibrateBTask(state, currentNozzleNumber);
                calibrationTask.setOnFailed(failedTaskHandler);
                Thread confirmMaterialExtrudingTaskThread = new Thread(calibrationTask);
                confirmMaterialExtrudingTaskThread.start();
                break;
            case FINISHED:
                startCalibrationButton.setVisible(false);
                cancelCalibrationButton.setVisible(false);
                yesButton.setVisible(false);
                noButton.setVisible(false);
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
                            nozzle0BOffset,
                            savedHeadData.getNozzle2XOffset(),
                            savedHeadData.getNozzle2YOffset(),
                            savedHeadData.getNozzle2ZOffset(),
                            nozzle1BOffset,
                            savedHeadData.getLastFilamentTemperature(),
                            savedHeadData.getHoursUsed());

                    printerToUse.transmitDirectGCode("G0 B0", false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
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
                calibrationStatus.setText(nozzleCalibrationFailedMessage);
                calibrationInstruction.setText("");
                break;
        }
    }

}
