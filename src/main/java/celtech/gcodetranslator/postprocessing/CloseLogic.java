package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.GCodeRoboxiser;
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
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.IteratorWithOrigin;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;
import celtech.utils.Math.MathUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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
    private final SlicerParametersFile settings;

    public CloseLogic(Project project, SlicerParametersFile settings,
            PostProcessorFeatureSet featureSet, String headType)
    {
        this.project = project;
        this.settings = settings;
        this.featureSet = featureSet;

        closeUtilities = new CloseUtilities(project, settings, headType);
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

        
        // If not a perimeter then attempt to close to the end of the extrusion
        boolean failedToCloseOverNonPerimeter = false;
        
        if (nodeToAppendClosesTo.getParent().isPresent()
                && !(nodeToAppendClosesTo.getParent().get() instanceof OuterPerimeterSectionNode)
                && !(nodeToAppendClosesTo.getParent().get() instanceof InnerPerimeterSectionNode))
        {
            closeResult = closeToEndOfSection(nodeToAppendClosesTo, nozzleInUse);
            failedToCloseOverNonPerimeter = !closeResult.hasSucceeded();
        }
        
        if (failedToCloseOverNonPerimeter)
        {
            // Attempt an inward close from the end of the non-perimeter extrusion
            close
        }
        else if (closeResult == null)
        {
            // We must be looking at a perimeter - clo
        }
        
        
        
//        else if (nodeToAppendClosesTo.getParent().isPresent()
//                && nodeToAppendClosesTo.getParent().get() instanceof OuterPerimeterSectionNode)
//        {
            //We're closing from an outer perimeter

            // If our outer perimeter has a smaller extrusion volume than the specified ejection volume
//            if (availableExtrusion < requiredEjectionVolume)
//            {
//                //Don't do anything....
//                String outputMessage;
//                if (nodeToAppendClosesTo instanceof Renderable)
//                {
//                    Renderable renderableNode = (Renderable) nodeToAppendClosesTo;
//                    outputMessage = "Short extrusion - leaving retract in place " + renderableNode.renderForOutput();
//                } else
//                {
//                    outputMessage = "Short extrusion - leaving retract in place " + nodeToAppendClosesTo.toString();
//                }
//                steno.debug(outputMessage);
//            } else
            {
                // If we have an available fill section to close over and we can close in the required volume then do it
//                Optional<GCodeEventNode> fillSection = nodeToAppendClosesTo.getParent().get().getSiblingAfter();
//                if (fillSection.isPresent())
//                {
//                    if (fillSection.get() instanceof FillSectionNode)
//                    {
//                        closeResult = closeUsingSectionTemplate(((SectionNode) fillSection.get()),
//                                (ExtrusionNode) nodeToAppendClosesTo,
//                                nozzleInUse);
//                    }
//                }
//
//                // If we didn't close in fill
//                if (closeResult == null
//                        || !closeResult.hasSucceeded())
//                {
//                    // If we have an available inner section to close over and we can close in the required volume then do it
//                    Optional<GCodeEventNode> innerSection = nodeToAppendClosesTo.getParent().get().getSiblingBefore();
//                    if (innerSection.isPresent())
//                    {
//                        if (innerSection.get() instanceof InnerPerimeterSectionNode)
//                        {
//                            closeResult = closeUsingSectionTemplate(((SectionNode) innerSection.get()),
//                                    (ExtrusionNode) nodeToAppendClosesTo,
//                                    nozzleInUse);
//                        }
//                    }
//                }
//
//                if (closeResult == null
//                        || !closeResult.hasSucceeded())
//                {
//                    GCodeEventNode lastNodeConsidered = null;
//                    GCodeEventNode limitNode = null;
//
//                    IteratorWithOrigin<GCodeEventNode> nodeIterator = nodeToAppendClosesTo.siblingsBackwardsIterator();
//
//                    while (nodeIterator.hasNext())
//                    {
//                        GCodeEventNode potentialExtrusionNode = nodeIterator.next();
//
//                        if (potentialExtrusionNode instanceof ExtrusionNode)
//                        {
//                            if (((ExtrusionNode) potentialExtrusionNode).getElidedExtrusion() > 0)
//                            {
//                                limitNode = lastNodeConsidered;
//                                break;
//                            }
//                            lastNodeConsidered = potentialExtrusionNode;
//                        }
//                    }
//
//                    if (limitNode == null && lastNodeConsidered != null)
//                    {
//                        //No problem - we just didn't find a prior close in this section
//                        limitNode = lastNodeConsidered;
//                    }
//
//                    if (limitNode != null)
//                    {
//                        closeResult = closeFromEreToEre(limitNode, nodeToAppendClosesTo, nozzleInUse, true);
//                    } else
//                    {
//                        throw new RuntimeException("Couldn't find limit to for close from ere to ere");
//                    }
//                }
//
//                if (closeResult != null
//                        && closeResult.hasSucceeded()
//                        && closeResult.getClosestNode().isPresent())
//                {
//                    //Add a travel to the closest node
//                    Movement movement = ((MovementProvider) closeResult.getClosestNode().get()).getMovement();
//                    TravelNode travelToClosestNode = new TravelNode();
//                    travelToClosestNode.getFeedrate().setFeedRate_mmPerMin(400);
//                    travelToClosestNode.getMovement().setX(movement.getX());
//                    travelToClosestNode.getMovement().setY(movement.getY());
//                    nodeToAppendClosesTo.addSiblingAfter(travelToClosestNode);
//                } else
//                {
//                    //Failed to add a close
//                    String outputMessage;
//                    if (nodeToAppendClosesTo instanceof Renderable)
//                    {
//                        Renderable renderableNode = (Renderable) nodeToAppendClosesTo;
//                        outputMessage = "Couldn't find closest node when looking for close trajectory from outer perimeter " + renderableNode.renderForOutput();
//                    } else
//                    {
//                        outputMessage = "Couldn't find closest node when looking for close trajectory from outer perimeter " + nodeToAppendClosesTo.toString();
//                    }
//                    steno.debug(outputMessage);
////                    throw new RuntimeException(outputMessage);
//                }
//            }
        }

        return closeResult;
    }
    
    private Optional<GCodeRoboxiser.CloseResult> closeInwardsFromEndOfLastExtrusion(final ExtrusionNode nodeToAppendClosesTo, final NozzleProxy nozzleInUse)
    {
        
    }

    private Optional<GCodeRoboxiser.CloseResult> closeInwardsFromEndOfPerimeter(final ExtrusionNode nodeToAppendClosesTo, final NozzleProxy nozzleInUse)
    {
        
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

        SectionNode thisSection = (SectionNode) (node.getParent().get());

        IteratorWithOrigin<GCodeEventNode> sectionChildAndMeBackwardsIterator = thisSection.childrenAndMeBackwardsIterator();

        float extrusionInSection = 0;

        while (sectionChildAndMeBackwardsIterator.hasNext())
        {
            GCodeEventNode nodeToTest = sectionChildAndMeBackwardsIterator.next();
            if (nodeToTest instanceof ExtrusionNode)
            {
                extrusionInSection += ((ExtrusionNode) nodeToTest).getExtrusion().getE();
            }
        }

        NozzleParameters nozzleParams = nozzleInUse.getNozzleParameters();

        double volumeToCloseOver = nozzleParams.getEjectionVolume();

        if (volumeToCloseOver > extrusionInSection)
        {
            volumeToCloseOver = extrusionInSection;
        }

        //Store the amount of extrusion that has been missed out - needed for unretracts
        elidedExtrusion = volumeToCloseOver;

        List<MovementProvider> movementNodes = new ArrayList<>();

        IteratorWithOrigin<GCodeEventNode> sectionBackwards = thisSection.childrenAndMeBackwardsIterator();

        while (sectionBackwards.hasNext())
        {
            GCodeEventNode potentialMovementProvider = sectionBackwards.next();

            if (potentialMovementProvider instanceof MovementProvider)
            {
                movementNodes.add((MovementProvider) potentialMovementProvider);
            }
        }

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

        return new CloseResult(true, elidedExtrusion);
    }

    /**
     *
     * @param startNode
     * @param endNode
     * @param nozzleInUse
     * @param forwards
     * @return CloseResult
     */
    protected CloseResult closeFromEreToEre(final GCodeEventNode startNode,
            final GCodeEventNode endNode,
            final NozzleProxy nozzleInUse,
            final boolean forwards)
    {
        double elidedExtrusion = 0;
        double availableExtrusion = 0;

        List<MovementProvider> movementNodes = new ArrayList<>();

        IteratorWithOrigin<GCodeEventNode> nodeIterator = null;

        if (forwards)
        {
            nodeIterator = startNode.siblingsIterator();
        } else
        {
            nodeIterator = startNode.siblingsBackwardsIterator();
        }

        while (nodeIterator.hasNext())
        {
            GCodeEventNode potentialMovementProvider = nodeIterator.next();

            if (potentialMovementProvider instanceof MovementProvider)
            {
                movementNodes.add((MovementProvider) potentialMovementProvider);
            }

            if (potentialMovementProvider instanceof ExtrusionNode)
            {
                availableExtrusion += ((ExtrusionNode) potentialMovementProvider).getExtrusion().getE();
            }

            if (potentialMovementProvider == endNode)
            {
                break;
            }
        }

        NozzleParameters nozzleParams = nozzleInUse.getNozzleParameters();

        double volumeToCloseOver = nozzleParams.getEjectionVolume();

        if (volumeToCloseOver > availableExtrusion)
        {
            volumeToCloseOver = availableExtrusion;
        }

        //Store the amount of extrusion that has been missed out - needed for unretracts
        elidedExtrusion = volumeToCloseOver;

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
                    double bValue;
                    if (forwards)
                    {
                        runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                        bValue = 1 - (runningTotalOfExtrusion / volumeToCloseOver);
                    } else
                    {
                        bValue = runningTotalOfExtrusion / volumeToCloseOver;
                        runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                    }

                    extrusionNodeBeingExamined.getNozzlePosition().setB(bValue);
                    //No extrusion during a close
                    extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                } else if (comparisonResult == MathUtils.EQUAL)
                {
                    //All  / volumedone
                    double bValue;
                    if (forwards)
                    {
                        runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                        bValue = 1 - (runningTotalOfExtrusion / volumeToCloseOver);
                    } else
                    {
                        bValue = runningTotalOfExtrusion / volumeToCloseOver;
                        runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                    }

                    extrusionNodeBeingExamined.getNozzlePosition().setB(bValue);
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
        if (!nodeToAddClosesTo.getParent().isPresent()
                || !(nodeToAddClosesTo.getParent().get() instanceof SectionNode))
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

        SectionNode parentSectionToAddClosesTo = (SectionNode) nodeToAddClosesTo.getParent().get();

        NozzleParameters nozzleParams = nozzleInUse.getNozzleParameters();

        double volumeToCloseOver = nozzleParams.getEjectionVolume();

        double runningTotalOfExtrusion = 0;
        double currentNozzlePosition = nozzleInUse.getCurrentPosition();
        double closePermm3Volume = closeOverAvailableVolume == false ? currentNozzlePosition / volumeToCloseOver : currentNozzlePosition / availableVolume;
        double requiredVolumeToCloseOver = currentNozzlePosition / closePermm3Volume;

        Iterator<GCodeEventNode> nodesToCopyFrom = null;

        if (towardsEnd)
        {
            nodesToCopyFrom = nodeToCopyCloseFrom.siblingsIterator();
        } else
        {
            nodesToCopyFrom = nodeToCopyCloseFrom.siblingsBackwardsIterator();
        }

        List<MovementProvider> movementNodes = new ArrayList<>();
        while (nodesToCopyFrom.hasNext())
        {
            GCodeEventNode potentialMovementNode = nodesToCopyFrom.next();
            if (potentialMovementNode instanceof MovementProvider)
            {
                movementNodes.add((MovementProvider) potentialMovementNode);
            }
        }

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
//                =========
//        
//        
//        while (nodesToCopyFrom.hasNext())
//        {
//            GCodeEventNode potentialExtrusionNode = nodesToCopyFrom.next();
//
//            if (potentialExtrusionNode instanceof ExtrusionNode)
//            {
//                ExtrusionNode extrusionNodeBeingExamined = (ExtrusionNode) potentialExtrusionNode;
//
//                int comparisonResult = MathUtils.compareDouble(runningTotalOfExtrusion + extrusionNodeBeingExamined.getExtrusion().getE(), requiredVolumeToCloseOver, 0.00001);
//
//                ExtrusionNode copy = extrusionNodeBeingExamined.clone();
//                parentSectionToAddClosesTo.addChildAtEnd(copy);
//
//                if (comparisonResult == MathUtils.LESS_THAN)
//                {
//                    //One step along the way
//                    currentNozzlePosition = currentNozzlePosition - copy.getExtrusion().getE() * closePermm3Volume;
//                    double bValue = currentNozzlePosition;
//                    copy.getNozzlePosition().setB(bValue);
//                    runningTotalOfExtrusion += copy.getExtrusion().getE();
//                    //No extrusion during a close
//                    copy.getExtrusion().eNotInUse();
//                } else if (comparisonResult == MathUtils.EQUAL)
//                {
//                    //All done
//                    currentNozzlePosition = 0;
//                    double bValue = 0;
//                    copy.getNozzlePosition().setB(bValue);
//                    runningTotalOfExtrusion += copy.getExtrusion().getE();
//                    //No extrusion during a close
//                    copy.getExtrusion().eNotInUse();
//                    break;
//                } else
//                {
//                    MovementProvider priorMovement = null;
//
//                    Optional<MovementProvider> priorMovementInThisSection = Optional.empty();
//
//                    try
//                    {
//                        priorMovementInThisSection = nodeManagementUtilities.findPriorMovement(nodeToCopyCloseFrom);
//                    } catch (NodeProcessingException priorSiblingException)
//                    {
//                        throw new RuntimeException("Unable to find prior node in section or siblings when splitting extrusion at node " + extrusionNodeBeingExamined.renderForOutput(), priorSiblingException);
//                    }
//
//                    if (!priorMovementInThisSection.isPresent())
//                    {
//                        //We dont have anywhere to go!
////                        try
////                        {
////                            Optional<MovementProvider> priorSectionMovement = findPriorMovementInPreviousSection(extrusionNodeBeingExamined);
////                            priorMovement = priorSectionMovement.get();
////                        } catch (NodeProcessingException ex)
////                        {
//                        throw new RuntimeException("No prior node to extrapolate from when closing around node " + extrusionNodeBeingExamined.renderForOutput());
////                        }
//
//                    } else
//                    {
//                        priorMovement = priorMovementInThisSection.get();
//                    }
//
//                    // We can work out how to split this extrusion
//                    Vector2D firstPoint = new Vector2D(priorMovement.getMovement().getX(), priorMovement.getMovement().getY());
//                    Vector2D secondPoint = new Vector2D(extrusionNodeBeingExamined.getMovement().getX(), extrusionNodeBeingExamined.getMovement().getY());
//
//                    double extrusionInFirstSection = runningTotalOfExtrusion + extrusionNodeBeingExamined.getExtrusion().getE() - requiredVolumeToCloseOver;
//
//                    double proportionOfDistanceInFirstSection = extrusionInFirstSection / extrusionNodeBeingExamined.getExtrusion().getE();
//
//                    Vector2D actualVector = secondPoint.subtract(firstPoint);
//                    Vector2D firstSegment = firstPoint.add(proportionOfDistanceInFirstSection,
//                            actualVector);
//
//                    copy.getMovement().setX(firstSegment.getX());
//                    copy.getMovement().setY(firstSegment.getY());
//                    copy.getExtrusion().setE(0);
//                    copy.getExtrusion().setD(0);
//                    copy.appendCommentText("End of close segment");
//                    copy.getNozzlePosition().setB(0);
//
//                    runningTotalOfExtrusion += copy.getExtrusion().getE();
//
//                    //No extrusion during a close
//                    copy.getExtrusion().eNotInUse();
//                    break;
//                }
//
//            }
//        }

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
        //Tool select nodes are directly under a layer
        Iterator<GCodeEventNode> layerChildIterator = layerNode.childIterator();

        while (layerChildIterator.hasNext())
        {
            GCodeEventNode layerChild = layerChildIterator.next();

            if (layerChild instanceof ToolSelectNode)
            {
                ToolSelectNode toolSelectNode = (ToolSelectNode) layerChild;

                NozzleProxy nozzleInUse = nozzleProxies.get(toolSelectNode.getToolNumber());

                if (featureSet.isEnabled(PostProcessorFeature.CLOSES_ON_RETRACT))
                {
                    Iterator<GCodeEventNode> toolSelectChildren = toolSelectNode.treeSpanningIterator(null);

                    List<RetractNode> retractNodes = new ArrayList<>();

                    while (toolSelectChildren.hasNext())
                    {
                        GCodeEventNode toolSelectChild = toolSelectChildren.next();

                        if (toolSelectChild instanceof RetractNode)
                        {
                            retractNodes.add((RetractNode) toolSelectChild);
                        }
                    }

                    for (RetractNode retractNode : retractNodes)
                    {
                        CloseResult closeResult = null;

                        Optional<ExtrusionNode> nextExtrusionNode = Optional.empty();
                        try
                        {
                            if (retractNode.getPriorExtrusionNode() != null)
                            {
                                closeResult = insertNozzleCloses(retractNode.getExtrusionSinceLastRetract(), retractNode.getPriorExtrusionNode(), nozzleInUse);
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
                                    GCodeEventNode lastExtrusionEventEventLastLayer = null;

                                    GCodeEventNode priorExtrusionNodeLastLayer = lastLayer.getAbsolutelyTheLastEvent();

                                    if (priorExtrusionNodeLastLayer != null)
                                    {
                                        if (!(priorExtrusionNodeLastLayer instanceof ExtrusionNode))
                                        {
                                            Optional<ExtrusionNode> potentialLastExtrusion = nodeManagementUtilities.findPriorExtrusion(layerChild);
                                            if (potentialLastExtrusion.isPresent())
                                            {
                                                lastExtrusionEventEventLastLayer = potentialLastExtrusion.get();
                                            }
                                        } else
                                        {
                                            lastExtrusionEventEventLastLayer = priorExtrusionNodeLastLayer;
                                        }
                                    }
                                    
                                    if (lastExtrusionEventEventLastLayer == null)
                                    {
                                        throw new NodeProcessingException("No suitable prior extrusion in previous layer", (GCodeEventNode) retractNode);
                                    }

                                    double availableExtrusion = nodeManagementUtilities.findAvailableExtrusion(lastExtrusionEventEventLastLayer, false);

                                    closeResult = insertNozzleCloses(availableExtrusion, lastExtrusionEventEventLastLayer, nozzleInUse);
                                }
                            }

                            if (closeResult != null
                                    && closeResult.hasSucceeded())
                            {
                                if (!nextExtrusionNode.isPresent())
                                {
                                    nextExtrusionNode = nodeManagementUtilities.findNextExtrusion(layerNode, retractNode);
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
                                    nextExtrusionNode = nodeManagementUtilities.findNextExtrusion(layerNode, retractNode);
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
                    }
                }

            }
        }
    }
}
