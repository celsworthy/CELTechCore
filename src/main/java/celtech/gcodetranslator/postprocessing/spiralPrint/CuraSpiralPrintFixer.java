package celtech.gcodetranslator.postprocessing.spiralPrint;

import celtech.gcodetranslator.postprocessing.LayerPostProcessResult;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.IteratorWithStartPoint;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Ian
 */
public class CuraSpiralPrintFixer
{

    public void fixSpiralPrint(List<LayerPostProcessResult> allLayerPostProcessResults)
    {
        boolean beginRemovingTravels = false;
        List<GCodeEventNode> travelNodesToDelete = new ArrayList<>();

        for (int layerCounter = 0; layerCounter < allLayerPostProcessResults.size(); layerCounter++)
        {
            LayerPostProcessResult layerPostProcessResult = allLayerPostProcessResults.get(layerCounter);

            IteratorWithStartPoint<GCodeEventNode> layerIterator = layerPostProcessResult.getLayerData().treeSpanningIterator(null);

            while (layerIterator.hasNext())
            {
                GCodeEventNode node = layerIterator.next();

                if (beginRemovingTravels
                        && node instanceof TravelNode)
                {
                    travelNodesToDelete.add(node);
                }
 
                if (!beginRemovingTravels
                        && node instanceof ExtrusionNode
                        && ((ExtrusionNode) node).getMovement().isZSet())
                {
                    beginRemovingTravels = true;
                }
            }
        }
        
        for (GCodeEventNode nodeToDelete : travelNodesToDelete)
        {
            nodeToDelete.removeFromParent();
        }
    }
}
