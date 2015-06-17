package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.NozzleValvePositionNode;
import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.TravelNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.Extrusion;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
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
            PostProcessorFeatureSet featureSet)
    {
        this.project = project;
        this.featureSet = featureSet;

        closeUtilities = new CloseUtilities(project);
        nodeManagementUtilities = new NodeManagementUtilities(featureSet);
    }

    /**
     *
     * @param node
     * @param nozzleInUse
     * @return True if the insert succeeded
     */
    protected boolean insertNozzleCloseFullyAfterEvent(GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
        newNozzleValvePositionNode.getNozzlePosition().setB(nozzleInUse.getNozzleParameters().getClosedPosition());
        node.addSiblingAfter(newNozzleValvePositionNode);

        return true;
    }

    /**
     *
     * @param availableExtrusion
     * @param nodeToAppendClosesTo
     * @param nozzleInUse
     */
    protected boolean insertProgressiveNozzleCloseUpToEvent(double availableExtrusion, GCodeEventNode nodeToAppendClosesTo, final NozzleProxy nozzleInUse)
    {
        boolean succeeded = false;

        double requiredEjectionVolume = nozzleInUse.getNozzleParameters().getEjectionVolume();

        if (nodeToAppendClosesTo.getParent() instanceof FillSectionNode)
        {
            closeToEndOfFill(nodeToAppendClosesTo, nozzleInUse);
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
                steno.info(outputMessage);
            } else
            {
                Optional<GCodeEventNode> closestNode = Optional.empty();

                // If we have an available fill section to close over and we can close in the required volume then do it
                Optional<GCodeEventNode> fillSection = nodeToAppendClosesTo.getParent().getSiblingAfter();
                if (fillSection.isPresent())
                {
                    if (fillSection.get() instanceof FillSectionNode)
                    {
                        closestNode = closeUsingSectionTemplate(((SectionNode) fillSection.get()),
                                (ExtrusionNode) nodeToAppendClosesTo,
                                nozzleInUse);
                    }
                }

                // If we didn't close in fill
                if (!closestNode.isPresent())
                {
                    // If we have an available inner section to close over and we can close in the required volume then do it
                    Optional<GCodeEventNode> innerSection = nodeToAppendClosesTo.getParent().getSiblingAfter();
                    if (innerSection.isPresent())
                    {
                        if (innerSection.get() instanceof InnerPerimeterSectionNode)
                        {
                            closestNode = closeUsingSectionTemplate(((SectionNode) innerSection.get()),
                                    (ExtrusionNode) nodeToAppendClosesTo,
                                    nozzleInUse);
                        }
                    }
                }

                if (closestNode.isPresent())
                {
                    //Add a travel to the closest node
                    Movement movement = ((MovementProvider) closestNode.get()).getMovement();
                    TravelNode travelToClosestNode = new TravelNode();
                    travelToClosestNode.getFeedrate().setFeedRate_mmPerMin(400);
                    travelToClosestNode.getMovement().setX(movement.getX());
                    travelToClosestNode.getMovement().setY(movement.getY());
                    nodeToAppendClosesTo.addSiblingAfter(travelToClosestNode);

                    succeeded = true;
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
                    steno.info(outputMessage);
//                    throw new RuntimeException(outputMessage);
                }
            }
        }

        return succeeded;
    }

    /**
     *
     * @param sectionToCopyFrom
     * @param nodeToAppendClosesTo
     * @param nozzleInUse
     * @return Returns the first closest node to the specified node
     */
    protected Optional<GCodeEventNode> closeUsingSectionTemplate(final SectionNode sectionToCopyFrom, final ExtrusionNode nodeToAppendClosesTo, final NozzleProxy nozzleInUse)
    {
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
                    addClosesUsingSpecifiedNode(nodeToAppendClosesTo, closestNode.get(), nozzleInUse, true, forwardExtrusionTotal, false);
                } else
                {
                    //Try backwards
                    double backwardExtrusionTotal = nodeManagementUtilities.findAvailableExtrusion(result.get().getClosestNode(), false);

                    if (backwardExtrusionTotal >= volumeToCloseOver)
                    {
                        closestNode = Optional.of(result.get().getClosestNode());
                        addClosesUsingSpecifiedNode(nodeToAppendClosesTo, closestNode.get(), nozzleInUse, false, backwardExtrusionTotal, false);
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
        return closestNode;
    }

    protected void closeToEndOfFill(final GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
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
        nozzleInUse.setElidedExtrusion(volumeToCloseOver);

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
                        try
                        {
                            Optional<MovementProvider> priorSectionMovement = nodeManagementUtilities.findPriorMovementInPreviousSection(extrusionNodeBeingExamined);
                            priorMovement = priorSectionMovement.get();
                        } catch (NodeProcessingException ex)
                        {
                            throw new RuntimeException("Unable to find prior node when splitting extrusion at node " + extrusionNodeBeingExamined.renderForOutput());
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
    }

    private void dontCloseAndReinstateUnretract()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected void addClosesUsingSpecifiedNode(final ExtrusionNode nodeToAddClosesTo,
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

        //Store the amount of extrusion that has been missed out - needed for unretracts
        nozzleInUse.setElidedExtrusion(volumeToCloseOver);
    }
}
