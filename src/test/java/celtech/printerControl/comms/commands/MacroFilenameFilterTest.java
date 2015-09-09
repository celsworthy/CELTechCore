package celtech.printerControl.comms.commands;

import celtech.printerControl.model.Head;
import java.io.File;
import java.io.FilenameFilter;
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
public class MacroFilenameFilterTest
{

    public MacroFilenameFilterTest()
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

    /**
     * Test of accept method, of class MacroFilenameFilter.
     */
    @Test
    public void testDontAcceptWithWrongMacroName()
    {
        FilenameFilter testFilter = new MacroFilenameFilter("testFile",
                null,
                GCodeMacros.NozzleUseIndicator.DONT_CARE,
                GCodeMacros.SafetyIndicator.DONT_CARE);
        
        assertFalse(testFilter.accept(null, "fred"));
    }

    @Test
    public void testAcceptWithCorrectMacroName()
    {
        FilenameFilter testFilter = new MacroFilenameFilter("testFile",
                null,
                GCodeMacros.NozzleUseIndicator.DONT_CARE,
                GCodeMacros.SafetyIndicator.DONT_CARE);
        
        assertTrue(testFilter.accept(null, "testFile"));
    }
    
    @Test
    public void testDontAcceptWithCorrectMacroNameUnrequestedModifier()
    {
        FilenameFilter testFilter = new MacroFilenameFilter("testFile",
                null,
                GCodeMacros.NozzleUseIndicator.DONT_CARE,
                GCodeMacros.SafetyIndicator.DONT_CARE);
        
        assertFalse(testFilter.accept(null, "testFile#U"));
    }
}
