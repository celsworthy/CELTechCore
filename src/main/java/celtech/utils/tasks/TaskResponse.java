/*
 * Copyright 2014 CEL UK
 */
package celtech.utils.tasks;

/**
 *
 * @author tony
 */
public class TaskResponse
{

    private boolean succeeded = false;
    private final String message;

    public TaskResponse(String message)
    {
        this.message = message;
    }

    @Override
    public String toString()
    {
        return message;
    }
    
    /**
     *
     * @return
     */
    public boolean succeeded()
    {
        return succeeded;
    }
    
    /**
     *
     * @param succeeded
     */
    public void setSucceeded(boolean succeeded)
    {
        this.succeeded = succeeded;
    }
}
