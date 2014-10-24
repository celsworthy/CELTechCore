package celtech.utils.tasks;

import celtech.appManager.TaskController;
import celtech.printerControl.model.HardwarePrinter;
import java.util.concurrent.Callable;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class LiveTaskExecutor implements TaskExecutor
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        HardwarePrinter.class.getName());

    @Override
    public void runOnGUIThread(Runnable runnable)
    {
        if (Platform.isFxApplicationThread())
        {
            runnable.run();
        } else
        {
            Platform.runLater(runnable);
        }
    }

    @Override
    public void respondOnGUIThread(TaskResponder responder, boolean success, String message)
    {
        if (responder != null)
        {
            TaskResponse taskResponse = new TaskResponse(message);
            taskResponse.setSucceeded(success);

            Platform.runLater(() ->
            {
                responder.taskEnded(taskResponse);
            });
        }
    }

    @Override
    public void respondOnCurrentThread(TaskResponder responder, boolean success, String message)
    {
        if (responder != null)
        {
            TaskResponse taskResponse = new TaskResponse(message);
            taskResponse.setSucceeded(success);

            responder.taskEnded(taskResponse);
        }
    }

    @Override
    public void runAsTask(Callable<Boolean> action, EventHandler<WorkerStateEvent> successHandler,
        EventHandler<WorkerStateEvent> failureHandler, String taskName)
    {
        
        Task task = new GenericTask(action);
        
        EventHandler<WorkerStateEvent> outerFailureHandler = (WorkerStateEvent event) ->
            {
                if (task.getException() != null) {
                    task.getException().printStackTrace();
                }
                failureHandler.handle(event);
            };
        
        task.setOnSucceeded(successHandler);
        task.setOnFailed(outerFailureHandler);
        TaskController.getInstance().manageTask(task);

        Thread taskThread = new Thread(task);


        taskThread.setName(taskName);
        taskThread.start();
    }
}
