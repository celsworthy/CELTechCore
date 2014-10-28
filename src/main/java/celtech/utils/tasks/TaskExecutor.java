package celtech.utils.tasks;

import java.util.concurrent.Callable;

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
     * Run the given action in a JavaFX task, using the given success,  failure and
     * cancelled handlers. The cancelled handler is called if the action returns false,
     * the failure handler is called if the action raises an exception.
     */
    public void runAsTask(Callable<Boolean> action, Runnable successHandler,
        Runnable failureHandler, Runnable cancelledHandler, String taskName);
}
