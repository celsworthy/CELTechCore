package celtech.utils;

import celtech.appManager.PurgeResponse;
import celtech.printerControl.model.Printer;
import celtech.utils.tasks.Cancellable;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.concurrent.Task;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class PrinterUtilsTest
{

    public PrinterUtilsTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

//    /**
//     * Test of getInstance method, of class PrinterUtils.
//     */
//    @Test
//    public void testGetInstance()
//    {
//        System.out.println("getInstance");
//        PrinterUtils expResult = null;
//        PrinterUtils result = PrinterUtils.getInstance();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
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
//
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

}
