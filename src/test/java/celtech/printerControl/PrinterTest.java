package celtech.printerControl;

import celtech.JavaFXConfiguredTest;
import celtech.printerControl.comms.PrinterStatusConsumer;
import celtech.printerControl.comms.TestCommandInterface;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacketFactory;
import celtech.printerControl.comms.commands.rx.RxPacketTypeEnum;
import celtech.printerControl.model.HardwarePrinter;
import celtech.printerControl.model.PrinterException;
import javafx.scene.paint.Color;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author Ian
 */
public class PrinterTest extends JavaFXConfiguredTest implements PrinterStatusConsumer
{

    @ClassRule
    public static TemporaryFolder temporaryUserStorageFolder = new TemporaryFolder();

    private TestCommandInterface testCommandInterface = null;
    private HardwarePrinter printer = null;
    private final int statusTimer = 500;
    private StatusConsumer statusConsumer = null;
    private boolean printerIsConnected = false;

    public PrinterTest()
    {
    }

    @Before
    public void setUpConsumer()
    {
        statusConsumer = new StatusConsumer();
        testCommandInterface = new TestCommandInterface(statusConsumer, "Test Printer", false,
                statusTimer);
        printer = new HardwarePrinter(null, testCommandInterface);

        long millisecondsWaited = 0;
        final long millisecondsDelay = 500;
        final long maxDelay = 2000;

        while (!printerIsConnected
                && millisecondsWaited <= maxDelay)
        {
            try
            {
                Thread.sleep(millisecondsDelay);
            } catch (InterruptedException ex)
            {
                System.err.println("Interrupted whilst waiting for printer initialisation");
                ex.printStackTrace();
            }
            millisecondsWaited += millisecondsDelay;
        }
        
        if (millisecondsWaited >= maxDelay)
        {
            fail("Failed to connect to test printer");
        }
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
        assertEquals(Color.CRIMSON, printer.getPrinterIdentity().printerColourProperty().get());
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
    public void testUpdateHead() throws RoboxCommsException
    {
        HeadEEPROMDataResponse response = (HeadEEPROMDataResponse) RoboxRxPacketFactory.createPacket(
                RxPacketTypeEnum.HEAD_EEPROM_DATA);
        response.setHeadTypeCode("RBX01-SM");
        response.setUniqueID("XYZ1");
        testCommandInterface.addHead(response);

        float nozzle1XOffset = 7.4f;
        printer.transmitWriteHeadEEPROM(
                "RBX01-SM", "XZYA",
                250f, 5f, 6f,
                nozzle1XOffset, 7f, 0.2f, 1.1f,
                100f, 7f, 0.2f, 1f,
                210f, 210f, 45f);

        assertEquals(nozzle1XOffset, printer.headProperty().get().getNozzles().get(0).xOffsetProperty().get(), 0.001);
    }

    @Test
    public void testDefaultPrinterName()
    {
        assertEquals("Dummy", printer.getPrinterIdentity().printerFriendlyNameProperty().get());
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

    @Override
    public void printerConnected(String portName)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void failedToConnect(String portName)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void disconnected(String portName)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    class StatusConsumer implements PrinterStatusConsumer
    {

        @Override
        public void printerConnected(String portName)
        {
            printerIsConnected = true;
        }

        @Override
        public void failedToConnect(String portName)
        {
        }

        @Override
        public void disconnected(String portName)
        {
            printerIsConnected = false;
        }
    }
}
