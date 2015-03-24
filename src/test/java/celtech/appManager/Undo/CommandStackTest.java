/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.Undo;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tony
 */
public class CommandStackTest
{
    public class TestModel {
        public int i = 0;
    }
    
    public class IncrementCommand extends Command {

        private TestModel testModel;
        private final int increment;
        private int oldI;

        public IncrementCommand(TestModel testModel, int increment)
        {
            this.testModel = testModel;
            this.increment = increment;
        }
        
        @Override
        public void do_()
        {
            oldI = testModel.i;
            redo();
        }

        @Override
        public void undo()
        {
            testModel.i = oldI;
        }

        @Override
        public void redo()
        {
            testModel.i += increment;
        }
        
    }
    

    @Test
    public void testDo_()
    {
        TestModel testModel = new TestModel();
        testModel.i = 5;
        
        CommandStack commandStack = new CommandStack();
        IncrementCommand incrementCommand  = new IncrementCommand(testModel, 6);
        commandStack.do_(incrementCommand);
        
        assertEquals(5 + 6, testModel.i);
        
    }
    
    @Test
    public void testUndo() throws CommandStack.UndoException
    {
        TestModel testModel = new TestModel();
        testModel.i = 5;
        
        CommandStack commandStack = new CommandStack();
        IncrementCommand incrementCommand  = new IncrementCommand(testModel, 6);
        commandStack.do_(incrementCommand);
        assertEquals(5 + 6, testModel.i);
        
        commandStack.undo();
        assertEquals(5, testModel.i);
        
    }    
    
    @Test
    public void testRedo() throws CommandStack.UndoException
    {
        TestModel testModel = new TestModel();
        testModel.i = 5;
        
        CommandStack commandStack = new CommandStack();
        IncrementCommand incrementCommand  = new IncrementCommand(testModel, 6);
        commandStack.do_(incrementCommand);
        assertEquals(5 + 6, testModel.i);
        
        commandStack.undo();
        assertEquals(5, testModel.i);
        
        commandStack.redo();
        assertEquals(5 + 6, testModel.i);
        
    }   
    
    @Test
    public void testCanRedo() throws CommandStack.UndoException
    {
        TestModel testModel = new TestModel();
        testModel.i = 5;
        
        CommandStack commandStack = new CommandStack();
        IncrementCommand incrementCommand  = new IncrementCommand(testModel, 6);
        commandStack.do_(incrementCommand);
        assertEquals(5 + 6, testModel.i);
        
        commandStack.undo();
        assertEquals(5, testModel.i);
        assertTrue(commandStack.getCanRedo().get());
        
        commandStack.redo();
        assertEquals(5 + 6, testModel.i);
        assertFalse(commandStack.getCanRedo().get());
    }     
    
    @Test
    public void testCanundo() throws CommandStack.UndoException
    {
        TestModel testModel = new TestModel();
        testModel.i = 5;
        
        CommandStack commandStack = new CommandStack();
        IncrementCommand incrementCommand  = new IncrementCommand(testModel, 6);
        commandStack.do_(incrementCommand);
        assertEquals(5 + 6, testModel.i);
        assertTrue(commandStack.getCanUndo().get());
        
        commandStack.undo();
        assertEquals(5, testModel.i);
        assertFalse(commandStack.getCanUndo().get());
        
        commandStack.redo();
        assertEquals(5 + 6, testModel.i);
        assertTrue(commandStack.getCanUndo().get());
    }          
    
}
