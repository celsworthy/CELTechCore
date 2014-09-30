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

    private CalibrationXAndYState state = CalibrationXAndYState.IDLE;
    private ArrayList<CalibrationXAndYStateListener> stateListeners = new ArrayList<>();
    private CalibrateXAndYTask calibrationTask;

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
    }

    public void setState(CalibrationXAndYState newState)
    {
        this.state = newState;
        for (CalibrationXAndYStateListener listener : stateListeners)
        {
            listener.setXAndYState(state);
        }

        switch (newState)
        {
            case IDLE:
                break;
            case HEATING:
                calibrationTask = new CalibrateXAndYTask(state, printerToUse);
                calibrationTask.setOnSucceeded(succeededTaskHandler);
                calibrationTask.setOnFailed(failedTaskHandler);
                TaskController.getInstance().manageTask(calibrationTask);

                Thread heatingTaskThread = new Thread(calibrationTask);
                heatingTaskThread.setName("Calibration - heating");
                heatingTaskThread.start();
            case FINISHED:
        {
            try
            {
                switchHeaterOffAndRaiseHead();
            } catch (RoboxCommsException ex)
            {
                steno.error("Error in x and y calibration - mode=" + state.name());
            }
        }
                break;
            case FAILED:
        {
            try
            {
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

}
