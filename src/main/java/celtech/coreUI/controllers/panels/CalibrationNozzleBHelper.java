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
import celtech.services.calibration.NozzleBCalibrationState;
import java.util.ArrayList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationNozzleBHelper
{

    private Stenographer steno = StenographerFactory.getStenographer(CalibrationNozzleBHelper.class.getName());

    private Printer printerToUse = null;

    private final float bOffsetStartingValue = 0.8f;
    private int currentNozzleNumber = 0;
    private float nozzlePosition = 0;
    private float nozzle0BOffset = 0;
    private float nozzle1BOffset = 0;

    private HeadEEPROMDataResponse savedHeadData = null;

    private CalibrateBTask calibrationTask = null;
    private boolean parkRequired = false;
    private NozzleBCalibrationState state = NozzleBCalibrationState.IDLE;
    private ArrayList<CalibrationBStateListener> stateListeners = new ArrayList<>();

    private EventHandler<WorkerStateEvent> failedTaskHandler = (WorkerStateEvent event) ->
    {
        cancelCalibrationAction();
    };

    private EventHandler<WorkerStateEvent> succeededTaskHandler = (WorkerStateEvent event) ->
    {
        setState(state.getNextState());
    };

    public void setPrinterToUse(Printer printer)
    {
        this.printerToUse = printer;
    }

    public void yesButtonAction()
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

    public void noButtonAction()
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
                nozzlePosition += 0.05;
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
        if (state != NozzleBCalibrationState.IDLE)
        {
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
        }
    }

    public NozzleBCalibrationState getState()
    {
        return state;
    }

    public int getCurrentNozzleNumber()
    {
        return currentNozzleNumber;
    }

    public void addStateListener(CalibrationBStateListener stateListener)
    {
        stateListeners.add(stateListener);
    }

    public void removeStateListener(CalibrationBStateListener stateListener)
    {
        stateListeners.remove(stateListener);
    }

    public void setState(NozzleBCalibrationState newState)
    {
        this.state = newState;
        for (CalibrationBStateListener listener : stateListeners)
        {
            listener.setState(state);
        }

        switch (state)
        {
            case IDLE:
                break;
            case INITIALISING:
                currentNozzleNumber = 0;

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
                calibrationTask = new CalibrateBTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread heatingTaskThread = new Thread(calibrationTask);
                heatingTaskThread.setName("Calibration - heating");
                heatingTaskThread.start();

                break;

            case PRIMING:
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
                break;
            case MATERIAL_EXTRUDING_CHECK:
                calibrationTask = new CalibrateBTask(state, currentNozzleNumber);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread materialExtrudingTaskThread = new Thread(calibrationTask);
                materialExtrudingTaskThread.setName("Calibration - extrusion check");
                materialExtrudingTaskThread.start();
                break;
            case HEAD_CLEAN_CHECK:
                break;
            case PRE_CALIBRATION_PRIMING:
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
                try
                {
                    printerToUse.transmitDirectGCode("G0 B" + nozzlePosition, false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in needle valve calibration - mode=" + state.name());
                }
                break;
            case HEAD_CLEAN_CHECK_POST_CALIBRATION:
                break;
            case POST_CALIBRATION_PRIMING:
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
                break;
            case CONFIRM_MATERIAL_EXTRUDING:
                calibrationTask = new CalibrateBTask(state, currentNozzleNumber);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread confirmMaterialExtrudingTaskThread = new Thread(calibrationTask);
                confirmMaterialExtrudingTaskThread.setName("Calibration - extruding");
                confirmMaterialExtrudingTaskThread.start();
                break;
            case FINISHED:
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
