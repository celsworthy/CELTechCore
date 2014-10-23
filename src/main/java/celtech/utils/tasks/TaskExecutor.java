package celtech.utils.tasks;

import java.util.concurrent.Callable;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

/**
 *
 * @author Ian
 */
public interface TaskExecutor
{
    public void respondOnGUIThread(TaskResponder responder, boolean success, String message);
    public void respondOnCurrentThread(TaskResponder responder, boolean success, String message);
    public void runOnGUIThread(Runnable runnable);

    /**
     * Run the given action in a JavaFX task, using the given success and failure handlers.
     */
    public void runAsTask(Callable<Boolean> action, EventHandler<WorkerStateEvent> successHandler,
        EventHandler<WorkerStateEvent> failureHandler, String taskName);
}
