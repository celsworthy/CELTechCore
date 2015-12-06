package celtech.gcodetranslator.postprocessing;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.CannotCloseFromPerimeterException;
import celtech.gcodetranslator.GCodeOutputWriter;
import celtech.gcodetranslator.NoPerimeterToCloseOverException;
import celtech.gcodetranslator.NotEnoughAvailableExtrusionException;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.PostProcessingError;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerChangeDirectiveNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.MergeableWithToolchange;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.IteratorWithStartPoint;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;
import celtech.printerControl.model.CameraTriggerManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class UtilityMethods
{

    private final Stenographer steno = StenographerFactory.getStenographer(UtilityMethods.class.getName());
    private final PostProcessorFeatureSet ppFeatureSet;
    private final NodeManagementUtilities nodeManagementUtilities;
    private final CloseLogic closeLogic;

    public UtilityMethods(final PostProcessorFeatureSet ppFeatureSet,
            final Project project,
            SlicerParametersFile settings,
            String headType)
    {
        this.ppFeatureSet = ppFeatureSet;
        nodeManagementUtilities = new NodeManagementUtilities(ppFeatureSet);
        this.closeLogic = new CloseLogic(project, settings, ppFeatureSet, headType);
    }

    protected void insertCameraTriggersAndCloses(LayerNode layerNode,
            LayerPostProcessResult lastLayerPostProcessResult,
            List<NozzleProxy> nozzleProxies)
    {
        if (Lookup.getUserPreferences().isGoProTriggerEnabled())
        {
            IteratorWithStartPoint<GCodeEventNode> layerForwards = layerNode.treeSpanningIterator(null);
            while (layerForwards.hasNext())
            {
                GCodeEventNode layerForwardsEvent = layerForwards.next();

                if (layerForwardsEvent instanceof LayerChangeDirectiveNode)
                {
                    CameraTriggerManager.appendLayerEndTriggerCode((LayerChangeDirectiveNode) layerForwardsEvent);
                    break;
                }
            }

            Iterator<GCodeEventNode> layerBackwards = layerNode.childBackwardsIterator();

            while (layerBackwards.hasNext())
            {
                GCodeEventNode layerChild = layerBackwards.next();
                if (layerChild instanceof ToolSelectNode)
                {
                    closeAtEndOfToolSelectIfNecessary((ToolSelectNode) layerChild, nozzleProxies);
                    break;
                }
            }
        }
    }

    protected void suppressUnnecessaryToolChangesAndInsertToolchangeCloses(LayerNode layerNode,
            LayerPostProcessResult lastLayerPostProcessResult,
            List<NozzleProxy> nozzleProxies)
    {
        ToolSelectNode lastToolSelectNode = null;

        if (lastLayerPostProcessResult.getLastToolSelectInForce() != null)
        {
            lastToolSelectNode = lastLayerPostProcessResult.getLastToolSelectInForce();
        }

        // We know that tool selects come directly under a layer node...        
        Iterator<GCodeEventNode> layerIterator = layerNode.childIterator();

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
            if (lastToolSelectNode == null)
            {
                //Our first ever tool select node...
            } else if (lastToolSelectNode.getToolNumber() == toolSelectNode.getToolNumber())
            {
                toolSelectNode.suppressNodeOutput(true);
            } else
            {
                if (ppFeatureSet.isEnabled(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES))
                {
                    closeAtEndOfToolSelectIfNecessary(lastToolSelectNode, nozzleProxies);
                }

                //Now look to see if we can consolidate the tool change with a travel
                if (lastToolSelectNode.getChildren().size() > 0)
                {
                    if (lastToolSelectNode.getChildren().get(lastToolSelectNode.getChildren().size() - 1) instanceof MergeableWithToolchange)
                    {
                        ((MergeableWithToolchange) lastToolSelectNode.getChildren().get(lastToolSelectNode.getChildren().size() - 1)).changeToolDuringMovement(toolSelectNode.getToolNumber());
                        toolSelectNode.suppressNodeOutput(true);
                    }
                }
            }

            lastToolSelectNode = toolSelectNode;
        }
    }

    protected void closeAtEndOfToolSelectIfNecessary(ToolSelectNode toolSelectNode, List<NozzleProxy> nozzleProxies)
    {
        // The tool has changed
        // Close the nozzle if it isn't already...
        //Insert a close at the end if there isn't already a close following the last extrusion
        Iterator<GCodeEventNode> nodeIterator = toolSelectNode.childBackwardsIterator();
        boolean keepLooking = true;
        boolean needToClose = false;
        GCodeEventNode eventToCloseFrom = null;

        List<SectionNode> sectionsToConsiderForClose = new ArrayList<>();

        //If we see a nozzle event BEFORE an extrusion then the nozzle has already been closed
        //If we see an extrusion BEFORE a nozzle event then we must close
        //Keep looking until we find a nozzle event, so that 
        while (nodeIterator.hasNext()
                && keepLooking)
        {
            GCodeEventNode node = nodeIterator.next();

            if (node instanceof SectionNode)
            {
                Iterator<GCodeEventNode> sectionIterator = node.childBackwardsIterator();
                while (sectionIterator.hasNext()
                        && keepLooking)
                {
                    GCodeEventNode sectionChild = sectionIterator.next();
                    if (sectionChild instanceof NozzlePositionProvider
                            && ((NozzlePositionProvider) sectionChild).getNozzlePosition().isBSet())
                    {
                        keepLooking = false;
                    } else if (sectionChild instanceof ExtrusionNode)
                    {
                        if (!sectionsToConsiderForClose.contains((SectionNode) node))
                        {
                            sectionsToConsiderForClose.add(0, (SectionNode) node);
                        }
                        if (eventToCloseFrom == null)
                        {
                            eventToCloseFrom = sectionChild;
                            needToClose = true;
                        }
                    }
                }
            } else
            {
                if (node instanceof NozzlePositionProvider
                        && ((NozzlePositionProvider) node).getNozzlePosition().isBSet())
                {
                    keepLooking = false;
                } else if (node instanceof ExtrusionNode)
                {
                    if (eventToCloseFrom == null)
                    {
                        eventToCloseFrom = node;
                        needToClose = true;
                    }
                }
            }
        }

        if (needToClose)
        {
            try
            {
                Optional<CloseResult> closeResult = closeLogic.insertProgressiveNozzleClose(eventToCloseFrom, sectionsToConsiderForClose, nozzleProxies.get(toolSelectNode.getToolNumber()));
                if (!closeResult.isPresent())
                {
                    steno.warning("Close failed - unable to record replenish");
                }
            } catch (NodeProcessingException | CannotCloseFromPerimeterException | NoPerimeterToCloseOverException | NotEnoughAvailableExtrusionException | PostProcessingError ex)
            {
                throw new RuntimeException("Error locating available extrusion during tool select normalisation", ex);
            }
        }
    }

    protected OpenResult insertOpens(LayerNode layerNode,
            OpenResult lastOpenResult,
            List<NozzleProxy> nozzleProxies,
            String headTypeCode)
    {
        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);
        Movement lastMovement = null;
        boolean nozzleOpen = false;
        double lastNozzleValue = 0;
        int lastToolNumber = -1;
        double replenishExtrusionE = 0;
        double replenishExtrusionD = 0;
        Map<ExtrusionNode, NozzleValvePositionNode> nodesToAdd = new HashMap<>();
        if (lastOpenResult != null)
        {
            nozzleOpen = lastOpenResult.isNozzleOpen();
            replenishExtrusionE = lastOpenResult.getOutstandingEReplenish();
            replenishExtrusionD = lastOpenResult.getOutstandingDReplenish();
            lastToolNumber = lastOpenResult.getLastToolNumber();
        }

        while (layerIterator.hasNext())
        {
            GCodeEventNode layerEvent = layerIterator.next();

            if (layerEvent instanceof ToolSelectNode)
            {
                lastToolNumber = ((ToolSelectNode) layerEvent).getToolNumber();
            } else if (layerEvent instanceof NozzlePositionProvider
                    && (((NozzlePositionProvider) layerEvent).getNozzlePosition().isPartialOpen()
                    || (((NozzlePositionProvider) layerEvent).getNozzlePosition().isBSet()
                    && ((NozzlePositionProvider) layerEvent).getNozzlePosition().getB() == 1.0)))
            {
                nozzleOpen = true;
                lastNozzleValue = ((NozzlePositionProvider) layerEvent).getNozzlePosition().getB();
                switch (HeadContainer.getHeadByID(headTypeCode).getNozzles().get(lastToolNumber).getAssociatedExtruder())
                {
                    case "E":
                        replenishExtrusionE = 0;
                        break;
                    case "D":
                        replenishExtrusionD = 0;
                        break;
                }

                if (layerEvent instanceof ExtrusionNode)
                {
                    if (lastMovement == null)
                    {
                        lastMovement = ((ExtrusionNode) layerEvent).getMovement();
                    }
                }
            } else if (layerEvent instanceof NozzlePositionProvider
                    && ((NozzlePositionProvider) layerEvent).getNozzlePosition().isBSet()
                    && ((NozzlePositionProvider) layerEvent).getNozzlePosition().getB() < 1.0
                    && !((NozzlePositionProvider) layerEvent).getNozzlePosition().isPartialOpen())
            {
                nozzleOpen = false;
                lastNozzleValue = ((NozzlePositionProvider) layerEvent).getNozzlePosition().getB();
                if (layerEvent instanceof ExtrusionNode)
                {
                    switch (HeadContainer.getHeadByID(headTypeCode).getNozzles().get(lastToolNumber).getAssociatedExtruder())
                    {
                        case "E":
                            replenishExtrusionE = ((ExtrusionNode) layerEvent).getElidedExtrusion();
                            break;
                        case "D":
                            replenishExtrusionD = ((ExtrusionNode) layerEvent).getElidedExtrusion();
                            break;
                    }

                    if (lastMovement == null)
                    {
                        lastMovement = ((ExtrusionNode) layerEvent).getMovement();
                    }
                }
            } else if (layerEvent instanceof ExtrusionNode
                    && !nozzleOpen)
            {
                if (lastNozzleValue > 0)
                {
                    String outputString = "Nozzle was not closed properly on layer " + layerNode.getLayerNumber() + " before extrusion " + ((ExtrusionNode) layerEvent).renderForOutput();
                    if (layerNode.getGCodeLineNumber().isPresent())
                    {
                        outputString += " on line " + layerNode.getGCodeLineNumber().get();
                    }
                    steno.warning(outputString);
                }
                NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
                newNozzleValvePositionNode.getNozzlePosition().setB(1);

                double replenishEToUse = 0;
                double replenishDToUse = 0;

                switch (HeadContainer.getHeadByID(headTypeCode).getNozzles().get(lastToolNumber).getAssociatedExtruder())
                {
                    case "E":
                        replenishEToUse = replenishExtrusionE;
                        replenishExtrusionE = 0;
                        replenishDToUse = 0;
                        break;
                    case "D":
                        replenishDToUse = replenishExtrusionD;
                        replenishExtrusionD = 0;
                        replenishEToUse = 0;
                        break;
                }
                newNozzleValvePositionNode.setReplenishExtrusionE(replenishEToUse);
                newNozzleValvePositionNode.setReplenishExtrusionD(replenishDToUse);
                nodesToAdd.put((ExtrusionNode) layerEvent, newNozzleValvePositionNode);
                nozzleOpen = true;

                if (lastMovement == null)
                {
                    lastMovement = ((ExtrusionNode) layerEvent).getMovement();
                }
            }
//            else if (layerEvent instanceof TravelNode)
//            {
//                if (lastMovement == null)
//                {
//                    lastMovement = ((TravelNode) layerEvent).getMovement();
//                } else
//                {
//                    Movement thisMovement = ((TravelNode) layerEvent).getMovement();
//                    Vector2D thisPoint = thisMovement.toVector2D();
//                    Vector2D lastPoint = lastMovement.toVector2D();
//
//                    if (lastPoint.distance(thisPoint) > 5 && !nozzleOpen)
//                    {
//                        steno.warning("Travel without close on layer " + layerNode.getLayerNumber() + " at " + ((TravelNode) layerEvent).renderForOutput());
//                    }
//                }
//            }
        }

        nodesToAdd.entrySet().stream().forEach((entryToUpdate) ->
        {
            entryToUpdate.getKey().addSiblingBefore(entryToUpdate.getValue());
        });

        return new OpenResult(replenishExtrusionE, replenishExtrusionD, nozzleOpen, lastToolNumber);
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

    public void recalculatePerSectionExtrusion(LayerNode layerNode)
    {
        Iterator<GCodeEventNode> childrenOfTheLayer = layerNode.childIterator();
        while (childrenOfTheLayer.hasNext())
        {
            GCodeEventNode potentialSectionNode = childrenOfTheLayer.next();

            if (potentialSectionNode instanceof SectionNode)
            {
                ((SectionNode) potentialSectionNode).recalculateExtrusion();
            }
        }
    }
}
