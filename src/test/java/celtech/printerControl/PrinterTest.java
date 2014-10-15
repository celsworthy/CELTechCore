package celtech.printerControl;

import celtech.JavaFXConfiguredTest;
import celtech.Lookup;
import celtech.appManager.TestSystemNotificationManager;
import celtech.printerControl.comms.TestCommandInterface;
import celtech.printerControl.model.HardwarePrinter;
import celtech.utils.tasks.TestTaskExecutor;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Ian
 */
public class PrinterTest extends JavaFXConfiguredTest
{

    private static TestCommandInterface testCommandInterface = null;
    private static HardwarePrinter printer = null;

    public PrinterTest()
    {
    }

    @Before
    @Override
    public void setUp()
    {
        super.setUp();
        Lookup.setTaskExecutor(new TestTaskExecutor());
        Lookup.setSystemNotificationHandler(new TestSystemNotificationManager());
        testCommandInterface = new TestCommandInterface(null, "Test Printer", false, 500);
        printer = new HardwarePrinter(null, testCommandInterface);
        testCommandInterface.preTestInitialisation();
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
        testCommandInterface.shutdown();
    }

    @Test
    public void testCanRemoveHead()
    {
        assertTrue(printer.canRemoveHeadProperty().get());
    }

    @Test
    public void testCannotPrintWhenHeadIsRemoved()
    {
        testCommandInterface.noHead();

        assertFalse(printer.canPrintProperty().get());
    }
//
//    @Test
//    public void testDefaultPrinterColour()
//    {
//        assertTrue(printer.getPrinterIdentity().printerColourProperty().get() == null);
//    }

//    @Test
//    public void testUpdatePrinterColour()
//    {
//        try
//        {
//            printer.updatePrinterDisplayColour(Color.ALICEBLUE);
//        } catch (PrinterException ex)
//        {
//            fail("Exception during update printer colour test - " + ex.getMessage());
//        }
//
//        assert ()
//    }
}
