package celtech.printerControl.model;

import celtech.JavaFXConfiguredTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class HeadTest extends JavaFXConfiguredTest
{
    public HeadTest()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

//    /**
//     * Test of createHead method, of class Head.
//     */
//    @Test
//    public void testCreateHead()
//    {
//        System.out.println("createHead");
//        HeadEEPROMDataResponse headResponse = null;
//        Head expResult = null;
//        Head result = Head.createHead(headResponse);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of typeCodeProperty method, of class Head.
//     */
//    @Test
//    public void testTypeCodeProperty()
//    {
//        System.out.println("typeCodeProperty");
//        Head instance = new Head();
//        StringProperty expResult = null;
//        StringProperty result = instance.typeCodeProperty();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of nameProperty method, of class Head.
//     */
//    @Test
//    public void testNameProperty()
//    {
//        System.out.println("nameProperty");
//        Head instance = new Head();
//        StringProperty expResult = null;
//        StringProperty result = instance.nameProperty();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of uniqueIDProperty method, of class Head.
//     */
//    @Test
//    public void testUniqueIDProperty()
//    {
//        System.out.println("uniqueIDProperty");
//        Head instance = new Head();
//        StringProperty expResult = null;
//        StringProperty result = instance.uniqueIDProperty();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of headHoursProperty method, of class Head.
//     */
//    @Test
//    public void testHeadHoursProperty()
//    {
//        System.out.println("headHoursProperty");
//        Head instance = new Head();
//        FloatProperty expResult = null;
//        FloatProperty result = instance.headHoursProperty();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNozzleHeaters method, of class Head.
//     */
//    @Test
//    public void testGetNozzleHeaters()
//    {
//        System.out.println("getNozzleHeaters");
//        Head instance = new Head();
//        ArrayList<NozzleHeater> expResult = null;
//        ArrayList<NozzleHeater> result = instance.getNozzleHeaters();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getNozzles method, of class Head.
//     */
//    @Test
//    public void testGetNozzles()
//    {
//        System.out.println("getNozzles");
//        Head instance = new Head();
//        ArrayList<Nozzle> expResult = null;
//        ArrayList<Nozzle> result = instance.getNozzles();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of toString method, of class Head.
//     */
//    @Test
//    public void testToString()
//    {
//        System.out.println("toString");
//        Head instance = new Head();
//        String expResult = "";
//        String result = instance.toString();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of clone method, of class Head.
//     */
//    @Test
//    public void testClone()
//    {
//        System.out.println("clone");
//        Head instance = new Head();
//        Head expResult = null;
//        Head result = instance.clone();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of updateFromEEPROMData method, of class Head.
//     */
//    @Test
//    public void testUpdateFromEEPROMData()
//    {
//        System.out.println("updateFromEEPROMData");
//        HeadEEPROMDataResponse eepromData = null;
//        Head instance = new Head();
//        instance.updateFromEEPROMData(eepromData);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of bringDataInBounds method, of class Head.
//     */
//    @Test
//    public void testBringDataInBounds()
//    {
//        System.out.println("bringDataInBounds");
//        Head instance = new Head();
//        HeadRepairResult expResult = null;
//        HeadRepairResult result = instance.bringDataInBounds();
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of resetToDefaults method, of class Head.
//     */
//    @Test
//    public void testResetToDefaults()
//    {
//        System.out.println("resetToDefaults");
//        Head instance = new Head();
//        instance.resetToDefaults();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
    /**
     * Test of isTypeCodeValid method, of class Head.
     */
    @Test
    public void testIsTypeCodeValid()
    {
        System.out.println("isTypeCodeValid");
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
        System.out.println("isTypeCodeValid");
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
        System.out.println("isTypeCodeInDatabase");
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
        System.out.println("isTypeCodeInDatabase");
        String typeCode = "blah";
        boolean expResult = false;
        boolean result = Head.isTypeCodeInDatabase(typeCode);
        assertEquals(expResult, result);
    }

//    /**
//     * Test of allocateRandomID method, of class Head.
//     */
//    @Test
//    public void testAllocateRandomID()
//    {
//        System.out.println("allocateRandomID");
//        Head instance = new Head();
//        instance.allocateRandomID();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
}
