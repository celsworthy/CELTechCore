package celtech.gcodetranslator.postprocessing;

import celtech.JavaFXConfiguredTest;
import static celtech.Lookup.setPostProcessorOutputWriterFactory;
import celtech.TestUtils;
import celtech.appManager.Project;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.LiveGCodeOutputWriter;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MovementNode;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.modelcontrol.ModelContainer;
import celtech.services.slicer.PrintQualityEnumeration;
import java.net.URL;
import java.util.Optional;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Ian
 */
public class PostProcessorTest extends JavaFXConfiguredTest
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
                ppFeatures);

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
                ppFeatures);

        RoboxiserResult result = postProcessor.processInput();
        assertTrue(result.isSuccess());
    }

    @Test
    public void testAssignExtrusionToCorrectExtruder()
    {
        LayerNode testLayer = new LayerNode();
        testLayer.setLayerNumber(1);

        ToolSelectNode tool1 = new ToolSelectNode();
        tool1.setToolNumber(0);
        ToolSelectNode tool2 = new ToolSelectNode();
        tool2.setToolNumber(1);
        ToolSelectNode tool3 = new ToolSelectNode();
        tool3.setToolNumber(0);

        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();
        InnerPerimeterSectionNode inner2 = new InnerPerimeterSectionNode();
        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        OuterPerimeterSectionNode outer2 = new OuterPerimeterSectionNode();
        FillSectionNode fill1 = new FillSectionNode();
        FillSectionNode fill2 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        ExtrusionNode extrusionNode13 = new ExtrusionNode();
        ExtrusionNode extrusionNode14 = new ExtrusionNode();
        ExtrusionNode extrusionNode15 = new ExtrusionNode();
        ExtrusionNode extrusionNode16 = new ExtrusionNode();
        ExtrusionNode extrusionNode17 = new ExtrusionNode();
        ExtrusionNode extrusionNode18 = new ExtrusionNode();

        tool1.addChild(0, inner1);
        tool1.addChild(1, outer1);

        tool2.addChild(0, fill1);
        tool2.addChild(1, fill2);

        tool3.addChild(0, inner2);
        tool3.addChild(1, outer2);

        inner1.addChild(0, extrusionNode1);
        inner1.addChild(1, extrusionNode2);
        inner1.addChild(2, extrusionNode3);

        outer1.addChild(0, extrusionNode4);
        outer1.addChild(1, extrusionNode5);
        outer1.addChild(2, extrusionNode6);

        fill1.addChild(0, extrusionNode7);
        fill1.addChild(1, extrusionNode8);
        fill1.addChild(2, extrusionNode9);

        inner2.addChild(0, extrusionNode10);
        inner2.addChild(1, extrusionNode11);
        inner2.addChild(2, extrusionNode12);

        outer2.addChild(0, extrusionNode13);
        outer2.addChild(1, extrusionNode14);
        outer2.addChild(2, extrusionNode15);

        fill2.addChild(0, extrusionNode16);
        fill2.addChild(1, extrusionNode17);
        fill2.addChild(2, extrusionNode18);

        testLayer.addChild(0, tool1);
        testLayer.addChild(1, tool2);
        testLayer.addChild(2, tool3);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-DM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(3, testLayer.getChildren().size());

        postProcessor.assignExtrusionToCorrectExtruder(testLayer);

        assertEquals(3, testLayer.getChildren().size());

        assertFalse(((MovementNode) (outer2.getChildren().get(0))).isEInUse());
        assertTrue(((MovementNode) (outer2.getChildren().get(0))).isDInUse());

        assertTrue(((MovementNode) (fill1.getChildren().get(0))).isEInUse());
        assertFalse(((MovementNode) (fill1.getChildren().get(0))).isDInUse());

        assertFalse(((MovementNode) (outer1.getChildren().get(0))).isEInUse());
        assertTrue(((MovementNode) (outer1.getChildren().get(0))).isDInUse());
    }

    @Test
    public void testRemoveUnretractNodes()
    {
        LayerNode testLayer = new LayerNode();
        UnretractNode unretractNode1 = new UnretractNode();
        UnretractNode unretractNode2 = new UnretractNode();
        UnretractNode unretractNode3 = new UnretractNode();

        testLayer.addChild(0, unretractNode3);
        testLayer.addChild(0, unretractNode2);
        testLayer.addChild(0, unretractNode1);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(3, testLayer.getChildren().size());

        postProcessor.removeUnretractNodes(testLayer);

        assertEquals(0, testLayer.getChildren().size());
    }

    @Test
    public void testRehomeOrphanSections()
    {
        LayerNode testLayer = new LayerNode();
        ObjectDelineationNode object1 = new ObjectDelineationNode();
        object1.setObjectNumber(1);

        OrphanSectionNode orphanSection1 = new OrphanSectionNode();
        FillSectionNode fill = new FillSectionNode();
        OuterPerimeterSectionNode outer = new OuterPerimeterSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        ExtrusionNode extrusionNode7 = new ExtrusionNode();

        orphanSection1.addChild(0, extrusionNode1);
        orphanSection1.addChild(1, extrusionNode2);
        orphanSection1.addChild(2, extrusionNode3);

        fill.addChild(0, extrusionNode4);
        fill.addChild(1, extrusionNode5);
        fill.addChild(2, extrusionNode6);

        outer.addChild(0, extrusionNode7);

        object1.addChild(0, outer);
        object1.addChild(1, orphanSection1);
        object1.addChild(2, fill);

        testLayer.addChild(0, object1);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(1, testLayer.getChildren().size());
        assertEquals(3, object1.getChildren().size());
        assertSame(outer, object1.getChildren().get(0));
        assertSame(orphanSection1, object1.getChildren().get(1));
        assertSame(fill, object1.getChildren().get(2));

        postProcessor.insertNozzleControlSectionsByTask(testLayer);

        assertEquals(2, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(1) instanceof ToolSelectNode);

        ToolSelectNode tool1 = (ToolSelectNode) testLayer.getChildren().get(0);
        ToolSelectNode tool2 = (ToolSelectNode) testLayer.getChildren().get(1);

        assertEquals(2, tool1.getChildren().size());
        assertEquals(1, tool2.getChildren().size());

        assertSame(outer, tool1.getChildren().get(0));
        assertTrue(tool1.getChildren().get(1) instanceof OuterPerimeterSectionNode);

        assertSame(fill, tool2.getChildren().get(0));
    }

    @Test
    public void testRehomeOrphanObjects_startOfFile()
    {
        LayerNode testLayer = new LayerNode();
        OrphanObjectDelineationNode orphan1 = new OrphanObjectDelineationNode();
        orphan1.setPotentialObjectNumber(0);

        ObjectDelineationNode object1 = new ObjectDelineationNode();
        object1.setObjectNumber(1);

        OuterPerimeterSectionNode outer = new OuterPerimeterSectionNode();
        FillSectionNode fill = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        ExtrusionNode extrusionNode6 = new ExtrusionNode();

        outer.addChild(0, extrusionNode1);
        outer.addChild(1, extrusionNode2);
        outer.addChild(2, extrusionNode3);

        fill.addChild(0, extrusionNode4);
        fill.addChild(1, extrusionNode5);
        fill.addChild(2, extrusionNode6);

        orphan1.addChild(0, outer);
        object1.addChild(0, fill);

        testLayer.addChild(0, orphan1);
        testLayer.addChild(1, object1);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(2, testLayer.getChildren().size());
        assertSame(outer, orphan1.getChildren().get(0));

        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), testLayer, 0, 0, 0, 0);

        postProcessor.rehomeOrphanObjects(testLayer, lastLayerParseResult);

        assertEquals(2, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ObjectDelineationNode);
        ObjectDelineationNode resultNode = (ObjectDelineationNode) testLayer.getChildren().get(0);
        assertEquals(0, resultNode.getObjectNumber());

        assertSame(outer, resultNode.getChildren().get(0));

        assertSame(object1, testLayer.getChildren().get(1));
        assertSame(fill, object1.getChildren().get(0));
    }

    @Test
    public void testRehomeOrphanObjects_sameObjectAsLastLayer()
    {
        LayerNode testLayer = new LayerNode();
        testLayer.setLayerNumber(0);
        OrphanObjectDelineationNode orphan1 = new OrphanObjectDelineationNode();
        orphan1.setPotentialObjectNumber(10);

        ObjectDelineationNode object1 = new ObjectDelineationNode();
        object1.setObjectNumber(0);

        OuterPerimeterSectionNode outer = new OuterPerimeterSectionNode();
        FillSectionNode fill = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        ExtrusionNode extrusionNode6 = new ExtrusionNode();

        outer.addChild(0, extrusionNode1);
        outer.addChild(1, extrusionNode2);
        outer.addChild(2, extrusionNode3);

        fill.addChild(0, extrusionNode4);
        fill.addChild(1, extrusionNode5);
        fill.addChild(2, extrusionNode6);

        orphan1.addChild(0, outer);
        object1.addChild(0, fill);

        testLayer.addChild(0, orphan1);
        testLayer.addChild(1, object1);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(2, testLayer.getChildren().size());
        assertSame(outer, orphan1.getChildren().get(0));

        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), testLayer, 0, 0, 0, 10);

        postProcessor.rehomeOrphanObjects(testLayer, lastLayerParseResult);

        assertEquals(2, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ObjectDelineationNode);
        ObjectDelineationNode resultNode = (ObjectDelineationNode) testLayer.getChildren().get(0);
        assertEquals(10, resultNode.getObjectNumber());

        assertSame(outer, resultNode.getChildren().get(0));

        assertSame(object1, testLayer.getChildren().get(1));
        assertSame(fill, object1.getChildren().get(0));
    }

    @Test
    public void testInsertNozzleOpenFullyBeforeEvent()
    {
        LayerNode testLayer = new LayerNode();
        InnerPerimeterSectionNode inner = new InnerPerimeterSectionNode();
        OuterPerimeterSectionNode outer = new OuterPerimeterSectionNode();
        FillSectionNode fill = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();

        outer.addChild(0, extrusionNode1);
        outer.addChild(1, extrusionNode2);
        outer.addChild(2, extrusionNode3);

        testLayer.addChild(0, inner);
        testLayer.addChild(1, outer);
        testLayer.addChild(2, fill);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(3, outer.getChildren().size());

        NozzleParameters testNozzleParameters = new NozzleParameters();
        testNozzleParameters.setOpenPosition(1.0f);
        testNozzleParameters.setClosedPosition(0);
        NozzleProxy testNozzle = new NozzleProxy(testNozzleParameters);

        postProcessor.insertNozzleOpenFullyBeforeEvent(extrusionNode1, testNozzle);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(4, outer.getChildren().size());
        assertTrue(outer.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) outer.getChildren().get(0)).getDesiredValvePosition(), 0.0001);
    }

    @Test
    public void testInsertNozzleCloseFullyAfterEvent()
    {
        LayerNode testLayer = new LayerNode();
        InnerPerimeterSectionNode inner = new InnerPerimeterSectionNode();
        OuterPerimeterSectionNode outer = new OuterPerimeterSectionNode();
        FillSectionNode fill = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();

        outer.addChild(0, extrusionNode1);
        outer.addChild(1, extrusionNode2);
        outer.addChild(2, extrusionNode3);

        testLayer.addChild(0, inner);
        testLayer.addChild(1, outer);
        testLayer.addChild(2, fill);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(3, outer.getChildren().size());

        NozzleParameters testNozzleParameters = new NozzleParameters();
        testNozzleParameters.setOpenPosition(1.0f);
        testNozzleParameters.setClosedPosition(0);
        NozzleProxy testNozzle = new NozzleProxy(testNozzleParameters);

        postProcessor.insertNozzleCloseFullyAfterEvent(extrusionNode1, testNozzle);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(4, outer.getChildren().size());
        assertTrue(outer.getChildren().get(1) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer.getChildren().get(1)).getDesiredValvePosition(), 0.0001);
    }

    @Test
    public void testInsertNozzleControlSectionsByTask()
    {
        LayerNode testLayer = new LayerNode();
        testLayer.setLayerNumber(1);

        ObjectDelineationNode object1 = new ObjectDelineationNode();
        object1.setObjectNumber(11);
        ObjectDelineationNode object2 = new ObjectDelineationNode();
        object2.setObjectNumber(22);

        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();
        InnerPerimeterSectionNode inner2 = new InnerPerimeterSectionNode();
        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        OuterPerimeterSectionNode outer2 = new OuterPerimeterSectionNode();
        FillSectionNode fill1 = new FillSectionNode();
        FillSectionNode fill2 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        ExtrusionNode extrusionNode13 = new ExtrusionNode();
        ExtrusionNode extrusionNode14 = new ExtrusionNode();
        ExtrusionNode extrusionNode15 = new ExtrusionNode();
        ExtrusionNode extrusionNode16 = new ExtrusionNode();
        ExtrusionNode extrusionNode17 = new ExtrusionNode();
        ExtrusionNode extrusionNode18 = new ExtrusionNode();

        object1.addChild(0, inner1);
        object1.addChild(1, outer1);
        object1.addChild(2, fill1);

        object2.addChild(0, fill2);
        object2.addChild(1, inner2);
        object2.addChild(2, outer2);

        inner1.addChild(0, extrusionNode1);
        inner1.addChild(1, extrusionNode2);
        inner1.addChild(2, extrusionNode3);

        outer1.addChild(0, extrusionNode4);
        outer1.addChild(1, extrusionNode5);
        outer1.addChild(2, extrusionNode6);

        fill1.addChild(0, extrusionNode7);
        fill1.addChild(1, extrusionNode8);
        fill1.addChild(2, extrusionNode9);

        inner2.addChild(0, extrusionNode10);
        inner2.addChild(1, extrusionNode11);
        inner2.addChild(2, extrusionNode12);

        outer2.addChild(0, extrusionNode13);
        outer2.addChild(1, extrusionNode14);
        outer2.addChild(2, extrusionNode15);

        fill2.addChild(0, extrusionNode16);
        fill2.addChild(1, extrusionNode17);
        fill2.addChild(2, extrusionNode18);

        testLayer.addChild(0, object1);
        testLayer.addChild(1, object2);

        // INPUT
        //                             layer
        //                               |
        //             -------------------------------------
        //             |                                   |
        //           object1                            object2
        //             |                                   |
        //     ---------------------         ----------------------------
        //     |         |         |         |             |            |
        //   inner1    outer1    fill1     fill2         inner2       outer2
        //     |         |         |         |             |            |
        //  -------   -------   -------   ---------    ---------    ---------
        //  |  |  |   |  |  |   |  |  |   |   |   |    |   |   |    |   |   |
        //  e1 e2 e3  e4 e5 e6  e7 e8 e9  e10 e11 e12  e13 e14 e15  e16 e17 e18
        // OUTPUT for task-based tool selection
        //
        //                             layer
        //                               |
        //          -----------------------------------------------    
        //          |                    |                        |
        //        tool(0)              tool(1)                 tool(0)
        //          |                    |                        |
        //     -----------         ------------            -------------
        //     |         |         |          |            |           |
        //   inner1    outer1    fill1      fill2        inner2       outer2
        //     |         |         |          |            |            |
        //  -------   -------   -------   ---------    ---------    ---------
        //  |  |  |   |  |  |   |  |  |   |   |   |    |   |   |    |   |   |
        //  e1 e2 e3  e4 e5 e6  e7 e8 e9  e10 e11 e12  e13 e14 e15  e16 e17 e18
        // OUTPUT for object-based tool selection - support in object material
        //
        //                             layer
        //                               |
        //               ------------------------------------
        //               |                                  |
        //            object1                             object2
        //               |                                  |
        //             tool(0)                            tool(1)
        //               |                                  |
        //     ---------------------          ----------------------------
        //     |         |         |          |             |            |
        //   inner     outer      fill      fill          inner        outer
        //     |         |         |          |             |            |
        //  -------   -------   -------   ---------    ---------    ---------
        //  |  |  |   |  |  |   |  |  |   |   |   |    |   |   |    |   |   |
        //  e1 e2 e3  e4 e5 e6  e7 e8 e9  e10 e11 e12  e13 e14 e15  e16 e17 e18
        // OUTPUT for support in material 2  TBD
        //
        //                             layer
        //                               |
        //               ------------------------------------
        //               |                                  |
        //            object1                             object2
        //               |                                  |
        //             tool(0)                            tool(1)
        //               |                                  |
        //     ---------------------          ----------------------------
        //     |         |         |          |             |            |
        //   inner     outer      fill      fill          inner        outer
        //     |         |         |          |             |            |
        //  -------   -------   -------   ---------    ---------    ---------
        //  |  |  |   |  |  |   |  |  |   |   |   |    |   |   |    |   |   |
        //  e1 e2 e3  e4 e5 e6  e7 e8 e9  e10 e11 e12  e13 e14 e15  e16 e17 e18
        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        TestUtils utils = new TestUtils();
        ModelContainer modelContainer1 = utils.makeModelContainer(true);
        testProject.addModel(modelContainer1);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(2, testLayer.getChildren().size());
        assertEquals(3, object1.getChildren().size());
        assertEquals(3, object2.getChildren().size());
        assertEquals(3, inner1.getChildren().size());
        assertEquals(3, outer1.getChildren().size());
        assertEquals(3, fill1.getChildren().size());
        assertEquals(3, inner2.getChildren().size());
        assertEquals(3, outer2.getChildren().size());
        assertEquals(3, fill2.getChildren().size());

        int lastObjectNumber = postProcessor.insertNozzleControlSectionsByTask(testLayer);

        assertEquals(22, lastObjectNumber);
        assertEquals(3, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(1) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(2) instanceof ToolSelectNode);

        ToolSelectNode tool1 = (ToolSelectNode) testLayer.getChildren().get(0);
        ToolSelectNode tool2 = (ToolSelectNode) testLayer.getChildren().get(1);
        ToolSelectNode tool3 = (ToolSelectNode) testLayer.getChildren().get(2);

        assertEquals(2, tool1.getChildren().size());
        assertEquals(2, tool2.getChildren().size());
        assertEquals(2, tool3.getChildren().size());

        assertSame(inner1, tool1.getChildren().get(0));
        assertSame(outer1, tool1.getChildren().get(1));

        assertSame(fill1, tool2.getChildren().get(0));
        assertSame(fill2, tool2.getChildren().get(1));

        assertSame(inner2, tool3.getChildren().get(0));
        assertSame(outer2, tool3.getChildren().get(1));
    }

    @Test
    public void testInsertNozzleControlSectionsByObject()
    {
        LayerNode testLayer = new LayerNode();
        testLayer.setLayerNumber(1);

        ObjectDelineationNode object1 = new ObjectDelineationNode();
        object1.setObjectNumber(0);
        ObjectDelineationNode object2 = new ObjectDelineationNode();
        object2.setObjectNumber(1);

        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();
        InnerPerimeterSectionNode inner2 = new InnerPerimeterSectionNode();
        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        OuterPerimeterSectionNode outer2 = new OuterPerimeterSectionNode();
        FillSectionNode fill1 = new FillSectionNode();
        FillSectionNode fill2 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        ExtrusionNode extrusionNode13 = new ExtrusionNode();
        ExtrusionNode extrusionNode14 = new ExtrusionNode();
        ExtrusionNode extrusionNode15 = new ExtrusionNode();
        ExtrusionNode extrusionNode16 = new ExtrusionNode();
        ExtrusionNode extrusionNode17 = new ExtrusionNode();
        ExtrusionNode extrusionNode18 = new ExtrusionNode();

        object1.addChild(0, inner1);
        object1.addChild(1, outer1);
        object1.addChild(2, fill1);

        object2.addChild(0, fill2);
        object2.addChild(1, inner2);
        object2.addChild(2, outer2);

        inner1.addChild(0, extrusionNode1);
        inner1.addChild(1, extrusionNode2);
        inner1.addChild(2, extrusionNode3);

        outer1.addChild(0, extrusionNode4);
        outer1.addChild(1, extrusionNode5);
        outer1.addChild(2, extrusionNode6);

        fill1.addChild(0, extrusionNode7);
        fill1.addChild(1, extrusionNode8);
        fill1.addChild(2, extrusionNode9);

        inner2.addChild(0, extrusionNode10);
        inner2.addChild(1, extrusionNode11);
        inner2.addChild(2, extrusionNode12);

        outer2.addChild(0, extrusionNode13);
        outer2.addChild(1, extrusionNode14);
        outer2.addChild(2, extrusionNode15);

        fill2.addChild(0, extrusionNode16);
        fill2.addChild(1, extrusionNode17);
        fill2.addChild(2, extrusionNode18);

        testLayer.addChild(0, object1);
        testLayer.addChild(1, object2);

        // INPUT
        //                             layer
        //                               |
        //             -------------------------------------
        //             |                                   |
        //           object1                            object2
        //             |                                   |
        //     ---------------------         ----------------------------
        //     |         |         |         |             |            |
        //   inner1    outer1    fill1     fill2         inner2       outer2
        //     |         |         |         |             |            |
        //  -------   -------   -------   ---------    ---------    ---------
        //  |  |  |   |  |  |   |  |  |   |   |   |    |   |   |    |   |   |
        //  e1 e2 e3  e4 e5 e6  e7 e8 e9  e10 e11 e12  e13 e14 e15  e16 e17 e18
        // OUTPUT for object-based tool selection - support in object material
        //
        //                             layer
        //                               |
        //               ------------------------------------
        //               |                                  |
        //             tool(0)                            tool(1)
        //               |                                  |
        //     ---------------------          ----------------------------
        //     |         |         |          |             |            |
        //   inner     outer      fill      fill          inner        outer
        //     |         |         |          |             |            |
        //  -------   -------   -------   ---------    ---------    ---------
        //  |  |  |   |  |  |   |  |  |   |   |   |    |   |   |    |   |   |
        //  e1 e2 e3  e4 e5 e6  e7 e8 e9  e10 e11 e12  e13 e14 e15  e16 e17 e18
        HeadFile dualMaterialHead = HeadContainer.getHeadByID("RBX01-DM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        TestUtils utils = new TestUtils();

        ModelContainer modelContainer1 = utils.makeModelContainer(true);
        testProject.addModel(modelContainer1);

        ModelContainer modelContainer2 = utils.makeModelContainer(false);
        testProject.addModel(modelContainer2);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                dualMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(2, testLayer.getChildren().size());
        assertEquals(3, object1.getChildren().size());
        assertEquals(3, object2.getChildren().size());
        assertEquals(3, inner1.getChildren().size());
        assertEquals(3, outer1.getChildren().size());
        assertEquals(3, fill1.getChildren().size());
        assertEquals(3, inner2.getChildren().size());
        assertEquals(3, outer2.getChildren().size());
        assertEquals(3, fill2.getChildren().size());

        int lastObjectNumber = postProcessor.insertNozzleControlSectionsByObject(testLayer);

        assertEquals(1, lastObjectNumber);
        assertEquals(2, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(1) instanceof ToolSelectNode);

        ToolSelectNode tool1 = (ToolSelectNode) testLayer.getChildren().get(0);
        ToolSelectNode tool2 = (ToolSelectNode) testLayer.getChildren().get(1);

        assertEquals(3, tool1.getChildren().size());
        assertEquals(3, tool2.getChildren().size());

        assertSame(inner1, tool1.getChildren().get(0));
        assertSame(outer1, tool1.getChildren().get(1));
        assertSame(fill1, tool1.getChildren().get(2));

        assertSame(fill2, tool2.getChildren().get(0));
        assertSame(inner2, tool2.getChildren().get(1));
        assertSame(outer2, tool2.getChildren().get(2));
    }

    @Test
    public void testInsertOpenAndCloseNodes()
    {
        LayerNode testLayer = new LayerNode();
        testLayer.setLayerNumber(1);

        ToolSelectNode tool1 = new ToolSelectNode();
        tool1.setToolNumber(0);
        ToolSelectNode tool2 = new ToolSelectNode();
        tool2.setToolNumber(1);
        ToolSelectNode tool3 = new ToolSelectNode();
        tool3.setToolNumber(0);

        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();
        InnerPerimeterSectionNode inner2 = new InnerPerimeterSectionNode();
        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        OuterPerimeterSectionNode outer2 = new OuterPerimeterSectionNode();
        FillSectionNode fill1 = new FillSectionNode();
        FillSectionNode fill2 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        ExtrusionNode extrusionNode13 = new ExtrusionNode();
        ExtrusionNode extrusionNode14 = new ExtrusionNode();
        ExtrusionNode extrusionNode15 = new ExtrusionNode();
        ExtrusionNode extrusionNode16 = new ExtrusionNode();
        ExtrusionNode extrusionNode17 = new ExtrusionNode();
        ExtrusionNode extrusionNode18 = new ExtrusionNode();

        tool1.addChild(0, inner1);
        tool1.addChild(1, outer1);

        tool2.addChild(0, fill1);
        tool2.addChild(1, fill2);

        tool3.addChild(0, inner2);
        tool3.addChild(1, outer2);

        inner1.addChild(0, extrusionNode1);
        inner1.addChild(1, extrusionNode2);
        inner1.addChild(2, extrusionNode3);

        outer1.addChild(0, extrusionNode4);
        outer1.addChild(1, extrusionNode5);
        outer1.addChild(2, extrusionNode6);

        fill1.addChild(0, extrusionNode7);
        fill1.addChild(1, extrusionNode8);
        fill1.addChild(2, extrusionNode9);

        inner2.addChild(0, extrusionNode10);
        inner2.addChild(1, extrusionNode11);
        inner2.addChild(2, extrusionNode12);

        outer2.addChild(0, extrusionNode13);
        outer2.addChild(1, extrusionNode14);
        outer2.addChild(2, extrusionNode15);

        fill2.addChild(0, extrusionNode16);
        fill2.addChild(1, extrusionNode17);
        fill2.addChild(2, extrusionNode18);

        testLayer.addChild(0, tool1);
        testLayer.addChild(1, tool2);
        testLayer.addChild(2, tool3);

        // INPUT
        //
        //                             layer
        //                               |
        //          -----------------------------------------------    
        //          |                    |                        |
        //        tool(0)              tool(1)                 tool(0)
        //          |                    |                        |
        //     -----------         ------------            -------------
        //     |         |         |          |            |           |
        //   inner1    outer1    fill1      fill2        inner2       outer2
        //     |         |         |          |            |            |
        //  -------   -------   -------   ---------    ---------    ---------
        //  |  |  |   |  |  |   |  |  |   |   |   |    |   |   |    |   |   |
        //  e1 e2 e3  e4 e5 e6  e7 e8 e9  e10 e11 e12  e13 e14 e15  e16 e17 e18
        // OUTPUT
        //
        //                                            layer
        //                                              |
        //              -------------------------------------------------------------------    
        //              |                               |                                 |
        //           tool(0)                         tool(1)                           tool(0)
        //              |                               |                                 |
        //     -------------------               -----------------                ------------------
        //     |                 |               |               |                |                |
        //   inner1            outer1          fill1           fill2            inner2           outer2
        //     |                 |               |               |                |                |
        //  ------------   ------------    ------------    --------------    ----------   --------------------
        //  |    |  |  |   |  |  |    |    |    |  |  |    |   |   |    |    |    |   |   |    |   |   |     |
        //  open e1 e2 e3  e4 e5 e6 close  open e7 e8 e9  e10 e11 e12 close  open e13 e14 e15  e16 e17 e18 close
        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(3, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(1) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(2) instanceof ToolSelectNode);
        assertEquals(2, tool1.getChildren().size());
        assertEquals(2, tool2.getChildren().size());
        assertEquals(2, tool2.getChildren().size());
        assertEquals(3, inner1.getChildren().size());
        assertEquals(3, outer1.getChildren().size());
        assertEquals(3, fill1.getChildren().size());
        assertEquals(3, inner2.getChildren().size());
        assertEquals(3, outer2.getChildren().size());
        assertEquals(3, fill2.getChildren().size());

        LayerPostProcessResult lastLayerParseResult = null;

        postProcessor.insertOpenAndCloseNodes(testLayer, null);

//        postProcessor.outputNodes(testLayer, 0);
        assertEquals(3, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(1) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(2) instanceof ToolSelectNode);

        assertEquals(4, inner1.getChildren().size());
        assertEquals(4, outer1.getChildren().size());
        assertEquals(4, fill1.getChildren().size());
        assertEquals(4, fill2.getChildren().size());
        assertEquals(4, inner2.getChildren().size());
        assertEquals(4, outer2.getChildren().size());

        assertTrue(inner1.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) inner1.getChildren().get(0)).getDesiredValvePosition(), 0.0001);

        assertTrue(outer1.getChildren().get(3) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer1.getChildren().get(3)).getDesiredValvePosition(), 0.0001);

        assertTrue(fill1.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) fill1.getChildren().get(0)).getDesiredValvePosition(), 0.0001);

        assertTrue(fill2.getChildren().get(3) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) fill2.getChildren().get(3)).getDesiredValvePosition(), 0.0001);

        assertTrue(inner2.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) inner2.getChildren().get(0)).getDesiredValvePosition(), 0.0001);

        assertTrue(outer2.getChildren().get(3) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer2.getChildren().get(3)).getDesiredValvePosition(), 0.0001);
    }

    @Test
    public void testInsertOpenAndCloseNodes_withRetracts()
    {
        LayerNode testLayer = new LayerNode();
        testLayer.setLayerNumber(1);

        ToolSelectNode tool1 = new ToolSelectNode();
        tool1.setToolNumber(0);
        ToolSelectNode tool2 = new ToolSelectNode();
        tool2.setToolNumber(1);
        ToolSelectNode tool3 = new ToolSelectNode();
        tool3.setToolNumber(0);

        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();
        InnerPerimeterSectionNode inner2 = new InnerPerimeterSectionNode();
        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        OuterPerimeterSectionNode outer2 = new OuterPerimeterSectionNode();
        FillSectionNode fill1 = new FillSectionNode();
        FillSectionNode fill2 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        ExtrusionNode extrusionNode13 = new ExtrusionNode();
        ExtrusionNode extrusionNode14 = new ExtrusionNode();
        ExtrusionNode extrusionNode15 = new ExtrusionNode();
        ExtrusionNode extrusionNode16 = new ExtrusionNode();
        ExtrusionNode extrusionNode17 = new ExtrusionNode();
        ExtrusionNode extrusionNode18 = new ExtrusionNode();

        RetractNode retract1 = new RetractNode();
        RetractNode retract2 = new RetractNode();
        RetractNode retract3 = new RetractNode();
        RetractNode retract4 = new RetractNode();

        tool1.addChild(0, inner1);
        tool1.addChild(1, outer1);

        tool2.addChild(0, fill1);
        tool2.addChild(1, fill2);

        tool3.addChild(0, inner2);
        tool3.addChild(1, outer2);

        inner1.addChild(0, extrusionNode1);
        inner1.addChild(1, extrusionNode2);
        inner1.addChild(2, extrusionNode3);

        outer1.addChild(0, extrusionNode4);
        outer1.addChild(1, extrusionNode5);
        outer1.addChild(2, retract1);
        outer1.addChild(3, extrusionNode6);

        fill1.addChild(0, extrusionNode7);
        fill1.addChild(1, extrusionNode8);
        fill1.addChild(2, extrusionNode9);

        inner2.addChild(0, extrusionNode10);
        inner2.addChild(1, extrusionNode11);
        inner2.addChild(2, extrusionNode12);
        inner2.addChild(3, retract4);

        outer2.addChild(0, extrusionNode13);
        outer2.addChild(1, extrusionNode14);
        outer2.addChild(2, extrusionNode15);

        fill2.addChild(0, extrusionNode16);
        fill2.addChild(1, retract2);
        fill2.addChild(2, extrusionNode17);
        fill2.addChild(3, extrusionNode18);
        fill2.addChild(4, retract3);

        testLayer.addChild(0, tool1);
        testLayer.addChild(1, tool2);
        testLayer.addChild(2, tool3);

        // INPUT
        //
        //                                layer
        //                                  |
        //          ----------------------------------------------------------    
        //          |                        |                               |
        //        tool(0)                  tool(1)                         tool(0)
        //          |                        |                               |
        //     -----------            ----------------                ---------------
        //     |         |            |              |                |             |
        //   inner1    outer1       fill1          fill2            inner2        outer2
        //     |         |            |              |                |             |
        //  -------   ----------   -------   ----------------   -------------   ---------
        //  |  |  |   |  |  |  |   |  |  |   |   |  |   |   |   |   |   |   |   |   |   |
        //  e1 e2 e3  e4 e5 r1 e6  e7 e8 e9  e10 r1 e11 e12 r3  e13 e14 e15 r4  e16 e17 e18
        // OUTPUT
        //
        //                                                           layer
        //                                                             |
        //                    ---------------------------------------------------------------------------------------    
        //                    |                                        |                                            |
        //                  tool(0)                                 tool(1)                                      tool(0)
        //                    |                                        |                                            |
        //         --------------------                      ----------------------                        ------------------
        //         |                  |                      |                    |                        |                |
        //       inner1             outer1                 fill1                fill2                    inner2           outer2
        //         |                  |                      |                    |                        |                |
        //    ----------   -----------------------      ----------   --------------------------      ------------------     -------------------
        //    |  |  |  |   |  |    |    |   |    |      |  |  |  |   |     |     |  |   |     |      |  |   |   |     |     |   |   |   |     |
        //  open e1 e2 e3  e4 e5 close open e6 close  open e7 e8 e9  e10 close open e11 e12 close  open e13 e14 e15 close  open e16 e17 e18 close
        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        assertEquals(3, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(1) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(2) instanceof ToolSelectNode);
        assertEquals(2, tool1.getChildren().size());
        assertEquals(2, tool2.getChildren().size());
        assertEquals(2, tool2.getChildren().size());
        assertEquals(3, inner1.getChildren().size());
        assertEquals(4, outer1.getChildren().size());
        assertEquals(3, fill1.getChildren().size());
        assertEquals(4, inner2.getChildren().size());
        assertEquals(3, outer2.getChildren().size());
        assertEquals(5, fill2.getChildren().size());

        LayerPostProcessResult lastLayerParseResult = null;

        postProcessor.insertOpenAndCloseNodes(testLayer, lastLayerParseResult);

//        postProcessor.outputNodes(testLayer, 0);
        assertEquals(3, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(1) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(2) instanceof ToolSelectNode);

        assertEquals(4, inner1.getChildren().size());
        assertEquals(6, outer1.getChildren().size());
        assertEquals(4, fill1.getChildren().size());
        assertEquals(6, fill2.getChildren().size());
        assertEquals(5, inner2.getChildren().size());
        assertEquals(5, outer2.getChildren().size());

        assertTrue(inner1.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) inner1.getChildren().get(0)).getDesiredValvePosition(), 0.0001);

        assertTrue(outer1.getChildren().get(2) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer1.getChildren().get(2)).getDesiredValvePosition(), 0.0001);

        assertTrue(outer1.getChildren().get(3) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) outer1.getChildren().get(3)).getDesiredValvePosition(), 0.0001);

        assertTrue(outer1.getChildren().get(5) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer1.getChildren().get(5)).getDesiredValvePosition(), 0.0001);

        assertTrue(fill1.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) fill1.getChildren().get(0)).getDesiredValvePosition(), 0.0001);

        assertTrue(fill2.getChildren().get(1) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) fill2.getChildren().get(1)).getDesiredValvePosition(), 0.0001);

        assertTrue(fill2.getChildren().get(2) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) fill2.getChildren().get(2)).getDesiredValvePosition(), 0.0001);

        assertTrue(fill2.getChildren().get(5) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) fill2.getChildren().get(5)).getDesiredValvePosition(), 0.0001);

        assertTrue(inner2.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) inner2.getChildren().get(0)).getDesiredValvePosition(), 0.0001);

        assertTrue(inner2.getChildren().get(4) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) inner2.getChildren().get(4)).getDesiredValvePosition(), 0.0001);

        assertTrue(outer2.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) outer2.getChildren().get(0)).getDesiredValvePosition(), 0.0001);

        assertTrue(outer2.getChildren().get(4) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer2.getChildren().get(4)).getDesiredValvePosition(), 0.0001);
    }

    @Test
    public void testCloseToEndOfFill_noSplitsRequired()
    {
        FillSectionNode fill1 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.setE(0.001f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.setE(0.001f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.setE(0.001f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.setE(0.001f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.setE(0.001f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.setE(0.001f);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.setE(0.001f);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.setE(0.001f);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.setE(0.001f);

        fill1.addChild(0, extrusionNode1);
        fill1.addChild(1, extrusionNode2);
        fill1.addChild(2, extrusionNode3);
        fill1.addChild(3, extrusionNode4);
        fill1.addChild(4, extrusionNode5);
        fill1.addChild(5, extrusionNode6);
        fill1.addChild(6, extrusionNode7);
        fill1.addChild(7, extrusionNode8);
        fill1.addChild(8, extrusionNode9);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.003f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        postProcessor.closeToEndOfFill(extrusionNode9, testProxy);

        assertEquals(9, fill1.getChildren().size());

        assertTrue(fill1.getChildren().get(5) instanceof ExtrusionNode);
        ExtrusionNode node0 = (ExtrusionNode) fill1.getChildren().get(5);
        assertFalse(node0.isBSet());
        assertEquals(0.001, node0.getE(), 0.0001);

        assertTrue(fill1.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode node1 = (ExtrusionNode) fill1.getChildren().get(6);
        assertTrue(node1.isBSet());
        assertEquals(0.67, node1.getB(), 0.01);
        assertFalse(node1.isEInUse());

        assertTrue(fill1.getChildren().get(7) instanceof ExtrusionNode);
        ExtrusionNode node2 = (ExtrusionNode) fill1.getChildren().get(7);
        assertTrue(node2.isBSet());
        assertEquals(0.33, node2.getB(), 0.01);
        assertFalse(node2.isEInUse());

        assertTrue(fill1.getChildren().get(8) instanceof ExtrusionNode);
        ExtrusionNode node3 = (ExtrusionNode) fill1.getChildren().get(8);
        assertTrue(node3.isBSet());
        assertEquals(0.0, node3.getB(), 0.01);
        assertFalse(node3.isEInUse());
    }

    @Test
    public void testCloseToEndOfFill_splitsRequired()
    {
        FillSectionNode fill1 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.setE(0.001f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.setE(0.001f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.setE(0.001f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.setE(0.001f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.setE(0.001f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.setE(0.001f);
        extrusionNode6.setX(0);
        extrusionNode6.setY(0);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.setE(0.0015f);
        extrusionNode7.setX(10);
        extrusionNode7.setY(0);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.setE(0.001f);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.setE(0.001f);

        fill1.addChild(0, extrusionNode1);
        fill1.addChild(1, extrusionNode2);
        fill1.addChild(2, extrusionNode3);
        fill1.addChild(3, extrusionNode4);
        fill1.addChild(4, extrusionNode5);
        fill1.addChild(5, extrusionNode6);
        fill1.addChild(6, extrusionNode7);
        fill1.addChild(7, extrusionNode8);
        fill1.addChild(8, extrusionNode9);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.003f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        postProcessor.closeToEndOfFill(extrusionNode9, testProxy);

        postProcessor.outputNodes(fill1, 0);

        assertEquals(10, fill1.getChildren().size());

        assertTrue(fill1.getChildren().get(5) instanceof ExtrusionNode);
        ExtrusionNode node0 = (ExtrusionNode) fill1.getChildren().get(5);
        assertFalse(node0.isBSet());
        assertEquals(0.001, node0.getE(), 0.0001);

        assertTrue(fill1.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode node1 = (ExtrusionNode) fill1.getChildren().get(6);
        assertFalse(node1.isBSet());
        assertEquals(0.0005, node1.getE(), 0.0001);

        assertTrue(fill1.getChildren().get(7) instanceof ExtrusionNode);
        ExtrusionNode node2 = (ExtrusionNode) fill1.getChildren().get(7);
        assertTrue(node2.isBSet());
        assertEquals(0.67, node2.getB(), 0.01);
        assertFalse(node2.isEInUse());

        assertTrue(fill1.getChildren().get(8) instanceof ExtrusionNode);
        ExtrusionNode node3 = (ExtrusionNode) fill1.getChildren().get(8);
        assertTrue(node3.isBSet());
        assertEquals(0.33, node3.getB(), 0.01);
        assertFalse(node3.isEInUse());

        assertTrue(fill1.getChildren().get(9) instanceof ExtrusionNode);
        ExtrusionNode node4 = (ExtrusionNode) fill1.getChildren().get(9);
        assertTrue(node4.isBSet());
        assertEquals(0.0, node4.getB(), 0.01);
        assertFalse(node4.isEInUse());
    }

    @Test
    public void testCloseFromHereTowardsEndOfSection()
    {
        ToolSelectNode tool1 = new ToolSelectNode();

        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.setX(0);
        extrusionNode1.setY(0);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.setX(10);
        extrusionNode2.setY(0);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.setX(10);
        extrusionNode3.setY(10);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.setX(0);
        extrusionNode4.setY(10);

        TravelNode travel1 = new TravelNode();
        travel1.setX(1);
        travel1.setY(9);

        outer1.addChild(0, extrusionNode1);
        outer1.addChild(1, extrusionNode2);
        outer1.addChild(2, extrusionNode3);
        outer1.addChild(3, extrusionNode4);
        outer1.addChild(4, travel1);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.setX(1);
        extrusionNode5.setY(1);
        extrusionNode5.setE(1);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.setX(9);
        extrusionNode6.setY(1);
        extrusionNode6.setE(1);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.setX(9);
        extrusionNode7.setY(9);
        extrusionNode7.setE(1);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.setX(1);
        extrusionNode8.setY(9);
        extrusionNode8.setE(1);

        inner1.addChild(0, extrusionNode5);
        inner1.addChild(1, extrusionNode6);
        inner1.addChild(2, extrusionNode7);
        inner1.addChild(3, extrusionNode8);

        tool1.addChild(0, inner1);
        tool1.addChild(1, outer1);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.5f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        postProcessor.closeFromHereTowardsEndOfSection(extrusionNode6, testProxy);

        postProcessor.outputNodes(inner1, 0);

//        assertEquals(10, fill1.getChildren().size());
//
//        assertTrue(fill1.getChildren().get(5) instanceof ExtrusionNode);
//        ExtrusionNode node0 = (ExtrusionNode) fill1.getChildren().get(5);
//        assertFalse(node0.isBSet());
//        assertEquals(0.001, node0.getE(), 0.0001);
//
//        assertTrue(fill1.getChildren().get(6) instanceof ExtrusionNode);
//        ExtrusionNode node1 = (ExtrusionNode) fill1.getChildren().get(6);
//        assertFalse(node1.isBSet());
//        assertEquals(0.0005, node1.getE(), 0.0001);
//
//        assertTrue(fill1.getChildren().get(7) instanceof ExtrusionNode);
//        ExtrusionNode node2 = (ExtrusionNode) fill1.getChildren().get(7);
//        assertTrue(node2.isBSet());
//        assertEquals(0.67, node2.getB(), 0.01);
//        assertFalse(node2.isEInUse());
//
//        assertTrue(fill1.getChildren().get(8) instanceof ExtrusionNode);
//        ExtrusionNode node3 = (ExtrusionNode) fill1.getChildren().get(8);
//        assertTrue(node3.isBSet());
//        assertEquals(0.33, node3.getB(), 0.01);
//        assertFalse(node3.isEInUse());
//
//        assertTrue(fill1.getChildren().get(9) instanceof ExtrusionNode);
//        ExtrusionNode node4 = (ExtrusionNode) fill1.getChildren().get(9);
//        assertTrue(node4.isBSet());
//        assertEquals(0.0, node4.getB(), 0.01);
//        assertFalse(node4.isEInUse());
    }

    @Test
    public void testFindClosestExtrusionNode()
    {
        ToolSelectNode tool1 = new ToolSelectNode();

        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();

        TravelNode travel1 = new TravelNode();
        travel1.setX(0);
        travel1.setY(0);
        
        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.setX(10);
        extrusionNode1.setY(0);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.setX(10);
        extrusionNode2.setY(10);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.setX(0);
        extrusionNode3.setY(10);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.setX(0);
        extrusionNode4.setY(0);

        outer1.addChild(0, travel1);
        outer1.addChild(1, extrusionNode1);
        outer1.addChild(2, extrusionNode2);
        outer1.addChild(3, extrusionNode3);
        outer1.addChild(4, extrusionNode4);

        TravelNode travel2 = new TravelNode();
        travel2.setX(1);
        travel2.setY(1);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.setX(9);
        extrusionNode5.setY(1);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.setX(9);
        extrusionNode6.setY(9);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.setX(1);
        extrusionNode7.setY(9);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.setX(1);
        extrusionNode8.setY(1);

        inner1.addChild(0, travel2);
        inner1.addChild(1, extrusionNode5);
        inner1.addChild(2, extrusionNode6);
        inner1.addChild(3, extrusionNode7);
        inner1.addChild(4, extrusionNode8);

        tool1.addChild(0, inner1);
        tool1.addChild(1, outer1);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.003f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        Optional<IntersectionResult> result = postProcessor.findClosestExtrusionNode(extrusionNode4, inner1);

        assertTrue(result.isPresent());
        assertSame(extrusionNode8, result.get().getClosestNode());
        assertEquals(1, result.get().getIntersectionPoint().getX(), 0.01);
        assertEquals(5, result.get().getIntersectionPoint().getY(), 0.01);
    }

    @Test
    public void testCloseInwardFromOuterPerimeter()
    {
        ToolSelectNode tool1 = new ToolSelectNode();

        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();

        TravelNode travel1 = new TravelNode();
        travel1.setX(0);
        travel1.setY(0);
        
        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.setX(10);
        extrusionNode1.setY(0);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.setX(10);
        extrusionNode2.setY(10);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.setX(0);
        extrusionNode3.setY(10);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.setX(0);
        extrusionNode4.setY(0);

        outer1.addChild(0, travel1);
        outer1.addChild(1, extrusionNode1);
        outer1.addChild(2, extrusionNode2);
        outer1.addChild(3, extrusionNode3);
        outer1.addChild(4, extrusionNode4);

        TravelNode travel2 = new TravelNode();
        travel2.setX(1);
        travel2.setY(1);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.setX(9);
        extrusionNode5.setY(1);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.setX(9);
        extrusionNode6.setY(9);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.setX(1);
        extrusionNode7.setY(9);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.setX(1);
        extrusionNode8.setY(1);

        inner1.addChild(0, travel2);
        inner1.addChild(1, extrusionNode5);
        inner1.addChild(2, extrusionNode6);
        inner1.addChild(3, extrusionNode7);
        inner1.addChild(4, extrusionNode8);

        tool1.addChild(0, inner1);
        tool1.addChild(1, outer1);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.003f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);

        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                testProject,
                ppFeatures);

        postProcessor.closeInwardFromOuterPerimeter(extrusionNode4, testProxy);

        assertEquals(ppFeatures, outer1);
    }

//    @Test
//    public void testCloseToEndOfFill_noPriorSibling()
//    {
//        FillSectionNode fill1 = new FillSectionNode();
//
//        ExtrusionNode extrusionNode1 = new ExtrusionNode();
//        extrusionNode1.setE(0.001f);
//
//        ExtrusionNode extrusionNode2 = new ExtrusionNode();
//        extrusionNode2.setE(0.001f);
//
//        ExtrusionNode extrusionNode3 = new ExtrusionNode();
//        extrusionNode3.setE(0.001f);
//
//        ExtrusionNode extrusionNode4 = new ExtrusionNode();
//        extrusionNode4.setE(0.001f);
//
//        ExtrusionNode extrusionNode5 = new ExtrusionNode();
//        extrusionNode5.setE(0.001f);
//
//        ExtrusionNode extrusionNode6 = new ExtrusionNode();
//        extrusionNode6.setE(0.001f);
//        extrusionNode6.setX(0);
//        extrusionNode6.setY(0);
//
//        ExtrusionNode extrusionNode7 = new ExtrusionNode();
//        extrusionNode7.setE(0.0015f);
//        extrusionNode7.setX(10);
//        extrusionNode7.setY(0);
//
//        ExtrusionNode extrusionNode8 = new ExtrusionNode();
//        extrusionNode8.setE(0.001f);
//
//        ExtrusionNode extrusionNode9 = new ExtrusionNode();
//        extrusionNode9.setE(0.001f);
//
//        fill1.addChild(0, extrusionNode1);
//        fill1.addChild(1, extrusionNode2);
//        fill1.addChild(2, extrusionNode3);
//        fill1.addChild(3, extrusionNode4);
//        fill1.addChild(4, extrusionNode5);
//        fill1.addChild(5, extrusionNode6);
//        fill1.addChild(6, extrusionNode7);
//        fill1.addChild(7, extrusionNode8);
//        fill1.addChild(8, extrusionNode9);
//
//        NozzleParameters nozzleParams = new NozzleParameters();
//        nozzleParams.setEjectionVolume(0.003f);
//
//        NozzleProxy testProxy = new NozzleProxy(nozzleParams);
//
//        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");
//
//        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
//        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
//        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
//        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
//        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);
//
//        Project testProject = new Project();
//        testProject.getPrinterSettings().setSettingsName("BothNozzles");
//        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);
//
//        PostProcessor postProcessor = new PostProcessor("",
//                "",
//                singleMaterialHead,
//                testProject,
//                ppFeatures);
//
//        postProcessor.closeToEndOfFill(extrusionNode9, testProxy);
//        
//        postProcessor.outputNodes(fill1, 0);
//
//        assertEquals(10, fill1.getChildren().size());
//
//        assertTrue(fill1.getChildren().get(5) instanceof ExtrusionNode);
//        ExtrusionNode node0 = (ExtrusionNode) fill1.getChildren().get(5);
//        assertFalse(node0.isBSet());
//        assertEquals(0.001, node0.getE(), 0.0001);
//
//        assertTrue(fill1.getChildren().get(6) instanceof ExtrusionNode);
//        ExtrusionNode node1 = (ExtrusionNode) fill1.getChildren().get(6);
//        assertFalse(node1.isBSet());
//        assertEquals(0.0005, node1.getE(), 0.0001);
//
//        assertTrue(fill1.getChildren().get(7) instanceof ExtrusionNode);
//        ExtrusionNode node2 = (ExtrusionNode) fill1.getChildren().get(7);
//        assertTrue(node2.isBSet());
//        assertEquals(0.67, node2.getB(), 0.01);
//        assertFalse(node2.isEInUse());
//
//        assertTrue(fill1.getChildren().get(8) instanceof ExtrusionNode);
//        ExtrusionNode node3 = (ExtrusionNode) fill1.getChildren().get(8);
//        assertTrue(node3.isBSet());
//        assertEquals(0.33, node3.getB(), 0.01);
//        assertFalse(node3.isEInUse());
//
//        assertTrue(fill1.getChildren().get(9) instanceof ExtrusionNode);
//        ExtrusionNode node4 = (ExtrusionNode) fill1.getChildren().get(9);
//        assertTrue(node4.isBSet());
//        assertEquals(0.0, node4.getB(), 0.01);
//        assertFalse(node4.isEInUse());
//    }
}
