/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.Printer;
import celtech.JavaFXConfiguredTest;
import celtech.Lookup;
import celtech.printerControl.comms.TestCommandInterface;
import celtech.utils.tasks.TaskResponse;
import celtech.utils.tasks.TestTaskExecutor;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Ian
 */
public class PrinterTest extends JavaFXConfiguredTest
{

    public PrinterTest()
    {
    }

    @Before
    @Override
    public void setUp()
    {
        super.setUp();
        Lookup.setTaskExecutor(new TestTaskExecutor());
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

        printer.removeHead((TaskResponse taskResponse) ->
        {
        });

        commandInterface.tick(2);
        assertFalse(printer.canPrint());
    }

    @Test
    public void testCannotPrintWhenHeadIsRemoved()
    {
        TestCommandInterface commandInterface = new TestCommandInterface();
        Printer printer = new PrinterImpl(null, null, commandInterface);

//        assertTrue(printer.removeHead());
        commandInterface.tick(4);
        assertFalse(printer.canPrint());
    }
}
