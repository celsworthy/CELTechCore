package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.Extrusion;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
import celtech.printerControl.model.Head.HeadType;
import celtech.utils.Math.MathUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class CloseLogic
{

    private final Stenographer steno = StenographerFactory.getStenographer(CloseLogic.class.getName());
    private final Project project;
    private final PostProcessorFeatureSet featureSet;

    private final CloseUtilities closeUtilities;
    private final NodeManagementUtilities nodeManagementUtilities;

    public CloseLogic(Project project,
            PostProcessorFeatureSet featureSet, HeadType headType)
    {
        this.project = project;
        this.featureSet = featureSet;

        closeUtilities = new CloseUtilities(project, headType);
        nodeManagementUtilities = new NodeManagementUtilities(featureSet);
    }

    /**
     *
     * @param node
     * @param nozzleInUse
     * @return @see CloseResult
     */
    protected CloseResult insertNozzleCloseFullyAfterEvent(GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
        newNozzleValvePositionNode.getNozzlePosition().setB(nozzleInUse.getNozzleParameters().getClosedPosition());
        node.addSiblingAfter(newNozzleValvePositionNode);

        return new CloseResult(true, 0);
    }

    /**
     *
     * @param availableExtrusion
     * @param nodeToAppendClosesTo
     * @param nozzleInUse
     * @return @see CloseResult
     */
    protected CloseResult insertProgressiveNozzleCloseUpToEvent(double availableExtrusion, GCodeEventNode nodeToAppendClosesTo, final NozzleProxy nozzleInUse)
    {
        CloseResult closeResult = null;

        double requiredEjectionVolume = nozzleInUse.getNozzleParameters().getEjectionVolume();

        if (nodeToAppendClosesTo.getParent() instanceof FillSectionNode)
        {
            closeResult = closeToEndOfSection(nodeToAppendClosesTo, nozzleInUse);
        } else if (nodeToAppendClosesTo.getParent() instanceof OuterPerimeterSectionNode)
        {
            //We're closing from an outer perimeter

            // If our outer perimeter has a smaller extrusion volume than the specified ejection volume
            if (availableExtrusion < requiredEjectionVolume)
            {
                //Don't do anything....
                String outputMessage;
                if (nodeToAppendClosesTo instanceof Renderable)
                {
                    Renderable renderableNode = (Renderable) nodeToAppendClosesTo;
                    outputMessage = "Short extrusion - leaving retract in place " + renderableNode.renderForOutput();
                } else
                {
                    outputMessage = "Short extrusion - leaving retract in place " + nodeToAppendClosesTo.toString();
                }
                steno.debug(outputMessage);
            } else
            {
                // If we have an available fill section to close over and we can close in the required volume then do it
                Optional<GCodeEventNode> fillSection = nodeToAppendClosesTo.getParent().getSiblingAfter();
                if (fillSection.isPresent())
                {
                    if (fillSection.get() instanceof FillSectionNode)
                    {
                        closeResult = closeUsingSectionTemplate(((SectionNode) fillSection.get()),
                                (ExtrusionNode) nodeToAppendClosesTo,
                                nozzleInUse);
                    }
                }

                // If we didn't close in fill
                if (closeResult == null
                        || !closeResult.hasSucceeded())
                {
                    // If we have an available inner section to close over and we can close in the required volume then do it
                    Optional<GCodeEventNode> innerSection = nodeToAppendClosesTo.getParent().getSiblingAfter();
                    if (innerSection.isPresent())
                    {
                        if (innerSection.get() instanceof InnerPerimeterSectionNode)
                        {
                            closeResult = closeUsingSectionTemplate(((SectionNode) innerSection.get()),
                                    (ExtrusionNode) nodeToAppendClosesTo,
                                    nozzleInUse);
                        }
                    }
                }

                if (closeResult == null
                        || !closeResult.hasSucceeded())
                {
                    closeResult = closeToEndOfSection(nodeToAppendClosesTo, nozzleInUse);
                }

                if (closeResult != null
                        && closeResult.hasSucceeded()
                        && closeResult.getClosestNode().isPresent())
                {
                    //Add a travel to the closest node
                    Movement movement = ((MovementProvider) closeResult.getClosestNode().get()).getMovement();
                    TravelNode travelToClosestNode = new TravelNode();
                    travelToClosestNode.getFeedrate().setFeedRate_mmPerMin(400);
                    travelToClosestNode.getMovement().setX(movement.getX());
                    travelToClosestNode.getMovement().setY(movement.getY());
                    nodeToAppendClosesTo.addSiblingAfter(travelToClosestNode);
                } else
                {
                    //Failed to add a close
                    String outputMessage;
                    if (nodeToAppendClosesTo instanceof Renderable)
                    {
                        Renderable renderableNode = (Renderable) nodeToAppendClosesTo;
                        outputMessage = "Couldn't find closest node when looking for close trajectory from outer perimeter " + renderableNode.renderForOutput();
                    } else
                    {
                        outputMessage = "Couldn't find closest node when looking for close trajectory from outer perimeter " + nodeToAppendClosesTo.toString();
                    }
                    steno.debug(outputMessage);
//                    throw new RuntimeException(outputMessage);
                }
            }
        }

        return closeResult;
    }

    /**
     *
     * @param sectionToCopyFrom
     * @param nodeToAppendClosesTo
     * @param nozzleInUse
     * @return @see CloseResult
     */
    protected CloseResult closeUsingSectionTemplate(final SectionNode sectionToCopyFrom, final ExtrusionNode nodeToAppendClosesTo, final NozzleProxy nozzleInUse)
    {
        CloseResult closeResult = new CloseResult(false, 0);
        Optional<GCodeEventNode> closestNode = Optional.empty();

        // Look for a valid intersection
        Optional<IntersectionResult> result = closeUtilities.findClosestMovementNode(nodeToAppendClosesTo, sectionToCopyFrom);
        if (result.isPresent())
        {
            //We'll close in the direction that has most space
            double volumeToCloseOver = nozzleInUse.getNozzleParameters().getEjectionVolume();

            try
            {
                //Try forwards first
                double forwardExtrusionTotal = nodeManagementUtilities.findAvailableExtrusion(result.get().getClosestNode(), true);

                if (forwardExtrusionTotal >= volumeToCloseOver)
                {
                    closestNode = Optional.of(result.get().getClosestNode());
                    closeResult = addClosesUsingSpecifiedNode(nodeToAppendClosesTo, closestNode.get(), nozzleInUse, true, forwardExtrusionTotal, false);
                    closeResult.setClosestNode(closestNode);
                } else
                {
                    //Try backwards
                    double backwardExtrusionTotal = nodeManagementUtilities.findAvailableExtrusion(result.get().getClosestNode(), false);

                    if (backwardExtrusionTotal >= volumeToCloseOver)
                    {
                        closestNode = Optional.of(result.get().getClosestNode());
                        closeResult = addClosesUsingSpecifiedNode(nodeToAppendClosesTo, closestNode.get(), nozzleInUse, false, backwardExtrusionTotal, false);
                        closeResult.setClosestNode(closestNode);
                    }
                }
            } catch (NodeProcessingException ex)
            {
                String outputMessage;
                if (nodeToAppendClosesTo instanceof Renderable)
                {
                    Renderable renderableNode = (Renderable) nodeToAppendClosesTo;
                    outputMessage = "Failure to find correct direction to traverse in for node " + renderableNode.renderForOutput();
                } else
                {
                    outputMessage = "Failure to find correct direction to traverse in for node " + nodeToAppendClosesTo.toString();
                }
                throw new RuntimeException(outputMessage);
            }
        }

        return closeResult;
    }

    /**
     *
     * @param node
     * @param nozzleInUse
     * @return CloseResult
     */
    protected CloseResult closeToEndOfSection(final GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        double elidedExtrusion = 0;

        SectionNode thisSection = (SectionNode) (node.getParent());

        float extrusionInSection = thisSection.streamChildrenAndMeBackwards()
                .filter(extrusionnode -> extrusionnode instanceof ExtrusionNode)
                .map(ExtrusionProvider.class::cast)
                .map(ExtrusionProvider::getExtrusion)
                .map(Extrusion::getE)
                .reduce(0f, (s1, s2) -> s1 + s2);

        NozzleParameters nozzleParams = nozzleInUse.getNozzleParameters();

        double volumeToCloseOver = nozzleParams.getEjectionVolume();

        if (volumeToCloseOver > extrusionInSection)
        {
            volumeToCloseOver = extrusionInSection;
        }

        //Store the amount of extrusion that has been missed out - needed for unretracts
        elidedExtrusion = volumeToCloseOver;

        List<MovementProvider> movementNodes = thisSection.streamChildrenAndMeBackwards()
                .filter(foundNode -> foundNode instanceof MovementProvider)
                .map(MovementProvider.class::cast)
                .collect(Collectors.toList());

        double runningTotalOfExtrusion = 0;

        for (int movementNodeCounter = 0;
                movementNodeCounter < movementNodes.size();
                movementNodeCounter++)
        {
            if (movementNodes.get(movementNodeCounter) instanceof ExtrusionNode)
            {
                ExtrusionNode extrusionNodeBeingExamined = (ExtrusionNode) movementNodes.get(movementNodeCounter);

                int comparisonResult = MathUtils.compareDouble(runningTotalOfExtrusion + extrusionNodeBeingExamined.getExtrusion().getE(), volumeToCloseOver, 0.00001);

                if (comparisonResult == MathUtils.LESS_THAN)
                {
                    //One step along the way
                    double bValue = runningTotalOfExtrusion / volumeToCloseOver;
                    extrusionNodeBeingExamined.getNozzlePosition().setB(bValue);
                    runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                    //No extrusion during a close
                    extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                } else if (comparisonResult == MathUtils.EQUAL)
                {
                    //All done
                    double bValue = runningTotalOfExtrusion / volumeToCloseOver;
                    extrusionNodeBeingExamined.getNozzlePosition().setB(bValue);
                    runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                    //No extrusion during a close
                    extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                    break;
                } else
                {
                    //If we got here then we need to split this extrusion
                    //We're splitting the last part of the line (we're just looking at it in reverse order as we consider the line from the end
                    MovementProvider priorMovement = null;

                    if (movementNodeCounter == movementNodes.size() - 1)
                    {
                        //We dont have anywhere to go!

                        //Look in the section before this one for a usable movement
                        try
                        {
                            Optional<MovementProvider> priorSectionMovement = nodeManagementUtilities.findPriorMovementInPreviousSection(extrusionNodeBeingExamined);
                            priorMovement = priorSectionMovement.get();
                        } catch (NodeProcessingException ex)
                        {
                            // Didn't find it in a prior section - look at prior siblings in case we have a loose travel we can use...
                            try
                            {
                                List<MovementProvider> priorSiblings = extrusionNodeBeingExamined.streamSiblingsAndMeBackwardsFromHere()
                                        .filter(priorNode -> priorNode instanceof MovementProvider)
                                        .map(MovementProvider.class::cast)
                                        .collect(Collectors.toList());

                                if (priorSiblings.size() > 0)
                                {
                                    priorMovement = priorSiblings.get(0);
                                } else
                                {
                                    //Didn't find any prior siblings that are MovementProviders
                                    throw new RuntimeException("Unable to find prior node when splitting extrusion at node " + extrusionNodeBeingExamined.renderForOutput());
                                }
                            } catch (NodeProcessingException priorSiblingException)
                            {
                                throw new RuntimeException("Unable to find prior node in section or siblings when splitting extrusion at node " + extrusionNodeBeingExamined.renderForOutput());
                            }
                        }
                    } else
                    {
                        priorMovement = movementNodes.get(movementNodeCounter + 1);
                    }

                    Vector2D firstPoint = new Vector2D(priorMovement.getMovement().getX(), priorMovement.getMovement().getY());
                    Vector2D secondPoint = new Vector2D(extrusionNodeBeingExamined.getMovement().getX(), extrusionNodeBeingExamined.getMovement().getY());

                    // We can work out how to split this extrusion
                    double extrusionInFirstSection = runningTotalOfExtrusion + extrusionNodeBeingExamined.getExtrusion().getE() - volumeToCloseOver;
                    double extrusionInSecondSection = extrusionNodeBeingExamined.getExtrusion().getE() - extrusionInFirstSection;

                    double proportionOfDistanceInSecondSection = extrusionInFirstSection / extrusionNodeBeingExamined.getExtrusion().getE();

                    Vector2D actualVector = secondPoint.subtract(firstPoint);
                    Vector2D firstSegment = firstPoint.add(proportionOfDistanceInSecondSection,
                            actualVector);

                    ExtrusionNode newExtrusionNode = new ExtrusionNode();
                    newExtrusionNode.setCommentText("Remainder pre close over fill");
                    newExtrusionNode.getExtrusion().setE((float) extrusionInFirstSection);
                    newExtrusionNode.getMovement().setX(firstSegment.getX());
                    newExtrusionNode.getMovement().setY(firstSegment.getY());
                    newExtrusionNode.getFeedrate().setFeedRate_mmPerMin(extrusionNodeBeingExamined.getFeedrate().getFeedRate_mmPerMin());

                    extrusionNodeBeingExamined.addSiblingBefore(newExtrusionNode);

                    extrusionNodeBeingExamined.getExtrusion().setE((float) extrusionInSecondSection);
                    extrusionNodeBeingExamined.appendCommentText("Start of close over fill");
                    double bValue = runningTotalOfExtrusion / volumeToCloseOver;
                    extrusionNodeBeingExamined.getNozzlePosition().setB(bValue);

                    runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                    //No extrusion during a close
                    extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                    break;
                }
            }
        }

        return new CloseResult(true, elidedExtrusion);
    }

    /**
     *
     * @param nodeToAddClosesTo
     * @param nodeToCopyCloseFrom
     * @param nozzleInUse
     * @param towardsEnd
     * @param availableVolume
     * @param closeOverAvailableVolume
     * @return
     */
    protected CloseResult addClosesUsingSpecifiedNode(final ExtrusionNode nodeToAddClosesTo,
            final GCodeEventNode nodeToCopyCloseFrom,
            final NozzleProxy nozzleInUse,
            boolean towardsEnd,
            double availableVolume,
            boolean closeOverAvailableVolume)
    {
        List<MovementProvider> movementNodes = null;

        if (nodeToAddClosesTo.getParent() == null
                || !(nodeToAddClosesTo.getParent() instanceof SectionNode))
        {
            String outputMessage;

            if (nodeToAddClosesTo instanceof Renderable)
            {
                outputMessage = "Parent of specified node " + ((Renderable) nodeToAddClosesTo).renderForOutput() + " is not a section";
            } else
            {
                outputMessage = "Parent of specified node " + nodeToAddClosesTo.toString() + " is not a section";
            }
            throw new RuntimeException(outputMessage);
        }

        SectionNode parentSectionToAddClosesTo = (SectionNode) nodeToAddClosesTo.getParent();

        try
        {
            if (towardsEnd)
            {
                movementNodes = nodeToCopyCloseFrom.streamSiblingsFromHere()
                        .filter(foundNode -> foundNode instanceof MovementProvider)
                        .map(MovementProvider.class::cast)
                        .collect(Collectors.toList());
            } else
            {
                movementNodes = nodeToCopyCloseFrom.streamSiblingsBackwardsFromHere()
                        .filter(foundNode -> foundNode instanceof MovementProvider)
                        .map(MovementProvider.class::cast)
                        .collect(Collectors.toList());
            }
        } catch (NodeProcessingException ex)
        {
            throw new RuntimeException("Failed to stream siblings", ex);
        }

        NozzleParameters nozzleParams = nozzleInUse.getNozzleParameters();

        double volumeToCloseOver = nozzleParams.getEjectionVolume();

        double runningTotalOfExtrusion = 0;
        double currentNozzlePosition = nozzleInUse.getCurrentPosition();
        double closePermm3Volume = closeOverAvailableVolume == false ? currentNozzlePosition / volumeToCloseOver : currentNozzlePosition / availableVolume;
        double requiredVolumeToCloseOver = currentNozzlePosition / closePermm3Volume;

        for (int movementNodeCounter = 0; movementNodeCounter < movementNodes.size(); movementNodeCounter++)
        {
            if (movementNodes.get(movementNodeCounter) instanceof ExtrusionNode)
            {
                ExtrusionNode extrusionNodeBeingExamined = (ExtrusionNode) movementNodes.get(movementNodeCounter);

                int comparisonResult = MathUtils.compareDouble(runningTotalOfExtrusion + extrusionNodeBeingExamined.getExtrusion().getE(), requiredVolumeToCloseOver, 0.00001);

                ExtrusionNode copy = extrusionNodeBeingExamined.clone();
                parentSectionToAddClosesTo.addChildAtEnd(copy);

                if (comparisonResult == MathUtils.LESS_THAN)
                {
                    //One step along the way
                    currentNozzlePosition = currentNozzlePosition - copy.getExtrusion().getE() * closePermm3Volume;
                    double bValue = currentNozzlePosition;
                    copy.getNozzlePosition().setB(bValue);
                    runningTotalOfExtrusion += copy.getExtrusion().getE();
                    //No extrusion during a close
                    copy.getExtrusion().eNotInUse();
                } else if (comparisonResult == MathUtils.EQUAL)
                {
                    //All done
                    currentNozzlePosition = 0;
                    double bValue = 0;
                    copy.getNozzlePosition().setB(bValue);
                    runningTotalOfExtrusion += copy.getExtrusion().getE();
                    //No extrusion during a close
                    copy.getExtrusion().eNotInUse();
                    break;
                } else
                {
                    MovementProvider priorMovement = null;

                    if (movementNodeCounter == 0)
                    {
                        priorMovement = (MovementProvider) nodeToCopyCloseFrom;
                        //We dont have anywhere to go!
//                        try
//                        {
//                            Optional<MovementProvider> priorSectionMovement = findPriorMovementInPreviousSection(extrusionNodeBeingExamined);
//                            priorMovement = priorSectionMovement.get();
//                        } catch (NodeProcessingException ex)
//                        {
//                        throw new RuntimeException("No prior node to extrapolate from when closing around node " + extrusionNodeBeingExamined.renderForOutput());
//                        }

                    } else
                    {
                        priorMovement = movementNodes.get(movementNodeCounter - 1);
                    }

                    // We can work out how to split this extrusion
                    Vector2D firstPoint = new Vector2D(priorMovement.getMovement().getX(), priorMovement.getMovement().getY());
                    Vector2D secondPoint = new Vector2D(extrusionNodeBeingExamined.getMovement().getX(), extrusionNodeBeingExamined.getMovement().getY());

                    double extrusionInFirstSection = runningTotalOfExtrusion + extrusionNodeBeingExamined.getExtrusion().getE() - requiredVolumeToCloseOver;

                    double proportionOfDistanceInFirstSection = extrusionInFirstSection / extrusionNodeBeingExamined.getExtrusion().getE();

                    Vector2D actualVector = secondPoint.subtract(firstPoint);
                    Vector2D firstSegment = firstPoint.add(proportionOfDistanceInFirstSection,
                            actualVector);

                    copy.getMovement().setX(firstSegment.getX());
                    copy.getMovement().setY(firstSegment.getY());
                    copy.getExtrusion().setE(0);
                    copy.getExtrusion().setD(0);
                    copy.appendCommentText("End of close segment");
                    copy.getNozzlePosition().setB(0);

                    runningTotalOfExtrusion += copy.getExtrusion().getE();

                    //No extrusion during a close
                    copy.getExtrusion().eNotInUse();
                    break;
                }

            }
        }

        return new CloseResult(true, volumeToCloseOver);
    }

    /**
     *
     * @param extrusionUpToClose
     * @param node
     * @param nozzleInUse
     * @return @see CloseResult
     */
    protected CloseResult insertNozzleCloses(double extrusionUpToClose, GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        CloseResult closeResult = null;

        //Assume the nozzle is always fully open...
        nozzleInUse.setCurrentPosition(1.0);
        if (featureSet.isEnabled(PostProcessorFeature.GRADUAL_CLOSE))
        {
            closeResult = insertProgressiveNozzleCloseUpToEvent(extrusionUpToClose, node, nozzleInUse);
        } else
        {
            closeResult = insertNozzleCloseFullyAfterEvent(node, nozzleInUse);
        }

        return closeResult;
    }

    protected void insertCloseNodes(LayerNode layerNode, LayerPostProcessResult lastLayerParseResult, List<NozzleProxy> nozzleProxies)
    {
        layerNode.stream()
                .filter(node -> node instanceof ToolSelectNode)
                .forEach(node ->
                        {
                            ToolSelectNode toolSelectNode = (ToolSelectNode) node;
                            NozzleProxy nozzleInUse = nozzleProxies.get(toolSelectNode.getToolNumber());

                            if (featureSet.isEnabled(PostProcessorFeature.CLOSES_ON_RETRACT))
                            {
                                // Find all of the retracts in this layer
                                layerNode.stream()
                                .filter(foundnode -> foundnode instanceof RetractNode)
                                .forEach(foundnode ->
                                        {
                                            CloseResult closeResult = null;
                                            RetractNode retractNode = (RetractNode) foundnode;
                                            Optional<GCodeEventNode> nextExtrusionNode = Optional.empty();
                                            try
                                            {
                                                Optional<GCodeEventNode> priorExtrusionNode = nodeManagementUtilities.findPriorExtrusion(retractNode);
                                                if (priorExtrusionNode.isPresent())
                                                {
                                                    closeResult = insertNozzleCloses(retractNode.getExtrusionSinceLastRetract(), priorExtrusionNode.get(), nozzleInUse);
                                                } else
                                                {
                                                    LayerNode lastLayer = lastLayerParseResult.getLayerData();

                                                    //Look for the last extrusion on the previous layer
                                                    if (lastLayer.getLayerNumber() < 0)
                                                    {
                                                        // There wasn't a last layer - this is a lone retract at the start of the file
                                                        steno.warning("Discarding retract from layer " + layerNode.getLayerNumber());
                                                    } else
                                                    {
                                                        Optional<GCodeEventNode> priorExtrusionNodeLastLayer = nodeManagementUtilities.findLastExtrusionEventInLayer(lastLayer);
                                                        if (!priorExtrusionNodeLastLayer.isPresent())
                                                        {
                                                            throw new NodeProcessingException("No suitable prior extrusion in previous layer", node);
                                                        }
                                                        double availableExtrusion = nodeManagementUtilities.findAvailableExtrusion(priorExtrusionNodeLastLayer.get(), false);

                                                        closeResult = insertNozzleCloses(availableExtrusion, priorExtrusionNodeLastLayer.get(), nozzleInUse);
                                                    }
                                                }

                                                if (closeResult != null
                                                && closeResult.hasSucceeded())
                                                {
                                                    if (!nextExtrusionNode.isPresent())
                                                    {
                                                        nextExtrusionNode = nodeManagementUtilities.findNextExtrusion(retractNode);
                                                    }

                                                    if (nextExtrusionNode.isPresent()
                                                    && closeResult.getElidedExtrusion() > 0)
                                                    {
                                                        //Add the elided extrusion to this node - the open routine will find it later
                                                        ((ExtrusionNode) nextExtrusionNode.get()).setElidedExtrusion(closeResult.getElidedExtrusion());
                                                    } else
                                                    {
                                                        steno.warning("Couldn't append elided extrusion after " + retractNode.renderForOutput());
                                                    }

                                                    retractNode.removeFromParent();
                                                } else
                                                {
                                                    retractNode.appendCommentText("Retract retained");

                                                    if (!nextExtrusionNode.isPresent())
                                                    {
                                                        nextExtrusionNode = nodeManagementUtilities.findNextExtrusion(retractNode);
                                                    }

                                                    if (nextExtrusionNode.isPresent())
                                                    {
                                                        //Insert an unretract to complement the retract
                                                        UnretractNode newUnretract = new UnretractNode();
                                                        newUnretract.getExtrusion().setE(Math.abs(retractNode.getExtrusion().getE()));
                                                        newUnretract.setCommentText("Compensation for retract");
                                                        nextExtrusionNode.get().addSiblingBefore(newUnretract);
                                                    } else
                                                    {
                                                        steno.warning("Couldn't insert compensation for retract " + retractNode.renderForOutput());
                                                    }
                                                }

                                            } catch (NodeProcessingException ex)
                                            {
                                                throw new RuntimeException("Failed to process retract on layer " + layerNode.getLayerNumber() + " this will affect open and close", ex);
                                            }
                                });
                            }
                }
                );

    }
}
