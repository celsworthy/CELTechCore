package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.CannotCloseFromPerimeterException;
import celtech.gcodetranslator.DidntFindEventException;
import celtech.gcodetranslator.EventType;
import celtech.gcodetranslator.NoPerimeterToCloseOverException;
import celtech.gcodetranslator.NotEnoughAvailableExtrusionException;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.PostProcessingError;
import celtech.gcodetranslator.events.NozzleChangeBValueEvent;
import celtech.gcodetranslator.events.NozzleOpenFullyEvent;
import celtech.gcodetranslator.postprocessing.nodes.CommentNode;
import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
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
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.IteratorWithOrigin;
import celtech.gcodetranslator.postprocessing.nodes.providers.Feedrate;
import celtech.gcodetranslator.postprocessing.nodes.providers.FeedrateProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;
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
    protected Optional<CloseResult> insertNozzleCloseFullyAfterEvent(GCodeEventNode node, final NozzleProxy nozzleInUse)
    {
        Optional<CloseResult> closeResult = Optional.of(new CloseResult(1.0, 0));

        NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
        newNozzleValvePositionNode.getNozzlePosition().setB(nozzleInUse.getNozzleParameters().getClosedPosition());
        node.addSiblingAfter(newNozzleValvePositionNode);

        return closeResult;
    }

    protected InScopeEvents extractAvailableMovements(GCodeEventNode startingNode,
            List<SectionNode> sectionsToConsider,
            boolean includePerimeters,
            boolean reprioritise)
    {
        List<SectionNode> availableSectionsToCloseOver = new ArrayList<>();

        if (reprioritise)
        {
            //Reorder the sections
            List<SectionNode> externalPerimeterSections = new ArrayList<>();
            List<SectionNode> innerPerimeterSections = new ArrayList<>();
            List<SectionNode> otherExtrusionSections = new ArrayList<>();

            for (int sectionCounter = sectionsToConsider.size() - 1; sectionCounter >= 0; sectionCounter--)
            {
                SectionNode sectionNode = sectionsToConsider.get(sectionCounter);

                if (sectionNode instanceof OuterPerimeterSectionNode)
                {
                    externalPerimeterSections.add(0, sectionNode);
                } else if (sectionNode instanceof InnerPerimeterSectionNode)
                {
                    innerPerimeterSections.add(0, sectionNode);
                } else
                {
                    otherExtrusionSections.add(0, sectionNode);
                }
            }

            availableSectionsToCloseOver.addAll(otherExtrusionSections);

            if (includePerimeters)
            {
                availableSectionsToCloseOver.addAll(innerPerimeterSections);
                availableSectionsToCloseOver.addAll(externalPerimeterSections);
            }
        } else
        {
            if (includePerimeters)
            {
                availableSectionsToCloseOver = sectionsToConsider;
            } else
            {
                List<SectionNode> tempSectionHolder = new ArrayList<>();
                for (SectionNode section : sectionsToConsider)
                {
                    if (!(section instanceof OuterPerimeterSectionNode)
                            && (!(section instanceof InnerPerimeterSectionNode)))
                    {
                        tempSectionHolder.add(section);
                    }
                }

                availableSectionsToCloseOver = tempSectionHolder;
            }
        }

        int sectionDelta = -1;
        int sectionCounter = 0;
        boolean haveConsumedStartNode = false;

        //Work out which section to start in
        if (startingNode.getParent().isPresent()
                && startingNode.getParent().get() instanceof SectionNode)
        {
            for (int sectionSearch = 0; sectionSearch < availableSectionsToCloseOver.size(); sectionSearch++)
            {
                if (availableSectionsToCloseOver.get(sectionSearch) == startingNode.getParent().get())
                {
                    sectionCounter = sectionSearch;
                    break;
                }
            }
        }

        sectionDelta = (sectionCounter == 0) ? 1 : -1;

        List<GCodeEventNode> movementNodes = new ArrayList<>();

        boolean keepLooking = true;
        double availableExtrusion = 0;

        while (sectionCounter >= 0
                && sectionCounter < availableSectionsToCloseOver.size()
                && keepLooking)
        {
            SectionNode sectionNode = availableSectionsToCloseOver.get(sectionCounter);
            sectionCounter += sectionDelta;

            Iterator<GCodeEventNode> sectionIterator = null;
            if (!haveConsumedStartNode)
            {
                sectionIterator = startingNode.meAndSiblingsBackwardsIterator();
                haveConsumedStartNode = true;
            } else
            {
                sectionIterator = sectionNode.childBackwardsIterator();
            }

            while (sectionIterator.hasNext())
            {
                GCodeEventNode node = sectionIterator.next();
                if (node instanceof NozzleValvePositionNode)
                {
                    movementNodes.add(node);
                    keepLooking = false;
                    break;
                } else if (node instanceof NozzlePositionProvider
                        && ((NozzlePositionProvider) node).getNozzlePosition().isBSet())
                {
                    keepLooking = false;
                    break;
                } else if (node instanceof MovementProvider)
                {
                    movementNodes.add(node);
                    if (node instanceof ExtrusionNode)
                    {
                        availableExtrusion += ((ExtrusionNode) node).getExtrusion().getE();
                    }
                }
            }
        }

        return new InScopeEvents(movementNodes, availableExtrusion);
    }

    protected Optional<CloseResult> insertProgressiveNozzleClose(GCodeEventNode startingNode,
            List<SectionNode> sectionsToConsider,
            final NozzleProxy nozzleInUse) throws NodeProcessingException, CannotCloseFromPerimeterException, NoPerimeterToCloseOverException, NotEnoughAvailableExtrusionException, PostProcessingError
    {
        nozzleInUse.setCurrentPosition(1.0);
        Optional<CloseResult> closeResult = Optional.empty();

        if (!startingNode.getParent().isPresent()
                || !(startingNode.getParent().get() instanceof SectionNode))
        {
            throw new NodeProcessingException();
        }

        //The last section is the one we want to close in...
        SectionNode sectionContainingNodeToAppendClosesTo = (SectionNode) startingNode.getParent().get();
        InScopeEvents unprioritisedAll = extractAvailableMovements(startingNode, sectionsToConsider, true, false);

        //For non-perimeters...
        if (!(sectionContainingNodeToAppendClosesTo instanceof OuterPerimeterSectionNode)
                && !(sectionContainingNodeToAppendClosesTo instanceof InnerPerimeterSectionNode))
        {
            try
            {
                InScopeEvents unprioritisedNoPerimeters = extractAvailableMovements(startingNode, sectionsToConsider, false, false);
                closeResult = overwriteClose(unprioritisedNoPerimeters, nozzleInUse, false);
            } catch (NotEnoughAvailableExtrusionException ex)
            {
                try
                {
                    //Should be close over specified sections?
                    InScopeEvents prioritisedAll = extractAvailableMovements(startingNode, sectionsToConsider, true, true);
                    closeResult = copyClose(prioritisedAll, startingNode, Optional.empty(), nozzleInUse);
                } catch (NotEnoughAvailableExtrusionException ex2)
                {
                    closeResult = partialOpenAndCloseAtEndOfExtrusion(unprioritisedAll, nozzleInUse);
                }
            }
        } else
        {
            //Do this if we're closing from a perimeter
            try
            {
                closeResult = closeInwardsOntoPerimeter(unprioritisedAll, nozzleInUse);
            } catch (CannotCloseFromPerimeterException ex)
            {
                steno.warning("Close failed: " + ex.getMessage());
            } catch (NotEnoughAvailableExtrusionException ex)
            {
                closeResult = partialOpenAndCloseAtEndOfExtrusion(unprioritisedAll, nozzleInUse);
            } catch (NoPerimeterToCloseOverException | DidntFindEventException ex)
            {
                try
                {
                    closeResult = copyClose(unprioritisedAll, startingNode,
                            Optional.of(unprioritisedAll.getInScopeEvents().get(unprioritisedAll.getInScopeEvents().size() - 1)),
                            nozzleInUse);
                } catch (NotEnoughAvailableExtrusionException ex2)
                {
                    closeResult = partialOpenAndCloseAtEndOfExtrusion(unprioritisedAll, nozzleInUse);
                }
            }
        }

        return closeResult;
    }

    private boolean replaceOpenNozzleWithPartialOpen(
            InScopeEvents inScopeEvents,
            double partialOpenValue)
    {
        boolean success = false;

        for (GCodeEventNode eventNode : inScopeEvents.getInScopeEvents())
        {
            if (eventNode instanceof NozzleValvePositionNode)
            {
                NozzleValvePositionNode nozzlePosition = (NozzleValvePositionNode) eventNode;
                nozzlePosition.setCommentText("Partial open");
                nozzlePosition.getNozzlePosition().setB(partialOpenValue);
                success = true;
                break;
            }
        }

        if (!success)
        {
            //No nozzle position events found
            //Insert a partial open node at the start of the section
            NozzleValvePositionNode nozzlePosition = new NozzleValvePositionNode();
            nozzlePosition.setCommentText("Partial open");
            nozzlePosition.getNozzlePosition().setB(partialOpenValue);

            inScopeEvents.getInScopeEvents().get(inScopeEvents.getInScopeEvents().size() - 1).addSiblingBefore(nozzlePosition);
        }

        return success;
    }

    private Optional<CloseResult> partialOpenAndCloseAtEndOfExtrusion(
            InScopeEvents inScopeEvents,
            NozzleProxy nozzleInUse)
    {
        Optional<CloseResult> closeResult = Optional.empty();
        double nozzleStartPosition = 0;
        double nozzleCloseOverVolume = 0;

        // We shouldn't have been asked to partial open - there is more than the ejection volume of material available
        if (inScopeEvents.getAvailableExtrusion() >= nozzleInUse.getNozzleParameters().getEjectionVolume())
        {
            return closeResult;
        }

        double bValue = Math.min(1, inScopeEvents.getAvailableExtrusion()
                / nozzleInUse.getNozzleParameters().
                getEjectionVolume());

        bValue = Math.max(bValue, nozzleInUse.getNozzleParameters().getPartialBMinimum());

        nozzleStartPosition = bValue;
        nozzleCloseOverVolume = inScopeEvents.getAvailableExtrusion();

        try
        {
            nozzleInUse.setCurrentPosition(nozzleStartPosition);
            closeResult = overwriteClose(inScopeEvents, nozzleInUse, true);
            replaceOpenNozzleWithPartialOpen(inScopeEvents, bValue);
        } catch (NotEnoughAvailableExtrusionException ex)
        {
            steno.error("Got Not Enough Available Extrusion - shouldn't see this");
        }

        closeResult = Optional.of(new CloseResult(nozzleStartPosition, nozzleCloseOverVolume));

        return closeResult;
    }

    private void copyExtrusionEvents(double targetVolume,
            List<SectionNode> sectionsToConsider,
            GCodeEventNode nodeToCopyFrom,
            boolean forwards) throws PostProcessingError
    {
        int closeExtrusionFeedRate_mmPerMin = 400;

        Iterator<GCodeEventNode> priorMovementIterator = nodeToCopyFrom.treeSpanningBackwardsIterator();
        Feedrate feedrate = null;
        while (priorMovementIterator.hasNext())
        {
            GCodeEventNode potentialPriorNode = priorMovementIterator.next();
            if (potentialPriorNode.getParent().isPresent()
                    && potentialPriorNode.getParent().get() instanceof SectionNode
                    && sectionsToConsider.contains((SectionNode) potentialPriorNode.getParent().get()))
            {
                if (potentialPriorNode instanceof FeedrateProvider)
                {
                    feedrate = ((FeedrateProvider) potentialPriorNode).getFeedrate();
                    break;
                }
            } else
            {
                //We're in a section we're not allowed to consider
                break;
            }
        }

        if (feedrate != null)
        {
            closeExtrusionFeedRate_mmPerMin = feedrate.getFeedRate_mmPerMin();
        }

        double cumulativeExtrusionVolume = 0;

        boolean startMessageOutput = false;

        int sectionDelta = (forwards) ? 1 : -1;
        int sectionCounter = (forwards) ? 0 : sectionsToConsider.size() - 1;
        boolean haveConsumedStartNode = false;

        //Work out which section to start in
        if (nodeToCopyFrom.getParent().isPresent()
                && nodeToCopyFrom.getParent().get() instanceof SectionNode
                && !forwards)
        {
            for (int sectionSearch = 0; sectionSearch < sectionsToConsider.size(); sectionSearch++)
            {
                if (sectionsToConsider.get(sectionSearch) == nodeToCopyFrom.getParent().get())
                {
                    sectionCounter = sectionSearch;
                    break;
                }
            }
        }

        List<MovementProvider> movementNodes = new ArrayList<>();

        while (sectionCounter >= 0
                && sectionCounter <= sectionsToConsider.size() - 1)
        {
            SectionNode sectionNode = sectionsToConsider.get(sectionCounter);
            sectionCounter += sectionDelta;

            Iterator<GCodeEventNode> sectionIterator = null;
            if (!haveConsumedStartNode)
            {
                if (forwards)
                {
                    sectionIterator = nodeToCopyFrom.siblingsIterator();
                } else
                {
                    sectionIterator = nodeToCopyFrom.siblingsBackwardsIterator();
                }
                haveConsumedStartNode = true;
            } else
            {
                if (forwards)
                {
                    sectionIterator = sectionNode.childIterator();
                } else
                {
                    sectionIterator = sectionNode.childBackwardsIterator();
                }
            }

            while (sectionIterator.hasNext())
            {
                GCodeEventNode node = sectionIterator.next();
                if (node instanceof MovementProvider)
                {
                    movementNodes.add((MovementProvider) node);
                }
            }
        }

        Iterator<MovementProvider> movementIterator = movementNodes.iterator();

        while (movementIterator.hasNext()
                && cumulativeExtrusionVolume < targetVolume)
        {
            MovementProvider provider = movementIterator.next();
            if (provider instanceof ExtrusionNode)
            {
                ExtrusionNode eventToCopy = (ExtrusionNode) provider;

                double segmentVolume = eventToCopy.getExtrusion().getE() + eventToCopy.getExtrusion().getD();
                double volumeDifference = targetVolume - cumulativeExtrusionVolume
                        - segmentVolume;

                if (volumeDifference < 0)
                {
                    double requiredSegmentVolume = segmentVolume + volumeDifference;
                    double segmentAlterationRatio = requiredSegmentVolume / segmentVolume;

                    ExtrusionNode eventToInsert = new ExtrusionNode();
                    eventToInsert.getExtrusion().setE(eventToCopy.getExtrusion().getE() * (float) segmentAlterationRatio);
                    eventToInsert.getExtrusion().setD(eventToCopy.getExtrusion().getD() * (float) segmentAlterationRatio);
                    eventToInsert.getFeedrate().setFeedRate_mmPerMin(closeExtrusionFeedRate_mmPerMin);

                    Vector2D fromPosition = null;

                    Vector2D fromReferencePosition = null;

                    // Prevent the extrusion being output for the events we've copied from
                    eventToCopy.getExtrusion().eNotInUse();
                    eventToCopy.getExtrusion().dNotInUse();

                    if (forwards)
                    {
                        //TODO WARNING - this will find the next movement BEYOND the sections we're considering - need a more restrictive method
                        try
                        {
                            Optional<MovementProvider> nextMovement = nodeManagementUtilities.findNextMovement((GCodeEventNode) provider);
                            if (nextMovement.isPresent())
                            {
                                fromReferencePosition = nextMovement.get().getMovement().toVector2D();
                            }
                        } catch (NodeProcessingException ex)
                        {
                            steno.warning("Exception when finding next movement for reverse");
                        }
                    } else
                    {
                        //TODO WARNING - this will find the prior movement BEYOND the sections we're considering - need a more restrictive method
                        try
                        {
                            Optional<MovementProvider> priorMovement = nodeManagementUtilities.findPriorMovement((GCodeEventNode) provider);
                            if (priorMovement.isPresent())
                            {
                                fromReferencePosition = priorMovement.get().getMovement().toVector2D();
                            }
                        } catch (NodeProcessingException ex)
                        {
                            steno.warning("Exception when finding next movement for reverse");
                        }
                    }

                    if (fromReferencePosition != null)
                    {
                        fromPosition = fromReferencePosition;
                    } else
                    {
                        throw new PostProcessingError(
                                "Couldn't locate from position for auto wipe");
                    }

                    Vector2D toPosition = new Vector2D(eventToCopy.getMovement().getX(),
                            eventToCopy.getMovement().getY());

                    Vector2D actualVector = toPosition.subtract(fromPosition);
                    Vector2D firstSegment = fromPosition.add(segmentAlterationRatio,
                            actualVector);

                    eventToInsert.getMovement().setX(firstSegment.getX());
                    eventToInsert.getMovement().setY(firstSegment.getY());
                    eventToInsert.setCommentText(" - end -");

                    nodeToCopyFrom.addSiblingAfter(eventToInsert);
                    cumulativeExtrusionVolume += requiredSegmentVolume;
                } else
                {
                    ExtrusionNode eventToInsert = new ExtrusionNode();
                    eventToInsert.getExtrusion().setE(eventToCopy.getExtrusion().getE());
                    eventToInsert.getExtrusion().setD(eventToCopy.getExtrusion().getD());
                    eventToInsert.getMovement().setX(eventToCopy.getMovement().getX());
                    eventToInsert.getMovement().setY(eventToCopy.getMovement().getY());
                    eventToInsert.setCommentText(((startMessageOutput == false) ? " - start -" : " - in progress -"));
                    startMessageOutput = true;
                    eventToInsert.getFeedrate().setFeedRate_mmPerMin(closeExtrusionFeedRate_mmPerMin);

                    nodeToCopyFrom.addSiblingAfter(eventToInsert);
                    cumulativeExtrusionVolume += eventToCopy.getExtrusion().getE() + eventToCopy.getExtrusion().getD();

                    // Prevent the extrusion being output for the events we've copied from
                    eventToCopy.getExtrusion().eNotInUse();
                    eventToCopy.getExtrusion().dNotInUse();
                }

            } else
            {
                if (provider instanceof TravelNode)
                {
                    TravelNode eventToCopy = (TravelNode) provider;
                    TravelNode eventToInsert = new TravelNode();
                    eventToInsert.getMovement().setX(eventToCopy.getMovement().getX());
                    eventToInsert.getMovement().setY(eventToCopy.getMovement().getY());
                    eventToInsert.setCommentText(eventToCopy.getCommentText());
                    eventToInsert.getFeedrate().setFeedRate_mmPerMin(closeExtrusionFeedRate_mmPerMin);
                    nodeToCopyFrom.addSiblingAfter(eventToInsert);
                }
            }
        }
    }

