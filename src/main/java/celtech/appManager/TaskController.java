/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.appManager;

import java.util.ArrayList;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class TaskController
{

    private static Stenographer steno = StenographerFactory.getStenographer(TaskController.class.getName());
    private static TaskController instance = null;
    private static ArrayList<Task> taskList = new ArrayList<>();
    private static boolean isShuttingDown = false;

    private TaskController()
    {
    }

    /**
     *
     * @return
     */
    public static TaskController getInstance()
    {
        if (instance == null)
        {
            instance = new TaskController();
        }

        return instance;
    }

    /**
     *
     * @param task
     */
    public void manageTask(Task task)
    {
        taskList.add(task);
        task.stateProperty().addListener(new ChangeListener()
        {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue)
            {
                if (newValue instanceof Worker.State)
                {
                    switch ((Worker.State) newValue)
                    {
                        case CANCELLED:
                        case FAILED:
                        case SUCCEEDED:
                            removeTask(task);
                            break;
                    }
                }
            }
        });
    }

    /**
     *
     * @param task
     */
    public void removeTask(Task task)
    {
        taskList.remove(task);
    }

    /**
     *
     */
    public void shutdownAllManagedTasks()
    {
        isShuttingDown = true;
        ArrayList<Task> tasksToCancel = new ArrayList<Task>(taskList);

        for (Task task : tasksToCancel)
        {
            steno.info("Cancelling task " + task.getTitle());
            task.cancel();
        }
    }
    
    /**
     *
     * @return
     */
    public int getNumberOfManagedTasks()
    {
        return taskList.size();
    }
    
    /**
     *
     * @return
     */
    public static boolean isShuttingDown()
    {
        return isShuttingDown;
    }
}
