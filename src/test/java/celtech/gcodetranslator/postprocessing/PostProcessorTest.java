package celtech.gcodetranslator.postprocessing;

import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.printerControl.model.Head;
import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Ian
 */
public class PostProcessorTest
{

    public PostProcessorTest()
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
     * Test of processInput method, of class PostProcessor.
     */
    @Test
    public void testProcessInput()
    {
        System.out.println("processInput");
        URL inputURL = this.getClass().getResource("/postprocessor/baseTest.gcode");
        String inputFilename = inputURL.getFile();
        String outputFilename = inputFilename + ".out";
        Head singleMaterialHead = new Head(HeadContainer.getHeadByID("RBX01-SM"));
        PostProcessor instance = new PostProcessor(inputFilename, outputFilename, singleMaterialHead);
        instance.processInput();
    }

}
