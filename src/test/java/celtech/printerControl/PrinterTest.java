package celtech.printerControl;

import celtech.AutoMakerTestConfigurator;
import celtech.printerControl.comms.PrinterStatusConsumer;
import celtech.printerControl.comms.TestCommandInterface;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacketFactory;
import celtech.printerControl.comms.commands.rx.RxPacketTypeEnum;
import celtech.printerControl.model.HardwarePrinter;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Nozzle;
import celtech.printerControl.model.NozzleHeater;
import celtech.printerControl.model.PrinterException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
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
        testCommandInterface = new TestCommandInterface(statusConsumer, "Test Printer", false,
                                                        statusTimer);
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
    public void testUpdateHead() throws RoboxCommsException
    {
        HeadEEPROMDataResponse response = (HeadEEPROMDataResponse) RoboxRxPacketFactory.createPacket(
            RxPacketTypeEnum.HEAD_EEPROM_DATA);
        response.setHeadTypeCode("RBX01-SM");
        response.setUniqueID("XYZ1");
        testCommandInterface.addHead(response);

        float nozzle1XOffset = 6f;
        printer.transmitWriteHeadEEPROM(
            "RBX01-SM", "XZYA",
            250f, 5f, 6f,
            nozzle1XOffset, 7f, 0.2f, 1.1f,
            100f, 7f, 0.2f, 1f,
            210f, 45f);

        assertEquals(nozzle1XOffset, printer.headProperty().get().getNozzles().get(0).xOffsetProperty().get(), 0.001);
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
