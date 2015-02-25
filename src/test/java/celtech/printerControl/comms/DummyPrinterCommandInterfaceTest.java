/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.comms;

import celtech.JavaFXConfiguredTest;
import celtech.configuration.HeaterMode;
import celtech.printerControl.model.HardwarePrinter;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author tony
 */
public class DummyPrinterCommandInterfaceTest extends JavaFXConfiguredTest
{

    @ClassRule
    public static TemporaryFolder temporaryUserStorageFolder = new TemporaryFolder();

    @Test
    public void testSetNozzleTargetTemperature() throws Exception
    {
        StatusConsumer statusConsumer = new StatusConsumer();
        DummyPrinterCommandInterface commandInterface = new DummyPrinterCommandInterface(
            statusConsumer, "Test Printer", false, 500);
        HardwarePrinter hardwarePrinter = new HardwarePrinter(statusConsumer, commandInterface);
        commandInterface.setPrinter(hardwarePrinter);
        
        hardwarePrinter.sendRawGCode("ATTACH HEAD RBX01-DM", true);
        
        hardwarePrinter.setNozzleTargetTemperature(200);

        assertEquals(200, commandInterface.nozzleTargetTemperature);
        
//        NozzleHeater nozzleHeater = hardwarePrinter.headProperty().get().getNozzleHeaters().get(0);
//        assertEquals(200, nozzleHeater.nozzleTargetTemperatureProperty().get());

    }
    
    @Test
    public void testGotoTargetNozzleTemperature() throws Exception
    {
        StatusConsumer statusConsumer = new StatusConsumer();
        DummyPrinterCommandInterface commandInterface = new DummyPrinterCommandInterface(
            statusConsumer, "Test Printer", false, 500);
        HardwarePrinter hardwarePrinter = new HardwarePrinter(statusConsumer, commandInterface);
        commandInterface.setPrinter(hardwarePrinter);
        hardwarePrinter.goToTargetNozzleTemperature();

        assertEquals(HeaterMode.NORMAL, commandInterface.nozzleHeaterMode);

    }  
    
    @Test
    public void testSwitchAllNozzleHeatersOff() throws Exception
    {
        StatusConsumer statusConsumer = new StatusConsumer();
        DummyPrinterCommandInterface commandInterface = new DummyPrinterCommandInterface(
            statusConsumer, "Test Printer", false, 500);
        HardwarePrinter hardwarePrinter = new HardwarePrinter(statusConsumer, commandInterface);
        commandInterface.setPrinter(hardwarePrinter);
        
        hardwarePrinter.goToTargetNozzleTemperature();
        assertEquals(HeaterMode.NORMAL, commandInterface.nozzleHeaterMode);
        hardwarePrinter.switchAllNozzleHeatersOff();
        assertEquals(HeaterMode.OFF, commandInterface.nozzleHeaterMode);

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
