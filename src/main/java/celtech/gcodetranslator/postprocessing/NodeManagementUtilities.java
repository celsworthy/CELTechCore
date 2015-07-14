package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.NodeProcessingException;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.RetractNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.UnretractNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
            Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);
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
        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);

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

        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);

        List<OrphanObjectDelineationNode> orphans = new ArrayList<>();

        while (layerIterator.hasNext())
        {
            GCodeEventNode node = layerIterator.next();
            if (node instanceof OrphanObjectDelineationNode)
            {
                orphans.add((OrphanObjectDelineationNode) node);
            }
        }

        for (OrphanObjectDelineationNode orphanNode : orphans)
        {
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
            List<GCodeEventNode> nodesToRemove = new ArrayList<>();

            while (childIterator.hasNext())
            {
                GCodeEventNode childNode = childIterator.next();
                nodesToRemove.add(childNode);
            }

            for (GCodeEventNode nodeToRemove : nodesToRemove)
            {
                nodeToRemove.removeFromParent();
                newObjectNode.addChildAtEnd(nodeToRemove);
            }

            //Add the new node
            orphanNode.addSiblingBefore(newObjectNode);

            //Remove the orphan
            orphanNode.removeFromParent();
        }
    }

    protected Optional<ExtrusionNode> findNextExtrusion(GCodeEventNode topLevelNode, GCodeEventNode node) throws NodeProcessingException
    {
        Optional<ExtrusionNode> nextExtrusion = Optional.empty();

        LinkedList<GCodeEventNode> nodeHierarchy = new LinkedList<>();

        boolean foundTopLevel = false;

        GCodeEventNode currentNode = node;

        nodeHierarchy.add(node);

        while (currentNode.getParent().isPresent() && !foundTopLevel)
        {
            GCodeEventNode parent = currentNode.getParent().get();
            if (parent == topLevelNode)
            {
                foundTopLevel = true;
            } else
            {
                nodeHierarchy.addFirst(parent);
            }

            currentNode = parent;
        }

        Iterator<GCodeEventNode> childIterator = topLevelNode.treeSpanningIterator(nodeHierarchy);

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof ExtrusionNode)
            {
                nextExtrusion = Optional.of((ExtrusionNode) childNode);
                break;
            }
        }

        return nextExtrusion;
    }

    protected Optional<MovementProvider> findNextMovement(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<MovementProvider> nextMovement = Optional.empty();

        Iterator<GCodeEventNode> childIterator = node.treeSpanningIterator(null);

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

    protected Optional<ExtrusionNode> findPriorExtrusion(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<ExtrusionNode> priorExtrusion = Optional.empty();

        Iterator<GCodeEventNode> childIterator = node.treeSpanningBackwardsIterator();

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof ExtrusionNode)
            {
                priorExtrusion = Optional.of((ExtrusionNode)childNode);
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

        Iterator<GCodeEventNode> childIterator = previousSection.get().childrenAndMeBackwardsIterator();

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

    public double findAvailableExtrusion(GCodeEventNode lastExtrusionNode, boolean forwards) throws NodeProcessingException
    {
        double availableExtrusion = 0;

        Iterator<GCodeEventNode> nozzlePositionCandidates;

        if (forwards)
        {
            nozzlePositionCandidates = lastExtrusionNode.meAndSiblingsIterator();
        } else
        {
            nozzlePositionCandidates = lastExtrusionNode.meAndSiblingsBackwardsIterator();
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
