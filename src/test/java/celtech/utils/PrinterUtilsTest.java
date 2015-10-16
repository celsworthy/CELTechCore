package celtech.utils;

import celtech.JavaFXConfiguredTest;
import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.NozzleHeaterData;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.model.Head;
import celtech.utils.TestHead.TestNozzleHeater;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class PrinterUtilsTest extends JavaFXConfiguredTest
{

    /**
     * Test of printJobIDIndicatesPrinting method, of class PrinterUtils.
     */
    @Test
    public void testPrintJobIDIndicatesPrinting()
    {
        System.out.println("printJobIDIndicatesPrinting");

        String printJobID = "";
        boolean result = PrinterUtils.printJobIDIndicatesPrinting(printJobID);
        assertEquals(false, result);

        printJobID = null;
        result = PrinterUtils.printJobIDIndicatesPrinting(printJobID);
        assertEquals(false, result);

        printJobID = "5793b413812d443a";
        result = PrinterUtils.printJobIDIndicatesPrinting(printJobID);
        assertEquals(true, result);
    }

    @Test
    public void testRequiresPurgeForNozzleHeater0()
    {
        int NOZZLE_TEMP = 120;
        Project project = new Project();
        Filament filament = Lookup.getFilamentContainer().getFilamentByID("RBX-ABS-PP156");
        filament.getNozzleTemperatureProperty().set(NOZZLE_TEMP);
        project.getPrinterSettings().setFilament0(filament);

        TestPrinter printer = new TestPrinter(1);
        HeadFile headFile = new HeadFile();
        headFile.setTypeCode("RBX01-SM");
        NozzleHeaterData nozzleHeaterData = new NozzleHeaterData();
        headFile.getNozzleHeaters().add(nozzleHeaterData);
        printer.addHeadForHeadFile(headFile);
        TestNozzleHeater testNozzleHeater = (TestNozzleHeater) printer.getHead().getNozzleHeaters().get(
                0);
        testNozzleHeater.lastFilamentTemperatureProperty().set(NOZZLE_TEMP
                - ApplicationConfiguration.maxPermittedTempDifferenceForPurge + 1);

        ModelContainer testModel = new ModelContainer();
        testModel.setUseExtruder0(true);
        project.addModel(testModel);

        boolean purgeIsNecessary = PrinterUtils.getInstance().isPurgeNecessary(printer, project);
        assertFalse(purgeIsNecessary);

        testNozzleHeater.lastFilamentTemperatureProperty().set(NOZZLE_TEMP
                - ApplicationConfiguration.maxPermittedTempDifferenceForPurge - 1);
        purgeIsNecessary = PrinterUtils.getInstance().isPurgeNecessary(printer, project);
        assertTrue(purgeIsNecessary);
    }

    @Test
    public void testRequiresPurgeForNozzleHeater1()
    {
        int NOZZLE_TEMP_0 = 120;
        int NOZZLE_TEMP_1 = 220;
        Project project = new Project();
        Filament filament0 = Lookup.getFilamentContainer().getFilamentByID("RBX-ABS-PP156");
        filament0.getNozzleTemperatureProperty().set(NOZZLE_TEMP_1);
        Filament filament1 = Lookup.getFilamentContainer().getFilamentByID("RBX-ABS-GR499");
        filament1.getNozzleTemperatureProperty().set(NOZZLE_TEMP_0);
        project.getPrinterSettings().setFilament0(filament0);
        project.getPrinterSettings().setFilament1(filament1);

        ModelContainer testModel = new ModelContainer();
        testModel.setUseExtruder0(true);
        project.addModel(testModel);

        TestPrinter printer = new TestPrinter(2);
        HeadFile headFile = new HeadFile();
        headFile.setTypeCode("RBX01-DM");
        headFile.setType(Head.HeadType.DUAL_MATERIAL_HEAD);
        
        NozzleHeaterData nozzleHeaterData0 = new NozzleHeaterData();
        headFile.getNozzleHeaters().add(nozzleHeaterData0);
        NozzleHeaterData nozzleHeaterData1 = new NozzleHeaterData();
        headFile.getNozzleHeaters().add(nozzleHeaterData1);
        printer.addHeadForHeadFile(headFile);
        
        TestNozzleHeater testNozzleHeater0 = (TestNozzleHeater) printer.getHead().getNozzleHeaters().get(
                0);
        testNozzleHeater0.lastFilamentTemperatureProperty().set(NOZZLE_TEMP_0
                - ApplicationConfiguration.maxPermittedTempDifferenceForPurge + 1);
        TestNozzleHeater testNozzleHeater1 = (TestNozzleHeater) printer.getHead().getNozzleHeaters().get(
                1);
        testNozzleHeater1.lastFilamentTemperatureProperty().set(NOZZLE_TEMP_1
                - ApplicationConfiguration.maxPermittedTempDifferenceForPurge + 1);

        boolean purgeIsNecessary = PrinterUtils.getInstance().isPurgeNecessary(printer, project);
        assertFalse(purgeIsNecessary);

        testNozzleHeater1.lastFilamentTemperatureProperty().set(NOZZLE_TEMP_1
                - ApplicationConfiguration.maxPermittedTempDifferenceForPurge - 1);
        purgeIsNecessary = PrinterUtils.getInstance().isPurgeNecessary(printer, project);
        assertTrue(purgeIsNecessary);
    }

//    /**
//     * Test of waitOnMacroFinished method, of class PrinterUtils.
//     */
//    @Test
//    public void testWaitOnMacroFinished_Printer_Task()
//    {
//        System.out.println("waitOnMacroFinished");
//        Printer printerToCheck = null;
//        Task task = null;
//        boolean expResult = false;
//        boolean result = PrinterUtils.waitOnMacroFinished(printerToCheck, task);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//    /**
//     * Test of waitOnMacroFinished method, of class PrinterUtils.
//     */
//    @Test
//    public void testWaitOnMacroFinished_Printer_Cancellable()
//    {
//        System.out.println("waitOnMacroFinished");
//        Printer printerToCheck = null;
//        Cancellable cancellable = null;
//        boolean expResult = false;
//        boolean result = PrinterUtils.waitOnMacroFinished(printerToCheck, cancellable);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of waitOnBusy method, of class PrinterUtils.
//     */
//    @Test
//    public void testWaitOnBusy_Printer_Task()
//    {
//        System.out.println("waitOnBusy");
//        Printer printerToCheck = null;
//        Task task = null;
//        boolean expResult = false;
//        boolean result = PrinterUtils.waitOnBusy(printerToCheck, task);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of waitOnBusy method, of class PrinterUtils.
//     */
//    @Test
//    public void testWaitOnBusy_Printer_Cancellable()
//    {
//        System.out.println("waitOnBusy");
//        Printer printerToCheck = null;
//        Cancellable cancellable = null;
//        boolean expResult = false;
//        boolean result = PrinterUtils.waitOnBusy(printerToCheck, cancellable);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of isPurgeNecessary method, of class PrinterUtils.
//     */
//    @Test
//    public void testIsPurgeNecessary()
//    {
//        System.out.println("isPurgeNecessary");
//        Printer printer = null;
//        PrinterUtils instance = null;
//        boolean expResult = false;
//        boolean result = instance.isPurgeNecessary(printer);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of offerPurgeIfNecessary method, of class PrinterUtils.
//     */
//    @Test
//    public void testOfferPurgeIfNecessary()
//    {
//        System.out.println("offerPurgeIfNecessary");
//        Printer printer = null;
//        PrinterUtils instance = null;
//        PurgeResponse expResult = null;
//        PurgeResponse result = instance.offerPurgeIfNecessary(printer);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of waitUntilTemperatureIsReached method, of class PrinterUtils.
//     */
//    @Test
//    public void testWaitUntilTemperatureIsReached_5args() throws Exception
//    {
//        System.out.println("waitUntilTemperatureIsReached");
//        ReadOnlyIntegerProperty temperatureProperty = null;
//        Task task = null;
//        int temperature = 0;
//        int tolerance = 0;
//        int timeoutSec = 0;
//        boolean expResult = false;
//        boolean result = PrinterUtils.waitUntilTemperatureIsReached(temperatureProperty, task,
//                                                                    temperature, tolerance,
//                                                                    timeoutSec);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of waitUntilTemperatureIsReached method, of class PrinterUtils.
//     */
//    @Test
//    public void testWaitUntilTemperatureIsReached_6args() throws Exception
//    {
//        System.out.println("waitUntilTemperatureIsReached");
//        ReadOnlyIntegerProperty temperatureProperty = null;
//        Task task = null;
//        int temperature = 0;
//        int tolerance = 0;
//        int timeoutSec = 0;
//        Cancellable cancellable = null;
//        boolean expResult = false;
//        boolean result = PrinterUtils.waitUntilTemperatureIsReached(temperatureProperty, task,
//                                                                    temperature, tolerance,
//                                                                    timeoutSec, cancellable);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of deriveNozzle1OverrunFromOffsets method, of class PrinterUtils.
//     */
//    @Test
//    public void testDeriveNozzle1OverrunFromOffsets()
//    {
//        System.out.println("deriveNozzle1OverrunFromOffsets");
//        float nozzle1Offset = 0.0F;
//        float nozzle2Offset = 0.0F;
//        float expResult = 0.0F;
//        float result = PrinterUtils.deriveNozzle1OverrunFromOffsets(nozzle1Offset, nozzle2Offset);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of deriveNozzle2OverrunFromOffsets method, of class PrinterUtils.
//     */
//    @Test
//    public void testDeriveNozzle2OverrunFromOffsets()
//    {
//        System.out.println("deriveNozzle2OverrunFromOffsets");
//        float nozzle1Offset = 0.0F;
//        float nozzle2Offset = 0.0F;
//        float expResult = 0.0F;
//        float result = PrinterUtils.deriveNozzle2OverrunFromOffsets(nozzle1Offset, nozzle2Offset);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of deriveNozzle1ZOffsetsFromOverrun method, of class PrinterUtils.
//     */
//    @Test
//    public void testDeriveNozzle1ZOffsetsFromOverrun()
//    {
//        System.out.println("deriveNozzle1ZOffsetsFromOverrun");
//        float nozzle1OverrunValue = 0.0F;
//        float nozzle2OverrunValue = 0.0F;
//        float expResult = 0.0F;
//        float result = PrinterUtils.deriveNozzle1ZOffsetsFromOverrun(nozzle1OverrunValue,
//                                                                     nozzle2OverrunValue);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of deriveNozzle2ZOffsetsFromOverrun method, of class PrinterUtils.
//     */
//    @Test
//    public void testDeriveNozzle2ZOffsetsFromOverrun()
//    {
//        System.out.println("deriveNozzle2ZOffsetsFromOverrun");
//        float nozzle1OverrunValue = 0.0F;
//        float nozzle2OverrunValue = 0.0F;
//        float expResult = 0.0F;
//        float result = PrinterUtils.deriveNozzle2ZOffsetsFromOverrun(nozzle1OverrunValue,
//                                                                     nozzle2OverrunValue);
//        assertEquals(expResult, result, 0.0);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