//    private Optional<CloseResult> reverseCloseFromEndOfExtrusion(List<SectionNode> sectionsToConsider,
//            ExtrusionNode nodeToAppendClosesTo,
//            NozzleProxy nozzleInUse) throws PostProcessingError, NotEnoughAvailableExtrusionException
//    {
//        Optional<CloseResult> closeResult = Optional.empty();
//        double nozzleStartPosition = 0;
//        double nozzleCloseOverVolume = 0;
//
//        double availableExtrusion = 0;
//
//        for (SectionNode sectionNode : sectionsToConsider)
//        {
//            availableExtrusion += sectionNode.getTotalExtrusion();
//        }
//
//        if (availableExtrusion >= nozzleInUse.getNozzleParameters().getEjectionVolume())
//        {
//            nozzleStartPosition = 1.0;
//            nozzleCloseOverVolume = nozzleInUse.getNozzleParameters().getEjectionVolume();
//
//            copyExtrusionEvents(nozzleCloseOverVolume,
//                    sectionsToConsider,
//                    nodeToAppendClosesTo,
//                    false);
//
//            closeResult = Optional.of(new CloseResult(nozzleStartPosition, nozzleCloseOverVolume));
//        } else
//        {
//            throw new NotEnoughAvailableExtrusionException("Not enough extrusion when attempting to reverse close");
//        }
//
//        return closeResult;
//    }
    protected Optional<CloseResult> closeInwardsOntoPerimeter(
            final InScopeEvents inScopeEvents,
            final NozzleProxy nozzleInUse
    ) throws CannotCloseFromPerimeterException, NotEnoughAvailableExtrusionException, NoPerimeterToCloseOverException, DidntFindEventException
    {
        Optional<CloseResult> closeResult = Optional.empty();
        Optional<GCodeEventNode> closestNode = Optional.empty();
        double nozzleStartPosition = 0;
        double nozzleCloseOverVolume = 0;

        if (inScopeEvents.getAvailableExtrusion() >= nozzleInUse.getNozzleParameters().getEjectionVolume())
        {
            // Look for a valid intersection
            Optional<IntersectionResult> result = closeUtilities.findClosestMovementNode(inScopeEvents.getInScopeEvents(), true);

            if (result.isPresent())
            {
                closeResult = copyClose(inScopeEvents, inScopeEvents.getInScopeEvents().get(0), Optional.of(result.get().getClosestNode()), nozzleInUse);
            } else
            {
                throw new NoPerimeterToCloseOverException("No valid perimeter");
            }
        } else
        {
            throw new NotEnoughAvailableExtrusionException("Not enough available extrusion to close");
        }

        //We'll close in the direction that has most space
//            try
//            {
//                //Try forwards first
//                double forwardExtrusionTotal = nodeManagementUtilities.findAvailableExtrusion(availableSectionsToCloseOver, result.get().getClosestNode(), true);
//
//                if (forwardExtrusionTotal >= volumeToCloseOver)
//                {
//                    closestNode = Optional.of(result.get().getClosestNode());
//                } else
//                {
//                    //Try backwards
//                    double backwardExtrusionTotal = nodeManagementUtilities.findAvailableExtrusion(availableSectionsToCloseOver, result.get().getClosestNode(), false);
//
//                    if (backwardExtrusionTotal >= volumeToCloseOver)
//                    {
//                        closestNode = Optional.of(result.get().getClosestNode());
//                        closeResult = copyClose(nodeToAppendClosesTo, closestNode.get(), nozzleInUse, false, backwardExtrusionTotal, false);
//                    }
//                }
//            } catch (NodeProcessingException ex)
//            {
//                String outputMessage;
//                if (nodeToAppendClosesTo instanceof Renderable)
//                {
//                    Renderable renderableNode = (Renderable) nodeToAppendClosesTo;
//                    outputMessage = "Failure to find correct direction to traverse in for node " + renderableNode.renderForOutput();
//                } else
//                {
//                    outputMessage = "Failure to find correct direction to traverse in for node " + nodeToAppendClosesTo.toString();
//                }
//                throw new RuntimeException(outputMessage);
//            }
        return closeResult;
    }

    /**
     * Close along the existing extrusion Don't add any nodes except where this
     * is necessary to insert breaks for close boundaries
     *
     * @param inScopeEvents
     * @param nozzleInUse
     * @param useAvailableExtrusion
     * @return CloseResult
     * @throws celtech.gcodetranslator.NotEnoughAvailableExtrusionException
     */
    protected Optional<CloseResult> overwriteClose(
            final InScopeEvents inScopeEvents,
            final NozzleProxy nozzleInUse,
            final boolean useAvailableExtrusion) throws NotEnoughAvailableExtrusionException
    {
        Optional<CloseResult> closeResult = Optional.empty();

        NozzleParameters nozzleParams = nozzleInUse.getNozzleParameters();

        double volumeToCloseOver = (useAvailableExtrusion)?inScopeEvents.getAvailableExtrusion():nozzleParams.getEjectionVolume();
        double nozzleStartingPosition = nozzleInUse.getCurrentPosition();

        if (useAvailableExtrusion
                || inScopeEvents.getAvailableExtrusion() >= nozzleInUse.getNozzleParameters().getEjectionVolume())
        {
            double runningTotalOfExtrusion = 0;

            for (int movementNodeCounter = 0;
                    movementNodeCounter < inScopeEvents.getInScopeEvents().size();
                    movementNodeCounter++)
            {
                if (inScopeEvents.getInScopeEvents().get(movementNodeCounter) instanceof ExtrusionNode)
                {
                    ExtrusionNode extrusionNodeBeingExamined = (ExtrusionNode) inScopeEvents.getInScopeEvents().get(movementNodeCounter);

                    int comparisonResult = MathUtils.compareDouble(runningTotalOfExtrusion + extrusionNodeBeingExamined.getExtrusion().getE(), volumeToCloseOver, 0.00001);

                    if (comparisonResult == MathUtils.LESS_THAN)
                    {
                        //One step along the way
                        double bValue = (runningTotalOfExtrusion / volumeToCloseOver) * nozzleStartingPosition;
                        extrusionNodeBeingExamined.getNozzlePosition().setB(bValue);
                        runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                        //No extrusion during a close
                        extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                        extrusionNodeBeingExamined.getExtrusion().dNotInUse();
                    } else if (comparisonResult == MathUtils.EQUAL)
                    {
                        //All done
                        double bValue = (runningTotalOfExtrusion / volumeToCloseOver) * nozzleStartingPosition;
                        extrusionNodeBeingExamined.getNozzlePosition().setB(bValue);
                        runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                        //No extrusion during a close
                        extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                        extrusionNodeBeingExamined.getExtrusion().dNotInUse();
                        break;
                    } else
                    {
                        //If we got here then we need to split this extrusion
                        //We're splitting the last part of the line (we're just looking at it in reverse order as we consider the line from the end
                        MovementProvider priorMovement = null;

                        if (movementNodeCounter == inScopeEvents.getInScopeEvents().size() - 1)
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
                            priorMovement = (MovementProvider) inScopeEvents.getInScopeEvents().get(movementNodeCounter + 1);
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
                        newExtrusionNode.setCommentText("Remainder pre close towards end");
                        newExtrusionNode.getExtrusion().setE((float) extrusionInFirstSection);
                        newExtrusionNode.getMovement().setX(firstSegment.getX());
                        newExtrusionNode.getMovement().setY(firstSegment.getY());
                        newExtrusionNode.getFeedrate().setFeedRate_mmPerMin(extrusionNodeBeingExamined.getFeedrate().getFeedRate_mmPerMin());

                        extrusionNodeBeingExamined.addSiblingBefore(newExtrusionNode);

                        extrusionNodeBeingExamined.getExtrusion().setE((float) extrusionInSecondSection);
                        extrusionNodeBeingExamined.appendCommentText("Start of close towards end");
                        double bValue = (runningTotalOfExtrusion / volumeToCloseOver) * nozzleStartingPosition;
                        extrusionNodeBeingExamined.getNozzlePosition().setB(bValue);

                        runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                        //No extrusion during a close
                        extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                        extrusionNodeBeingExamined.getExtrusion().dNotInUse();
                        break;
                    }
                }
            }

            closeResult = Optional.of(new CloseResult(1.0, nozzleInUse.getNozzleParameters().getEjectionVolume()));
        } else
        {
            throw new NotEnoughAvailableExtrusionException("When closing towards end of extrusion");
        }

        return closeResult;
    }

    /**
     * Create new nodes whilst closing the nozzle along the specified path
     *
     * @param extractedMovements
     * @param nodeToAddClosesTo
     * @param nodeToStartCopyingFrom
     * @param nozzleInUse
     * @return
     * @throws celtech.gcodetranslator.NotEnoughAvailableExtrusionException
     */
    protected Optional<CloseResult> copyClose(
            final InScopeEvents extractedMovements,
            final GCodeEventNode nodeToAddClosesTo,
            final Optional<GCodeEventNode> nodeToStartCopyingFrom,
            final NozzleProxy nozzleInUse) throws NotEnoughAvailableExtrusionException
    {
        NozzleParameters nozzleParams = nozzleInUse.getNozzleParameters();

        double volumeToCloseOver = nozzleParams.getEjectionVolume();

        double runningTotalOfExtrusion = 0;
        double currentNozzlePosition = nozzleInUse.getCurrentPosition();
        double closePermm3Volume = currentNozzlePosition / volumeToCloseOver;
        double requiredVolumeToCloseOver = currentNozzlePosition / closePermm3Volume;

        int nodeToStartCopyingFromIndex = 0;
        int inScopeEventDelta = 1;
        double availableExtrusion = 0;

        if (nodeToStartCopyingFrom.isPresent())
        {

            // Find the index of this node
            for (int inScopeEventCounter = 0; inScopeEventCounter < extractedMovements.getInScopeEvents().size(); inScopeEventCounter++)
            {
                if (extractedMovements.getInScopeEvents().get(inScopeEventCounter) == nodeToStartCopyingFrom.get())
                {
                    nodeToStartCopyingFromIndex = inScopeEventCounter;
                    break;
                }
            }

            double availableExtrusionForwards = 0;
            for (int inScopeEventCounter = nodeToStartCopyingFromIndex; inScopeEventCounter < extractedMovements.getInScopeEvents().size(); inScopeEventCounter++)
            {
                if (extractedMovements.getInScopeEvents().get(inScopeEventCounter) instanceof ExtrusionNode)
                {
                    availableExtrusionForwards += ((ExtrusionNode) extractedMovements.getInScopeEvents().get(inScopeEventCounter)).getExtrusion().getE();
                }
            }

            if (availableExtrusionForwards >= volumeToCloseOver)
            {
                availableExtrusion = availableExtrusionForwards;
            } else
            {
                double availableExtrusionBackwards = 0;
                for (int inScopeEventCounter = nodeToStartCopyingFromIndex; inScopeEventCounter >= 0; inScopeEventCounter--)
                {
                    if (extractedMovements.getInScopeEvents().get(inScopeEventCounter) instanceof ExtrusionNode)
                    {
                        availableExtrusionBackwards += ((ExtrusionNode) extractedMovements.getInScopeEvents().get(inScopeEventCounter)).getExtrusion().getE();
                    }
                }

                if (availableExtrusionBackwards >= volumeToCloseOver)
                {
                    availableExtrusion = availableExtrusionBackwards;
                    inScopeEventDelta = -1;
                }
            }
        } else
        {
            availableExtrusion = extractedMovements.getAvailableExtrusion();
        }

        if (extractedMovements.getAvailableExtrusion() >= nozzleInUse.getNozzleParameters().getEjectionVolume())
        {
            for (int inScopeEventCounter = nodeToStartCopyingFromIndex;
                    inScopeEventCounter >= 0 && inScopeEventCounter < extractedMovements.getInScopeEvents().size();
                    inScopeEventCounter += inScopeEventDelta)
            {
                if (extractedMovements.getInScopeEvents().get(inScopeEventCounter) instanceof ExtrusionNode)
                {
                    ExtrusionNode extrusionNodeBeingExamined = (ExtrusionNode) extractedMovements.getInScopeEvents().get(inScopeEventCounter);

                    int comparisonResult = MathUtils.compareDouble(runningTotalOfExtrusion + extrusionNodeBeingExamined.getExtrusion().getE(), requiredVolumeToCloseOver, 0.00001);

                    ExtrusionNode copy = extrusionNodeBeingExamined.clone();
                    nodeToAddClosesTo.addSiblingAfter(copy);

                    if (comparisonResult == MathUtils.LESS_THAN)
                    {
                        //One step along the way
                        double bValue = runningTotalOfExtrusion / volumeToCloseOver;
                        copy.getNozzlePosition().setB(bValue);
                        runningTotalOfExtrusion += copy.getExtrusion().getE();
                        //No extrusion during a close
                        copy.getExtrusion().eNotInUse();
                        copy.getExtrusion().dNotInUse();
                        //Wipe out extrusion in the area we copied from as well
                        extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                        extrusionNodeBeingExamined.getExtrusion().dNotInUse();
                    } else if (comparisonResult == MathUtils.EQUAL)
                    {
                        //All done
                        double bValue = 0;
                        copy.getNozzlePosition().setB(bValue);
                        runningTotalOfExtrusion += copy.getExtrusion().getE();
                        //No extrusion during a close
                        copy.getExtrusion().eNotInUse();
                        copy.getExtrusion().dNotInUse();
                        //Wipe out extrusion in the area we copied from as well
                        extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                        extrusionNodeBeingExamined.getExtrusion().dNotInUse();
                        break;
                    } else
                    {
                        MovementProvider priorMovement = null;

                        //TODO needs to take account of direction
                        if (inScopeEventCounter == 0)
                        {
                            priorMovement = (MovementProvider) nodeToAddClosesTo;
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
                            priorMovement = (MovementProvider) extractedMovements.getInScopeEvents().get(inScopeEventCounter - 1);
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
                        copy.appendCommentText("Start of close segment");
                        copy.getNozzlePosition().setB(runningTotalOfExtrusion / volumeToCloseOver);

                        runningTotalOfExtrusion += copy.getExtrusion().getE();

                        //No extrusion during a close
                        copy.getExtrusion().eNotInUse();
                        copy.getExtrusion().dNotInUse();
                        break;
                    }

                }
            }
        } else
        {
            throw new NotEnoughAvailableExtrusionException("Not enough extrusion when attempting to reverse close");
        }

        return Optional.of(new CloseResult(1.0, volumeToCloseOver));
    }

//    protected Optional<CloseResult> insertNozzleCloses(RetractNode retractNode, final NozzleProxy nozzleInUse)
//            throws NodeProcessingException, CannotCloseFromPerimeterException, NoPerimeterToCloseOverException, NotEnoughAvailableExtrusionException, PostProcessingError
//    {
//        Optional<CloseResult> closeResult = null;
//
//        //Assume the nozzle is always fully open...
//        nozzleInUse.setCurrentPosition(1.0);
//        if (featureSet.isEnabled(PostProcessorFeature.GRADUAL_CLOSE))
//        {
//            closeResult = insertProgressiveNozzleClose(retractNode, nozzleInUse);
//        } else
//        {
//            closeResult = insertNozzleCloseFullyAfterEvent(retractNode.getPriorExtrusionNode(), nozzleInUse);
//        }
//
//        return closeResult;
//    }
//    /**
//     *
//     * @param layerNode
//     * @param extrusionUpToClose
//     * @param lastExtrusionNodeBeforeClose
//     * @param nozzleInUse
//     * @return
//     * @throws
//     * celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException @see
//     * CloseResult
//     * @throws celtech.gcodetranslator.CannotCloseFromPerimeterException
//     * @throws celtech.gcodetranslator.NoPerimeterToCloseOverException
//     * @throws celtech.gcodetranslator.NotEnoughAvailableExtrusionException
//     */
//    protected Optional<CloseResult> insertsff (LayerNode layerNode,
//            double extrusionUpToClose, ExtrusionNode lastExtrusionNodeBeforeClose, final NozzleProxy nozzleInUse)
//            throws NodeProcessingException, CannotCloseFromPerimeterException, NoPerimeterToCloseOverException, NotEnoughAvailableExtrusionException, PostProcessingError
//    {
//        Optional<CloseResult> closeResult = Optional.empty();
//
//        List<SectionNode> sectionsToConsider = new ArrayList<>();
//        Iterator<GCodeEventNode> childrenOfTheLayer = layerNode.childIterator();
//        while (childrenOfTheLayer.hasNext())
//        {
//            GCodeEventNode potentialSectionNode = childrenOfTheLayer.next();
//
//            if (potentialSectionNode instanceof SectionNode)
//            {
//                sectionsToConsider.add((SectionNode) potentialSectionNode);
//            }
//        }
//
//        if (sectionsToConsider.size() > 0)
//        {
//            //Assume the nozzle is always fully open...
//            nozzleInUse.setCurrentPosition(1.0);
//            if (featureSet.isEnabled(PostProcessorFeature.GRADUAL_CLOSE))
//            {
////                closeResult = insertProgressiveNozzleClose(sectionsToConsider, lastExtrusionNodeBeforeClose, nozzleInUse);
//            } else
//            {
//                closeResult = insertNozzleCloseFullyAfterEvent(lastExtrusionNodeBeforeClose, nozzleInUse);
//            }
//        }
//
//        return closeResult;
//    }
    protected void insertCloseNodes(LayerNode layerNode, LayerPostProcessResult lastLayerParseResult, List<NozzleProxy> nozzleProxies)
    {
        //Tool select nodes are directly under a layer
        Iterator<GCodeEventNode> layerChildIterator = layerNode.childIterator();

        ToolSelectNode lastToolSelectNode = null;

        if (lastLayerParseResult != null)
        {
            lastToolSelectNode = lastLayerParseResult.getLastToolSelectInForce();
        }

        List<RetractHolder> retractNodes = new ArrayList<>();

        while (layerChildIterator.hasNext())
        {
            GCodeEventNode layerChild = layerChildIterator.next();

            if (layerChild instanceof RetractNode)
            {
                if (lastToolSelectNode != null)
                {
                    NozzleProxy nozzleInUse = nozzleProxies.get(lastToolSelectNode.getToolNumber());
                    retractNodes.add(new RetractHolder((RetractNode) layerChild, nozzleInUse));
                } else
                {
                    steno.warning("Removed retract on layer " + layerNode.getLayerNumber() + " with no prior tool selected");
                    retractNodes.add(new RetractHolder((RetractNode) layerChild, null));
                }
            } else if (layerChild instanceof ToolSelectNode)
            {
                ToolSelectNode toolSelectNode = (ToolSelectNode) layerChild;

                NozzleProxy nozzleInUse = nozzleProxies.get(toolSelectNode.getToolNumber());

                Iterator<GCodeEventNode> toolSelectChildren = toolSelectNode.treeSpanningIterator(null);

                while (toolSelectChildren.hasNext())
                {
                    GCodeEventNode toolSelectChild = toolSelectChildren.next();

                    if (toolSelectChild instanceof RetractNode)
                    {
                        retractNodes.add(new RetractHolder((RetractNode) toolSelectChild, nozzleInUse));
                    }
                }
            }
        }

        for (RetractHolder retractHolder : retractNodes)
        {
            if (retractHolder.getNozzle() != null)
            {
                Optional<CloseResult> closeResult = processRetractNode(retractHolder.getNode(), retractHolder.getNozzle(), layerNode, lastLayerParseResult);
                if (!closeResult.isPresent())
                {
                    steno.warning("Close failed - removing retract anyway on layer " + layerNode.getLayerNumber());
                }
            }
            retractHolder.getNode().removeFromParent();
        }
    }

    private Optional<CloseResult> processRetractNode(RetractNode retractNode,
            NozzleProxy nozzleInUse,
            LayerNode thisLayer,
            LayerPostProcessResult lastLayerParseResult)
    {
        Optional<CloseResult> closeResult = Optional.empty();

        Optional<ExtrusionNode> nextExtrusionNode = Optional.empty();
        try
        {
            if (retractNode.getPriorExtrusionNode() != null)
            {
                closeResult = insertProgressiveNozzleClose(retractNode, retractNode.getSectionsToConsider(), nozzleInUse);
            } else
            {
                LayerNode lastLayer = lastLayerParseResult.getLayerData();

                //Look for the last extrusion on the previous layer
                if (lastLayer.getLayerNumber() < 0)
                {
                    // There wasn't a last layer - this is a lone retract at the start of the file
                    steno.warning("Discarding retract from layer " + thisLayer.getLayerNumber());
                } else
                {
                    ExtrusionNode extrusionToCloseFrom = null;

                    List<SectionNode> sectionsToConsider = new ArrayList<>();
                    GCodeEventNode lastNodeOnLastLayer = lastLayer.getAbsolutelyTheLastEvent();
                    IteratorWithOrigin<GCodeEventNode> layerBackwardsIterator = lastNodeOnLastLayer.treeSpanningBackwardsIterator();
                    SectionNode lastSectionNode = null;

                    if (lastNodeOnLastLayer instanceof ExtrusionNode)
                    {
                        extrusionToCloseFrom = (ExtrusionNode) lastNodeOnLastLayer;
                    }

                    while (layerBackwardsIterator.hasNext())
                    {
                        GCodeEventNode node = layerBackwardsIterator.next();

                        if (node instanceof NozzlePositionProvider)
                        {
                            if (((NozzlePositionProvider) node).getNozzlePosition().isBSet()
                                    && ((NozzlePositionProvider) node).getNozzlePosition().getB() < 1.0)
                            {
//                                Optional<SectionNode> parentSection = nodeManagementUtilities.lookForParentSectionNode(node);
//                                if (parentSection.isPresent())
//                                {
//                                    //Exclude the section we're in just in case...
//                                    sectionsToConsider.remove(parentSection.get());
//                                }
                                break;
                            }
                        }

                        if (node instanceof ExtrusionNode)
                        {
                            if (extrusionToCloseFrom == null)
                            {
                                extrusionToCloseFrom = (ExtrusionNode) node;
                            }

                            Optional<SectionNode> parentSection = nodeManagementUtilities.lookForParentSectionNode(node);
                            if (parentSection.isPresent())
                            {
                                if (lastSectionNode != parentSection.get())
                                {
                                    sectionsToConsider.add(0, parentSection.get());
                                    lastSectionNode = parentSection.get();
                                }
                            }
                        }
                    }

                    if (sectionsToConsider.size() > 0)
                    {
                        closeResult = insertProgressiveNozzleClose(extrusionToCloseFrom, sectionsToConsider, nozzleInUse);
                        if (closeResult.isPresent())
                        {
                            lastLayerParseResult.getNozzleStateAtEndOfLayer().get().closeNozzleFully();
                        }
                    } else
                    {
                        //We only seem to have one extrusion to close over...
                        if (extrusionToCloseFrom != null)
                        {
                            sectionsToConsider.add((SectionNode) extrusionToCloseFrom.getParent().get());
                            closeResult = insertProgressiveNozzleClose(extrusionToCloseFrom, sectionsToConsider, nozzleInUse);
                            if (closeResult.isPresent())
                            {
                                lastLayerParseResult.getNozzleStateAtEndOfLayer().get().closeNozzleFully();
                            }
                        }
                    }
                }
            }

            if (closeResult.isPresent())
            {
                if (!nextExtrusionNode.isPresent())
                {
                    nextExtrusionNode = nodeManagementUtilities.findNextExtrusion(thisLayer, retractNode);
                }

                if (nextExtrusionNode.isPresent())
                {
                    //Add the elided extrusion to this node - the open routine will find it later
                    ((ExtrusionNode) nextExtrusionNode.get()).setElidedExtrusion(closeResult.get().getNozzleCloseOverVolume());
                }
            } else
            {
                throw new NodeProcessingException("Failed to close after retract", (Renderable) retractNode);
            }

        } catch (NodeProcessingException | CannotCloseFromPerimeterException | NoPerimeterToCloseOverException | NotEnoughAvailableExtrusionException | PostProcessingError ex)
        {
            throw new RuntimeException("Failed to process retract on layer " + thisLayer.getLayerNumber() + " this will affect open and close", ex);
        }

        return closeResult;
    }
}
