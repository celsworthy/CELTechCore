package celtech.printerControl;

import celtech.printerControl.model.HardwarePrinter;
import celtech.JavaFXConfiguredTest;
import celtech.Lookup;
import celtech.appManager.TestSystemNotificationManager;
import celtech.printerControl.comms.TestCommandInterface;
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
    public void setUp()
    {
        Lookup.setTaskExecutor(new TestTaskExecutor());
        Lookup.setSystemNotificationHandler(new TestSystemNotificationManager());
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
        HardwarePrinter printer = new HardwarePrinter(null, new TestCommandInterface(null, "Test Printer", false, 500));
        
        assertTrue(printer.canRemoveHeadProperty().get());
    }

    @Test
    public void testCannotPrintWhenHeadIsRemoved()
    {
        TestCommandInterface testCommandInterface = new TestCommandInterface(null, "Test Printer", false, 500);
        testCommandInterface.noHead();
        
        HardwarePrinter printer = new HardwarePrinter(null, testCommandInterface);
        
        assertFalse(printer.canPrintProperty().get());
    }
}
