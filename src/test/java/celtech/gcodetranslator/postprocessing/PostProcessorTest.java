package celtech.gcodetranslator.postprocessing;

import celtech.JavaFXConfiguredTest;
import celtech.appManager.Project;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.printerControl.model.Head;
import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
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
        Head singleMaterialHead = new Head(HeadContainer.getHeadByID("RBX01-SM"));
        Project testProject = new Project();
        PostProcessor instance = new PostProcessor(inputFilename,
                outputFilename,
                singleMaterialHead,
                SlicerParametersContainer.getSettingsByProfileName("Draft_1"),
                testProject);
        boolean success = instance.processInput();
        assertTrue(success);
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

        Head singleMaterialHead = new Head(HeadContainer.getHeadByID("RBX01-SM"));
        Project testProject = new Project();
        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                SlicerParametersContainer.getSettingsByProfileName("Draft_1"),
                testProject);

        assertEquals(3, testLayer.getChildren().size());

        postProcessor.removeUnretractNodes(testLayer);

        assertEquals(0, testLayer.getChildren().size());
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

        Head singleMaterialHead = new Head(HeadContainer.getHeadByID("RBX01-SM"));
        Project testProject = new Project();
        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                SlicerParametersContainer.getSettingsByProfileName("Draft_1"),
                testProject);

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

        Head singleMaterialHead = new Head(HeadContainer.getHeadByID("RBX01-SM"));
        Project testProject = new Project();
        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                SlicerParametersContainer.getSettingsByProfileName("Draft_1"),
                testProject);

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
        object1.setObjectNumber(1);
        ObjectDelineationNode object2 = new ObjectDelineationNode();
        object2.setObjectNumber(2);

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
        Head singleMaterialHead = new Head(HeadContainer.getHeadByID("RBX01-SM"));
        Project testProject = new Project();
        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                SlicerParametersContainer.getSettingsByProfileName("Draft_1"),
                testProject);

        assertEquals(2, testLayer.getChildren().size());
        assertEquals(3, object1.getChildren().size());
        assertEquals(3, object2.getChildren().size());
        assertEquals(3, inner1.getChildren().size());
        assertEquals(3, outer1.getChildren().size());
        assertEquals(3, fill1.getChildren().size());
        assertEquals(3, inner2.getChildren().size());
        assertEquals(3, outer2.getChildren().size());
        assertEquals(3, fill2.getChildren().size());

        postProcessor.insertNozzleControlSectionsByTask(testLayer);

//        postProcessor.outputNodes(testLayer, 0);
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
        Head singleMaterialHead = new Head(HeadContainer.getHeadByID("RBX01-SM"));
        Project testProject = new Project();
        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                SlicerParametersContainer.getSettingsByProfileName("Draft_1"),
                testProject);

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

        LayerParseResult lastLayerParseResult = null;

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
        Head singleMaterialHead = new Head(HeadContainer.getHeadByID("RBX01-SM"));
        Project testProject = new Project();
        PostProcessor postProcessor = new PostProcessor("",
                "",
                singleMaterialHead,
                SlicerParametersContainer.getSettingsByProfileName("Draft_1"),
                testProject);

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

        LayerParseResult lastLayerParseResult = null;

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
}
