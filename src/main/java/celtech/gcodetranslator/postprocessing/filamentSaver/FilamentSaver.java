package celtech.gcodetranslator.postprocessing.filamentSaver;

import celtech.gcodetranslator.postprocessing.LayerPostProcessResult;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MCodeNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.IteratorWithOrigin;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class FilamentSaver
{

    private final Stenographer steno = StenographerFactory.getStenographer(FilamentSaver.class.getName());
    private final int layer0MValue = 103;
    private final int otherLayerMValue = 104;
    private final double heatUpTime_secs;
    private final double switchOffTime_secs;

    public FilamentSaver(double heatUpTime_secs, double switchOffTime_secs)
    {
        this.heatUpTime_secs = heatUpTime_secs;
        this.switchOffTime_secs = switchOffTime_secs;
    }

    private enum HeaterState
    {

        OFF, ON_FIRST_LAYER, ON
    }

    public void saveHeaters(List<LayerPostProcessResult> allLayerPostProcessResults)
    {

        ToolSelectNode[] lastToolSelects =
        {
            null, null
        };

        // We assume that both heaters were on at the start using M103
        HeaterState[] nozzleHeaterState =
        {
            HeaterState.ON_FIRST_LAYER, HeaterState.ON_FIRST_LAYER
        };

        for (int layerCounter = 0; layerCounter < allLayerPostProcessResults.size(); layerCounter++)
        {
            LayerPostProcessResult layerPostProcessResult = allLayerPostProcessResults.get(layerCounter);

            List<NodeAddStore> nodesToAdd = new ArrayList<>();

            //Make sure that each heater is switched off if not required for specified time
            //Use tool selects to determine which is required...
            // We know that tool selects come directly under a layer node...        
            Iterator<GCodeEventNode> layerIterator = layerPostProcessResult.getLayerData().childIterator();

            while (layerIterator.hasNext())
            {
                GCodeEventNode node = layerIterator.next();

                if (node instanceof ToolSelectNode)
                {
                    ToolSelectNode toolSelect = (ToolSelectNode) node;

                    int thisToolNumber = toolSelect.getToolNumber();
                    int otherToolNumber = (thisToolNumber == 0) ? 1 : 0;

                    //Do we need to switch the heater on for this tool?
                    if (nozzleHeaterState[thisToolNumber] == HeaterState.OFF)
                    {
                        double targetTimeAfterStart = Math.max(0, toolSelect.getStartTimeFromStartOfPrint_secs().get() - heatUpTime_secs);

                        FoundHeatUpNode foundNodeToHeatBefore = findNodeToHeatBefore(allLayerPostProcessResults, layerCounter, toolSelect, targetTimeAfterStart);

                        if (foundNodeToHeatBefore != null)
                        {
                            int mValue = (foundNodeToHeatBefore.getFoundInLayer() == 0) ? layer0MValue : otherLayerMValue;
                            MCodeNode heaterOnNode = generateHeaterOnNode(thisToolNumber, mValue);
                            heaterOnNode.appendCommentText("Switch on in " + heatUpTime_secs + " seconds");
                            nodesToAdd.add(new NodeAddStore(foundNodeToHeatBefore.getFoundNode(), heaterOnNode, false));
                            nozzleHeaterState[thisToolNumber] = (foundNodeToHeatBefore.getFoundInLayer() == 0) ? HeaterState.ON_FIRST_LAYER : HeaterState.ON;
                        } else
                        {
                            steno.error("Failed to find place to heat nozzle");
                        }
                    } else if (layerCounter == 1
                            && nozzleHeaterState[thisToolNumber] == HeaterState.ON_FIRST_LAYER)
                    {
                        int mValue = otherLayerMValue;
                        MCodeNode heaterOnNode = generateHeaterOnNode(thisToolNumber, mValue);
                        heaterOnNode.appendCommentText("Switch to subsequent layer temperature");
                        nodesToAdd.add(new NodeAddStore(toolSelect, heaterOnNode, false));
                        nozzleHeaterState[thisToolNumber] = HeaterState.ON;
                    }

                    //Do we need to switch the other tool heater off?
                    double finishTimeForOtherTool = (lastToolSelects[otherToolNumber] != null) ? lastToolSelects[otherToolNumber].getFinishTimeFromStartOfPrint_secs().get() : 0;
                    if ((toolSelect.getFinishTimeFromStartOfPrint_secs().get() - finishTimeForOtherTool > switchOffTime_secs)
                            && nozzleHeaterState[otherToolNumber] != HeaterState.OFF)
                    {
                        if (lastToolSelects[otherToolNumber] == null)
                        {
                            //We need to switch off the other heater at the start of this layer
                            // Add a node before the first node in the layer.
                            MCodeNode heaterOffNode = generateHeaterOffNode(layerCounter, otherToolNumber);
                            nodesToAdd.add(new NodeAddStore(layerPostProcessResult.getLayerData().getChildren().get(0), heaterOffNode, false));
                        } else
                        {
                            //We need to switch off the other heater after the last tool select for the other tool...
                            MCodeNode heaterOffNode = generateHeaterOffNode(((LayerNode) lastToolSelects[otherToolNumber].getParent().get()).getLayerNumber(), otherToolNumber);
                            nodesToAdd.add(new NodeAddStore(lastToolSelects[otherToolNumber], heaterOffNode, true));
                        }
                        nozzleHeaterState[otherToolNumber] = HeaterState.OFF;
                    }

                    //Remember things for next iteration...
                    lastToolSelects[thisToolNumber] = toolSelect;
                }
            }

            for (NodeAddStore addStore : nodesToAdd)
            {
                if (addStore.isAddAfter())
                {
                    addStore.getSiblingForAddedNode().addSiblingAfter(addStore.getNodeToAdd());
                } else
                {
                    addStore.getSiblingForAddedNode().addSiblingBefore(addStore.getNodeToAdd());
                }
            }
        }
    }

    private MCodeNode generateHeaterOffNode(int layerNumber, int heaterNumber)
    {
        MCodeNode heaterOffNode = new MCodeNode();
        heaterOffNode.setMNumber((layerNumber == 0) ? layer0MValue : otherLayerMValue);
        switch (heaterNumber)
        {
            case 0:
                //Extruder D
                heaterOffNode.setSNumber(0);
                heaterOffNode.appendCommentText("Switch heater 0 off");
                break;
            case 1:
                //Extruder E
                heaterOffNode.setTNumber(0);
                heaterOffNode.appendCommentText("Switch heater 1 off");
                break;
        }

        return heaterOffNode;
    }

    private MCodeNode generateHeaterOnNode(int heaterNumber, int mValue)
    {
        MCodeNode heaterOnNode = new MCodeNode();
        heaterOnNode.setMNumber(mValue);
        switch (heaterNumber)
        {
            case 0:
                //Extruder D
                heaterOnNode.setSOnly(true);
                heaterOnNode.appendCommentText("Switch heater 0 on");
                break;
            case 1:
                //Extruder E
                heaterOnNode.setTOnly(true);
                heaterOnNode.appendCommentText("Switch heater 1 on");
                break;
        }

        return heaterOnNode;
    }

    private FoundHeatUpNode findNodeToHeatBefore(
            List<LayerPostProcessResult> allLayerPostProcessResults,
            final int startingLayer,
            ToolSelectNode toolSelect,
            double targetTimeAfterStart)
    {
        FoundHeatUpNode foundNode = null;

        int layerCounter = startingLayer;

        while (foundNode == null
                && layerCounter >= 0)
        {
            GCodeEventNode startingNode = null;

            if (layerCounter == startingLayer)
            {
                startingNode = toolSelect;
            } else
            {
                startingNode = allLayerPostProcessResults.get(layerCounter).getLayerData().getAbsolutelyTheLastEvent();
            }

            IteratorWithOrigin<GCodeEventNode> openFinder = startingNode.treeSpanningBackwardsIterator();

            while (openFinder.hasNext())
            {
                GCodeEventNode nodeUnderConsideration = openFinder.next();

                if (nodeUnderConsideration instanceof MCodeNode
                        && ((MCodeNode) nodeUnderConsideration).getMNumber() == 104)
                {
                    steno.info("Warning - came across MCodeNode");
                }

                if ((nodeUnderConsideration instanceof ExtrusionNode || nodeUnderConsideration instanceof TravelNode)
                        && nodeUnderConsideration.getFinishTimeFromStartOfPrint_secs().isPresent()
                        && nodeUnderConsideration.getFinishTimeFromStartOfPrint_secs().get() < targetTimeAfterStart)
                {
                    foundNode = new FoundHeatUpNode(layerCounter, nodeUnderConsideration);
                    break;
                }
            }

            layerCounter--;
        }

        return foundNode;
    }

    private class NodeAddStore
    {

        private final GCodeEventNode siblingForAddedNode;
        private final GCodeEventNode nodeToAdd;
        private final boolean addAfter;

        public NodeAddStore(GCodeEventNode siblingForAddedNode,
                GCodeEventNode nodeToAdd,
                boolean addAfter)
        {
            this.siblingForAddedNode = siblingForAddedNode;
            this.nodeToAdd = nodeToAdd;
            this.addAfter = addAfter;
        }

        public GCodeEventNode getSiblingForAddedNode()
        {
            return siblingForAddedNode;
        }

        public GCodeEventNode getNodeToAdd()
        {
            return nodeToAdd;
        }

        public boolean isAddAfter()
        {
            return addAfter;
        }
    }

    private class FoundHeatUpNode
    {

        private final int foundInLayer;
        private final GCodeEventNode foundNode;

        public FoundHeatUpNode(int foundInLayer,
                GCodeEventNode foundNode)
        {
            this.foundInLayer = foundInLayer;
            this.foundNode = foundNode;
        }

        public int getFoundInLayer()
        {
            return foundInLayer;
        }

        public GCodeEventNode getFoundNode()
        {
            return foundNode;
        }
    }
}
