package celtech.gcodetranslator.postprocessing;

import celtech.JavaFXConfiguredTest;
import celtech.TestUtils;
import celtech.appManager.Project;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.modelcontrol.ModelContainer;
import celtech.services.slicer.PrintQualityEnumeration;
import java.util.ArrayList;
import java.util.List;
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
public class NozzleAssignmentUtilitiesTest extends JavaFXConfiguredTest
{

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

        List<NozzleProxy> nozzleProxies = new ArrayList<>();

        for (int nozzleIndex = 0;
                nozzleIndex < testProject.getPrinterSettings().getSettings(SlicerParametersFile.HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters()
                .size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(testProject.getPrinterSettings().getSettings(SlicerParametersFile.HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        NozzleAssignmentUtilities assignmentUtilities = new NozzleAssignmentUtilities(
                nozzleProxies,
                testProject.getPrinterSettings().getSettings(SlicerParametersFile.HeadType.SINGLE_MATERIAL_HEAD),
                singleMaterialHead,
                ppFeatures,
                testProject,
                PostProcessingMode.USE_OBJECT_MATERIAL);

        assertEquals(3, testLayer.getChildren().size());

        assignmentUtilities.assignExtrusionToCorrectExtruder(testLayer);

        assertEquals(3, testLayer.getChildren().size());

        assertFalse(((ExtrusionProvider) (outer2.getChildren().get(0))).getExtrusion().isEInUse());
        assertTrue(((ExtrusionProvider) (outer2.getChildren().get(0))).getExtrusion().isDInUse());

        assertTrue(((ExtrusionProvider) (fill1.getChildren().get(0))).getExtrusion().isEInUse());
        assertFalse(((ExtrusionProvider) (fill1.getChildren().get(0))).getExtrusion().isDInUse());

        assertFalse(((ExtrusionProvider) (outer1.getChildren().get(0))).getExtrusion().isEInUse());
        assertTrue(((ExtrusionProvider) (outer1.getChildren().get(0))).getExtrusion().isDInUse());
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

        List<NozzleProxy> nozzleProxies = new ArrayList<>();

        for (int nozzleIndex = 0;
                nozzleIndex < testProject.getPrinterSettings().getSettings(SlicerParametersFile.HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters()
                .size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(testProject.getPrinterSettings().getSettings(SlicerParametersFile.HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        TestUtils utils = new TestUtils();

        ModelContainer modelContainer1 = utils.makeModelContainer(true);
        testProject.addModel(modelContainer1);

        ModelContainer modelContainer2 = utils.makeModelContainer(false);
        testProject.addModel(modelContainer2);

        NozzleAssignmentUtilities assignmentUtilities = new NozzleAssignmentUtilities(
                nozzleProxies,
                testProject.getPrinterSettings().getSettings(SlicerParametersFile.HeadType.SINGLE_MATERIAL_HEAD),
                dualMaterialHead,
                ppFeatures,
                testProject,
                PostProcessingMode.USE_OBJECT_MATERIAL);

        assertEquals(2, testLayer.getChildren().size());
        assertEquals(3, object1.getChildren().size());
        assertEquals(3, object2.getChildren().size());
        assertEquals(3, inner1.getChildren().size());
        assertEquals(3, outer1.getChildren().size());
        assertEquals(3, fill1.getChildren().size());
        assertEquals(3, inner2.getChildren().size());
        assertEquals(3, outer2.getChildren().size());
        assertEquals(3, fill2.getChildren().size());

        int lastObjectNumber = assignmentUtilities.insertNozzleControlSectionsByObject(testLayer);

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

        List<NozzleProxy> nozzleProxies = new ArrayList<>();

        for (int nozzleIndex = 0;
                nozzleIndex < testProject.getPrinterSettings().getSettings(SlicerParametersFile.HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters()
                .size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(testProject.getPrinterSettings().getSettings(SlicerParametersFile.HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        NozzleAssignmentUtilities assignmentUtilities = new NozzleAssignmentUtilities(
                nozzleProxies,
                testProject.getPrinterSettings().getSettings(SlicerParametersFile.HeadType.SINGLE_MATERIAL_HEAD),
                singleMaterialHead,
                ppFeatures,
                testProject,
                PostProcessingMode.TASK_BASED_NOZZLE_SELECTION);

        assertEquals(2, testLayer.getChildren().size());
        assertEquals(3, object1.getChildren().size());
        assertEquals(3, object2.getChildren().size());
        assertEquals(3, inner1.getChildren().size());
        assertEquals(3, outer1.getChildren().size());
        assertEquals(3, fill1.getChildren().size());
        assertEquals(3, inner2.getChildren().size());
        assertEquals(3, outer2.getChildren().size());
        assertEquals(3, fill2.getChildren().size());

        int lastObjectNumber = assignmentUtilities.insertNozzleControlSectionsByTask(testLayer);

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
}
