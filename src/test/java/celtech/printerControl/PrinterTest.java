package celtech.printerControl;

import celtech.AutoMakerTestConfigurator;
import celtech.JavaFXConfiguredTest;
import celtech.printerControl.comms.PrinterStatusConsumer;
import celtech.printerControl.comms.TestCommandInterface;
import celtech.printerControl.model.HardwarePrinter;
import celtech.printerControl.model.PrinterException;
import javafx.scene.paint.Color;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Ian
 */
public class PrinterTest
{

    @ClassRule
    public static TemporaryFolder temporaryUserStorageFolder = new TemporaryFolder();

    private TestCommandInterface testCommandInterface = null;
    private HardwarePrinter printer = null;
    private final int statusTimer = 500;
    private StatusConsumer statusConsumer = null;

    public PrinterTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
        AutoMakerTestConfigurator.setUp(temporaryUserStorageFolder);
    }

    @Before
    public void setUp()
    {
        statusConsumer = new StatusConsumer();
        testCommandInterface = new TestCommandInterface(statusConsumer, "Test Printer", false, statusTimer);
        printer = new HardwarePrinter(null, testCommandInterface);
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

    @Test
    public void testDefaultPrinterColour()
    {
        assertNull(printer.getPrinterIdentity().printerColourProperty().get());
    }

    @Test
    public void testUpdatePrinterColour()
    {
        Color colourToWrite = Color.CHARTREUSE;

        try
        {
            printer.updatePrinterDisplayColour(colourToWrite);
        } catch (PrinterException ex)
        {
            fail("Exception during update printer colour test - " + ex.getMessage());
        }

        assertEquals(colourToWrite, printer.getPrinterIdentity().printerColourProperty().get());
    }

    @Test
    public void testDefaultPrinterName()
    {
        assertEquals("", printer.getPrinterIdentity().printerFriendlyNameProperty().get());
    }

    @Test
    public void testUpdatePrinterName()
    {
        String testName = "Fred";
        
        try
        {
            printer.updatePrinterName(testName);
        } catch (PrinterException ex)
        {
            fail("Exception during update printer name test - " + ex.getMessage());
        }

        assertEquals(testName, printer.getPrinterIdentity().printerFriendlyNameProperty().get());
    }

    class StatusConsumer implements PrinterStatusConsumer
    {

        @Override
        public void printerConnected(String portName)
        {
        }

        @Override
        public void failedToConnect(String portName)
        {
        }

        @Override
        public void disconnected(String portName)
        {
        }
    }
}
