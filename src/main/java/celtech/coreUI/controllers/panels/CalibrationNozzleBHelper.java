/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.appManager.TaskController;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.services.calibration.CalibrateBTask;
import celtech.services.calibration.NozzleOpeningCalibrationState;
import java.util.ArrayList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationNozzleBHelper implements CalibrationHelper
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationNozzleBHelper.class.getName());

    private final static int FINE_NOZZLE = 0;
    private final static int FILL_NOZZLE = 1;

    private Printer printerToUse = null;

    private final float bOffsetStartingValue = 0.8f;
    private float nozzlePosition = 0;
    private float nozzle0BOffset = 0;
    private float nozzle1BOffset = 0;

    private HeadEEPROMDataResponse savedHeadData = null;

    private CalibrateBTask calibrationTask = null;
    private boolean parkRequired = false;
    private NozzleOpeningCalibrationState state = NozzleOpeningCalibrationState.IDLE;
    private final ArrayList<CalibrationBStateListener> stateListeners = new ArrayList<>();

    private final EventHandler<WorkerStateEvent> failedTaskHandler = (WorkerStateEvent event) ->
    {
        cancelCalibrationAction();
    };

    private final EventHandler<WorkerStateEvent> succeededTaskHandler = (WorkerStateEvent event) ->
    {
        setState(state.getNextState());
    };

    @Override
    public void setPrinterToUse(Printer printer)
    {
        this.printerToUse = printer;
    }

    @Override
    public void buttonAAction()
    {
        switch (state)
        {
            case NO_MATERIAL_CHECK:
                setState(NozzleOpeningCalibrationState.FAILED);
                break;
            case CALIBRATE_FINE_NOZZLE:
                try
                {
                    // close nozzle
                    printerToUse.transmitDirectGCode("G0 B0", false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
                // calculate offset
                nozzle0BOffset = bOffsetStartingValue - 0.1f + nozzlePosition;
                setState(NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FINE_NOZZLE);
                break;
            case HEAD_CLEAN_CHECK_FINE_NOZZLE:
                setState(NozzleOpeningCalibrationState.PRE_CALIBRATION_PRIMING_FILL);
                break;
            case CALIBRATE_FILL_NOZZLE:
                try
                {
                    printerToUse.transmitDirectGCode("G0 B0", false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }

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
                setState(NozzleOpeningCalibrationState.HEAD_CLEAN_CHECK_FILL_NOZZLE);
                break;
            case HEAD_CLEAN_CHECK_FILL_NOZZLE:
                setState(NozzleOpeningCalibrationState.CONFIRM_NO_MATERIAL);
                break;
            case CONFIRM_NO_MATERIAL:
                setState(NozzleOpeningCalibrationState.FAILED);
                break;
            case CONFIRM_MATERIAL_EXTRUDING_FINE:
                setState(NozzleOpeningCalibrationState.CONFIRM_MATERIAL_EXTRUDING_FILL);
                break;
            case CONFIRM_MATERIAL_EXTRUDING_FILL:
                setState(NozzleOpeningCalibrationState.FINISHED);
                break;
        }

    }

    @Override
    public void nextButtonAction()
    {
        setState(state.getNextState());
    }

    @Override
    public void buttonBAction()
    {
        switch (state)
        {
            case NO_MATERIAL_CHECK:
                setState(NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE);
                break;

            case CALIBRATE_FINE_NOZZLE:
                nozzlePosition += 0.05;
                steno.info("(FINE) nozzle position set to " + nozzlePosition);
                if (nozzlePosition <= 2f)
                {
                    setState(NozzleOpeningCalibrationState.CALIBRATE_FINE_NOZZLE);
                } else
                {
                    setState(NozzleOpeningCalibrationState.FAILED);
                }
                break;

            case CALIBRATE_FILL_NOZZLE:
                nozzlePosition += 0.05;
                steno.info("(FILL) nozzle position set to " + nozzlePosition);
                if (nozzlePosition <= 2f)
                {
                    setState(NozzleOpeningCalibrationState.CALIBRATE_FILL_NOZZLE);
                } else
                {
                    setState(NozzleOpeningCalibrationState.FAILED);
                }
                break;
            case CONFIRM_NO_MATERIAL:
                setState(NozzleOpeningCalibrationState.CONFIRM_MATERIAL_EXTRUDING_FINE);
                break;
            case CONFIRM_MATERIAL_EXTRUDING_FINE:
                setState(NozzleOpeningCalibrationState.FAILED);
                break;
            case CONFIRM_MATERIAL_EXTRUDING_FILL:
                setState(NozzleOpeningCalibrationState.FAILED);
                break;
        }

    }

    /**
     *
     */
    @Override
    public void cancelCalibrationAction()
    {
        if (calibrationTask != null)
        {
            if (calibrationTask.isRunning())
            {
                calibrationTask.cancel();
            }
        }
        if (state != NozzleOpeningCalibrationState.IDLE)
        {
            try
            {
                if (savedHeadData != null && state != NozzleOpeningCalibrationState.FINISHED)
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
        }
    }

    public NozzleOpeningCalibrationState getState()
    {
        return state;
    }

    public void addStateListener(CalibrationBStateListener stateListener)
    {
        stateListeners.add(stateListener);
    }

    public void removeStateListener(CalibrationBStateListener stateListener)
    {
        stateListeners.remove(stateListener);
    }

    public void setState(NozzleOpeningCalibrationState newState)
    {
        steno.info("Go to state " + newState);

        this.state = newState;
        for (CalibrationBStateListener listener : stateListeners)
        {
            listener.setNozzleOpeningState(state);
        }

        switch (state)
        {
            case IDLE:
                break;

            case HEATING:
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

                calibrationTask = new CalibrateBTask(state, printerToUse);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread heatingTaskThread = new Thread(calibrationTask);
                heatingTaskThread.setName("Calibration - heating");
                heatingTaskThread.start();

                break;

            case NO_MATERIAL_CHECK:
                calibrationTask = new CalibrateBTask(state, printerToUse);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread primingTaskThread = new Thread(calibrationTask);
                primingTaskThread.setName("Calibration - no material check");
                primingTaskThread.start();

                parkRequired = true;
                break;
            case PRE_CALIBRATION_PRIMING_FINE:
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

                calibrationTask = new CalibrateBTask(state, FINE_NOZZLE, printerToUse);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread preCalibrationPrimingTaskThread = new Thread(calibrationTask);
                preCalibrationPrimingTaskThread.setName("Calibration - pre calibration priming");
                preCalibrationPrimingTaskThread.start();
                break;
            case CALIBRATE_FINE_NOZZLE:
                try
                {
                    // open / close nozzle with specific B value
                    printerToUse.transmitDirectGCode("G0 B" + nozzlePosition, false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
                break;
            case PRE_CALIBRATION_PRIMING_FILL:
                calibrationTask = new CalibrateBTask(state, FILL_NOZZLE, printerToUse);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread preCalibrationPrimingFillTaskThread = new Thread(calibrationTask);
                preCalibrationPrimingFillTaskThread.setName("Calibration - pre calibration priming");
                preCalibrationPrimingFillTaskThread.start();
                break;
            case CALIBRATE_FILL_NOZZLE:
                try
                {
                    printerToUse.transmitDirectGCode("G0 B" + nozzlePosition, false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
                break;
            case HEAD_CLEAN_CHECK_FINE_NOZZLE:
                break;
            case HEAD_CLEAN_CHECK_FILL_NOZZLE:
                break;
            case CONFIRM_NO_MATERIAL:
                nozzlePosition = 0;
                break;
            case CONFIRM_MATERIAL_EXTRUDING_FINE:
                calibrationTask = new CalibrateBTask(state, FINE_NOZZLE, printerToUse);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread confirmMaterialExtrudingTaskThread = new Thread(calibrationTask);
                confirmMaterialExtrudingTaskThread.setName("Calibration - extruding");
                confirmMaterialExtrudingTaskThread.start();
                break;
            case CONFIRM_MATERIAL_EXTRUDING_FILL:
                calibrationTask = new CalibrateBTask(state, FILL_NOZZLE, printerToUse);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread confirmMaterialExtrudingTaskFillThread = new Thread(calibrationTask);
                confirmMaterialExtrudingTaskFillThread.setName("Calibration - extruding");
                confirmMaterialExtrudingTaskFillThread.start();
                break;
            case PARKING:
                calibrationTask = new CalibrateBTask(state, printerToUse);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread parkingTaskThread = new Thread(calibrationTask);
                parkingTaskThread.setName("Calibration - parking");
                parkingTaskThread.start();
                parkRequired = false;
                break;
            case FINISHED:
                try
                {
                    printerToUse.transmitDirectGCode("G0 B0", false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
                break;
            case FAILED:
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

    @Override
    public void saveSettings()
    {
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

        } catch (RoboxCommsException ex)
        {
            steno.error("Error in needle valve calibration - saving settings");
        }
    }

    @Override
    public void goToIdleState()
    {
        setState(NozzleOpeningCalibrationState.IDLE);
    }
}
