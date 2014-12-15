/*
 * Copyright 2014 CEL UK
 */
package celtech.utils.tasks;

/**
 *
 * @author tony
 * @param <T>
 */
public interface TaskResponder<T>
{
    public void taskEnded(TaskResponse<T> taskResponse);
}
