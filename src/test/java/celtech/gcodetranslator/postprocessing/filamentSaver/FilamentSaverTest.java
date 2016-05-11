package celtech.gcodetranslator.postprocessing.filamentSaver;

import celtech.gcodetranslator.postprocessing.filamentSaver.FilamentSaver;
import celtech.gcodetranslator.postprocessing.LayerPostProcessResult;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
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
public class FilamentSaverTest
{

    public FilamentSaverTest()
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

    private class ToolDefinition
    {

        private final int toolNumber;
        private final double duration;

        public ToolDefinition(int toolNumber, double duration)
        {
            this.toolNumber = toolNumber;
            this.duration = duration;
        }

        public int getToolNumber()
        {
            return toolNumber;
        }

        public double getDuration()
        {
            return duration;
        }

    }

    private class LayerDefinition
    {

        private final int layerNumber;
        private final ToolDefinition[] tools;

        public LayerDefinition(int layerNumber, ToolDefinition[] tools)
        {
            this.layerNumber = layerNumber;
            this.tools = tools;
        }

        public int getLayerNumber()
        {
            return layerNumber;
        }

        public ToolDefinition[] getTools()
        {
            return tools;
        }
    }

    /**
     * Test of saveHeaters method, of class FilamentSaver.
     */
    @Test
    public void testSetup()
    {
        System.out.println("saveHeaters");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 5),
            new ToolDefinition(1, 500)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = generateLayerResults(layers);

        assertEquals(1, allLayerPostProcessResults.size());

        assertEquals(505, allLayerPostProcessResults.get(0).getLayerData().getFinishTimeFromStartOfPrint_secs().get(), 0.0001);

