package celtech.utils.tasks;

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
        Thread newThread = new Thread(runnable);
        newThread.start();
    }
}
