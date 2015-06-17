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
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.NozzlePositionProvider;
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
            layerNode.stream()
                    .filter(node -> node instanceof UnretractNode)
                    .forEach(node ->
                            {
                                TreeUtils.removeChild(node.getParent(), node);
                    });
        }
    }

    protected void calculatePerRetractExtrusionAndNode(LayerNode layerNode)
    {
        List<GCodeEventNode> nodes = layerNode.stream().collect(Collectors.toList());

        ExtrusionNode lastExtrusionNode = null;
        double extrusionInRetract = 0;

        for (GCodeEventNode node : nodes)
        {
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

        layerNode.stream()
                .filter(node -> node instanceof OrphanObjectDelineationNode)
                .map(OrphanObjectDelineationNode.class::cast)
                .forEach(orphanNode ->
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
                            List<GCodeEventNode> children = orphanNode.getChildren().stream().collect(Collectors.toList());
                            for (GCodeEventNode childNode : children)
                            {
                                childNode.removeFromParent();
                                newObjectNode.addChildAtEnd(childNode);
                            }

                            //Add the new node
                            orphanNode.addSiblingBefore(newObjectNode);

                            //Remove the orphan
                            orphanNode.removeFromParent();
                }
                );
    }

    protected Optional<GCodeEventNode> findNextExtrusion(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<GCodeEventNode> nextExtrusion = node.streamFromHere()
                .filter(filteredNode -> filteredNode instanceof ExtrusionNode)
                .findFirst();

        return nextExtrusion;
    }

    protected Optional<MovementProvider> findNextMovement(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<MovementProvider> nextExtrusion = node.streamFromHere()
                .filter(filteredNode -> filteredNode instanceof MovementProvider)
                .findFirst()
                .map(MovementProvider.class::cast);

        return nextExtrusion;
    }

    protected Optional<GCodeEventNode> findPriorExtrusion(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<GCodeEventNode> nextExtrusion = node.streamBackwardsFromHere()
                .filter(filteredNode -> filteredNode instanceof ExtrusionNode)
                .findFirst();

        return nextExtrusion;
    }

    protected Optional<MovementProvider> findPriorMovementInPreviousSection(GCodeEventNode node) throws NodeProcessingException
    {
        GCodeEventNode nodeParent = node.getParent();

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

        Optional<MovementProvider> nextExtrusion = previousSection.get().streamChildrenAndMeBackwards()
                .filter(filteredNode -> filteredNode instanceof MovementProvider)
                .findFirst()
                .map(MovementProvider.class::cast);

        return nextExtrusion;
    }

    protected Optional<GCodeEventNode> findLastExtrusionEventInLayer(LayerNode layerNode)
    {
        Optional<GCodeEventNode> lastExtrusionNode = Optional.empty();

        List<GCodeEventNode> toolSelectNodes = layerNode
                .stream()
                .filter(node -> node instanceof ToolSelectNode)
                .collect(Collectors.toList());

        if (!toolSelectNodes.isEmpty())
        {
            ToolSelectNode lastToolSelect = (ToolSelectNode) toolSelectNodes.get(toolSelectNodes.size() - 1);

            List<GCodeEventNode> extrusionNodes = lastToolSelect
                    .stream()
                    .filter(node -> node instanceof ExtrusionNode)
                    .collect(Collectors.toList());

            if (!extrusionNodes.isEmpty())
            {
                lastExtrusionNode = Optional.of(extrusionNodes.get(extrusionNodes.size() - 1));
            }
        }

        return lastExtrusionNode;
    }

    public double findAvailableExtrusion(GCodeEventNode lastExtrusionNode, boolean forwards) throws NodeProcessingException
    {
        double availableExtrusion = 0;
        List<GCodeEventNode> nozzlePositionProviders;

        if (forwards)
        {
            //Go backwards until we see a nozzle provider that has 
            nozzlePositionProviders = lastExtrusionNode.streamSiblingsAndMeFromHere()
                    .filter(node -> node instanceof NozzlePositionProvider)
                    .collect(Collectors.toList());
        } else
        {
            //Go backwards until we see a nozzle provider that has 
            nozzlePositionProviders = lastExtrusionNode.streamSiblingsAndMeBackwardsFromHere()
                    .filter(node -> node instanceof NozzlePositionProvider)
                    .collect(Collectors.toList());
        }
        
        for (GCodeEventNode node : nozzlePositionProviders)
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
        
        return availableExtrusion;
    }

}
