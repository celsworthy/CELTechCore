package celtech.gcodetranslator.postprocessing.verifier;

import celtech.gcodetranslator.postprocessing.LayerPostProcessResult;
import celtech.gcodetranslator.postprocessing.helpers.LayerDefinition;
import celtech.gcodetranslator.postprocessing.helpers.TestDataGenerator;
import celtech.gcodetranslator.postprocessing.helpers.ToolDefinition;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.printerControl.model.Head.HeadType;
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
public class OutputVerifierTest
{

    public OutputVerifierTest()
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
     * Test of verifyAllLayers method, of class OutputVerifier.
     */
    @Test
    public void testVerifyAllLayers_noNozzleOpen()
    {
        System.out.println("verifyAllLayers");

        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 5),
            new ToolDefinition(1, 500)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = TestDataGenerator.generateLayerResults(layers);

        OutputVerifier instance = new OutputVerifier();
        List<VerifierResult> verifierResults = instance.verifyAllLayers(allLayerPostProcessResults, HeadType.DUAL_MATERIAL_HEAD);

        assertEquals(0, verifierResults.size());
    }

    /**
     * Test of verifyAllLayers method, of class OutputVerifier.
     */
    @Test
    public void testVerifyAllLayers_allGood()
    {
        System.out.println("verifyAllLayers");

        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 5),
            new ToolDefinition(1, 500)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = TestDataGenerator.generateLayerResults(layers);

        NozzleValvePositionNode openNozzle = new NozzleValvePositionNode();
        openNozzle.getNozzlePosition().setB(1.0);
        allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0).addChildAtStart(openNozzle);

        OutputVerifier instance = new OutputVerifier();
        List<VerifierResult> verifierResults = instance.verifyAllLayers(allLayerPostProcessResults, HeadType.DUAL_MATERIAL_HEAD);

        assertEquals(0, verifierResults.size());
    }

    /**
     * Test of verifyAllLayers method, of class OutputVerifier.
     */
    @Test
    public void testVerifyAllLayers_heaterOnOff()
    {
        System.out.println("heaterOnOff");

        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 5),
            new ToolDefinition(1, 500),
            new ToolDefinition(0, 500)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = TestDataGenerator.generateLayerResults(layers);

        NozzleValvePositionNode openNozzle = new NozzleValvePositionNode();
        openNozzle.getNozzlePosition().setB(1.0);
        allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0).addChildAtStart(openNozzle);

        MCodeNode switchOffNozzle0 = new MCodeNode();
        switchOffNozzle0.setMNumber(104);
        switchOffNozzle0.setSNumber(0);
        allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0).addChildAtEnd(switchOffNozzle0);

        MCodeNode heatNozzle0 = new MCodeNode();
        heatNozzle0.setMNumber(104);
        heatNozzle0.setSOnly(true);
        allLayerPostProcessResults.get(0).getLayerData().getChildren().get(2).addChildAtStart(heatNozzle0);

        OutputVerifier instance = new OutputVerifier();
        List<VerifierResult> verifierResults = instance.verifyAllLayers(allLayerPostProcessResults, HeadType.DUAL_MATERIAL_HEAD);

        assertEquals(0, verifierResults.size());
    }

    /**
     * Test of verifyAllLayers method, of class OutputVerifier.
     */
    @Test
    public void testVerifyAllLayers_nozzleCloseInExtrusion()
    {
        System.out.println("nozzleCloseInExtrusion");

        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 5),
            new ToolDefinition(1, 500)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = TestDataGenerator.generateLayerResults(layers);

        NozzleValvePositionNode openNozzle = new NozzleValvePositionNode();
        openNozzle.getNozzlePosition().setB(1.0);
        allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0).addChildAtStart(openNozzle);

        ExtrusionNode extrusionToOperateOn = ((ExtrusionNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0).getAbsolutelyTheLastEvent());
        extrusionToOperateOn.getExtrusion().dNotInUse();
        extrusionToOperateOn.getExtrusion().eNotInUse();
        extrusionToOperateOn.getNozzlePosition().setB(0);

        NozzleValvePositionNode openNozzle2 = new NozzleValvePositionNode();
        openNozzle2.getNozzlePosition().setB(1.0);
        allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).addChildAtStart(openNozzle2);

        OutputVerifier instance = new OutputVerifier();
        List<VerifierResult> verifierResults = instance.verifyAllLayers(allLayerPostProcessResults, HeadType.DUAL_MATERIAL_HEAD);

        assertEquals(0, verifierResults.size());
    }

}
