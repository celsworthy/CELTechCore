package celtech.gcodetranslator.postprocessing.timeCalc;

import celtech.gcodetranslator.postprocessing.LayerPostProcessResult;
import celtech.gcodetranslator.postprocessing.helpers.LayerDefinition;
import celtech.gcodetranslator.postprocessing.helpers.TestDataGenerator;
import celtech.gcodetranslator.postprocessing.helpers.ToolDefinition;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.printerControl.model.Head;
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
public class TimeAndVolumeCalcTest
{
    
    public TimeAndVolumeCalcTest()
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
     * Test of calculateVolumeAndTime method, of class TimeAndVolumeCalc.
     */
    @Test
    public void testCalculateVolumeAndTime()
    {
        System.out.println("testCalculateVolumeAndTime");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 50, 100),
            new ToolDefinition(1, 500, 5000)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = TestDataGenerator.generateLayerResults(layers);
        
        TimeAndVolumeCalc timeAndVolumeCalc = new TimeAndVolumeCalc(Head.HeadType.DUAL_MATERIAL_HEAD);
        TimeAndVolumeCalcResult result = timeAndVolumeCalc.calculateVolumeAndTime(allLayerPostProcessResults);

//        assertEquals(5, result.getExtruderEStats().getVolume(), 0.1);
//        assertEquals(500, result.getExtruderDStats().getVolume(), 0.1);
    }
    
}
