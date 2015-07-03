package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.gcodetranslator.postprocessing.nodes.nodeFunctions.IteratorWithOrigin;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.parboiled.trees.TreeUtils;

/**
 *
 * @author Ian
 */
public class NodeManagementUtilities
{

    private final PostProcessorFeatureSet featureSet;

    public NodeManagementUtilities(PostProcessorFeatureSet featureSet)
    {
        this.featureSet = featureSet;
    }

    protected void removeUnretractNodes(LayerNode layerNode)
    {
        if (featureSet.isEnabled(PostProcessorFeature.REMOVE_ALL_UNRETRACTS))
        {
            Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator();
            List<UnretractNode> nodesToDelete = new ArrayList<>();

            while (layerIterator.hasNext())
            {
                GCodeEventNode node = layerIterator.next();
                if (node instanceof UnretractNode)
                {
                    nodesToDelete.add((UnretractNode) node);
                }
            }

            for (UnretractNode unretractNode : nodesToDelete)
            {
                unretractNode.removeFromParent();
            }
        }
    }

    protected void calculatePerRetractExtrusionAndNode(LayerNode layerNode)
    {
        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator();

        ExtrusionNode lastExtrusionNode = null;
        double extrusionInRetract = 0;

        while (layerIterator.hasNext())
        {
            GCodeEventNode node = layerIterator.next();
            if (node instanceof ExtrusionNode)
            {
                ExtrusionNode extrusionNode = (ExtrusionNode) node;
                extrusionInRetract += extrusionNode.getExtrusion().getE();
                lastExtrusionNode = extrusionNode;
            } else if (node instanceof RetractNode)
            {
                RetractNode retractNode = (RetractNode) node;
                retractNode.setExtrusionSinceLastRetract(extrusionInRetract);
                extrusionInRetract = 0;

                if (lastExtrusionNode != null)
                {
                    retractNode.setPriorExtrusionNode(lastExtrusionNode);
                    lastExtrusionNode = null;
                }
            }
        }
    }

    protected void rehomeOrphanObjects(LayerNode layerNode, final LayerPostProcessResult lastLayerParseResult)
    {
        // Orphans occur when there is no Tn directive in a layer
        //
        // At the start of the file we should treat this as object 0
        // Subsequently we should look at the last layer to see which object was in force and create an object with the same reference

        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator();

        while (layerIterator.hasNext())
        {
            GCodeEventNode node = layerIterator.next();
            if (node instanceof OrphanObjectDelineationNode)
            {
                OrphanObjectDelineationNode orphanNode = (OrphanObjectDelineationNode) node;

                ObjectDelineationNode newObjectNode = new ObjectDelineationNode();

                int potentialObjectNumber = orphanNode.getPotentialObjectNumber();

                if (potentialObjectNumber
                        < 0)
                {
                    if (layerNode.getLayerNumber() == 0)
                    {
                        // Has to be 0 if we're on the first layer
                        potentialObjectNumber = 0;
                    } else if (lastLayerParseResult.getLastObjectNumber().isPresent())
                    {
                        potentialObjectNumber = lastLayerParseResult.getLastObjectNumber().get();
                    } else
                    {
                        throw new RuntimeException("Cannot determine object number for orphan on layer " + layerNode.getLayerNumber());
                    }
                }

                newObjectNode.setObjectNumber(potentialObjectNumber);

                //Transfer the children from the orphan to the new node
                Iterator<GCodeEventNode> childIterator = orphanNode.childIterator();

                while (childIterator.hasNext())
                {
                    GCodeEventNode childNode = childIterator.next();
                    childNode.removeFromParent();
                    newObjectNode.addChildAtEnd(childNode);
                }

                //Add the new node
                orphanNode.addSiblingBefore(newObjectNode);

                //Remove the orphan
                orphanNode.removeFromParent();
            }
        }
    }

