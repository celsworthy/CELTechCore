package celtech.utils.tasks;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

/**
 *
 * @author Ian
 */
public class TestTaskExecutor implements TaskExecutor
{

    @Override
    public void respondOnGUIThread(TaskResponder responder, boolean success, String message)
    {
        TaskResponse taskResponse = new TaskResponse(message);
        taskResponse.setSucceeded(success);

        responder.taskEnded(taskResponse);
    }

    @Override
    public void respondOnCurrentThread(TaskResponder responder, boolean success, String message)
    {
        TaskResponse taskResponse = new TaskResponse(message);
        taskResponse.setSucceeded(success);

        responder.taskEnded(taskResponse);
    }

    @Override
    public void runOnGUIThread(Runnable runnable)
    {
        runnable.run();
    }

    @Override
    public void runAsTask(Callable<Boolean> action, EventHandler<WorkerStateEvent> successHandler,
        EventHandler<WorkerStateEvent> failureHandler, String taskName)
    {
        try
        {
            boolean success = action.call();
            if (success) {
                successHandler.handle(null);
            } else {
                failureHandler.handle(null);
            }
        } catch (Exception ex)
        {
            failureHandler.handle(null);
        }
    }
}
