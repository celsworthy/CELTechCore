package celtech.gcodetranslator.postprocessing;

import celtech.JavaFXConfiguredTest;
import celtech.appManager.Project;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.services.slicer.PrintQualityEnumeration;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class CloseLogicTest extends JavaFXConfiguredTest
{
    private double movementEpsilon = 0.001;
    private double nozzleEpsilon = 0.01;

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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseLogic closeLogic = new CloseLogic(testProject, ppFeatures);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(3, outer.getChildren().size());

        NozzleParameters testNozzleParameters = new NozzleParameters();
        testNozzleParameters.setOpenPosition(1.0f);
        testNozzleParameters.setClosedPosition(0);
        NozzleProxy testNozzle = new NozzleProxy(testNozzleParameters);

        closeLogic.insertNozzleCloseFullyAfterEvent(extrusionNode1, testNozzle);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(4, outer.getChildren().size());
        assertTrue(outer.getChildren().get(1) instanceof NozzleValvePositionNode);
        assertEquals(0.0, ((NozzleValvePositionNode) outer.getChildren().get(1)).getNozzlePosition().getB(), 0.0001);
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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseLogic closeLogic = new CloseLogic(testProject, ppFeatures);

        closeLogic.closeToEndOfFill(extrusionNode9, testProxy);

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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseLogic closeLogic = new CloseLogic(testProject, ppFeatures);

        closeLogic.closeToEndOfFill(extrusionNode9, testProxy);

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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseLogic closeLogic = new CloseLogic(testProject, ppFeatures);

        closeLogic.closeToEndOfFill(extrusionNode9, testProxy);

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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseLogic closeLogic = new CloseLogic(testProject, ppFeatures);

        closeLogic.closeToEndOfFill(extrusionNode9, testProxy);

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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseLogic closeLogic = new CloseLogic(testProject, ppFeatures);

        closeLogic.closeToEndOfFill(extrusionNode9, testProxy);

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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseLogic closeLogic = new CloseLogic(testProject, ppFeatures);

        closeLogic.addClosesUsingSpecifiedNode(tool1.getChildren().get(1).getChildren().get(4),
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
    public void testAddClosesUsingSpecifiedNode_backwardInFirstSegment()
    {
        ToolSelectNode tool1 = setupToolNodeWithInnerAndOuterSquare();

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.05f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);
        testProxy.setCurrentPosition(1.0); // The nozzle starts fully open

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseLogic closeLogic = new CloseLogic(testProject, ppFeatures);

        closeLogic.addClosesUsingSpecifiedNode(tool1.getChildren().get(1).getChildren().get(4),
                tool1.getChildren().get(0).getChildren().get(4),
                testProxy, false,
                0, false);

        //Should have elided the ejection volume
        assertEquals(0.05, testProxy.getElidedExtrusion(), 0.01);

        OuterPerimeterSectionNode outerResult = (OuterPerimeterSectionNode) tool1.getChildren().get(1);
        assertEquals(6, outerResult.getChildren().size());

        assertTrue(outerResult.getChildren().get(5) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult1 = (ExtrusionNode) outerResult.getChildren().get(5);
        assertEquals(1, extrusionResult1.getMovement().getX(), movementEpsilon);
        assertEquals(5, extrusionResult1.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult1.getExtrusion().isEInUse());
        assertEquals(0, extrusionResult1.getNozzlePosition().getB(), nozzleEpsilon);
        assertEquals(20, extrusionResult1.getFeedrate().getFeedRate_mmPerMin(), 0.01);
    }

    @Test
    public void testAddClosesUsingSpecifiedNode_overAvailableVolume()
    {
        ToolSelectNode tool1 = setupToolNodeWithInnerAndOuterSquare();

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(4f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);
        testProxy.setCurrentPosition(1.0); // The nozzle starts fully open

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseLogic closeLogic = new CloseLogic(testProject, ppFeatures);

        closeLogic.addClosesUsingSpecifiedNode(tool1.getChildren().get(1).getChildren().get(4),
                tool1.getChildren().get(0).getChildren().get(4),
                testProxy, false,
                0.3, true);

        //The elided volume should be equivalent to that of the nodes we copied (4 in this instance)
        assertEquals(4, testProxy.getElidedExtrusion(), 0.01);

        OuterPerimeterSectionNode outerResult = (OuterPerimeterSectionNode) tool1.getChildren().get(1);
        assertEquals(8, outerResult.getChildren().size());

        assertTrue(outerResult.getChildren().get(5) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult1 = (ExtrusionNode) outerResult.getChildren().get(5);
        assertEquals(1, extrusionResult1.getMovement().getX(), movementEpsilon);
        assertEquals(9, extrusionResult1.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult1.getExtrusion().isEInUse());
        assertEquals(0.67, extrusionResult1.getNozzlePosition().getB(), nozzleEpsilon);

        assertTrue(outerResult.getChildren().get(6) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult2 = (ExtrusionNode) outerResult.getChildren().get(6);
        assertEquals(9, extrusionResult2.getMovement().getX(), movementEpsilon);
        assertEquals(9, extrusionResult2.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult2.getExtrusion().isEInUse());
        assertEquals(0.33, extrusionResult2.getNozzlePosition().getB(), nozzleEpsilon);

        assertTrue(outerResult.getChildren().get(7) instanceof ExtrusionNode);
        ExtrusionNode extrusionResult3 = (ExtrusionNode) outerResult.getChildren().get(7);
        assertEquals(9, extrusionResult3.getMovement().getX(), movementEpsilon);
        assertEquals(1, extrusionResult3.getMovement().getY(), movementEpsilon);
        assertFalse(extrusionResult3.getExtrusion().isEInUse());
        assertEquals(0, extrusionResult3.getNozzlePosition().getB(), nozzleEpsilon);
    }

    @Test
    public void testCloseInwardFromOuterPerimeter()
    {
        ToolSelectNode tool1 = setupToolNodeWithInnerAndOuterSquare();

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(0.15f);

        NozzleProxy testProxy = new NozzleProxy(nozzleParams);
        testProxy.setCurrentPosition(1.0);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        CloseLogic closeLogic = new CloseLogic(testProject, ppFeatures);

        closeLogic.closeInwardFromOuterPerimeter(((ExtrusionNode) tool1.getChildren().get(1).getChildren().get(4)), testProxy);

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
