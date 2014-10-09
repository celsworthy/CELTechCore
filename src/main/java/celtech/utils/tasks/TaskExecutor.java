package celtech.utils.tasks;

/**
 *
 * @author Ian
 */
public interface TaskExecutor
{
    public void runOnGUIThread(TaskResponder responder, boolean success, String message);
    public void runOnCurrentThread(TaskResponder responder, boolean success, String message);
}
