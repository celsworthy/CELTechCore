/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.appManager.TaskController;
import celtech.configuration.Head;
import celtech.configuration.HeadContainer;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.GCodeConstants;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.services.calibration.CalibrateNozzleOffsetTask;
import celtech.services.calibration.NozzleBCalibrationState;
import celtech.services.calibration.NozzleOffsetCalibrationState;
import celtech.services.calibration.NozzleOffsetCalibrationStepResult;
import java.util.ArrayList;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationNozzleOffsetHelper
{

    private Stenographer steno = StenographerFactory.getStenographer(CalibrationNozzleOffsetHelper.class.getName());

    private Printer printerToUse = null;

    private double zco = 0;
    private double zDifference = 0;

    private HeadEEPROMDataResponse savedHeadData = null;

    private CalibrateNozzleOffsetTask calibrationTask = null;

    private NozzleOffsetCalibrationState state = NozzleOffsetCalibrationState.IDLE;
    private ArrayList<CalibrationNozzleOffsetStateListener> stateListeners = new ArrayList<>();

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

    public void setPrinterToUse(Printer printer)
    {
        this.printerToUse = printer;
    }

    public void yesButtonAction()
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
            case HEAD_CLEAN_CHECK:
                setState(NozzleOffsetCalibrationState.MEASURE_Z_DIFFERENCE);
                break;
        }
    }

    public void tooLooseAction()
    {
        zco -= 0.05;

        try
        {
            printerToUse.transmitDirectGCode("G0 Z" + zco, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error changing Z height");
        }
    }

    public void tooTightAction()
    {
        zco += 0.05;

        if (zco <= 0)
        {
            zco = 0;
        }

        try
        {
            printerToUse.transmitDirectGCode("G0 Z" + zco, false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error changing Z height");
        }
    }

    public double getZCo()
    {
        return zco;
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

            printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
            printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
            printerToUse.transmitDirectGCode("G90", false);
            printerToUse.transmitDirectGCode("G0 Z25", false);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error in nozzle offset calibration - mode=" + state.name());
        }

    }

    public void addStateListener(CalibrationNozzleOffsetStateListener stateListener)
    {
        stateListeners.add(stateListener);
    }

    public void removeStateListener(CalibrationNozzleOffsetStateListener stateListener)
    {
        stateListeners.remove(stateListener);
    }

    public void setState(NozzleOffsetCalibrationState newState)
    {
        this.state = newState;
        for (CalibrationNozzleOffsetStateListener listener : stateListeners)
        {
            listener.setState(state);
        }

        switch (newState)
        {
            case IDLE:
                break;
            case INITIALISING:
                try
                {
                    savedHeadData = printerToUse.transmitReadHeadEEPROM();

                    zco = 0.5 * (savedHeadData.getNozzle1ZOffset() + savedHeadData.getNozzle2ZOffset());
                    zDifference = savedHeadData.getNozzle2ZOffset() - savedHeadData.getNozzle1ZOffset();

                    Head defaultHead = HeadContainer.getCompleteHeadList().get(0);
                    printerToUse.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
                                                         savedHeadData.getUniqueID(),
                                                         savedHeadData.getMaximumTemperature(),
                                                         savedHeadData.getBeta(),
                                                         savedHeadData.getTCal(),
                                                         defaultHead.getNozzle1XOffset(),
                                                         defaultHead.getNozzle1YOffset(),
                                                         0,
                                                         savedHeadData.getNozzle1BOffset(),
                                                         defaultHead.getNozzle2XOffset(),
                                                         defaultHead.getNozzle2YOffset(),
                                                         0,
                                                         savedHeadData.getNozzle2BOffset(),
                                                         savedHeadData.getLastFilamentTemperature(),
                                                         savedHeadData.getHeadHours());
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
            case HEAD_CLEAN_CHECK:
                break;
            case MEASURE_Z_DIFFERENCE:
                calibrationTask = new CalibrateNozzleOffsetTask(state);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread measureTaskThread = new Thread(calibrationTask);
                measureTaskThread.setName("Calibration N - measuring");
                measureTaskThread.start();
                break;
            case INSERT_PAPER:
                break;
            case PROBING:
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
                                                         (float) (-zco - (0.5 * zDifference)),
                                                         savedHeadData.getNozzle1BOffset(),
                                                         savedHeadData.getNozzle2XOffset(),
                                                         savedHeadData.getNozzle2YOffset(),
                                                         (float) (-zco + (0.5 * zDifference)),
                                                         savedHeadData.getNozzle2BOffset(),
                                                         savedHeadData.getLastFilamentTemperature(),
                                                         savedHeadData.getHeadHours());

                    printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
                    printerToUse.transmitDirectGCode("G90", false);
                    printerToUse.transmitDirectGCode("G0 Z25", false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in nozzle offset calibration - mode=" + state.name());
                }
                break;
            case FAILED:
                try
                {
                    printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
                    printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
                    printerToUse.transmitDirectGCode("G90", false);
                    printerToUse.transmitDirectGCode("G0 Z25", false);
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error clearing up after failed calibration");
                }
                break;
        }
    }

    public NozzleOffsetCalibrationState getState()
    {
        return state;
    }

}
