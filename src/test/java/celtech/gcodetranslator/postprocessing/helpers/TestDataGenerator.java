package celtech.gcodetranslator.postprocessing.helpers;

import celtech.gcodetranslator.postprocessing.LayerPostProcessResult;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ian
 */
public class TestDataGenerator
{

    public static List<LayerPostProcessResult> generateLayerResults(List<LayerDefinition> layerDefinitions)
    {
        List<LayerPostProcessResult> results = new ArrayList<>();
        double startingTimeForLayer = 0;

        for (LayerDefinition layerDefinition : layerDefinitions)
        {
            LayerNode layerNode = generateLayer(startingTimeForLayer, layerDefinition);

            LayerPostProcessResult result = new LayerPostProcessResult(
                    layerNode,
                    0,
                    null,
                    null,
                    null,
                    0);
            results.add(result);

            startingTimeForLayer += layerNode.getFinishTimeFromStartOfPrint_secs().get();
        }

        return results;
    }

    private static LayerNode generateLayer(double startingTimeForLayer, LayerDefinition layerDefinition)
    {
        LayerNode layerNode = new LayerNode(layerDefinition.getLayerNumber());

        double currentLayerTime = startingTimeForLayer;

        for (ToolDefinition tool : layerDefinition.getTools())
        {
            ToolSelectNode tsNode = new ToolSelectNode();
            tsNode.setToolNumber(tool.getToolNumber());
            tsNode.setEstimatedDuration(tool.getDuration());
            tsNode.setFinishTimeFromStartOfPrint_secs(currentLayerTime + tool.getDuration());

            double decrementValue = 15.0;

            int numberOfIntervals = (int)(tool.getDuration() / decrementValue);
            float extrusionPerInterval = (numberOfIntervals > 1)?tool.getExtrusion() / numberOfIntervals:tool.getExtrusion();

            double durationCountdown = tool.getDuration();

            do
            {
                ExtrusionNode exNode = new ExtrusionNode();
                exNode.getExtrusion().setE(extrusionPerInterval);
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
