package celtech.gcodetranslator.postprocessing;

import celtech.JavaFXConfiguredTest;
import static celtech.Lookup.setPostProcessorOutputWriterFactory;
import celtech.TestUtils;
import celtech.appManager.Project;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.gcodetranslator.LiveGCodeOutputWriter;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.modelcontrol.ModelContainer;
import celtech.services.slicer.PrintQualityEnumeration;
import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Ian
 */
public class PostProcessorTest extends JavaFXConfiguredTest
{

    private double movementEpsilon = 0.001;
    private double nozzleEpsilon = 0.01;

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
        URL inputURL = this.getClass().getResource("/postprocessor/curaTwoObjects.gcode");
        String inputFilename = inputURL.getFile();
        String outputFilename = inputFilename + ".out";
        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        setPostProcessorOutputWriterFactory(LiveGCodeOutputWriter::new);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor(inputFilename,
                outputFilename,
                singleMaterialHead,
                testProject,
                testProject.getPrinterSettings().getSettings("RBX01-SM"),
                ppFeatures,
                "RBX01-SM",
                null);

        RoboxiserResult result = postProcessor.processInput();
        assertTrue(result.isSuccess());
    }

    /**
     * Test of processInput method, of class PostProcessor.
     */
    @Test
    public void testComplexInput()
    {
        System.out.println("complexInput");
        URL inputURL = this.getClass().getResource("/postprocessor/complexTest.gcode");
        String inputFilename = inputURL.getFile();
        String outputFilename = inputFilename + ".out";
        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-DM");

        setPostProcessorOutputWriterFactory(LiveGCodeOutputWriter::new);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("Draft");
        testProject.setPrintQuality(PrintQualityEnumeration.DRAFT);

        TestUtils utils = new TestUtils();
        ModelContainer modelContainer1 = utils.makeModelContainer(true);
        testProject.addModel(modelContainer1);

        ModelContainer modelContainer2 = utils.makeModelContainer(false);
        testProject.addModel(modelContainer2);

        PostProcessor postProcessor = new PostProcessor(inputFilename,
                outputFilename,
                singleMaterialHead,
                testProject,
                testProject.getPrinterSettings().getSettings("RBX01-DM"),
                ppFeatures,
                "RBX01-SM",
                null);

        RoboxiserResult result = postProcessor.processInput();
        assertTrue(result.isSuccess());
    }

    /**
     * Test of processInput method, of class PostProcessor.
     */
    @Test
    public void testCura_2_colour_dice()
    {
        System.out.println("Cura 2 colour dice");
        URL inputURL = this.getClass().getResource("/postprocessor/cura_2_colour_dice.gcode");
        String inputFilename = inputURL.getFile();
        String outputFilename = inputFilename + ".out";
        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-DM");

        setPostProcessorOutputWriterFactory(LiveGCodeOutputWriter::new);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("Draft");
        testProject.setPrintQuality(PrintQualityEnumeration.DRAFT);

        TestUtils utils = new TestUtils();
        ModelContainer modelContainer1 = utils.makeModelContainer(true);
        testProject.addModel(modelContainer1);

        ModelContainer modelContainer2 = utils.makeModelContainer(false);
        testProject.addModel(modelContainer2);

        PostProcessor postProcessor = new PostProcessor(inputFilename,
                outputFilename,
                singleMaterialHead,
                testProject,
                testProject.getPrinterSettings().getSettings("RBX01-DM"),
                ppFeatures,
                "RBX01-SM",
                null);

        RoboxiserResult result = postProcessor.processInput();
        assertTrue(result.isSuccess());
    }
}
