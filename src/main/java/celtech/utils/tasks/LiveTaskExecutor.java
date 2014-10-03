package celtech.utils.tasks;

import javafx.application.Platform;

/**
 *
 * @author Ian
 */
public class LiveTaskExecutor implements TaskExecutor
{
    @Override
    public void runOnGUIThread(TaskResponder responder, boolean success, String message)
    {
        TaskResponse taskResponse = new TaskResponse(message);
        taskResponse.setSucceeded(success);
        
        Platform.runLater(() ->
        {
            responder.taskEnded(taskResponse);
        });
    }
}
