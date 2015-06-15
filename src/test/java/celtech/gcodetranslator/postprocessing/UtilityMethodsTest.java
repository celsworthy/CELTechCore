package celtech.gcodetranslator.postprocessing;

import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import java.util.List;
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
public class UtilityMethodsTest
{

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

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();
        ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
        ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSES_ON_RETRACT);
        ppFeatures.enableFeature(PostProcessorFeature.CLOSE_ON_TASK_CHANGE);

        UtilityMethods utilityMethods = new UtilityMethods();
        LayerPostProcessResult lastLayerParseResult = new LayerPostProcessResult(Optional.empty(), layer, 0, 0, 0, 10);

        utilityMethods.suppressUnnecessaryToolChanges(layer, lastLayerParseResult);

        assertEquals(2, layer.getChildren().size());
        assertFalse(tool1.isNodeOutputSuppressed());
        assertTrue(tool2.isNodeOutputSuppressed());
    }
}
