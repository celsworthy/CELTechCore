package celtech.utils.tasks;

import java.util.concurrent.Callable;

/**
 *
 * @author Ian
 */
public interface TaskExecutor
{
    
    public interface NoArgsConsumer {
        void run() throws Exception;
    }
    
    public void respondOnGUIThread(TaskResponder responder, boolean success, String message);
    public void respondOnGUIThread(TaskResponder responder, boolean success, String message, Object returnedObject);
    public void respondOnCurrentThread(TaskResponder responder, boolean success, String message);
    public void runOnGUIThread(Runnable runnable);

    /**
     * Run the given action in a JavaFX task, using the given success,  failure and
     * cancelled handlers. The cancelled handler is called if the action returns false,
     * the failure handler is called if the action raises an exception.
     */
    public void runAsTask(NoArgsConsumer action, NoArgsConsumer successHandler,
        NoArgsConsumer failureHandler, String taskName);
}
