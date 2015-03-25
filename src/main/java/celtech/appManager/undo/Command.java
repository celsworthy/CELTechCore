/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

/**
 * A Command represents an atomic change that can be undone and redone. It can also be merged
 * with a previous Command of the same type, if desired.
 * @author tony
 */
public abstract class Command
{
    
    /**
     * Perform the command.
     */
    public abstract void do_();
    
    /**
     * Undo the command.
     */
    public abstract void undo();    
    
    /**
     * Redo the command.
     */
    public abstract void redo();        
    
}
