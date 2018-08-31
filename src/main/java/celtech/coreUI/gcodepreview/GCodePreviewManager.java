/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.gcodepreview;

import celtech.roboxbase.utils.tasks.Cancellable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * TimeCostThreadManager makes sure that all tasks are properly cancelled.
 *
 * @author tony
 */
public class GCodePreviewManager
{

    private final ExecutorService executorService;
    private Future previewFuture;
    private Cancellable cancellable;
    private static GCodePreviewManager instance;
    
    private GCodePreviewManager()
    {
        ThreadFactory threadFactory = (Runnable runnable) ->
        {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        };
        executorService = Executors.newFixedThreadPool(1, threadFactory);
    }
    
    public static GCodePreviewManager getInstance()
    {
        if (instance == null)
        {
            instance = new GCodePreviewManager();
        }
        
        return instance;
    }

    public void cancelRunningTasks()
    {
        if (cancellable != null)
        {
            cancellable.cancelled().set(true);
            previewFuture.cancel(true);
            cancellable = null;
        }
    }

    public void cancelRunningAndRun(Runnable runnable, Cancellable cancellable)
    {
        cancelRunningTasks();
        this.cancellable = cancellable;
        previewFuture = executorService.submit(() ->
        {
            try {
            runnable.run();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

}