        assertEquals(5, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0).getFinishTimeFromStartOfPrint_secs().get(), 0.0001);
        assertEquals(5, ((ToolSelectNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0)).getEstimatedDuration(), 0.0001);
        assertEquals(1, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0).getChildren().size());
        assertEquals(5, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0).getChildren().get(0).getFinishTimeFromStartOfPrint_secs().get(), 0.001);

        assertEquals(505, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getFinishTimeFromStartOfPrint_secs().get(), 0.0001);
        assertEquals(500, ((ToolSelectNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1)).getEstimatedDuration(), 0.0001);
        assertEquals(34, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().size());
        assertEquals(505, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(33).getFinishTimeFromStartOfPrint_secs().get(), 0.001);
    }

    /**
     * Test of saveHeaters method, of class FilamentSaver.
     */
    @Test
    public void testSaveHeaters_switch_off_simple()
    {
        System.out.println("switch_off_simple");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 5),
            new ToolDefinition(1, 500)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = generateLayerResults(layers);
        FilamentSaver instance = new FilamentSaver();
        instance.saveHeaters(allLayerPostProcessResults);

        assertEquals(1, allLayerPostProcessResults.size());
        assertEquals(3, allLayerPostProcessResults.get(0).getLayerData().getChildren().size());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1)).getMNumber());
        assertEquals(0, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1)).getSNumber());
    }

    /**
     * Test of saveHeaters method, of class FilamentSaver.
     */
    @Test
    public void testSaveHeaters_switch_off_simple2()
    {
        //Test that multiple tools times are added to switch of the other heater
        System.out.println("switch_off_simple2");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 5),
            new ToolDefinition(1, 10),
            new ToolDefinition(1, 15),
            new ToolDefinition(1, 14),
            new ToolDefinition(1, 16),
            new ToolDefinition(1, 150)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = generateLayerResults(layers);
        FilamentSaver instance = new FilamentSaver();
        instance.saveHeaters(allLayerPostProcessResults);

        assertEquals(1, allLayerPostProcessResults.size());
        assertEquals(7, allLayerPostProcessResults.get(0).getLayerData().getChildren().size());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1)).getMNumber());
        assertEquals(0, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1)).getSNumber());
    }

    /**
     * Test of saveHeaters method, of class FilamentSaver.
     */
    @Test
    public void testSaveHeaters_switch_off_simple3()
    {
        //Test that we can switch off in an earlier layer
        System.out.println("switch_off_simple3");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 5),
            new ToolDefinition(1, 10),
            new ToolDefinition(1, 15)
        }));
        layers.add(new LayerDefinition(1, new ToolDefinition[]
        {
            new ToolDefinition(1, 14),
            new ToolDefinition(1, 16),
            new ToolDefinition(1, 150)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = generateLayerResults(layers);
        FilamentSaver instance = new FilamentSaver();
        instance.saveHeaters(allLayerPostProcessResults);

        assertEquals(2, allLayerPostProcessResults.size());
        assertEquals(4, allLayerPostProcessResults.get(0).getLayerData().getChildren().size());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1)).getMNumber());
        assertEquals(0, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1)).getSNumber());
    }

    /**
     * Test of saveHeaters method, of class FilamentSaver.
     */
    @Test
    public void testSaveHeaters_switch_off_complex1()
    {
        //Test that we switch off at the right point when multiple uses of the first heater are present
        System.out.println("switch_off_complex1");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 5),
            new ToolDefinition(1, 10),
            new ToolDefinition(0, 6),
            new ToolDefinition(1, 45)
        }));
        layers.add(new LayerDefinition(1, new ToolDefinition[]
        {
            new ToolDefinition(1, 35),
            new ToolDefinition(1, 25),
            new ToolDefinition(1, 150)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = generateLayerResults(layers);
        FilamentSaver instance = new FilamentSaver();
        instance.saveHeaters(allLayerPostProcessResults);

        assertEquals(2, allLayerPostProcessResults.size());
        assertEquals(5, allLayerPostProcessResults.get(0).getLayerData().getChildren().size());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(3) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(3)).getMNumber());
        assertEquals(0, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(3)).getSNumber());
    }

    /**
     * Test of saveHeaters method, of class FilamentSaver.
     */
    @Test
    public void testSaveHeaters_switch_off_complex2()
    {
        //Test that we switch off at the right point when multiple uses of the first heater are present
        System.out.println("switch_off_complex1");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            //Inserts: M104 T0
            new ToolDefinition(0, 260),
            //Inserts: M104 T
            new ToolDefinition(1, 10),
            new ToolDefinition(0, 6),
            //Inserts: M104 S0
            new ToolDefinition(1, 45)
        }));
        layers.add(new LayerDefinition(1, new ToolDefinition[]
        {
            new ToolDefinition(1, 35),
            new ToolDefinition(1, 25),
            new ToolDefinition(1, 150)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = generateLayerResults(layers);
        FilamentSaver instance = new FilamentSaver();
        instance.saveHeaters(allLayerPostProcessResults);

        assertEquals(2, allLayerPostProcessResults.size());

        assertEquals(6, allLayerPostProcessResults.get(0).getLayerData().getChildren().size());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0)).isTAndNumber());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(3) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(3)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(3)).isTOnly());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4)).isSAndNumber());
    }

    @Test
    public void testSaveHeaters_switch_on_simple1()
    {
        //Test that we switch on at the right point
        System.out.println("switch_on_simple1");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            //Inserts: M104 T0
            new ToolDefinition(0, 260),
                //Inserts: M104 T in T0 section
            new ToolDefinition(1, 10),
            new ToolDefinition(0, 6),
            //Inserts: M104 S0
            new ToolDefinition(1, 45)
        }));
        layers.add(new LayerDefinition(1, new ToolDefinition[]
        {
            new ToolDefinition(1, 35),
            new ToolDefinition(1, 25),
            new ToolDefinition(1, 150)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = generateLayerResults(layers);
        FilamentSaver instance = new FilamentSaver();
        instance.saveHeaters(allLayerPostProcessResults);

        assertEquals(2, allLayerPostProcessResults.size());
        assertEquals(6, allLayerPostProcessResults.get(0).getLayerData().getChildren().size());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0)).isTAndNumber());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1) instanceof ToolSelectNode);
        assertEquals(19, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().size());
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(3)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(3)).isTOnly());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4)).isSAndNumber());

        assertEquals(3, allLayerPostProcessResults.get(1).getLayerData().getChildren().size());
    }

    @Test
    public void testSaveHeaters_switch_on_complex2()
    {
        //Test that we switch on at the right point even if it is in the previous layer...
        System.out.println("switch_on_complex2");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            //Inserts: M104 T0
            new ToolDefinition(0, 260),
            //Inserts: M104 T
            new ToolDefinition(1, 10),
            new ToolDefinition(0, 6),
            //Inserts: M104 S0
            new ToolDefinition(1, 450)
        //Inserts: M104 S
        }));
        layers.add(new LayerDefinition(1, new ToolDefinition[]
        {
            new ToolDefinition(1, 50),
            new ToolDefinition(0, 24),
            //Inserts: M104 S0
            new ToolDefinition(1, 75),
            new ToolDefinition(1, 150)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = generateLayerResults(layers);
        FilamentSaver instance = new FilamentSaver();
        instance.saveHeaters(allLayerPostProcessResults);

        assertEquals(2, allLayerPostProcessResults.size());
        assertEquals(6, allLayerPostProcessResults.get(0).getLayerData().getChildren().size());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0)).isTAndNumber());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1) instanceof ToolSelectNode);
        assertEquals(19, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().size());
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(3)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(3)).isTOnly());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4)).isSAndNumber());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(5).getChildren().get(18) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(5).getChildren().get(18)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(5).getChildren().get(18)).isSOnly());

        assertEquals(5, allLayerPostProcessResults.get(1).getLayerData().getChildren().size());

        assertTrue(allLayerPostProcessResults.get(1).getLayerData().getChildren().get(2) instanceof MCodeNode);
        assertEquals(104, ((MCodeNode) allLayerPostProcessResults.get(1).getLayerData().getChildren().get(2)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(1).getLayerData().getChildren().get(2)).isSAndNumber());
        assertEquals(0, ((MCodeNode) allLayerPostProcessResults.get(1).getLayerData().getChildren().get(2)).getSNumber());
    }

    @Test
    public void testSaveHeaters_switch_on_complex1()
    {
        //Test that we switch on at the right point
        System.out.println("switch_on_complex1");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            //Inserts: M104 T0
            new ToolDefinition(0, 260),
            //Inserts: M104 T
            new ToolDefinition(1, 10),
            new ToolDefinition(0, 6),
            //Inserts: M104 S0
            new ToolDefinition(1, 45)
        }));
        layers.add(new LayerDefinition(1, new ToolDefinition[]
        {
            new ToolDefinition(1, 350),
            //Inserts: M104 S
            new ToolDefinition(0, 24),
            //Inserts: M104 S0
            new ToolDefinition(1, 75),
            new ToolDefinition(1, 150)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = generateLayerResults(layers);
        FilamentSaver instance = new FilamentSaver();
        instance.saveHeaters(allLayerPostProcessResults);

        assertEquals(2, allLayerPostProcessResults.size());
        assertEquals(6, allLayerPostProcessResults.get(0).getLayerData().getChildren().size());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0)).isTAndNumber());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1) instanceof ToolSelectNode);
        assertEquals(19, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().size());
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(3)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(3)).isTOnly());

        //M103 because this is layer 0
        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4) instanceof MCodeNode);
        assertEquals(103, ((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(0).getLayerData().getChildren().get(4)).isSAndNumber());

        assertEquals(5, allLayerPostProcessResults.get(1).getLayerData().getChildren().size());

        assertTrue(allLayerPostProcessResults.get(1).getLayerData().getChildren().get(0).getChildren().get(9) instanceof MCodeNode);
        assertEquals(104, ((MCodeNode) allLayerPostProcessResults.get(1).getLayerData().getChildren().get(0).getChildren().get(9)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(1).getLayerData().getChildren().get(0).getChildren().get(9)).isSOnly());

        assertTrue(allLayerPostProcessResults.get(1).getLayerData().getChildren().get(2) instanceof MCodeNode);
        assertEquals(104, ((MCodeNode) allLayerPostProcessResults.get(1).getLayerData().getChildren().get(2)).getMNumber());
        assertTrue(((MCodeNode) allLayerPostProcessResults.get(1).getLayerData().getChildren().get(2)).isSAndNumber());
        assertEquals(0, ((MCodeNode) allLayerPostProcessResults.get(1).getLayerData().getChildren().get(2)).getSNumber());
    }

    private List<LayerPostProcessResult> generateLayerResults(List<LayerDefinition> layerDefinitions)
    {
        List<LayerPostProcessResult> results = new ArrayList<>();
        double startingTimeForLayer = 0;

        for (LayerDefinition layerDefinition : layerDefinitions)
        {
            LayerNode layerNode = generateLayer(startingTimeForLayer, layerDefinition);
            LayerPostProcessResult result = new LayerPostProcessResult(
                    layerNode,
                    0,
                    0,
                    0,
                    0,
                    0,
                    null,
                    null,
                    null,
                    0,
                    0);
            results.add(result);

            startingTimeForLayer += layerNode.getFinishTimeFromStartOfPrint_secs().get();
        }

        return results;
    }

    private LayerNode generateLayer(double startingTimeForLayer, LayerDefinition layerDefinition)
    {
        LayerNode layerNode = new LayerNode(layerDefinition.getLayerNumber());

        double currentLayerTime = startingTimeForLayer;

        for (ToolDefinition tool : layerDefinition.getTools())
        {
            ToolSelectNode tsNode = new ToolSelectNode();
            tsNode.setToolNumber(tool.getToolNumber());
            tsNode.setEstimatedDuration(tool.getDuration());
            tsNode.setFinishTimeFromStartOfPrint_secs(currentLayerTime + tool.getDuration());

            double durationCountdown = tool.getDuration();
            double decrementValue = 15.0;

            do
            {
                ExtrusionNode exNode = new ExtrusionNode();
                double durationToUse = (durationCountdown > 0) ? durationCountdown : durationCountdown + decrementValue;
                exNode.setFinishTimeFromStartOfPrint_secs(durationToUse + currentLayerTime);
                tsNode.addChildAtStart(exNode);
                durationCountdown -= decrementValue;
            } while (durationCountdown > 0);

            layerNode.addChildAtEnd(tsNode);

            currentLayerTime += tool.getDuration();
        }

        layerNode.setFinishTimeFromStartOfPrint_secs(currentLayerTime);

        return layerNode;
    }
}
