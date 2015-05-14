/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.utils.tasks.Cancellable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * TimeCostThreadManager makes sure that all tasks are properly cancelled.
 *
 * @author tony
 */
public class TimeCostThreadManager
{

    private final ExecutorService executorService;
    private Future timeCostFuture;
    private Cancellable cancellable;
    
    public TimeCostThreadManager()
    {
        ThreadFactory threadFactory = (Runnable runnable) ->
        {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        };
        executorService = Executors.newFixedThreadPool(1, threadFactory);
    }

    public void cancelRunningTimeCostTasks()
    {
        if (cancellable != null)
        {
            cancellable.cancelled().set(true);
            timeCostFuture.cancel(true);
        }
    }

    public void cancelRunningTimeCostTasksAndRun(Runnable runnable, Cancellable cancellable)
    {
        cancelRunningTimeCostTasks();
        this.cancellable = cancellable;
        timeCostFuture = executorService.submit(() ->
        {
            runnable.run();
        });
    }

}
