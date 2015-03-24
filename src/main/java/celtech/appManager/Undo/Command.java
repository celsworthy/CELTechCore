/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.Undo;

/**
 * A Command represents an atomic change that can be undone and redone. It can also be merged
 * with a previous Command of the same type, if desired.
 * @author tony
 */
public abstract class Command
{
    
    /**
     * Save all necessary information to be able to undo a command.
     */
    public abstract void saveState();
    
    /**
     * Perform the command.
     */
    public void do_() {
        saveState();
        redo();
    }
    
    /**
     * Undo the command.
     */
    public abstract void undo();    
    
    /**
     * Redo the command.
     */
    public abstract void redo();        
    
}
