/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model.calibration;

import celtech.appManager.TaskController;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.model.Printer;
import celtech.services.calibration.CalibrationXAndYState;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.Cancellable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class CalibrationAlignmentManager
{

    private final Printer printer;
    private HeadEEPROMDataResponse savedHeadData;

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationAlignmentManager.class.getName());
    
    Set<StateTransition> allowedTransitions;

    public enum GUIName
    {

        CANCEL, BACK, NEXT, RETRY, COMPLETE;
    }

    public CalibrationAlignmentManager(Printer printer)
    {
        this.printer = printer;
    }

    private final ObjectProperty<CalibrationXAndYState> state = new SimpleObjectProperty<>(
        CalibrationXAndYState.IDLE);

    public ReadOnlyObjectProperty<CalibrationXAndYState> stateProperty()
    {
        return state;
    }
    
    public void setTransitions(Set<StateTransition> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    public Set<StateTransition> getTransitions(CalibrationXAndYState startState)
    {
        Set<StateTransition> transitions = new HashSet<>();
        for (StateTransition allowedTransition : allowedTransitions)
        {
            if (allowedTransition.fromState == startState) {
                transitions.add(allowedTransition);
            }
        }
        return transitions;
    }

    private void setState(CalibrationXAndYState state)
    {
        this.state.set(state);
    }

    public void followTransition(StateTransition stateTransition)
    {

        EventHandler<WorkerStateEvent> goToNextState = (WorkerStateEvent event) ->
        {
            setState(stateTransition.toState);
        };

        EventHandler<WorkerStateEvent> gotToFailedState = (WorkerStateEvent event) ->
        {
            setState(stateTransition.transitionFailedState);
        };

        Task calibrationTask = new CalibrationTask(stateTransition.action);
        calibrationTask.setOnSucceeded(goToNextState);
        calibrationTask.setOnFailed(gotToFailedState);
        TaskController.getInstance().manageTask(calibrationTask);

        Thread taskThread = new Thread(calibrationTask);
        taskThread.setName("Calibration");
        taskThread.start();
    }

    private void doPrintCircleAction() throws Exception
    {
        try
        {
            savedHeadData = printer.readHeadEEPROM();
            printer.runMacro("rbx_XY_offset_roboxised");
            PrinterUtils.waitOnMacroFinished(printer, (Cancellable) null);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error in nozzle alignment");
            setState(CalibrationXAndYState.FAILED);
        }
    }

}
