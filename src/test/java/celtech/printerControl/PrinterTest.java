/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.JavaFXConfiguredTest;
import celtech.printerControl.comms.TestCommandInterface;
import celtech.utils.tasks.TaskResponder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class PrinterTest extends JavaFXConfiguredTest
{

    public PrinterTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void testCanRemoveHead()
    {
        Printer printer = new PrinterImpl(null, null, null);

        assertTrue(printer.canRemoveHead());
    }

    @Test
    public void testCannotPrintDuringRemoveHead() throws PrinterException
    {
        TestCommandInterface commandInterface = new TestCommandInterface();
        Printer printer = new PrinterImpl(null, null, commandInterface);
        
        TaskResponder tr = new TaskResponder()
        {

            @Override
            public void taskEnded(TaskResult result)
            {
         
            }
        }
        printer.removeHead();
        commandInterface.tick(2);
        assertFalse(printer.canPrint());
    }

    @Test
    public void testCannotPrintWhenHeadIsRemoved()
    {
        TestCommandInterface commandInterface = new TestCommandInterface();
        Printer printer = new PrinterImpl(null, null, commandInterface);
        
        assertTrue(printer.removeHead());
        commandInterface.tick(4);
        assertFalse(printer.canPrint());
    }
}
