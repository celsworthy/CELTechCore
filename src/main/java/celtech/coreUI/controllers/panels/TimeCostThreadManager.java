/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.services.slicer.SlicerTask;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.concurrent.Task;

/**
 * TimeCostThreadManager makes sure that all tasks are properly cancelled.
 *
 * @author tony
 */
public class TimeCostThreadManager
{

    ExecutorService executorService;

    public TimeCostThreadManager()
    {
        executorService = Executors.newFixedThreadPool(1);
    }

    private void cancelTask(Task task)
    {
        if (task != null && !(task.isDone() || task.isCancelled()))
        {
            task.cancel();
        }
    }

    public void cancelRunningTimeCostTasks()
    {
        System.out.println("cancel running tasks");
//        for (Task task : tasks)
//        {
//            cancelTask(task);
//        }
    }

    public void cancelRunningTimeCostTasksAndRun(Runnable runnable)
    {
       
        cancelRunningTimeCostTasks();
        runTask(runnable);
        executorService.submit(runnable);
    }

    void runTask(Runnable runnable)
    {
        System.out.println("start task " + runnable);
        executorService.submit(runnable);
    }
}
