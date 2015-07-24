package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.ReplenishNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePosition;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;
import celtech.printerControl.model.Head.HeadType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author Ian
 */
public class UtilityMethods
{

    private final PostProcessorFeatureSet ppFeatureSet;
    private final NodeManagementUtilities nodeManagementUtilities;
    private final CloseLogic closeLogic;

    public UtilityMethods(final PostProcessorFeatureSet ppFeatureSet,
            final Project project,
            SlicerParametersFile settings,
            HeadType headType)
    {
        this.ppFeatureSet = ppFeatureSet;
        nodeManagementUtilities = new NodeManagementUtilities(ppFeatureSet);
        this.closeLogic = new CloseLogic(project, settings, ppFeatureSet, headType);
    }

    protected void suppressUnnecessaryToolChangesAndInsertToolchangeCloses(LayerNode layerNode,
            LayerPostProcessResult lastLayerPostProcessResult,
            List<NozzleProxy> nozzleProxies)
    {
        // We know that tool selects come directly under a layer node...        
        Iterator<GCodeEventNode> layerIterator = layerNode.childIterator();

        int lastToolNumber = -1;

        if (lastLayerPostProcessResult.getNozzleStateAtEndOfLayer()
                .isPresent())
        {
            lastToolNumber = lastLayerPostProcessResult.getNozzleStateAtEndOfLayer().get().getNozzleReferenceNumber();
        }

        List<ToolSelectNode> toolSelectNodes = new ArrayList<>();

        while (layerIterator.hasNext())
        {
            GCodeEventNode potentialToolSelectNode = layerIterator.next();

            if (potentialToolSelectNode instanceof ToolSelectNode)
            {
                toolSelectNodes.add((ToolSelectNode) potentialToolSelectNode);
            }
        }

        for (ToolSelectNode toolSelectNode : toolSelectNodes)
        {
            if (lastToolNumber == toolSelectNode.getToolNumber())
            {
                toolSelectNode.suppressNodeOutput(true);
            } else
            {
                // The tool number has changed
                // Close the nozzle if it isn't already...
                //Insert a close at the end if there isn't already a close following the last extrusion
                GCodeEventNode lastEvent = toolSelectNode.getAbsolutelyTheLastEvent();

                try
                {
                    Optional<ExtrusionNode> lastExtrusion;

                    if (lastEvent instanceof ExtrusionNode)
                    {
                        lastExtrusion = Optional.of((ExtrusionNode)lastEvent);
                    } else
                    {
                        lastExtrusion = nodeManagementUtilities.findPriorExtrusion(lastEvent);
                    }

                    if (lastExtrusion.isPresent())
                    {
                        if (!lastExtrusion.get().getNozzlePosition().isBSet()
                                || lastExtrusion.get().getNozzlePosition().getB() > 0)
                        {
                            //We need to close
                            double availableExtrusion = nodeManagementUtilities.findAvailableExtrusion(lastExtrusion.get(), false);

                            closeLogic.insertNozzleCloses(availableExtrusion, lastExtrusion.get(), nozzleProxies.get(toolSelectNode.getToolNumber()));
                        }
                    }
                } catch (NodeProcessingException ex)
                {
                    throw new RuntimeException("Error locating available extrusion during tool select normalisation", ex);
                }
            }

            lastToolNumber = toolSelectNode.getToolNumber();
        }
    }

    protected void insertNozzleOpenFullyBeforeEvent(ExtrusionNode node)
    {
        // Insert a replenish if required
        if (ppFeatureSet.isEnabled(PostProcessorFeature.REPLENISH_BEFORE_OPEN))
        {
            if (node.getElidedExtrusion() > 0)
            {
                ReplenishNode replenishNode = new ReplenishNode();
                replenishNode.getExtrusion().setE((float) node.getElidedExtrusion());
                replenishNode.setCommentText("Replenishing elided extrusion");
                node.addSiblingBefore(replenishNode);
            }
        }

        NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
        newNozzleValvePositionNode.getNozzlePosition().setB(1);
        node.addSiblingBefore(newNozzleValvePositionNode);
    }

    /**
     * A 'brute force' walkthrough of all nozzle position providers This method
     * inserts full opens if it encounters extrusion nodes without B position
     * which are preceded by a partially open or closed nozzle
     *
     * @param layerNode
     * @param lastLayerPostProcessResult
     *
     */
    protected void insertOpenNodes(final LayerNode layerNode, final LayerPostProcessResult lastLayerPostProcessResult)
    {
        // We know that tool selects come directly under a layer node...
        Iterator<GCodeEventNode> layerIterator = layerNode.childIterator();

        int lastNozzleNumber = -1;

        List<ExtrusionNode> nodesToOpenBefore = new ArrayList<>();

        while (layerIterator.hasNext())
        {
            GCodeEventNode potentialToolSelectNode = layerIterator.next();

            if (potentialToolSelectNode instanceof ToolSelectNode)
            {
                ToolSelectNode toolSelectNode = (ToolSelectNode) potentialToolSelectNode;

                double lastBPosition = 0;

                if (lastNozzleNumber != toolSelectNode.getToolNumber())
                {
                    // We may have to walk the tree below the tool select node to get the parts we want
                    Iterator<GCodeEventNode> toolSelectChildIterator = toolSelectNode.treeSpanningIterator(null);

                    while (toolSelectChildIterator.hasNext())
                    {
                        GCodeEventNode potentialNozzlePositionProviderNode = toolSelectChildIterator.next();

                        if (potentialNozzlePositionProviderNode instanceof NozzlePositionProvider)
                        {
                            NozzlePositionProvider nozzlePositionProvider = (NozzlePositionProvider) potentialNozzlePositionProviderNode;

                            NozzlePosition nozzlePosition = (NozzlePosition) nozzlePositionProvider.getNozzlePosition();
                            if (nozzlePosition.isBSet())
                            {
                                lastBPosition = nozzlePosition.getB();
                            } else if (nozzlePositionProvider instanceof ExtrusionNode)
                            {
                                if (lastBPosition < 1)
                                {
                                    //The nozzle needs to be opened
//                                    try
//                                    {
//                                        GCodeEventNode nextExtrusionNode = nodeManagementUtilities.findNextExtrusion(layerNode, (GCodeEventNode) nozzlePositionProvider).orElseThrow(NodeProcessingException::new);
                                    nodesToOpenBefore.add((ExtrusionNode) nozzlePositionProvider);
                                    lastBPosition = 1.0;
//                                    } catch (NodeProcessingException ex)
//                                    {
//                                        throw new RuntimeException("Failed to insert open nodes on layer " + layerNode.getLayerNumber(), ex);
//                                    }
                                }
                            }
                        }
                    }

                    lastNozzleNumber = toolSelectNode.getToolNumber();
                }
            }
        }

        for (ExtrusionNode extrusionNode : nodesToOpenBefore)
        {
            insertNozzleOpenFullyBeforeEvent(extrusionNode);
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
