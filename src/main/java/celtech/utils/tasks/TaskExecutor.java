package celtech.utils.tasks;

/**
 *
 * @author Ian
 */
public interface TaskExecutor
{
    public void respondOnGUIThread(TaskResponder responder, boolean success, String message);
    public void respondOnCurrentThread(TaskResponder responder, boolean success, String message);
    public void runOnGUIThread(Runnable runnable);
}
