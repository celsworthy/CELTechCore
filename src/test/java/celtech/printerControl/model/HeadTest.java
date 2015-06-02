package celtech.printerControl.model;

import celtech.JavaFXConfiguredTest;
import celtech.printerControl.comms.DummyPrinterCommandInterface;
import celtech.printerControl.comms.PrinterStatusConsumer;
import celtech.printerControl.comms.TestCommandInterface;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.utils.TestHead;
import celtech.utils.TestPrinter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class HeadTest extends JavaFXConfiguredTest
{
    
    @Test
    public void testWriteAndReadLastFilamentTempFor2Heaters() throws RoboxCommsException {
        PrinterStatusConsumer printerStatusConsumer = new PrinterStatusConsumer()
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
        };
        
        DummyPrinterCommandInterface commandInterface =
            new DummyPrinterCommandInterface(printerStatusConsumer, "Test Printer", false, 500);
        
        Printer printer = new HardwarePrinter(null, commandInterface);
        
        printer.sendRawGCode("DEFAULS", false);
        printer.readHeadEEPROM();
        System.out.println("head is " + printer.headProperty().get());
        int numNozzleHeaters = printer.headProperty().get().getNozzleHeaters().size();
        System.out.println("num heaters: " + numNozzleHeaters);
        
        HeadEEPROMDataResponse headData = printer.readHeadEEPROM();
        
        float NOZZLE_TEMP_0 = 101f;
        float NOZZLE_TEMP_1 = 102f;
        
        printer.transmitWriteHeadEEPROM(
            headData.getTypeCode(),
            headData.getUniqueID(),
            headData.getMaximumTemperature(),
            headData.getBeta(),
            headData.getTCal(),
            headData.getNozzle1XOffset(),
            headData.getNozzle1YOffset(),
            headData.getNozzle1ZOffset(),
            headData.getNozzle1BOffset(),
            headData.getFilamentID(0),
            headData.getFilamentID(1),
            headData.getNozzle2XOffset(),
            headData.getNozzle2YOffset(),
            headData.getNozzle2ZOffset(),
            headData.getNozzle2BOffset(),
            NOZZLE_TEMP_0,
            NOZZLE_TEMP_1,
            headData.getHeadHours());
        
        NozzleHeater nozzleHeater0 = printer.headProperty().get().getNozzleHeaters().get(0);
        NozzleHeater nozzleHeater1 = printer.headProperty().get().getNozzleHeaters().get(1);
        
        assertEquals(NOZZLE_TEMP_0, nozzleHeater0.lastFilamentTemperature.get(), 0.001);
        assertEquals(NOZZLE_TEMP_1, nozzleHeater1.lastFilamentTemperature.get(), 0.001);
    }

    /**
     * Test of isTypeCodeValid method, of class Head.
     */
    @Test
    public void testIsTypeCodeValid()
    {
        String typeCode = "RBX01-SM";
        boolean expResult = true;
        boolean result = Head.isTypeCodeValid(typeCode);
        assertEquals(expResult, result);
    }

    /**
     * Test of isTypeCodeValid method, of class Head.
     */
    @Test
    public void testIsTypeCodeValidFail()
    {
        String typeCode = "RBXa1-SM";
        boolean expResult = false;
        boolean result = Head.isTypeCodeValid(typeCode);
        assertEquals(expResult, result);
    }

    /**
     * Test of isTypeCodeInDatabase method, of class Head.
     */
    @Test
    public void testIsTypeCodeInDatabasePass()
    {
        String typeCode = "RBX01-SM";
        boolean expResult = true;
        boolean result = Head.isTypeCodeInDatabase(typeCode);
        assertEquals(expResult, result);
    }

    /**
     * Test of isTypeCodeInDatabase method, of class Head.
     */
    @Test
    public void testIsTypeCodeInDatabaseFail()
    {
        String typeCode = "blah";
        boolean expResult = false;
        boolean result = Head.isTypeCodeInDatabase(typeCode);
        assertEquals(expResult, result);
    }
}
