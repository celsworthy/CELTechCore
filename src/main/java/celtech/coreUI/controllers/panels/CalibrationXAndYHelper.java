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
import celtech.services.calibration.CalibrateXAndYTask;
import celtech.services.calibration.CalibrationXAndYState;
import java.util.ArrayList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CalibrationXAndYHelper implements CalibrationHelper
{

    private Stenographer steno = StenographerFactory.getStenographer(
        CalibrationXAndYHelper.class.getName());

    private Printer printerToUse = null;
    private HeadEEPROMDataResponse savedHeadData = null;

    private CalibrationXAndYState state = CalibrationXAndYState.IDLE;
    private ArrayList<CalibrationXAndYStateListener> stateListeners = new ArrayList<>();
    private CalibrateXAndYTask calibrationTask;

    private int xOffset = 0;
    private int yOffset = 0;

    private final EventHandler<WorkerStateEvent> failedTaskHandler = new EventHandler<WorkerStateEvent>()
    {
        @Override
        public void handle(WorkerStateEvent event)
        {
            cancelCalibrationAction();
        }
    };
    public BooleanProperty showDownButton = new SimpleBooleanProperty(true);

    public void addStateListener(CalibrationXAndYStateListener stateListener)
    {
        stateListeners.add(stateListener);
    }

    public void removeStateListener(CalibrationXAndYStateListener stateListener)
    {
        stateListeners.remove(stateListener);
    }

    private final EventHandler<WorkerStateEvent> succeededTaskHandler = new EventHandler<WorkerStateEvent>()
    {
        @Override
        public void handle(WorkerStateEvent event)
        {
            setState(state.getNextState());
        }
    };

    @Override
    public void setPrinterToUse(Printer printer)
    {
        this.printerToUse = printer;
    }

    @Override
    public void buttonBAction() // Alt = UP button = too tight
    {

    }

    @Override
    public void buttonAAction() // ALT = down button = too loose
    {
    }

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
        if (state != CalibrationXAndYState.IDLE)
        {
            try
            {
                if (savedHeadData != null && state != CalibrationXAndYState.FINISHED)
                {
                    steno.info("Calibration cancelled - restoring head data");
                    restoreHeadData();
                }

                switchHeaterOffAndRaiseHead();
            } catch (RoboxCommsException ex)
            {
                steno.error("Error in nozzle offset calibration - mode=" + state.name());
            }
        } else
        {
            steno.info("Cancelling from state " + state.name() + " - no change to head data");
        }

    }

    public void setState(CalibrationXAndYState newState)
    {
        steno.debug("Enter state " + newState);
        this.state = newState;
        for (CalibrationXAndYStateListener listener : stateListeners)
        {
            listener.setXAndYState(state);
        }

        switch (newState)
        {
            case IDLE:
                break;
//            case HEATING:
//            {
//                try
//                {
//                    savedHeadData = printerToUse.transmitReadHeadEEPROM();
//                    calibrationTask = new CalibrateXAndYTask(state, printerToUse);
//                    calibrationTask.setOnSucceeded(succeededTaskHandler);
//                    calibrationTask.setOnFailed(failedTaskHandler);
//                    TaskController.getInstance().manageTask(calibrationTask);
//
//                    Thread heatingTaskThread = new Thread(calibrationTask);
//                    heatingTaskThread.setName("Calibration - heating");
//                    heatingTaskThread.start();
//                } catch (RoboxCommsException ex)
//                {
//                    steno.error("Error in X And Y calibration - mode=" + state.name());
//                }
//            }
//            break;
            case PRINT_PATTERN:
                calibrationTask = new CalibrateXAndYTask(state, printerToUse);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread printingPatterTaskThread = new Thread(calibrationTask);
                printingPatterTaskThread.setName("Calibration - printing pattern");
                printingPatterTaskThread.start();
                break;
            case GET_Y_OFFSET:
                break;
            case PRINT_CIRCLE:
                calibrationTask = new CalibrateXAndYTask(state, printerToUse);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread printingCircleTaskThread = new Thread(calibrationTask);
                printingCircleTaskThread.setName("Calibration - printing circle");
                printingCircleTaskThread.start();
            case FINISHED:
                try
                {
                    saveSettings();
                    switchHeaterOffAndRaiseHead();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in x and y calibration - mode=" + state.name());
                }
                break;
            case FAILED:
            {
                try
                {
                    restoreHeadData();
                    switchHeaterOffAndRaiseHead();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Error in x and y calibration - mode=" + state.name());
                }
            }
            break;
        }
    }

    private void switchHeaterOffAndRaiseHead() throws RoboxCommsException
    {
        printerToUse.transmitDirectGCode(GCodeConstants.switchNozzleHeaterOff, false);
        printerToUse.transmitDirectGCode(GCodeConstants.switchOffHeadLEDs, false);
        printerToUse.transmitDirectGCode("G90", false);
        printerToUse.transmitDirectGCode("G0 Z25", false);
    }

    @Override
    public void nextButtonAction()
    {
        setState(state.getNextState());
    }

    @Override
    public void goToIdleState()
    {
        setState(CalibrationXAndYState.IDLE);
    }

    @Override
    public void retryAction()
    {
        setState(CalibrationXAndYState.PRINT_PATTERN);
    }

    private void restoreHeadData() throws RoboxCommsException
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

    private void saveSettings()
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
                                                 savedHeadData.getNozzle1BOffset(),
                                                 savedHeadData.getNozzle2XOffset(),
                                                 savedHeadData.getNozzle2YOffset(),
                                                 savedHeadData.getNozzle2ZOffset(),
                                                 savedHeadData.getNozzle2BOffset(),
                                                 savedHeadData.getLastFilamentTemperature(),
                                                 savedHeadData.getHeadHours());

        } catch (RoboxCommsException ex)
        {
            steno.error("Error in needle valve calibration - saving settings");
        }
    }

    @Override
    public void setXOffset(String xStr)
    {
        switch (xStr)
        {
            case "A":
                xOffset = 0;
                break;
            case "B":
                xOffset = 1;
                break;
            case "C":
                xOffset = 2;
                break;
            case "D":
                xOffset = 3;
                break;
            case "E":
                xOffset = 4;
                break;
            case "F":
                xOffset = 5;
                break;
            case "G":
                xOffset = 6;
                break;
            case "H":
                xOffset = 6;
                break;
            case "I":
                xOffset = 6;
                break;
            case "J":
                xOffset = 6;
                break;
            case "K":
                xOffset = 6;
                break;                
        }
    }

    @Override
    public void setYOffset(Integer yOffset)
    {
        this.yOffset = yOffset;
    }

}
