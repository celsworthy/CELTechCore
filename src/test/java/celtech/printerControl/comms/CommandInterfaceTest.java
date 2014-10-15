package celtech.printerControl.comms;

import celtech.JavaFXConfiguredTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Ian
 */
public class CommandInterfaceTest extends JavaFXConfiguredTest
{

    private static TestCommandInterface instance = null;
    private static CommandInterfaceTestPrinter printer = null;

    public CommandInterfaceTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
        instance = new TestCommandInterface(null, "Test Command Interface", false, 100);
        printer = new CommandInterfaceTestPrinter();
        instance.setPrinter(printer);
    }

    @AfterClass
    public static void tearDownClass()
    {
        instance.shutdown();
    }

    @Before
    public void setUp()
    {
        instance.preTestInitialisation();
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void testSomething()
    {
        
    }
}