    protected Optional<GCodeEventNode> findNextExtrusion(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<GCodeEventNode> nextExtrusion = Optional.empty();

        Iterator<GCodeEventNode> childIterator = node.treeSpanningIterator();

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof ExtrusionNode)
            {
                nextExtrusion = Optional.of(childNode);
                break;
            }
        }

        return nextExtrusion;
    }

    protected Optional<MovementProvider> findNextMovement(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<MovementProvider> nextMovement = Optional.empty();

        Iterator<GCodeEventNode> childIterator = node.treeSpanningIterator();

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof MovementProvider)
            {
                nextMovement = Optional.of((MovementProvider) childNode);
                break;
            }
        }

        return nextMovement;
    }

    protected Optional<MovementProvider> findPriorMovement(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<MovementProvider> priorMovement = Optional.empty();

        Iterator<GCodeEventNode> childIterator = node.treeSpanningBackwardsIterator();

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof MovementProvider)
            {
                priorMovement = Optional.of((MovementProvider) childNode);
                break;
            }
        }

        return priorMovement;
    }

    protected Optional<GCodeEventNode> findPriorExtrusion(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<GCodeEventNode> priorExtrusion = Optional.empty();

        Iterator<GCodeEventNode> childIterator = node.treeSpanningBackwardsIterator();

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof ExtrusionNode)
            {
                priorExtrusion = Optional.of(childNode);
                break;
            }
        }

        return priorExtrusion;
    }

    protected Optional<MovementProvider> findPriorMovementInPreviousSection(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<MovementProvider> priorMovement = Optional.empty();

        GCodeEventNode nodeParent = node.getParent().get();

        if (nodeParent == null
                || !(nodeParent instanceof SectionNode))
        {
            throw new NodeProcessingException("Parent of is not present or not a section", node);
        }

        Optional<GCodeEventNode> previousSection = nodeParent.getSiblingBefore();

        if (!previousSection.isPresent())
        {
            throw new NodeProcessingException("Couldn't find a prior section", node);

        }

        Iterator<GCodeEventNode> childIterator = node.childrenAndMeBackwardsIterator();

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof MovementProvider)
            {
                priorMovement = Optional.of((MovementProvider) childNode);
                break;
            }
        }

        return priorMovement;
    }

    protected Optional<GCodeEventNode> findLastExtrusionEventInLayer(LayerNode layerNode)
    {
        Optional<GCodeEventNode> lastExtrusionEvent = Optional.empty();

        GCodeEventNode lastEventNode = layerNode.getAbsolutelyTheLastEvent();

        Iterator<GCodeEventNode> backwardsIterator = lastEventNode.treeSpanningBackwardsIterator();
        while (backwardsIterator.hasNext())
        {
            GCodeEventNode nodeUnderConsideration = backwardsIterator.next();
            if (nodeUnderConsideration instanceof ExtrusionNode)
            {
                lastExtrusionEvent = Optional.of(nodeUnderConsideration);
                break;
            }
        }

        return lastExtrusionEvent;
    }

    public double findAvailableExtrusion(GCodeEventNode lastExtrusionNode, boolean forwards) throws NodeProcessingException
    {
        double availableExtrusion = 0;

        Iterator<GCodeEventNode> nozzlePositionCandidates;

        if (forwards)
        {
            nozzlePositionCandidates = lastExtrusionNode.meAndSiblingsBackwardsIterator();
        } else
        {
            nozzlePositionCandidates = lastExtrusionNode.meAndSiblingsIterator();
        }

        while (nozzlePositionCandidates.hasNext())
        {
            GCodeEventNode node = nozzlePositionCandidates.next();

            if (node instanceof NozzlePositionProvider)
            {
                NozzlePositionProvider provider = (NozzlePositionProvider) node;
                if (provider.getNozzlePosition().isBSet())
                {
                    break;
                } else if (node instanceof ExtrusionNode)
                {
                    ExtrusionNode extrusionNode = (ExtrusionNode) node;
                    availableExtrusion += extrusionNode.getExtrusion().getE();
                }
            }
        }

        return availableExtrusion;
    }

}
