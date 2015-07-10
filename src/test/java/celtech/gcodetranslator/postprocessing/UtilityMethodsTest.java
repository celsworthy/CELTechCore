package celtech.gcodetranslator.postprocessing;

import celtech.JavaFXConfiguredTest;
import celtech.appManager.Project;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ReplenishNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;
import celtech.printerControl.model.Head.HeadType;
import celtech.services.slicer.PrintQualityEnumeration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class UtilityMethodsTest extends JavaFXConfiguredTest
{

    @Test
    public void testInsertOpenNodes()
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
        extrusionNode1.setCommentText("Ex1");

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(1f);
        extrusionNode2.setCommentText("Ex2");

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(1f);
        extrusionNode3.setCommentText("Ex3");

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

        tool1.addChildAtEnd(inner1);
        tool1.addChildAtEnd(outer1);

        tool2.addChildAtEnd(fill1);
        tool2.addChildAtEnd(fill2);

        tool3.addChildAtEnd(inner2);
        tool3.addChildAtEnd(outer2);

        inner1.addChildAtEnd(extrusionNode1);
        inner1.addChildAtEnd(extrusionNode2);
        inner1.addChildAtEnd(extrusionNode3);

        outer1.addChildAtEnd(extrusionNode4);
        outer1.addChildAtEnd(extrusionNode5);
        outer1.addChildAtEnd(extrusionNode6);

        fill1.addChildAtEnd(extrusionNode7);
        fill1.addChildAtEnd(extrusionNode8);
        fill1.addChildAtEnd(extrusionNode9);

        inner2.addChildAtEnd(extrusionNode10);
        inner2.addChildAtEnd(extrusionNode11);
        inner2.addChildAtEnd(extrusionNode12);

        outer2.addChildAtEnd(extrusionNode13);
        outer2.addChildAtEnd(extrusionNode14);
        outer2.addChildAtEnd(extrusionNode15);

        fill2.addChildAtEnd(extrusionNode16);
        fill2.addChildAtEnd(extrusionNode17);
        fill2.addChildAtEnd(extrusionNode18);

        testLayer.addChildAtEnd(tool1);
        testLayer.addChildAtEnd(tool2);
        testLayer.addChildAtEnd(tool3);

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
        //              ------------------------------------------------------    
        //              |                       |                            |
        //           tool(0)                  tool(1)                      tool(0)
        //              |                       |                            |
        //     ----------------            --------------             -----------------
        //     |              |            |            |             |               |
        //   inner1         outer1       fill1        fill2         inner2          outer2
        //     |              |            |            |             |               |
        //  ------------   -------   ------------   ---------    -----------    -------------
        //  |    |  |  |   |  |  |   |    |  |  |   |   |   |    |    |    |    |   |   |   |
        //  open e1 e2 e3  e4 e5 e6  open e7 e8 e9  e10 e11 e12  open e13 e14  e15  e16 e17 e18
        //
        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);
        ppFeatures.enableFeature(PostProcessorFeature.GRADUAL_CLOSE);

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

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        List<NozzleProxy> nozzleProxies = new ArrayList<>();
        for (int nozzleIndex = 0;
                nozzleIndex < testProject.getPrinterSettings().getSettings(HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters()
                .size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(testProject.getPrinterSettings().getSettings(HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        UtilityMethods utilityMethods = new UtilityMethods(ppFeatures, testProject, HeadType.SINGLE_MATERIAL_HEAD);
        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), testLayer, 0, 0, 0, 10);

        utilityMethods.insertOpenNodes(testLayer, lastLayerParseResult);

        OutputUtilities output = new OutputUtilities();
        output.outputNodes(testLayer, 0);

        assertEquals(3, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(1) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(2) instanceof ToolSelectNode);

        assertEquals(4, inner1.getChildren().size());
        assertEquals(3, outer1.getChildren().size());
        assertEquals(4, fill1.getChildren().size());
        assertEquals(3, fill2.getChildren().size());
        assertEquals(4, inner2.getChildren().size());
        assertEquals(3, outer2.getChildren().size());

        assertTrue(inner1.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) inner1.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);

        assertTrue(fill1.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) fill1.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);

        assertTrue(inner2.getChildren().get(0) instanceof NozzleValvePositionNode);
        assertEquals(1.0, ((NozzleValvePositionNode) inner2.getChildren().get(0)).getNozzlePosition().getB(), 0.0001);
    }

    @Test
    public void testInsertCloseNodes()
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
        extrusionNode1.setCommentText("Ex1");

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(1f);
        extrusionNode2.setCommentText("Ex2");

        ExtrusionNode extrusionNode3 = new ExtrusionNode();
        extrusionNode3.getExtrusion().setE(1f);
        extrusionNode3.setCommentText("Ex3");

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

        tool1.addChildAtEnd(inner1);
        tool1.addChildAtEnd(outer1);

        tool2.addChildAtEnd(fill1);
        tool2.addChildAtEnd(fill2);

        tool3.addChildAtEnd(inner2);
        tool3.addChildAtEnd(outer2);

        inner1.addChildAtEnd(extrusionNode1);
        inner1.addChildAtEnd(extrusionNode2);
        inner1.addChildAtEnd(extrusionNode3);

        outer1.addChildAtEnd(extrusionNode4);
        outer1.addChildAtEnd(extrusionNode5);
        outer1.addChildAtEnd(extrusionNode6);

        fill1.addChildAtEnd(extrusionNode7);
        fill1.addChildAtEnd(extrusionNode8);
        fill1.addChildAtEnd(extrusionNode9);

        inner2.addChildAtEnd(extrusionNode10);
        inner2.addChildAtEnd(extrusionNode11);
        inner2.addChildAtEnd(extrusionNode12);

        outer2.addChildAtEnd(extrusionNode13);
        outer2.addChildAtEnd(extrusionNode14);
        outer2.addChildAtEnd(extrusionNode15);

        fill2.addChildAtEnd(extrusionNode16);
        fill2.addChildAtEnd(extrusionNode17);
        fill2.addChildAtEnd(extrusionNode18);

        testLayer.addChildAtEnd(tool1);
        testLayer.addChildAtEnd(tool2);
        testLayer.addChildAtEnd(tool3);

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
        //              --------------------------------------------------------    
        //              |                       |                              |
        //           tool(0)                  tool(1)                        tool(0)
        //              |                       |                              |
        //     --------------            --------------                -----------------
        //     |            |            |            |                |               |
        //   inner1       outer1       fill1        fill2            inner2          outer2
        //     |            |            |            |                |               |
        //  -------   ------------    -------   --------------     ----------   --------------
        //  |  |  |   |  |  |    |    |  |  |   |   |   |    |     |    |   |   |    |   |   |
        //  e1 e2 e3  e4 e5 e6 close  e7 e8 e9  e10 e11 e12 close  e13 e14 e15  e16 e17 e18 close
        HeadFile singleMaterialHead = HeadContainer.getHeadByID("RBX01-SM");

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);
        ppFeatures.enableFeature(PostProcessorFeature.GRADUAL_CLOSE);

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

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        List<NozzleProxy> nozzleProxies = new ArrayList<>();
        for (int nozzleIndex = 0;
                nozzleIndex < testProject.getPrinterSettings().getSettings(HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters()
                .size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(testProject.getPrinterSettings().getSettings(HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        UtilityMethods utilityMethods = new UtilityMethods(ppFeatures, testProject, HeadType.SINGLE_MATERIAL_HEAD);
        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), testLayer, 0, 0, 0, 10);

        utilityMethods.suppressUnnecessaryToolChangesAndInsertToolchangeCloses(testLayer, lastLayerParseResult, nozzleProxies);

        assertEquals(3, testLayer.getChildren().size());
        assertTrue(testLayer.getChildren().get(0) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(1) instanceof ToolSelectNode);
        assertTrue(testLayer.getChildren().get(2) instanceof ToolSelectNode);

        assertEquals(3, inner1.getChildren().size());
        assertEquals(4, outer1.getChildren().size());
        assertEquals(3, fill1.getChildren().size());
        assertEquals(3, fill2.getChildren().size());
        assertEquals(3, inner2.getChildren().size());
        assertEquals(4, outer2.getChildren().size());

        assertTrue(outer1.getChildren().get(3) instanceof NozzlePositionProvider);
        assertEquals(0.0, ((NozzlePositionProvider) outer1.getChildren().get(3)).getNozzlePosition().getB(), 0.0001);

        assertTrue(fill2.getChildren().get(2) instanceof NozzlePositionProvider);
        assertEquals(0.0, ((NozzlePositionProvider) fill2.getChildren().get(2)).getNozzlePosition().getB(), 0.0001);

        assertTrue(outer2.getChildren().get(3) instanceof NozzlePositionProvider);
        assertEquals(0.0, ((NozzlePositionProvider) outer2.getChildren().get(3)).getNozzlePosition().getB(), 0.0001);
    }

    @Test
    public void testSuppressUnnecessaryToolChangesAndInsertToolchangeCloses()
    {
        ToolSelectNode tool1 = new ToolSelectNode();
        tool1.setToolNumber(0);

        InnerPerimeterSectionNode inner1 = new InnerPerimeterSectionNode();
        OuterPerimeterSectionNode outer1 = new OuterPerimeterSectionNode();
        tool1.addChildAtEnd(inner1);
        tool1.addChildAtEnd(outer1);

        ToolSelectNode tool2 = new ToolSelectNode();
        tool2.setToolNumber(0);

        InnerPerimeterSectionNode inner2 = new InnerPerimeterSectionNode();
        OuterPerimeterSectionNode outer2 = new OuterPerimeterSectionNode();
        tool2.addChildAtEnd(inner2);
        tool2.addChildAtEnd(outer2);

        ToolSelectNode tool3 = new ToolSelectNode();
        tool3.setToolNumber(1);

        InnerPerimeterSectionNode inner3 = new InnerPerimeterSectionNode();
        OuterPerimeterSectionNode outer3 = new OuterPerimeterSectionNode();
        tool3.addChildAtEnd(inner3);
        tool3.addChildAtEnd(outer3);

        ExtrusionNode extrusionNode1 = new ExtrusionNode();
        extrusionNode1.getExtrusion().setE(1f);

        ExtrusionNode extrusionNode2 = new ExtrusionNode();
        extrusionNode2.getExtrusion().setE(1f);

        outer3.addChildAtEnd(extrusionNode1);
        outer3.addChildAtEnd(extrusionNode2);

        LayerNode layer = new LayerNode();

        layer.addChildAtEnd(tool1);
        layer.addChildAtEnd(tool2);
        layer.addChildAtEnd(tool3);

        NozzleParameters nozzleParams = new NozzleParameters();
        nozzleParams.setEjectionVolume(1f);

        Optional<NozzleProxy> testProxy = Optional.of(new NozzleProxy(nozzleParams));
        testProxy.get().setCurrentPosition(1.0);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);
        ppFeatures.enableFeature(PostProcessorFeature.GRADUAL_CLOSE);

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        List<NozzleProxy> nozzleProxies = new ArrayList<>();
        for (int nozzleIndex = 0;
                nozzleIndex < testProject.getPrinterSettings().getSettings(HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters()
                .size(); nozzleIndex++)
        {
            NozzleProxy proxy = new NozzleProxy(testProject.getPrinterSettings().getSettings(HeadType.SINGLE_MATERIAL_HEAD).getNozzleParameters().get(nozzleIndex));
            proxy.setNozzleReferenceNumber(nozzleIndex);
            nozzleProxies.add(proxy);
        }

        UtilityMethods utilityMethods = new UtilityMethods(ppFeatures, testProject, HeadType.SINGLE_MATERIAL_HEAD);
        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(testProxy, layer, 0, 0, 0, 10);

        utilityMethods.suppressUnnecessaryToolChangesAndInsertToolchangeCloses(layer, lastLayerParseResult, nozzleProxies);

        assertEquals(3, layer.getChildren().size());
        assertFalse(tool1.isNodeOutputSuppressed());
        assertTrue(tool2.isNodeOutputSuppressed());
        assertFalse(tool3.isNodeOutputSuppressed());

        assertTrue(extrusionNode2.getNozzlePosition().isBSet());
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

        outer.addChildAtEnd(extrusionNode1);
        outer.addChildAtEnd(extrusionNode2);
        outer.addChildAtEnd(extrusionNode3);

        testLayer.addChildAtEnd(inner);
        testLayer.addChildAtEnd(outer);
        testLayer.addChildAtEnd(fill);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(3, outer.getChildren().size());

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        UtilityMethods utilityMethods = new UtilityMethods(ppFeatures, testProject, HeadType.SINGLE_MATERIAL_HEAD);
        utilityMethods.insertNozzleOpenFullyBeforeEvent(extrusionNode1);

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

        outer.addChildAtEnd(extrusionNode1);
        outer.addChildAtEnd(extrusionNode2);
        outer.addChildAtEnd(extrusionNode3);

        testLayer.addChildAtEnd(inner);
        testLayer.addChildAtEnd(outer);
        testLayer.addChildAtEnd(fill);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);
        ppFeatures.enableFeature(PostProcessorFeature.REPLENISH_BEFORE_OPEN);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(3, outer.getChildren().size());

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        UtilityMethods utilityMethods = new UtilityMethods(ppFeatures, testProject, HeadType.SINGLE_MATERIAL_HEAD);
        utilityMethods.insertNozzleOpenFullyBeforeEvent(extrusionNode1);

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

        outer.addChildAtEnd(extrusionNode1);
        outer.addChildAtEnd(extrusionNode2);
        outer.addChildAtEnd(extrusionNode3);

        testLayer.addChildAtEnd(inner);
        testLayer.addChildAtEnd(outer);
        testLayer.addChildAtEnd(fill);

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);
        ppFeatures.enableFeature(PostProcessorFeature.REPLENISH_BEFORE_OPEN);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(3, outer.getChildren().size());

        Project testProject = new Project();
        testProject.getPrinterSettings().setSettingsName("BothNozzles");
        testProject.setPrintQuality(PrintQualityEnumeration.CUSTOM);

        UtilityMethods utilityMethods = new UtilityMethods(ppFeatures, testProject, HeadType.SINGLE_MATERIAL_HEAD);
        extrusionNode1.setElidedExtrusion(0.4);
        utilityMethods.insertNozzleOpenFullyBeforeEvent(extrusionNode1);
        OutputUtilities output = new OutputUtilities();
        output.outputNodes(testLayer, 0);

        assertEquals(3, testLayer.getChildren().size());
        assertEquals(5, outer.getChildren().size());
        assertTrue(outer.getChildren().get(0) instanceof ReplenishNode);
        assertTrue(outer.getChildren().get(1) instanceof NozzleValvePositionNode);
        assertEquals(0.4, ((ReplenishNode) outer.getChildren().get(0)).getExtrusion().getE(), 0.0001);
        assertEquals(1.0, ((NozzleValvePositionNode) outer.getChildren().get(1)).getNozzlePosition().getB(), 0.0001);
    }
}
