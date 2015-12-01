package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.NozzleParameters;
import celtech.gcodetranslator.CannotCloseFromPerimeterException;
import celtech.gcodetranslator.DidntFindEventException;
import celtech.gcodetranslator.NoPerimeterToCloseOverException;
import celtech.gcodetranslator.NotEnoughAvailableExtrusionException;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.PostProcessingError;
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
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
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
import org.apache.commons.math3.geometry.euclidean.twod.Segment;
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

    protected InScopeEvents extractAvailableMovements(GCodeEventNode startingNode,
            List<SectionNode> sectionsToConsider,
            boolean includeInternalPerimeters,
            boolean includeExternalPerimeters,
            boolean stopAtNozzlePositionEvent)
    {
        List<SectionNode> availableSectionsToCloseOver = new ArrayList<>();

        List<SectionNode> tempSectionHolder = new ArrayList<>();
        for (SectionNode section : sectionsToConsider)
        {
            if ((!(section instanceof OuterPerimeterSectionNode)
                    && !(section instanceof InnerPerimeterSectionNode))
                    || ((section instanceof OuterPerimeterSectionNode) && includeExternalPerimeters)
                    || ((section instanceof InnerPerimeterSectionNode) && includeInternalPerimeters))
            {
                tempSectionHolder.add(section);
            }
        }

        availableSectionsToCloseOver = tempSectionHolder;

        int sectionDelta = -1;
        int sectionCounter = 0;
        boolean haveConsumedStartNode = true;

        //Work out which section to start in
        if (startingNode.getParent().isPresent()
                && startingNode.getParent().get() instanceof SectionNode)
        {
            for (int sectionSearch = 0; sectionSearch < availableSectionsToCloseOver.size(); sectionSearch++)
            {
                if (availableSectionsToCloseOver.get(sectionSearch) == startingNode.getParent().get())
                {
                    sectionCounter = sectionSearch;
                    haveConsumedStartNode = false;
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
                //Changed to allow closes to be carried out on any part of previous section
                if (node instanceof NozzleValvePositionNode && stopAtNozzlePositionEvent)
                {
//                    movementNodes.add(node);
                    keepLooking = false;
                    break;
                } else if (node instanceof NozzlePositionProvider
                        && ((NozzlePositionProvider) node).getNozzlePosition().isBSet()
                        && stopAtNozzlePositionEvent)
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

        //Now trim off the first travel nodes until we reach an extrusion.
        // This prevents odd inward close decisions if the slicer puts in strange moves after the end of the extrusion
        List<GCodeEventNode> travelNodesToDelete = new ArrayList<>();
        for (GCodeEventNode node : movementNodes)
        {
            if (node instanceof TravelNode)
            {
                travelNodesToDelete.add(node);
            } else
            {
                break;
            }
        }

        movementNodes.removeAll(travelNodesToDelete);

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

        //IF the node containing the close request is in a non-perimeter section THEN
        //  IF the sum of extrusion in the inner and non-perimeter sections up to the next nozzle control point > target THEN
        //      Overwrite close towards the end of the extrusion using inner and non-perimeter
        //  ELSE IF 
        //  END IF
        //ELSE
        //IF the node containing the close request is in an inner perimeter section THEN
        //ELSE (must be a close from an outer perimeter
        //END IF
        //For non-perimeters...
        if (!(sectionContainingNodeToAppendClosesTo instanceof OuterPerimeterSectionNode)
                && !(sectionContainingNodeToAppendClosesTo instanceof InnerPerimeterSectionNode))
        {
            try
            {
                boolean processedOK = false;

                InScopeEvents unprioritisedNoPerimetersForOverwrite = extractAvailableMovements(startingNode, sectionsToConsider, false, false, true);

                if (unprioritisedNoPerimetersForOverwrite.getAvailableExtrusion() >= nozzleInUse.getNozzleParameters().getEjectionVolume())
                {
                    processedOK = true;
                    closeResult = overwriteClose(unprioritisedNoPerimetersForOverwrite, nozzleInUse, false);
                }

                if (!processedOK)
                {
                    InScopeEvents unprioritisedNoOuterPerimeterForCopy = extractAvailableMovements(startingNode, sectionsToConsider, true, false, false);
                    if (unprioritisedNoOuterPerimeterForCopy.getAvailableExtrusion() >= nozzleInUse.getNozzleParameters().getEjectionVolume())
                    {
                        Optional<Segment> finalSegment = nodeManagementUtilities.findPriorMovementPoints(startingNode);
                        Optional<IntersectionResult> result = Optional.empty();

                        if (finalSegment.isPresent())
                        {
                            result = closeUtilities.findClosestMovementNode(finalSegment.get(), unprioritisedNoOuterPerimeterForCopy.getInScopeEvents(), false);
                        }

                        if (result.isPresent())
                        {
                            processedOK = true;
                            closeResult = copyClose(unprioritisedNoOuterPerimeterForCopy, startingNode, Optional.of(result.get().getClosestNode()), nozzleInUse, false);
                        }
                    }
                }

                if (!processedOK)
                {
                    InScopeEvents unprioritisedAllForOverwrite = extractAvailableMovements(startingNode, sectionsToConsider, true, true, true);
                    if (unprioritisedAllForOverwrite.getAvailableExtrusion() >= nozzleInUse.getNozzleParameters().getEjectionVolume())
                    {
                        processedOK = true;
                        closeResult = overwriteClose(unprioritisedAllForOverwrite, nozzleInUse, false);
                    }
                }

                if (!processedOK)
                {
                    InScopeEvents unprioritisedAllFromLastClose = extractAvailableMovements(startingNode, sectionsToConsider, true, true, true);
                    closeResult = partialOpenAndCloseAtEndOfExtrusion(unprioritisedAllFromLastClose, nozzleInUse);
                }
            } catch (NotEnoughAvailableExtrusionException ex)
            {
                steno.error("Failed to close from retract in non-perimeter");
            }
        } else if (sectionContainingNodeToAppendClosesTo instanceof InnerPerimeterSectionNode)
        {
            //Do this if we're closing from an inner perimeter
            try
            {
                InScopeEvents unprioritisedNoOuterPerimeterAllOfInner = extractAvailableMovements(startingNode, sectionsToConsider, true, false, false);

                if (unprioritisedNoOuterPerimeterAllOfInner.getAvailableExtrusion() >= nozzleInUse.getNozzleParameters().getEjectionVolume())
                {
                    closeResult = overwriteClose(unprioritisedNoOuterPerimeterAllOfInner, nozzleInUse, false);
                } else
                {
                    InScopeEvents unprioritisedAllFromLastClose = extractAvailableMovements(startingNode, sectionsToConsider, true, true, true);
                    closeResult = partialOpenAndCloseAtEndOfExtrusion(unprioritisedAllFromLastClose, nozzleInUse);
                }
            } catch (NotEnoughAvailableExtrusionException ex)
            {
                steno.error("Failed to close from retract in inner perimeter");
            }
        } else
        {
            boolean processedOK = false;

            //Do this if we're closing from an outer perimeter
            InScopeEvents unprioritisedNoOuterPerimeterForCopy = extractAvailableMovements(startingNode, sectionsToConsider, true, false, false);
            if (unprioritisedNoOuterPerimeterForCopy.getAvailableExtrusion() > nozzleInUse.getNozzleParameters().getEjectionVolume())
            {
                //There is some inner perimeter and we should have enough to close
                // Look for a valid intersection

                Optional<Segment> finalSegment = nodeManagementUtilities.findPriorMovementPoints(startingNode);
                Optional<IntersectionResult> result = Optional.empty();

                if (finalSegment.isPresent())
                {
                    result = closeUtilities.findClosestMovementNode(finalSegment.get(), unprioritisedNoOuterPerimeterForCopy.getInScopeEvents(), true);
                }

                if (result.isPresent())
                {
                    processedOK = true;
                    closeResult = copyClose(unprioritisedNoOuterPerimeterForCopy, startingNode, Optional.of(result.get().getClosestNode()), nozzleInUse, false);
                }
            }

            if (!processedOK)
            {
                InScopeEvents unprioritisedAll = extractAvailableMovements(startingNode, sectionsToConsider, true, true, false);
                if (unprioritisedAll.getAvailableExtrusion() >= nozzleInUse.getNozzleParameters().getEjectionVolume())
                {
                    processedOK = true;
                    // No inner perimeter but we have enough outer to close
                    closeResult = copyClose(unprioritisedAll, startingNode,
                            Optional.empty(),
                            nozzleInUse, true);
                }
            }

            if (!processedOK)
            {
                // We'll have to partial open
                InScopeEvents unprioritisedAllFromLastClose = extractAvailableMovements(startingNode, sectionsToConsider, true, true, true);
                closeResult = partialOpenAndCloseAtEndOfExtrusion(unprioritisedAllFromLastClose, nozzleInUse);
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
            //TODO put replenish in for partial opens

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

        closeResult = Optional.of(new CloseResult(nozzleStartPosition, nozzleCloseOverVolume, closeResult.get().getNodeContainingFinalClose()));

        return closeResult;
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
//    protected Optional<CloseResult> closeInwardsOntoPerimeter(
//            final InScopeEvents inScopeEvents,
//            final NozzleProxy nozzleInUse
//    ) throws CannotCloseFromPerimeterException, NotEnoughAvailableExtrusionException, NoPerimeterToCloseOverException, DidntFindEventException, NodeProcessingException
//    {
//        Optional<CloseResult> closeResult = Optional.empty();
//
//        if (inScopeEvents.getAvailableExtrusion() >= nozzleInUse.getNozzleParameters().getEjectionVolume())
//        {
//            // Look for a valid intersection
//            Optional<IntersectionResult> result = closeUtilities.findClosestMovementNode(inScopeEvents.getInScopeEvents(), true);
//
//            if (result.isPresent())
//            {
//                closeResult = copyClose(inScopeEvents, inScopeEvents.getInScopeEvents().get(0), Optional.of(result.get().getClosestNode()), nozzleInUse);
//            } else
//            {
//                throw new NoPerimeterToCloseOverException("No valid perimeter");
//            }
//        } else
//        {
//            throw new NotEnoughAvailableExtrusionException("Not enough available extrusion to close");
//        }
//
//        return closeResult;
//    }
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

        double volumeToCloseOver = (useAvailableExtrusion) ? inScopeEvents.getAvailableExtrusion() : nozzleParams.getEjectionVolume();
        double nozzleStartingPosition = nozzleInUse.getCurrentPosition();

        ExtrusionNode finalCloseNode = null;
        boolean closeStarted = false;

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
                        if (finalCloseNode == null)
                        {
                            finalCloseNode = extrusionNodeBeingExamined;
                        }
                        closeStarted = true;
                    } else if (comparisonResult == MathUtils.EQUAL)
                    {
                        //All done
                        double bValue = (runningTotalOfExtrusion / volumeToCloseOver) * nozzleStartingPosition;
                        extrusionNodeBeingExamined.getNozzlePosition().setB(bValue);
                        runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                        //No extrusion during a close
                        extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                        extrusionNodeBeingExamined.getExtrusion().dNotInUse();
                        if (finalCloseNode == null)
                        {
                            finalCloseNode = extrusionNodeBeingExamined;
                        }
                        break;
                    } else
                    {
                        //If we got here then we need to split this extrusion
                        //We're splitting the last part of the line (we're just looking at it in reverse order as we consider the line from the end)
                        MovementProvider priorMovement = null;

                        if ((movementNodeCounter == inScopeEvents.getInScopeEvents().size() - 1
                                || !(inScopeEvents.getInScopeEvents().get(movementNodeCounter + 1) instanceof MovementProvider)))
                        {
                            //We dont have anywhere to go!
                            try
                            {
                                Optional<MovementProvider> priorSectionMovement = nodeManagementUtilities.findPriorMovement(extrusionNodeBeingExamined);
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
                        extrusionNodeBeingExamined.appendCommentText("Start of overwrite close towards end");
                        double bValue = (runningTotalOfExtrusion / volumeToCloseOver) * nozzleStartingPosition;
                        if (closeStarted)
                        {
                            extrusionNodeBeingExamined.getNozzlePosition().setB(bValue);
                        } else
                        {
                            extrusionNodeBeingExamined.getNozzlePosition().setB(0);
                        }

                        runningTotalOfExtrusion += extrusionNodeBeingExamined.getExtrusion().getE();
                        //No extrusion during a close
                        extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                        extrusionNodeBeingExamined.getExtrusion().dNotInUse();
                        if (finalCloseNode == null)
                        {
                            finalCloseNode = extrusionNodeBeingExamined;
                        }
                        break;
                    }
                }
            }

            if (finalCloseNode != null)
            {
                finalCloseNode.setElidedExtrusion(volumeToCloseOver);
            }
            closeResult = Optional.of(new CloseResult(1.0, nozzleInUse.getNozzleParameters().getEjectionVolume(), finalCloseNode));
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
     * @throws
     * celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException
     */
    protected Optional<CloseResult> copyClose(
            final InScopeEvents extractedMovements,
            final GCodeEventNode nodeToAddClosesTo,
            final Optional<GCodeEventNode> nodeToStartCopyingFrom,
            final NozzleProxy nozzleInUse,
            final boolean preferForwards) throws NotEnoughAvailableExtrusionException, NodeProcessingException
    {
        String additionalComment = "";
        NozzleParameters nozzleParams = nozzleInUse.getNozzleParameters();

        double volumeToCloseOver = nozzleParams.getEjectionVolume();

        double runningTotalOfExtrusion = 0;
        double currentNozzlePosition = nozzleInUse.getCurrentPosition();
        double closePermm3Volume = currentNozzlePosition / volumeToCloseOver;
        double requiredVolumeToCloseOver = currentNozzlePosition / closePermm3Volume;

        int nodeToStartCopyingFromIndex = 0;
        int inScopeEventDelta = 1;
        double availableExtrusion = 0;

        TravelNode travelToStart = null;

        GCodeEventNode nodeToAddToPlaceholder = nodeToAddClosesTo;

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

            double availableExtrusionBackwardsToStartOfExtrusion = 0;
            availableExtrusionBackwardsToStartOfExtrusion = nodeManagementUtilities.findAvailableExtrusion(extractedMovements, nodeToStartCopyingFromIndex, false);

            double availableExtrusionForwardsToEndOfExtrusion = 0;
            availableExtrusionForwardsToEndOfExtrusion = nodeManagementUtilities.findAvailableExtrusion(extractedMovements, nodeToStartCopyingFromIndex, true);

            if (availableExtrusionForwardsToEndOfExtrusion >= volumeToCloseOver)
            {
                additionalComment = "forwards to end";
                availableExtrusion = availableExtrusionForwardsToEndOfExtrusion;
                inScopeEventDelta = -1;
            } else if (availableExtrusionBackwardsToStartOfExtrusion >= volumeToCloseOver)
            {
                additionalComment = "backwards to start";
                availableExtrusion = availableExtrusionBackwardsToStartOfExtrusion;
            } else
            {
                additionalComment = "can't go forward or back - going from start";
                availableExtrusion = extractedMovements.getAvailableExtrusion();
                nodeToStartCopyingFromIndex = extractedMovements.getInScopeEvents().size() - 1;
                inScopeEventDelta = -1;
            }

            if (nodeToStartCopyingFrom.get() instanceof MovementProvider)
            {
                if (nodeToStartCopyingFrom.get() instanceof ExtrusionProvider)
                {
                    ((ExtrusionProvider) nodeToStartCopyingFrom.get()).getExtrusion().eNotInUse();
                    ((ExtrusionProvider) nodeToStartCopyingFrom.get()).getExtrusion().dNotInUse();
                }

                Movement firstMovement = ((MovementProvider) nodeToStartCopyingFrom.get()).getMovement();
                //Travel to the start of the intersected extrusion
                travelToStart = new TravelNode();
                travelToStart.getMovement().setX(firstMovement.getX());
                travelToStart.getMovement().setY(firstMovement.getY());
                travelToStart.appendCommentText("Travel to start of close");

                if (inScopeEventDelta == 1 && nodeToStartCopyingFromIndex < extractedMovements.getInScopeEvents().size() - 1)
                {
                    nodeToStartCopyingFromIndex++;
                } else if (inScopeEventDelta == -1 && nodeToStartCopyingFromIndex > 0)
                {
                    nodeToStartCopyingFromIndex--;
                }
            }
        } else
        {
            additionalComment = "no start point";
            availableExtrusion = extractedMovements.getAvailableExtrusion();
            if (preferForwards)
            {
                additionalComment += " forwards preferred";
                nodeToStartCopyingFromIndex = 0;
                inScopeEventDelta = 1;
            } else
            {
                additionalComment += " backwards preferred";
                nodeToStartCopyingFromIndex = extractedMovements.getInScopeEvents().size() - 1;
                inScopeEventDelta = -1;
            }
        }

        ExtrusionNode finalCloseNode = null;

        if (availableExtrusion >= nozzleInUse.getNozzleParameters().getEjectionVolume())
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
                    nodeToAddToPlaceholder.addSiblingAfter(copy);
                    nodeToAddToPlaceholder = copy;

                    if (comparisonResult == MathUtils.LESS_THAN)
                    {
                        //One step along the way
                        runningTotalOfExtrusion += copy.getExtrusion().getE();
                        double bValue = 1 - (runningTotalOfExtrusion / volumeToCloseOver);
                        copy.getNozzlePosition().setB(bValue);
                        //No extrusion during a close
                        copy.getExtrusion().eNotInUse();
                        copy.getExtrusion().dNotInUse();
                        copy.appendCommentText("copied node");
                        //Wipe out extrusion in the area we copied from as well
                        extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                        extrusionNodeBeingExamined.getExtrusion().dNotInUse();
                        if (finalCloseNode == null)
                        {
                            finalCloseNode = copy;
                        }
                    } else if (comparisonResult == MathUtils.EQUAL)
                    {
                        //All done
                        runningTotalOfExtrusion += copy.getExtrusion().getE();
                        double bValue = 0;
                        copy.getNozzlePosition().setB(bValue);
                        //No extrusion during a close
                        copy.getExtrusion().eNotInUse();
                        copy.getExtrusion().dNotInUse();
                        copy.appendCommentText("copied node");
                        //Wipe out extrusion in the area we copied from as well
                        extrusionNodeBeingExamined.getExtrusion().eNotInUse();
                        extrusionNodeBeingExamined.getExtrusion().dNotInUse();
                        if (finalCloseNode == null)
                        {
                            finalCloseNode = copy;
                        }
                        break;
                    } else
                    {
                        MovementProvider priorMovement = null;

                        if (inScopeEventDelta > 0)
                        {
                            // Go backwards (-1)
                            if (inScopeEventCounter == 0)
                            {
                                Optional<SectionNode> parentSection = nodeManagementUtilities.lookForParentSectionNode(extrusionNodeBeingExamined);
                                Optional<MovementProvider> movement = Optional.empty();

                                if (parentSection.isPresent())
                                {
                                    movement = nodeManagementUtilities.findNextMovement(parentSection.get().getParent().get(), extrusionNodeBeingExamined);
                                }
                                if (!movement.isPresent())
                                {
                                    steno.error("Nowhere to go");
                                    throw new RuntimeException("Nowhere nowhere nowhere to go");
                                }
                                priorMovement = movement.get();
                            } else
                            {
                                priorMovement = (MovementProvider) extractedMovements.getInScopeEvents().get(inScopeEventCounter - 1);
                            }
                        } else
                        {
                            // Go forwards (1)
                            if (inScopeEventCounter == extractedMovements.getInScopeEvents().size() - 1
                                    || !(extractedMovements.getInScopeEvents().get(inScopeEventCounter + 1) instanceof MovementProvider))
                            {
                                Optional<MovementProvider> movement = nodeManagementUtilities.findPriorMovement(extractedMovements.getInScopeEvents().get(inScopeEventCounter));
                                if (!movement.isPresent())
                                {
                                    steno.error("Nowhere to go");
                                    throw new RuntimeException("Nowhere nowhere nowhere to go");
                                }
                                priorMovement = movement.get();
                            } else
                            {
                                priorMovement = (MovementProvider) extractedMovements.getInScopeEvents().get(inScopeEventCounter + 1);
                            }
                        }

                        // We can work out how to split this extrusion
                        Vector2D firstPoint = new Vector2D(priorMovement.getMovement().getX(), priorMovement.getMovement().getY());
                        Vector2D secondPoint = new Vector2D(extrusionNodeBeingExamined.getMovement().getX(), extrusionNodeBeingExamined.getMovement().getY());

                        double extrusionInFirstSection = extrusionNodeBeingExamined.getExtrusion().getE() - (extrusionNodeBeingExamined.getExtrusion().getE() + runningTotalOfExtrusion - requiredVolumeToCloseOver);

                        double proportionOfDistanceInFirstSection = extrusionInFirstSection / extrusionNodeBeingExamined.getExtrusion().getE();

                        Vector2D actualVector = secondPoint.subtract(firstPoint);
                        Vector2D firstSegment = firstPoint.add(proportionOfDistanceInFirstSection,
                                actualVector);

                        copy.getMovement().setX(firstSegment.getX());
                        copy.getMovement().setY(firstSegment.getY());
                        copy.getExtrusion().setE(0);
                        copy.getExtrusion().setD(0);
                        copy.getNozzlePosition().bNotInUse();
                        copy.appendCommentText("End of copy close segment - " + additionalComment);
                        runningTotalOfExtrusion += copy.getExtrusion().getE();
                        copy.getNozzlePosition().setB(0);

                        //No extrusion during a close
                        copy.getExtrusion().eNotInUse();
                        copy.getExtrusion().dNotInUse();
                        if (finalCloseNode == null)
                        {
                            finalCloseNode = copy;
                        }
                        break;
                    }
                } else if (extractedMovements.getInScopeEvents().get(inScopeEventCounter) instanceof TravelNode)
                {
                    TravelNode copy = ((TravelNode)extractedMovements.getInScopeEvents().get(inScopeEventCounter)).clone();
                    nodeToAddToPlaceholder.addSiblingAfter(copy);
                    nodeToAddToPlaceholder = copy;
                }
            }

            if (travelToStart != null)
            {
                nodeToAddClosesTo.addSiblingAfter(travelToStart);
            }
        } else
        {
            throw new NotEnoughAvailableExtrusionException("Not enough extrusion when attempting to copy close");
        }

        if (finalCloseNode != null)
        {
            finalCloseNode.setElidedExtrusion(volumeToCloseOver);
        }

        return Optional.of(new CloseResult(1.0, volumeToCloseOver, finalCloseNode));
    }

    protected void insertCloseNodes(LayerNode layerNode, LayerPostProcessResult lastLayerParseResult, List<NozzleProxy> nozzleProxies)
    {
        if (featureSet.isEnabled(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES))
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
                    boolean success = processRetractNode(retractHolder.getNode(), retractHolder.getNozzle(), layerNode, lastLayerParseResult);
                    if (!success)
                    {
                        steno.warning("Close failed - removing retract anyway on layer " + layerNode.getLayerNumber());
                    }
                }
                retractHolder.getNode().removeFromParent();
            }
        }
    }

    private boolean processRetractNode(RetractNode retractNode,
            NozzleProxy nozzleInUse,
            LayerNode thisLayer,
            LayerPostProcessResult lastLayerParseResult)
    {
        Optional<CloseResult> closeResult = Optional.empty();

        boolean processedClose = false;

        boolean succeeded = false;

        try
        {
            if (retractNode.getPriorExtrusionNode() != null)
            {
                closeResult = insertProgressiveNozzleClose(retractNode, retractNode.getSectionsToConsider(), nozzleInUse);
                processedClose = true;
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

                    //Don't need to close if the nozzle is already closed!
                    if (!(lastNodeOnLastLayer instanceof NozzlePositionProvider
                            && ((NozzlePositionProvider) lastNodeOnLastLayer).getNozzlePosition().isBSet()))
                    {
                        if (lastNodeOnLastLayer instanceof ExtrusionNode)
                        {
                            extrusionToCloseFrom = (ExtrusionNode) lastNodeOnLastLayer;
                        }

                        boolean foundExtrusionBeforeNozzleClose = false;

                        search:
                        while (layerBackwardsIterator.hasNext())
                        {
                            GCodeEventNode node = layerBackwardsIterator.next();

//                            if (node instanceof NozzlePositionProvider)
//                            {
//                                if (node instanceof NozzlePositionProvider
//                                        && ((NozzlePositionProvider) node).getNozzlePosition().isBSet())
//                                {
////                                Optional<SectionNode> parentSection = nodeManagementUtilities.lookForParentSectionNode(node);
////                                if (parentSection.isPresent())
////                                {
////                                    //Exclude the section we're in just in case...
////                                    sectionsToConsider.remove(parentSection.get());
////                                }
//                                    break search;
//                                }
//                            }
                            if (node instanceof ExtrusionNode)
                            {
                                foundExtrusionBeforeNozzleClose = true;

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

                        if (foundExtrusionBeforeNozzleClose)
                        {
                            processedClose = true;
                            if (sectionsToConsider.size() > 0)
                            {
                                closeResult = insertProgressiveNozzleClose(extrusionToCloseFrom, sectionsToConsider, nozzleInUse);
                            } else
                            {
                                //We only seem to have one extrusion to close over...
                                if (extrusionToCloseFrom != null)
                                {
                                    sectionsToConsider.add((SectionNode) extrusionToCloseFrom.getParent().get());
                                    closeResult = insertProgressiveNozzleClose(extrusionToCloseFrom, sectionsToConsider, nozzleInUse);
                                }
                            }
                        }
                    }
                }
            }

            if (processedClose)
            {
                if (closeResult.isPresent())
                {
//                    if (!nextExtrusionNode.isPresent())
//                    {
//                        nextExtrusionNode = nodeManagementUtilities.findNextExtrusion(thisLayer, retractNode);
//                    }
//
//                    if (nextExtrusionNode.isPresent())
//                    {
//                        //Add the elided extrusion to this node - the open routine will find it later
//                        ((ExtrusionNode) nextExtrusionNode.get()).setElidedExtrusion(closeResult.get().getNozzleCloseOverVolume());
//                    }

                    succeeded = true;
                } else
                {
                    throw new NodeProcessingException("Failed to close after retract", (Renderable) retractNode);
                }
            } else
            {
                succeeded = true;
            }

        } catch (NodeProcessingException | CannotCloseFromPerimeterException | NoPerimeterToCloseOverException | NotEnoughAvailableExtrusionException | PostProcessingError ex)
        {
            throw new RuntimeException("Failed to process retract on layer " + thisLayer.getLayerNumber() + " this will affect open and close", ex);
        }

        return succeeded;
    }
}
