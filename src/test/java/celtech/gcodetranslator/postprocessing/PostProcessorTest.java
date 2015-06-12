package celtech.gcodetranslator.postprocessing;

import celtech.JavaFXConfiguredTest;
import static celtech.Lookup.setPostProcessorOutputWriterFactory;
import celtech.TestUtils;
import celtech.appManager.Project;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.LiveGCodeOutputWriter;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.RoboxiserResult;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ReplenishNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.modelcontrol.ModelContainer;
import celtech.services.slicer.PrintQualityEnumeration;
import java.net.URL;
import java.util.Optional;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        extrusionNode1.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode10 = new ExtrusionNode();
        extrusionNode10.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode11 = new ExtrusionNode();
        extrusionNode11.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode12 = new ExtrusionNode();
        extrusionNode12.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode13 = new ExtrusionNode();
        extrusionNode13.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode14 = new ExtrusionNode();
        extrusionNode14.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode15 = new ExtrusionNode();
        extrusionNode15.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode16 = new ExtrusionNode();
        extrusionNode16.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode17 = new ExtrusionNode();
        extrusionNode17.getExtrusion().setE(1f);
        ExtrusionNode extrusionNode18 = new ExtrusionNode();
        extrusionNode18.getExtrusion().setE(1f);

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

        assertFalse(((ExtrusionProvider) (outer2.getChildren().get(0))).getExtrusion().isEInUse());
        assertTrue(((ExtrusionProvider) (outer2.getChildren().get(0))).getExtrusion().isDInUse());

        assertTrue(((ExtrusionProvider) (fill1.getChildren().get(0))).getExtrusion().isEInUse());
        assertFalse(((ExtrusionProvider) (fill1.getChildren().get(0))).getExtrusion().isDInUse());

        assertFalse(((ExtrusionProvider) (outer1.getChildren().get(0))).getExtrusion().isEInUse());
        assertTrue(((ExtrusionProvider) (outer1.getChildren().get(0))).getExtrusion().isDInUse());
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
    public void testInsertNozzleOpenFullyBeforeEvent_noReplenish()
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
        assertEquals(1.0, ((NozzleValvePositionNode) outer.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);
    }

    @Test
    public void testInsertNozzleOpenFullyBeforeEvent_noReplenishRequired()
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
        ppFeatures.enableFeature(PostProcessorFeature.REPLENISH_BEFORE_OPEN);

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
        assertEquals(1.0, ((NozzleValvePositionNode) outer.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);
    }

    @Test
    public void testInsertNozzleOpenFullyBeforeEvent_withReplenish()
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
        ppFeatures.enableFeature(PostProcessorFeature.REPLENISH_BEFORE_OPEN);

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
        testNozzle.setElidedExtrusion(0.4);

        postProcessor.insertNozzleOpenFullyBeforeEvent(extrusionNode1, testNozzle);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(5, outer.getChildren().size());
        assertTrue(outer.getChildren().get(0) instanceof ReplenishNode);
        assertTrue(outer.getChildren().get(1) instanceof NozzleValvePositionNode);
        assertEquals(0, testNozzle.getElidedExtrusion(), 0.0001);
        assertEquals(0.4, ((ReplenishNode) outer.getChildren().get(0)).getExtrusion().getE(), 0.0001);
        assertEquals(1.0, ((NozzleValvePositionNode) outer.getChildren().get(1)).getNozzlePosition().getB(), 0.0001);
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
        assertEquals(0.0, ((NozzleValvePositionNode) outer.getChildren().get(1)).getNozzlePosition().getB(), 0.0001);
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
        assertEquals(1.0, ((NozzleValvePositionNode) inner1.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);

        assertTrue(outer1.getChildren().get(3) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer1.getChildren().get(3)).getNozzlePosition().getB(), 0.0001);

        assertTrue(fill1.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) fill1.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);

        assertTrue(fill2.getChildren().get(3) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) fill2.getChildren().get(3)).getNozzlePosition().getB(), 0.0001);

        assertTrue(inner2.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) inner2.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);

        assertTrue(outer2.getChildren().get(3) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer2.getChildren().get(3)).getNozzlePosition().getB(), 0.0001);
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
        extrusionNode10.getMovement().setX(10);
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

        postProcessor.outputNodes(testLayer, 0);
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
        assertEquals(1.0, ((NozzleValvePositionNode) inner1.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);

        assertTrue(outer1.getChildren().get(2) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer1.getChildren().get(2)).getNozzlePosition().getB(), 0.0001);

        assertTrue(outer1.getChildren().get(3) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) outer1.getChildren().get(3)).getNozzlePosition().getB(), 0.0001);

        assertTrue(outer1.getChildren().get(5) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer1.getChildren().get(5)).getNozzlePosition().getB(), 0.0001);

        assertTrue(fill1.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) fill1.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);

        assertTrue(fill2.getChildren().get(1) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) fill2.getChildren().get(1)).getNozzlePosition().getB(), 0.0001);

        assertTrue(fill2.getChildren().get(2) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) fill2.getChildren().get(2)).getNozzlePosition().getB(), 0.0001);

        assertTrue(fill2.getChildren().get(5) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) fill2.getChildren().get(5)).getNozzlePosition().getB(), 0.0001);

        assertTrue(inner2.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) inner2.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);

        assertTrue(inner2.getChildren().get(4) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) inner2.getChildren().get(4)).getNozzlePosition().getB(), 0.0001);

        assertTrue(outer2.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) outer2.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);

        assertTrue(outer2.getChildren().get(4) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer2.getChildren().get(4)).getNozzlePosition().getB(), 0.0001);
    }

    @Test
    public void testCloseToEndOfFill_noSplitsRequired()
    {
        FillSectionNode fill1 = new FillSectionNode();

        TravelNode travel1 = new TravelNode();
        travel1.getMovement().setX(0);
        travel1.getMovement().setY(0);

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1f);
        extrusionNode1.getMovement().setX(1);
        extrusionNode1.getMovement().setY(1);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(1f);
        extrusionNode2.getMovement().setX(2);
        extrusionNode2.getMovement().setY(2);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(1f);
        extrusionNode3.getMovement().setX(3);
        extrusionNode3.getMovement().setY(3);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(1f);
        extrusionNode4.getMovement().setX(4);
        extrusionNode4.getMovement().setY(4);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(1f);
        extrusionNode5.getMovement().setX(5);
        extrusionNode5.getMovement().setY(5);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(1f);
        extrusionNode6.getMovement().setX(6);
        extrusionNode6.getMovement().setY(6);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(1f);
        extrusionNode7.getMovement().setX(7);
        extrusionNode7.getMovement().setY(7);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(1f);
        extrusionNode8.getMovement().setX(8);
        extrusionNode8.getMovement().setY(8);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(1f);
        extrusionNode9.getMovement().setX(9);
        extrusionNode9.getMovement().setY(9);

        fill1.addChild(0, travel1);
        fill1.addChild(1, extrusionNode1);
        fill1.addChild(2, extrusionNode2);
        fill1.addChild(3, extrusionNode3);
        fill1.addChild(4, extrusionNode4);
        fill1.addChild(5, extrusionNode5);
        fill1.addChild(6, extrusionNode6);
        fill1.addChild(7, extrusionNode7);
        fill1.addChild(8, extrusionNode8);
        fill1.addChild(9, extrusionNode9);

        NozzleParameters nozzleParams = new NozzleParameters();

        nozzleParams.setEjectionVolume(2);

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

        //Should have elided the same volume as the ejection volume
        assertEquals(2, testProxy.getElidedExtrusion(), 0.01);

        assertEquals(10, fill1.getChildren().size());

        assertTrue(fill1.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode node0 = (ExtrusionNode) fill1.getChildren().get(6);
        assertSame(extrusionNode6, node0);
        assertFalse(node0.getNozzlePosition().isBSet());
        assertEquals(1, node0.getExtrusion().getE(), movementEpsilon);

        assertTrue(fill1.getChildren().get(7) instanceof ExtrusionNode);
        ExtrusionNode node1 = (ExtrusionNode) fill1.getChildren().get(7);
        assertSame(extrusionNode7, node1);
        assertFalse(node1.getNozzlePosition().isBSet());
        assertEquals(1, node1.getExtrusion().getE(), movementEpsilon);

        assertTrue(fill1.getChildren().get(8) instanceof ExtrusionNode);
        ExtrusionNode node2 = (ExtrusionNode) fill1.getChildren().get(8);
        assertSame(extrusionNode8, node2);
        assertTrue(node2.getNozzlePosition().isBSet());
        assertEquals(0.5, node2.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node2.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(9) instanceof ExtrusionNode);
        ExtrusionNode node3 = (ExtrusionNode) fill1.getChildren().get(9);
        assertSame(extrusionNode9, node3);
        assertTrue(node3.getNozzlePosition().isBSet());
        assertEquals(0.0, node3.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node3.getExtrusion().isEInUse());
    }

    @Test
    public void testCloseToEndOfFill_splitsRequired()
    {
        FillSectionNode fill1 = new FillSectionNode();

        TravelNode travel1 = new TravelNode();
        travel1.getMovement().setX(0);
        travel1.getMovement().setY(0);

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1f);
        extrusionNode1.getMovement().setX(1);
        extrusionNode1.getMovement().setY(1);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(1f);
        extrusionNode2.getMovement().setX(2);
        extrusionNode2.getMovement().setY(2);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(1f);
        extrusionNode3.getMovement().setX(3);
        extrusionNode3.getMovement().setY(3);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(1f);
        extrusionNode4.getMovement().setX(4);
        extrusionNode4.getMovement().setY(4);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(1f);
        extrusionNode5.getMovement().setX(5);
        extrusionNode5.getMovement().setY(5);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(1f);
        extrusionNode6.getMovement().setX(6);
        extrusionNode6.getMovement().setY(6);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(1f);
        extrusionNode7.getMovement().setX(7);
        extrusionNode7.getMovement().setY(7);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(1f);
        extrusionNode8.getMovement().setX(8);
        extrusionNode8.getMovement().setY(8);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(1f);
        extrusionNode9.getMovement().setX(9);
        extrusionNode9.getMovement().setY(9);

        fill1.addChild(0, travel1);
        fill1.addChild(1, extrusionNode1);
        fill1.addChild(2, extrusionNode2);
        fill1.addChild(3, extrusionNode3);
        fill1.addChild(4, extrusionNode4);
        fill1.addChild(5, extrusionNode5);
        fill1.addChild(6, extrusionNode6);
        fill1.addChild(7, extrusionNode7);
        fill1.addChild(8, extrusionNode8);
        fill1.addChild(9, extrusionNode9);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(7.75f);

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

        //Should have elided the same volume as the ejection volume
        assertEquals(7.75, testProxy.getElidedExtrusion(), 0.01);

        assertEquals(11, fill1.getChildren().size());

        assertTrue(fill1.getChildren().get(0) instanceof TravelNode);
        TravelNode tnode0 = (TravelNode) fill1.getChildren().get(0);
        assertEquals(0, tnode0.getMovement().getX(), movementEpsilon);
        assertEquals(0, tnode0.getMovement().getY(), movementEpsilon);

        assertTrue(fill1.getChildren().get(1) instanceof ExtrusionNode);
        ExtrusionNode node0 = (ExtrusionNode) fill1.getChildren().get(1);
        assertFalse(node0.getNozzlePosition().isBSet());
        assertEquals(1, node0.getExtrusion().getE(), movementEpsilon);
        assertEquals(1, node0.getMovement().getX(), movementEpsilon);
        assertEquals(1, node0.getMovement().getY(), movementEpsilon);

        assertTrue(fill1.getChildren().get(2) instanceof ExtrusionNode);
        ExtrusionNode node1 = (ExtrusionNode) fill1.getChildren().get(2);
        assertFalse(node1.getNozzlePosition().isBSet());
        assertEquals(0.25, node1.getExtrusion().getE(), movementEpsilon);
        assertEquals(1.25, node1.getMovement().getX(), movementEpsilon);
        assertEquals(1.25, node1.getMovement().getY(), movementEpsilon);

        assertTrue(fill1.getChildren().get(3) instanceof ExtrusionNode);
        ExtrusionNode node2 = (ExtrusionNode) fill1.getChildren().get(3);
        assertTrue(node2.getNozzlePosition().isBSet());
        assertEquals(0.9, node2.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node2.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(4) instanceof ExtrusionNode);
        ExtrusionNode node3 = (ExtrusionNode) fill1.getChildren().get(4);
        assertTrue(node3.getNozzlePosition().isBSet());
        assertEquals(0.77, node3.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node3.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(5) instanceof ExtrusionNode);
        ExtrusionNode node4 = (ExtrusionNode) fill1.getChildren().get(5);
        assertTrue(node4.getNozzlePosition().isBSet());
        assertEquals(0.65, node4.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node4.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode node5 = (ExtrusionNode) fill1.getChildren().get(6);
        assertTrue(node5.getNozzlePosition().isBSet());
        assertEquals(0.52, node5.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node5.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(7) instanceof ExtrusionNode);
        ExtrusionNode node6 = (ExtrusionNode) fill1.getChildren().get(7);
        assertTrue(node6.getNozzlePosition().isBSet());
        assertEquals(0.39, node6.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node6.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(8) instanceof ExtrusionNode);
        ExtrusionNode node7 = (ExtrusionNode) fill1.getChildren().get(8);
        assertTrue(node7.getNozzlePosition().isBSet());
        assertEquals(0.26, node7.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node7.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(9) instanceof ExtrusionNode);
        ExtrusionNode node8 = (ExtrusionNode) fill1.getChildren().get(9);
        assertTrue(node8.getNozzlePosition().isBSet());
        assertEquals(0.13, node8.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node8.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(10) instanceof ExtrusionNode);
        ExtrusionNode node9 = (ExtrusionNode) fill1.getChildren().get(10);
        assertTrue(node9.getNozzlePosition().isBSet());
        assertEquals(0, node9.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node9.getExtrusion().isEInUse());

    }

    @Test
    public void testCloseToEndOfFill_splitAtEndRequired()
    {
        FillSectionNode fill1 = new FillSectionNode();

        TravelNode travel1 = new TravelNode();
        travel1.getMovement().setX(0);
        travel1.getMovement().setY(0);

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1f);
        extrusionNode1.getMovement().setX(1);
        extrusionNode1.getMovement().setY(1);
        extrusionNode1.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(1f);
        extrusionNode2.getMovement().setX(2);
        extrusionNode2.getMovement().setY(2);
        extrusionNode2.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(1f);
        extrusionNode3.getMovement().setX(3);
        extrusionNode3.getMovement().setY(3);
        extrusionNode3.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(1f);
        extrusionNode4.getMovement().setX(4);
        extrusionNode4.getMovement().setY(4);
        extrusionNode4.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(1f);
        extrusionNode5.getMovement().setX(5);
        extrusionNode5.getMovement().setY(5);
        extrusionNode5.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(1f);
        extrusionNode6.getMovement().setX(6);
        extrusionNode6.getMovement().setY(6);
        extrusionNode6.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(1f);
        extrusionNode7.getMovement().setX(7);
        extrusionNode7.getMovement().setY(7);
        extrusionNode7.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(1f);
        extrusionNode8.getMovement().setX(8);
        extrusionNode8.getMovement().setY(8);
        extrusionNode8.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(1f);
        extrusionNode9.getMovement().setX(9);
        extrusionNode9.getMovement().setY(9);
        extrusionNode9.getFeedrate().setFeedRate_mmPerMin(10);

        fill1.addChild(0, travel1);
        fill1.addChild(1, extrusionNode1);
        fill1.addChild(2, extrusionNode2);
        fill1.addChild(3, extrusionNode3);
        fill1.addChild(4, extrusionNode4);
        fill1.addChild(5, extrusionNode5);
        fill1.addChild(6, extrusionNode6);
        fill1.addChild(7, extrusionNode7);
        fill1.addChild(8, extrusionNode8);
        fill1.addChild(9, extrusionNode9);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(8.75f);

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

        //Should have elided the same volume as the ejection volume
        assertEquals(8.75, testProxy.getElidedExtrusion(), 0.01);

        assertEquals(11, fill1.getChildren().size());

        assertTrue(fill1.getChildren().get(0) instanceof TravelNode);
        TravelNode tnode0 = (TravelNode) fill1.getChildren().get(0);
        assertEquals(0, tnode0.getMovement().getX(), movementEpsilon);
        assertEquals(0, tnode0.getMovement().getY(), movementEpsilon);

        assertTrue(fill1.getChildren().get(1) instanceof ExtrusionNode);
        ExtrusionNode node0 = (ExtrusionNode) fill1.getChildren().get(1);
        assertFalse(node0.getNozzlePosition().isBSet());
        assertEquals(0.25, node0.getExtrusion().getE(), movementEpsilon);
        assertEquals(0.25, node0.getMovement().getX(), movementEpsilon);
        assertEquals(0.25, node0.getMovement().getY(), movementEpsilon);
        //The new node needs to have the same feedrate as the other extrusions
        assertEquals(extrusionNode9.getFeedrate().getFeedRate_mmPerMin(), node0.getFeedrate().getFeedRate_mmPerMin(), 0.01);

        assertTrue(fill1.getChildren().get(2) instanceof ExtrusionNode);
        ExtrusionNode node1 = (ExtrusionNode) fill1.getChildren().get(2);
        assertTrue(node1.getNozzlePosition().isBSet());
        assertEquals(0.91, node1.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node1.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(3) instanceof ExtrusionNode);
        ExtrusionNode node2 = (ExtrusionNode) fill1.getChildren().get(3);
        assertTrue(node2.getNozzlePosition().isBSet());
        assertEquals(0.8, node2.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node2.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(4) instanceof ExtrusionNode);
        ExtrusionNode node3 = (ExtrusionNode) fill1.getChildren().get(4);
        assertTrue(node3.getNozzlePosition().isBSet());
        assertEquals(0.69, node3.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node3.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(5) instanceof ExtrusionNode);
        ExtrusionNode node4 = (ExtrusionNode) fill1.getChildren().get(5);
        assertTrue(node4.getNozzlePosition().isBSet());
        assertEquals(0.57, node4.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node4.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode node5 = (ExtrusionNode) fill1.getChildren().get(6);
        assertTrue(node5.getNozzlePosition().isBSet());
        assertEquals(0.46, node5.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node5.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(7) instanceof ExtrusionNode);
        ExtrusionNode node6 = (ExtrusionNode) fill1.getChildren().get(7);
        assertTrue(node6.getNozzlePosition().isBSet());
        assertEquals(0.34, node6.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node6.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(8) instanceof ExtrusionNode);
        ExtrusionNode node7 = (ExtrusionNode) fill1.getChildren().get(8);
        assertTrue(node7.getNozzlePosition().isBSet());
        assertEquals(0.23, node7.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node7.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(9) instanceof ExtrusionNode);
        ExtrusionNode node8 = (ExtrusionNode) fill1.getChildren().get(9);
        assertTrue(node8.getNozzlePosition().isBSet());
        assertEquals(0.11, node8.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node8.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(10) instanceof ExtrusionNode);
        ExtrusionNode node9 = (ExtrusionNode) fill1.getChildren().get(10);
        assertTrue(node9.getNozzlePosition().isBSet());
        assertEquals(0, node9.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node9.getExtrusion().isEInUse());
    }

    @Test
    public void testCloseToEndOfFill_notEnoughVolumeInSection()
    {
        FillSectionNode fill1 = new FillSectionNode();

        TravelNode travel1 = new TravelNode();
        travel1.getMovement().setX(0);
        travel1.getMovement().setY(0);

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1f);
        extrusionNode1.getMovement().setX(1);
        extrusionNode1.getMovement().setY(1);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(1f);
        extrusionNode2.getMovement().setX(2);
        extrusionNode2.getMovement().setY(2);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(1f);
        extrusionNode3.getMovement().setX(3);
        extrusionNode3.getMovement().setY(3);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(1f);
        extrusionNode4.getMovement().setX(4);
        extrusionNode4.getMovement().setY(4);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(1f);
        extrusionNode5.getMovement().setX(5);
        extrusionNode5.getMovement().setY(5);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(1f);
        extrusionNode6.getMovement().setX(6);
        extrusionNode6.getMovement().setY(6);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(1f);
        extrusionNode7.getMovement().setX(7);
        extrusionNode7.getMovement().setY(7);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(1f);
        extrusionNode8.getMovement().setX(8);
        extrusionNode8.getMovement().setY(8);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(1f);
        extrusionNode9.getMovement().setX(9);
        extrusionNode9.getMovement().setY(9);

        fill1.addChild(0, travel1);
        fill1.addChild(1, extrusionNode1);
        fill1.addChild(2, extrusionNode2);
        fill1.addChild(3, extrusionNode3);
        fill1.addChild(4, extrusionNode4);
        fill1.addChild(5, extrusionNode5);
        fill1.addChild(6, extrusionNode6);
        fill1.addChild(7, extrusionNode7);
        fill1.addChild(8, extrusionNode8);
        fill1.addChild(9, extrusionNode9);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(10f);

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

        //Should have elided the volume in the fill section
        //We didn't have enough volume for the entire eject volume
        assertEquals(9, testProxy.getElidedExtrusion(), 0.01);

        assertEquals(10, fill1.getChildren().size());

        assertTrue(fill1.getChildren().get(0) instanceof TravelNode);
        TravelNode tnode0 = (TravelNode) fill1.getChildren().get(0);
        assertEquals(0, tnode0.getMovement().getX(), movementEpsilon);
        assertEquals(0, tnode0.getMovement().getY(), movementEpsilon);

        assertTrue(fill1.getChildren().get(1) instanceof ExtrusionNode);
        ExtrusionNode node0 = (ExtrusionNode) fill1.getChildren().get(1);
        assertTrue(node0.getNozzlePosition().isBSet());
        assertEquals(0.89, node0.getNozzlePosition().getB(), nozzleEpsilon);
        assertEquals(1, node0.getMovement().getX(), movementEpsilon);
        assertEquals(1, node0.getMovement().getY(), movementEpsilon);
        assertFalse(node0.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(2) instanceof ExtrusionNode);
        ExtrusionNode node1 = (ExtrusionNode) fill1.getChildren().get(2);
        assertTrue(node1.getNozzlePosition().isBSet());
        assertEquals(0.77, node1.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node1.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(3) instanceof ExtrusionNode);
        ExtrusionNode node2 = (ExtrusionNode) fill1.getChildren().get(3);
        assertTrue(node2.getNozzlePosition().isBSet());
        assertEquals(0.66, node2.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node2.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(4) instanceof ExtrusionNode);
        ExtrusionNode node3 = (ExtrusionNode) fill1.getChildren().get(4);
        assertTrue(node3.getNozzlePosition().isBSet());
        assertEquals(0.56, node3.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node3.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(5) instanceof ExtrusionNode);
        ExtrusionNode node4 = (ExtrusionNode) fill1.getChildren().get(5);
        assertTrue(node4.getNozzlePosition().isBSet());
        assertEquals(0.44, node4.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node4.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode node5 = (ExtrusionNode) fill1.getChildren().get(6);
        assertTrue(node5.getNozzlePosition().isBSet());
        assertEquals(0.33, node5.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node5.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(7) instanceof ExtrusionNode);
        ExtrusionNode node6 = (ExtrusionNode) fill1.getChildren().get(7);
        assertTrue(node6.getNozzlePosition().isBSet());
        assertEquals(0.22, node6.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node6.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(8) instanceof ExtrusionNode);
        ExtrusionNode node7 = (ExtrusionNode) fill1.getChildren().get(8);
        assertTrue(node7.getNozzlePosition().isBSet());
        assertEquals(0.11, node7.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node7.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(9) instanceof ExtrusionNode);
        ExtrusionNode node8 = (ExtrusionNode) fill1.getChildren().get(9);
        assertTrue(node8.getNozzlePosition().isBSet());
        assertEquals(0, node8.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node8.getExtrusion().isEInUse());
    }

    @Test
    public void testCloseToEndOfFill_splitAtStartPriorIsInSection()
    {
        FillSectionNode fill1 = new FillSectionNode();

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1f);
        extrusionNode1.getMovement().setX(1);
        extrusionNode1.getMovement().setY(1);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(1f);
        extrusionNode2.getMovement().setX(2);
        extrusionNode2.getMovement().setY(2);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(1f);
        extrusionNode3.getMovement().setX(3);
        extrusionNode3.getMovement().setY(3);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(1f);
        extrusionNode4.getMovement().setX(4);
        extrusionNode4.getMovement().setY(4);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(1f);
        extrusionNode5.getMovement().setX(5);
        extrusionNode5.getMovement().setY(5);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(1f);
        extrusionNode6.getMovement().setX(6);
        extrusionNode6.getMovement().setY(6);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getExtrusion().setE(1f);
        extrusionNode7.getMovement().setX(7);
        extrusionNode7.getMovement().setY(7);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getExtrusion().setE(1f);
        extrusionNode8.getMovement().setX(8);
        extrusionNode8.getMovement().setY(8);

        ExtrusionNode extrusionNode9 = new ExtrusionNode();
        extrusionNode9.getExtrusion().setE(1f);
        extrusionNode9.getMovement().setX(9);
        extrusionNode9.getMovement().setY(9);

        fill1.addChild(0, extrusionNode1);
        fill1.addChild(1, extrusionNode2);
        fill1.addChild(2, extrusionNode3);
        fill1.addChild(3, extrusionNode4);
        fill1.addChild(4, extrusionNode5);
        fill1.addChild(5, extrusionNode6);
        fill1.addChild(6, extrusionNode7);
        fill1.addChild(7, extrusionNode8);
        fill1.addChild(8, extrusionNode9);

        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();

        TravelNode travel1 = new TravelNode();
        travel1.getMovement().setX(0);
        travel1.getMovement().setY(0);

        outer1.addChild(0, travel1);

        ToolSelectNode tool1 = new ToolSelectNode();
        tool1.addChild(0, outer1);
        tool1.addChild(1, fill1);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(8.75f);

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

        assertEquals(10, fill1.getChildren().size());

        assertTrue(fill1.getChildren().get(0) instanceof ExtrusionNode);
        ExtrusionNode node0 = (ExtrusionNode) fill1.getChildren().get(0);
        assertFalse(node0.getNozzlePosition().isBSet());
        assertEquals(0.25, node0.getExtrusion().getE(), movementEpsilon);
        assertEquals(0.25, node0.getMovement().getX(), movementEpsilon);
        assertEquals(0.25, node0.getMovement().getY(), movementEpsilon);

        assertTrue(fill1.getChildren().get(1) instanceof ExtrusionNode);
        ExtrusionNode node1 = (ExtrusionNode) fill1.getChildren().get(1);
        assertTrue(node1.getNozzlePosition().isBSet());
        assertEquals(0.91, node1.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node1.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(2) instanceof ExtrusionNode);
        ExtrusionNode node2 = (ExtrusionNode) fill1.getChildren().get(2);
        assertTrue(node2.getNozzlePosition().isBSet());
        assertEquals(0.8, node2.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node2.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(3) instanceof ExtrusionNode);
        ExtrusionNode node3 = (ExtrusionNode) fill1.getChildren().get(3);
        assertTrue(node3.getNozzlePosition().isBSet());
        assertEquals(0.69, node3.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node3.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(4) instanceof ExtrusionNode);
        ExtrusionNode node4 = (ExtrusionNode) fill1.getChildren().get(4);
        assertTrue(node4.getNozzlePosition().isBSet());
        assertEquals(0.57, node4.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node4.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(5) instanceof ExtrusionNode);
        ExtrusionNode node5 = (ExtrusionNode) fill1.getChildren().get(5);
        assertTrue(node5.getNozzlePosition().isBSet());
        assertEquals(0.46, node5.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node5.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode node6 = (ExtrusionNode) fill1.getChildren().get(6);
        assertTrue(node6.getNozzlePosition().isBSet());
        assertEquals(0.34, node6.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node6.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(7) instanceof ExtrusionNode);
        ExtrusionNode node7 = (ExtrusionNode) fill1.getChildren().get(7);
        assertTrue(node7.getNozzlePosition().isBSet());
        assertEquals(0.23, node7.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node7.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(8) instanceof ExtrusionNode);
        ExtrusionNode node8 = (ExtrusionNode) fill1.getChildren().get(8);
        assertTrue(node8.getNozzlePosition().isBSet());
        assertEquals(0.11, node8.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node8.getExtrusion().isEInUse());

        assertTrue(fill1.getChildren().get(9) instanceof ExtrusionNode);
        ExtrusionNode node9 = (ExtrusionNode) fill1.getChildren().get(9);
        assertTrue(node9.getNozzlePosition().isBSet());
        assertEquals(0, node9.getNozzlePosition().getB(), nozzleEpsilon);
        assertFalse(node9.getExtrusion().isEInUse());
    }

    @Test
    public void testAddClosesUsingSpecifiedNode_backward()
    {
        ToolSelectNode tool1 = setupToolNodeWithInnerAndOuterSquare();

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.15f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);
        testProxy.setCurrentPosition(1.0); // The nozzle starts fully open

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

        postProcessor.addClosesUsingSpecifiedNode(tool1.getChildren().get(1).getChildren().get(4),
                tool1.getChildren().get(0).getChildren().get(4),
                testProxy, false,
                0, false);

        //Should have elided the ejection volume
        assertEquals(0.15, testProxy.getElidedExtrusion(), 0.01);

        OuterPerimeterSectionNode outerResult = (OuterPerimeterSectionNode) tool1.getChildren().get(1);
        assertEquals(7, outerResult.getChildren().size());

        assertTrue(outerResult.getChildren().get(5) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult1 = (ExtrusionNode) outerResult.getChildren().get(5);
        assertEquals(1, extrusionResult1.getMovement().getX(), movementEpsilon);
        assertEquals(9, extrusionResult1.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult1.getExtrusion().isEInUse());
        assertEquals(0.333, extrusionResult1.getNozzlePosition().getB(), movementEpsilon);
        assertEquals(20, extrusionResult1.getFeedrate().getFeedRate_mmPerMin(), 0.01);

        assertTrue(outerResult.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult2 = (ExtrusionNode) outerResult.getChildren().get(6);
        assertEquals(5, extrusionResult2.getMovement().getX(), movementEpsilon);
        assertEquals(9, extrusionResult2.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult2.getExtrusion().isEInUse());
        assertEquals(0, extrusionResult2.getNozzlePosition().getB(), movementEpsilon);
        assertEquals(20, extrusionResult2.getFeedrate().getFeedRate_mmPerMin(), 0.01);
    }

    @Test
    public void testAddClosesUsingSpecifiedNode_overAvailableVolume()
    {
        ToolSelectNode tool1 = setupToolNodeWithInnerAndOuterSquare();

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(4f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);
        testProxy.setCurrentPosition(1.0); // The nozzle starts fully open

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

        postProcessor.addClosesUsingSpecifiedNode(tool1.getChildren().get(1).getChildren().get(4),
                tool1.getChildren().get(0).getChildren().get(4),
                testProxy, false,
                0.4, true);

        postProcessor.outputNodes(tool1, 0);

        //The elided volume should be equivalent to that of the nodes we copied (4 in this instance)
        assertEquals(4, testProxy.getElidedExtrusion(), 0.01);

        OuterPerimeterSectionNode outerResult = (OuterPerimeterSectionNode) tool1.getChildren().get(1);
        assertEquals(9, outerResult.getChildren().size());

        assertTrue(outerResult.getChildren().get(5) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult1 = (ExtrusionNode) outerResult.getChildren().get(5);
        assertEquals(1, extrusionResult1.getMovement().getX(), movementEpsilon);
        assertEquals(9, extrusionResult1.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult1.getExtrusion().isEInUse());
        assertEquals(0.75, extrusionResult1.getNozzlePosition().getB(), movementEpsilon);

        assertTrue(outerResult.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult2 = (ExtrusionNode) outerResult.getChildren().get(6);
        assertEquals(9, extrusionResult2.getMovement().getX(), movementEpsilon);
        assertEquals(9, extrusionResult2.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult2.getExtrusion().isEInUse());
        assertEquals(0.5, extrusionResult2.getNozzlePosition().getB(), movementEpsilon);

        assertTrue(outerResult.getChildren().get(7) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult3 = (ExtrusionNode) outerResult.getChildren().get(7);
        assertEquals(9, extrusionResult3.getMovement().getX(), movementEpsilon);
        assertEquals(1, extrusionResult3.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult3.getExtrusion().isEInUse());
        assertEquals(0.25, extrusionResult3.getNozzlePosition().getB(), movementEpsilon);

        assertTrue(outerResult.getChildren().get(8) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult4 = (ExtrusionNode) outerResult.getChildren().get(8);
        assertEquals(1, extrusionResult4.getMovement().getX(), movementEpsilon);
        assertEquals(1, extrusionResult4.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult4.getExtrusion().isEInUse());
        assertEquals(0, extrusionResult4.getNozzlePosition().getB(), movementEpsilon);
    }

    @Test
    public void testFindClosestExtrusionNode()
    {
        ToolSelectNode tool1 = setupToolNodeWithInnerAndOuterSquare();

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

        Optional<IntersectionResult> result = postProcessor.findClosestExtrusionNode(((ExtrusionNode) tool1.getChildren().get(1).getChildren().get(4)),
                ((SectionNode) tool1.getChildren().get(0)));

        assertTrue(result.isPresent());
        assertSame(tool1.getChildren().get(0).getChildren().get(4), result.get().getClosestNode());
        assertEquals(1, result.get().getIntersectionPoint().getX(), 0.01);
        assertEquals(5, result.get().getIntersectionPoint().getY(), 0.01);
    }

    @Test
    public void testCloseInwardFromOuterPerimeter()
    {
        ToolSelectNode tool1 = setupToolNodeWithInnerAndOuterSquare();

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.15f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);
        testProxy.setCurrentPosition(1.0);

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

        postProcessor.closeInwardFromOuterPerimeter(((ExtrusionNode) tool1.getChildren().get(1).getChildren().get(4)), testProxy);

        OuterPerimeterSectionNode outerResult = (OuterPerimeterSectionNode) tool1.getChildren().get(1);
        assertEquals(8, outerResult.getChildren().size());

        assertTrue(outerResult.getChildren().get(5) instanceof TravelNode);
        TravelNode travelResult1 = (TravelNode) outerResult.getChildren().get(5);
        assertEquals(1, travelResult1.getMovement().getX(), movementEpsilon);
        assertEquals(1, travelResult1.getMovement().getY(), movementEpsilon);

        assertTrue(outerResult.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult1 = (ExtrusionNode) outerResult.getChildren().get(6);
        assertEquals(1, extrusionResult1.getMovement().getX(), movementEpsilon);
        assertEquals(9, extrusionResult1.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult1.getExtrusion().isEInUse());
        assertEquals(0.333, extrusionResult1.getNozzlePosition().getB(), movementEpsilon);

        assertTrue(outerResult.getChildren().get(7) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult2 = (ExtrusionNode) outerResult.getChildren().get(7);
        assertEquals(5, extrusionResult2.getMovement().getX(), movementEpsilon);
        assertEquals(9, extrusionResult2.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult2.getExtrusion().isEInUse());
        assertEquals(0, extrusionResult2.getNozzlePosition().getB(), movementEpsilon);
    }

    @Test
    public void testSuppressUnnecessaryToolChanges()
    {
        ToolSelectNode tool1 = new ToolSelectNode();
        tool1.setToolNumber(4);

        ToolSelectNode tool2 = new ToolSelectNode();
        tool2.setToolNumber(4);

        LayerNode layer = new LayerNode();

        layer.addChild(0, tool1);
        layer.addChild(1, tool2);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.15f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);
        testProxy.setCurrentPosition(1.0);

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

        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), layer, 0, 0, 0, 0);

        postProcessor.suppressUnnecessaryToolChanges(layer, lastLayerParseResult);

        assertEquals(2, layer.getChildren().size());
        assertFalse(tool1.isNodeOutputSuppressed());
        assertTrue(tool2.isNodeOutputSuppressed());
    }

    private ToolSelectNode setupToolNodeWithInnerAndOuterSquare()
    {
        ToolSelectNode tool1 = new ToolSelectNode();

        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();

        TravelNode travel1 = new TravelNode();
        travel1.getMovement().setX(0);
        travel1.getMovement().setY(0);

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getMovement().setX(10);
        extrusionNode1.getMovement().setY(0);
        extrusionNode1.getExtrusion().setE(0.1f);
        extrusionNode1.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getMovement().setX(10);
        extrusionNode2.getMovement().setY(10);
        extrusionNode2.getExtrusion().setE(0.1f);
        extrusionNode2.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getMovement().setX(0);
        extrusionNode3.getMovement().setY(10);
        extrusionNode3.getExtrusion().setE(0.1f);
        extrusionNode3.getFeedrate().setFeedRate_mmPerMin(10);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getMovement().setX(0);
        extrusionNode4.getMovement().setY(0);
        extrusionNode4.getExtrusion().setE(0.1f);
        extrusionNode4.getFeedrate().setFeedRate_mmPerMin(10);

        outer1.addChild(0, travel1);
        outer1.addChild(1, extrusionNode1);
        outer1.addChild(2, extrusionNode2);
        outer1.addChild(3, extrusionNode3);
        outer1.addChild(4, extrusionNode4);

        TravelNode travel2 = new TravelNode();
        travel2.getMovement().setX(1);
        travel2.getMovement().setY(1);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getMovement().setX(9);
        extrusionNode5.getMovement().setY(1);
        extrusionNode5.getExtrusion().setE(0.1f);
        extrusionNode5.getFeedrate().setFeedRate_mmPerMin(20);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getMovement().setX(9);
        extrusionNode6.getMovement().setY(9);
        extrusionNode6.getExtrusion().setE(0.1f);
        extrusionNode6.getFeedrate().setFeedRate_mmPerMin(20);

        ExtrusionNode extrusionNode7 = new ExtrusionNode();
        extrusionNode7.getMovement().setX(1);
        extrusionNode7.getMovement().setY(9);
        extrusionNode7.getExtrusion().setE(0.1f);
        extrusionNode7.getFeedrate().setFeedRate_mmPerMin(20);

        ExtrusionNode extrusionNode8 = new ExtrusionNode();
        extrusionNode8.getMovement().setX(1);
        extrusionNode8.getMovement().setY(1);
        extrusionNode8.getExtrusion().setE(0.1f);
        extrusionNode8.getFeedrate().setFeedRate_mmPerMin(20);

        inner1.addChild(0, travel2);
        inner1.addChild(1, extrusionNode5);
        inner1.addChild(2, extrusionNode6);
        inner1.addChild(3, extrusionNode7);
        inner1.addChild(4, extrusionNode8);

        tool1.addChild(0, inner1);
        tool1.addChild(1, outer1);

        return tool1;
    }

}
