package celtech.utils.tasks;

import celtech.appManager.TaskController;
import celtech.printerControl.model.HardwarePrinter;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public void runAsTask(Callable<Boolean> action, Runnable successHandler,
        Runnable failureHandler, Runnable cancelledHandler, String taskName)
    {

        Runnable runTask = () ->
        {
            try
            {
                Boolean result = action.call();
                if (result)
                {
                    runOnGUIThread(successHandler);
                } else {
                    runOnGUIThread(cancelledHandler);
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
                steno.error("Failure running task: " + ex);
                runOnGUIThread(failureHandler);
            }
        };
        Thread taskThread = new Thread(runTask);
        taskThread.setName(taskName);
        taskThread.start();
    }
}
