package celtech.gcodetranslator.postprocessing;

import celtech.JavaFXConfiguredTest;
import celtech.appManager.Project;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.services.slicer.PrintQualityEnumeration;
import java.util.Optional;
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
public class NodeManagementUtilitiesTest extends JavaFXConfiguredTest
{

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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        assertEquals(3, testLayer.getChildren().size());

        nodeManagementUtilities.removeUnretractNodes(testLayer);

        assertEquals(0, testLayer.getChildren().size());
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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);
        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), testLayer, 0, 0, 0, 0);

        assertEquals(2, testLayer.getChildren().size());
        assertSame(outer, orphan1.getChildren().get(0));

        nodeManagementUtilities.rehomeOrphanObjects(testLayer, lastLayerParseResult);

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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);
        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), testLayer, 0, 0, 0, 10);

        assertEquals(2, testLayer.getChildren().size());
        assertSame(outer, orphan1.getChildren().get(0));

        nodeManagementUtilities.rehomeOrphanObjects(testLayer, lastLayerParseResult);

        assertEquals(2, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ObjectDelineationNode);
        ObjectDelineationNode resultNode = (ObjectDelineationNode) testLayer.getChildren().get(0);
        assertEquals(10, resultNode.getObjectNumber());

        assertSame(outer, resultNode.getChildren().get(0));

        assertSame(object1, testLayer.getChildren().get(1));
        assertSame(fill, object1.getChildren().get(0));
    }

    @Test
    public void testFindAvailableExtrusion_forwards()
    {
        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(6.0f);

        FillSectionNode fill1 = new FillSectionNode();
        fill1.addChildAtEnd(extrusionNode1);
        fill1.addChildAtEnd(extrusionNode2);
        fill1.addChildAtEnd(extrusionNode3);
        fill1.addChildAtEnd(extrusionNode4);
        fill1.addChildAtEnd(extrusionNode5);
        fill1.addChildAtEnd(extrusionNode6);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        try
        {
            double result1 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode1, true);
            assertEquals(21, result1, 0.1);

            double result2 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode4, true);
            assertEquals(15, result2, 0.1);

            double result3 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode6, true);
            assertEquals(6, result3, 0.1);
        } catch (NodeProcessingException ex)
        {
            fail("Got exception during test " + ex);
        }
    }

    @Test
    public void testFindAvailableExtrusion_backwards()
    {
        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1.0f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(2.0f);

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(3.0f);

        ExtrusionNode extrusionNode4 = new ExtrusionNode();
        extrusionNode4.getExtrusion().setE(4.0f);

        ExtrusionNode extrusionNode5 = new ExtrusionNode();
        extrusionNode5.getExtrusion().setE(5.0f);

        ExtrusionNode extrusionNode6 = new ExtrusionNode();
        extrusionNode6.getExtrusion().setE(6.0f);

        FillSectionNode fill1 = new FillSectionNode();
        fill1.addChildAtEnd(extrusionNode1);
        fill1.addChildAtEnd(extrusionNode2);
        fill1.addChildAtEnd(extrusionNode3);
        fill1.addChildAtEnd(extrusionNode4);
        fill1.addChildAtEnd(extrusionNode5);
        fill1.addChildAtEnd(extrusionNode6);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        NodeManagementUtilities nodeManagementUtilities = new NodeManagementUtilities(ppFeatures);

        try
        {
            double result1 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode1, false);
            assertEquals(1, result1, 0.1);

            double result2 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode4, false);
            assertEquals(10, result2, 0.1);

            double result3 = nodeManagementUtilities.findAvailableExtrusion(extrusionNode6, false);
            assertEquals(21, result3, 0.1);
        } catch (NodeProcessingException ex)
        {
            fail("Got exception during test " + ex);
        }
    }

}
