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

    private DeviceDetector.DetectedPrinter printerHandle = new DeviceDetector.DetectedPrinter(DeviceDetector.PrinterConnectionType.SERIAL, "Test Printer");

    @ClassRule
    public static TemporaryFolder temporaryUserStorageFolder = new TemporaryFolder();

    @Test
    public void testSetNozzleTargetTemperature() throws Exception
    {
        StatusConsumer statusConsumer = new StatusConsumer();
        DummyPrinterCommandInterface commandInterface = new DummyPrinterCommandInterface(
                statusConsumer, printerHandle, false, 500);
        HardwarePrinter hardwarePrinter = new HardwarePrinter(statusConsumer, commandInterface);
        commandInterface.setPrinter(hardwarePrinter);

        hardwarePrinter.sendRawGCode("ATTACH HEAD RBX01-DM", true);

        hardwarePrinter.setNozzleHeaterTargetTemperature(0, 200);

        assertEquals(210, commandInterface.nozzleTargetTemperatureS);

//        NozzleHeater nozzleHeater = hardwarePrinter.headProperty().get().getNozzleHeaters().get(0);
//        assertEquals(200, nozzleHeater.nozzleTargetTemperatureProperty().get());
    }

    //@Test DISABLED23/09/15
    public void testGotoTargetNozzleTemperature() throws Exception
    {
        StatusConsumer statusConsumer = new StatusConsumer();
        DummyPrinterCommandInterface commandInterface = new DummyPrinterCommandInterface(
                statusConsumer, printerHandle, false, 500);
        HardwarePrinter hardwarePrinter = new HardwarePrinter(statusConsumer, commandInterface);
        commandInterface.setPrinter(hardwarePrinter);
        hardwarePrinter.goToTargetNozzleHeaterTemperature(0);

        assertEquals(HeaterMode.NORMAL, commandInterface.nozzleHeaterModeS);

    }

    //@Test DISABLED23/09/15
    public void testSwitchAllNozzleHeatersOff() throws Exception
    {
        StatusConsumer statusConsumer = new StatusConsumer();
        DummyPrinterCommandInterface commandInterface = new DummyPrinterCommandInterface(
                statusConsumer, printerHandle, false, 500);
        HardwarePrinter hardwarePrinter = new HardwarePrinter(statusConsumer, commandInterface);
        commandInterface.setPrinter(hardwarePrinter);

        hardwarePrinter.goToTargetNozzleHeaterTemperature(0);
        assertEquals(HeaterMode.NORMAL, commandInterface.nozzleHeaterModeS);
        hardwarePrinter.switchAllNozzleHeatersOff();
        assertEquals(HeaterMode.OFF, commandInterface.nozzleHeaterModeS);

    }

    class StatusConsumer implements PrinterStatusConsumer
    {

        @Override
        public void printerConnected(DeviceDetector.DetectedPrinter printerHandle)
        {
        }

        @Override
        public void failedToConnect(DeviceDetector.DetectedPrinter printerHandle)
        {
        }

        @Override
        public void disconnected(DeviceDetector.DetectedPrinter printerHandle)
        {
        }
    }

}
