package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Ian
 */
public class UtilityMethods
{

    protected void suppressUnnecessaryToolChanges(LayerNode layerNode, LayerPostProcessResult lastLayerPostProcessResult)
    {
        List<ToolSelectNode> toolSelectNodes = layerNode.stream()
                .filter(node -> node instanceof ToolSelectNode)
                .map(ToolSelectNode.class::cast)
                .collect(Collectors.toList());

        int lastToolNumber = -1;

        if (lastLayerPostProcessResult.getNozzleStateAtEndOfLayer()
                .isPresent())
        {
            lastToolNumber = lastLayerPostProcessResult.getNozzleStateAtEndOfLayer().get().getNozzleReferenceNumber();
        }

        for (ToolSelectNode toolSelectNode : toolSelectNodes)
        {
            if (lastToolNumber >= 0)
            {
                if (lastToolNumber == toolSelectNode.getToolNumber())
                {
                    toolSelectNode.suppressNodeOutput(true);
                }
            }

            lastToolNumber = toolSelectNode.getToolNumber();
        }
    }
    
    protected void updateLayerToLineNumber(LayerPostProcessResult lastLayerParseResult,
            List<Integer> layerNumberToLineNumber,
            GCodeOutputWriter writer)
    {
        if (lastLayerParseResult.getLayerData()
                != null)
        {
            int layerNumber = lastLayerParseResult.getLayerData().getLayerNumber();
            if (layerNumber >= 0)
            {
                layerNumberToLineNumber.add(layerNumber, writer.getNumberOfLinesOutput());
            }
        }
    }

    protected double updateLayerToPredictedDuration(LayerPostProcessResult lastLayerParseResult,
            List<Double> layerNumberToPredictedDuration,
            GCodeOutputWriter writer)
    {
        double predictedDuration = 0;
        
        if (lastLayerParseResult.getLayerData() != null)
        {
            int layerNumber = lastLayerParseResult.getLayerData().getLayerNumber();
            if (layerNumber >= 0)
            {
                layerNumberToPredictedDuration.add(layerNumber, lastLayerParseResult.getTimeForLayer());
                predictedDuration += lastLayerParseResult.getTimeForLayer();
            }
        }
        
        return predictedDuration;
    }
}
