package celtech.utils.tasks;

import celtech.printerControl.model.HardwarePrinter;
import java.util.concurrent.Callable;
import javafx.application.Platform;
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
        respondOnGUIThread(responder, success, message, null);
    }

    @Override
    public void respondOnGUIThread(TaskResponder responder, boolean success, String message, Object returnedObject)
    {
        if (responder != null)
        {
            TaskResponse taskResponse = new TaskResponse(message);
            taskResponse.setSucceeded(success);
            
            if (returnedObject != null)
            {
                taskResponse.setReturnedObject(returnedObject);
            }

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
                    successHandler.run();
                } else
                {
                    cancelledHandler.run();
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
                steno.error("Failure running task: " + ex);
                failureHandler.run();
            }
        };
        Thread taskThread = new Thread(runTask);
        // This is not strictly necessary if the cancelling logic is implemented correctly, but
        // just in case.
        taskThread.setDaemon(true);
        taskThread.setName(taskName);
        taskThread.start();
    }
}
