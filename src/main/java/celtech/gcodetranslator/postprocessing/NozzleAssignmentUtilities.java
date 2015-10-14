package celtech.gcodetranslator.postprocessing;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import celtech.gcodetranslator.postprocessing.nodes.ObjectDelineationNode;
import celtech.gcodetranslator.postprocessing.nodes.OrphanSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SkirtSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SupportInterfaceSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.SupportSectionNode;
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Head.HeadType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.shape.MeshView;

/**
 *
 * @author Ian
 */
public class NozzleAssignmentUtilities
{

    private final List<NozzleProxy> nozzleProxies;
    private final SlicerParametersFile slicerParametersFile;
    private final HeadFile headFile;
    private final PostProcessorFeatureSet featureSet;
    private final Project project;
    private final PostProcessingMode postProcessingMode;

    private final NozzleManagementUtilities nozzleControlUtilities;
    private final Map<Integer, Integer> extruderToNozzleMap;
    private final Map<Integer, Integer> nozzleToExtruderMap;

    public NozzleAssignmentUtilities(List<NozzleProxy> nozzleProxies,
            SlicerParametersFile slicerParametersFile,
            HeadFile headFile,
            PostProcessorFeatureSet featureSet,
            Project project,
            PostProcessingMode postProcessingMode)
    {
        this.nozzleProxies = nozzleProxies;
        this.slicerParametersFile = slicerParametersFile;
        this.headFile = headFile;
        this.featureSet = featureSet;
        this.project = project;
        this.postProcessingMode = postProcessingMode;

        nozzleControlUtilities = new NozzleManagementUtilities(nozzleProxies, slicerParametersFile,
                headFile);

        extruderToNozzleMap = new HashMap<>();
        nozzleToExtruderMap = new HashMap<>();
        for (int extruderNumber = 0; extruderNumber < 2; extruderNumber++)
        {
            Optional<NozzleProxy> proxy = nozzleControlUtilities.chooseNozzleProxyByExtruderNumber(
                    extruderNumber);
            if (proxy.isPresent())
            {
                extruderToNozzleMap.put(extruderNumber, proxy.get().getNozzleReferenceNumber());
                nozzleToExtruderMap.put(proxy.get().getNozzleReferenceNumber(), extruderNumber);
            }
        }
    }

    protected void assignExtrusionToCorrectExtruder(LayerNode layerNode)
    {
        // Don't change anything if we're in task-based selection as this always uses extruder E
        if (postProcessingMode != PostProcessingMode.TASK_BASED_NOZZLE_SELECTION)
        {
            //Tool select nodes live directly under layers

            Iterator<GCodeEventNode> layerIterator = layerNode.childIterator();

            while (layerIterator.hasNext())
            {
                GCodeEventNode potentialToolSelectNode = layerIterator.next();

                if (potentialToolSelectNode instanceof ToolSelectNode)
                {
                    ToolSelectNode toolSelectNode = (ToolSelectNode) potentialToolSelectNode;

                    Iterator<GCodeEventNode> toolSelectNodeIterator = toolSelectNode.treeSpanningIterator(
                            null);

                    while (toolSelectNodeIterator.hasNext())
                    {
                        GCodeEventNode potentialExtrusionProvider = toolSelectNodeIterator.next();

                        if (potentialExtrusionProvider instanceof ExtrusionProvider)
                        {
                            ExtrusionProvider extrusionNode = (ExtrusionProvider) potentialExtrusionProvider;

                            switch (toolSelectNode.getToolNumber())
                            {
                                case 1:
                                    extrusionNode.getExtrusion().extrudeUsingEOnly();
                                    break;
                                case 0:
                                    extrusionNode.getExtrusion().extrudeUsingDOnly();
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *
     * @param layerNode
     * @param lastLayerResult
     * @param postProcessingMode
     * @return The reference number of the last object in the layer
     */
    protected int insertNozzleControlSectionsByTask(LayerNode layerNode,
            LayerPostProcessResult lastLayerResult,
            PostProcessingMode postProcessingMode)
    {
        int lastObjectReferenceNumber = -1;

        if (featureSet.isEnabled(PostProcessorFeature.CLOSE_ON_TASK_CHANGE))
        {
            //TODO put in first layer forced nozzle select
            //
//        if (layerNode.getLayerNumber() == 0)
//        {
//            //First layer
//            //Look for travels that exceed 2mm and close/open the nozzle as necessary
//
//            if (postProcessingMode == PostProcessingMode.TASK_BASED_NOZZLE_SELECTION)
//            {
//                // Look to see if a first layer nozzle has been selected
//                if (slicerParameters.getFirstLayerNozzle() > -1)
//                {
//                    currentNozzleProxy = nozzleProxies.get(slicerParameters.getFirstLayerNozzle());
//                } else
//                {
//                    currentNozzleProxy
//                }
//            }
//        }
            //We'll need at least one of these per layer
            ToolSelectNode toolSelectNode = null;

            SectionNode lastSectionNode = null;

            if (lastLayerResult != null && lastLayerResult.getLayerData() != null)
            {
                GCodeEventNode lastEventOnLastLayer = lastLayerResult.getLayerData().getAbsolutelyTheLastEvent();
                if (lastEventOnLastLayer != null)
                {
                    Optional<GCodeEventNode> potentialLastSection = lastEventOnLastLayer.getParent();
                    if (potentialLastSection.isPresent()
                            && potentialLastSection.get() instanceof SectionNode)
                    {
                        lastSectionNode = (SectionNode) potentialLastSection.get();

                        Optional<GCodeEventNode> potentialToolSelectNode = lastSectionNode.getParent();
                        if (potentialToolSelectNode.isPresent()
                                && potentialToolSelectNode.get() instanceof ToolSelectNode)
                        {
                            toolSelectNode = (ToolSelectNode) potentialToolSelectNode.get();
                        }
                    }
                }
            }

            // At this stage the object nodes are directly beneath the layer node
            // Find all of the objects in this layer
            Iterator<GCodeEventNode> layerChildIterator = layerNode.childIterator();
            List<ObjectDelineationNode> objectNodes = new ArrayList<>();

            while (layerChildIterator.hasNext())
            {
                GCodeEventNode potentialObjectNode = layerChildIterator.next();

                if (potentialObjectNode instanceof ObjectDelineationNode)
                {
                    objectNodes.add((ObjectDelineationNode) potentialObjectNode);
                }
            }

            boolean blankToolSelect = false;

            for (ObjectDelineationNode objectNode : objectNodes)
            {
                lastObjectReferenceNumber = objectNode.getObjectNumber();

                List<GCodeEventNode> childNodes = objectNode.getChildren().stream().collect(
                        Collectors.toList());

                if (toolSelectNode == null)
                {
                    //Need to create a new Tool Select Node
                    toolSelectNode = new ToolSelectNode();
                    layerNode.addChildAtEnd(toolSelectNode);
                    blankToolSelect = true;
                }

                for (GCodeEventNode childNode : childNodes)
                {
                    if (childNode instanceof SectionNode)
                    {
                        SectionNode sectionNodeBeingExamined = (SectionNode) childNode;

                        NozzleProxy requiredNozzle = null;

                        try
                        {
                            if (sectionNodeBeingExamined instanceof OrphanSectionNode)
                            {
                                if (lastSectionNode == null)
                                {
                                    throw new RuntimeException(
                                            "Failed to process orphan section on layer "
                                            + layerNode.getLayerNumber()
                                            + " as last section didn't exist");
                                }

                                if (postProcessingMode == PostProcessingMode.TASK_BASED_NOZZLE_SELECTION)
                                {
                                    requiredNozzle = nozzleControlUtilities.chooseNozzleProxyByTask(
                                            lastSectionNode);
                                } else
                                {
                                    NozzleProxy requiredNozzleForSupportRaftSkirt = nozzleProxies.get((postProcessingMode == PostProcessingMode.SUPPORT_IN_FIRST_MATERIAL) ? 0 : 1);
                                    NozzleProxy requiredNozzleForObject = nozzleProxies.get(lastObjectReferenceNumber);
                                    requiredNozzle = nozzleControlUtilities.chooseNozzleProxyForDifferentialSupportMaterial(lastSectionNode,
                                            requiredNozzleForSupportRaftSkirt,
                                            requiredNozzleForObject);
                                }

                                try
                                {
                                    SectionNode replacementSection = lastSectionNode.getClass().newInstance();

                                    // Move the child nodes to the replacement section
                                    Iterator<GCodeEventNode> sectionNodeChildIterator = sectionNodeBeingExamined.childIterator();
                                    while (sectionNodeChildIterator.hasNext())
                                    {
                                        GCodeEventNode child = sectionNodeChildIterator.next();

                                        replacementSection.addChildAtEnd(child);
                                    }

                                    sectionNodeBeingExamined.removeFromParent();
                                    lastSectionNode.addSiblingAfter(replacementSection);
                                    sectionNodeBeingExamined = replacementSection;
                                } catch (InstantiationException | IllegalAccessException ex)
                                {
                                    throw new RuntimeException(
                                            "Failed to process orphan section on layer "
                                            + layerNode.getLayerNumber(), ex);
                                }
                            } else
                            {
                                if (postProcessingMode == PostProcessingMode.TASK_BASED_NOZZLE_SELECTION)
                                {
                                    requiredNozzle = nozzleControlUtilities.chooseNozzleProxyByTask(
                                            sectionNodeBeingExamined);
                                } else
                                {
                                    NozzleProxy requiredNozzleForSupportRaftSkirt = nozzleProxies.get((postProcessingMode == PostProcessingMode.SUPPORT_IN_FIRST_MATERIAL) ? 0 : 1);
                                    NozzleProxy requiredNozzleForObject = nozzleProxies.get(lastObjectReferenceNumber);
                                    requiredNozzle = nozzleControlUtilities.chooseNozzleProxyForDifferentialSupportMaterial(sectionNodeBeingExamined,
                                            requiredNozzleForSupportRaftSkirt,
                                            requiredNozzleForObject);
                                }
                            }

                            if (!blankToolSelect
                                    && (toolSelectNode == null
                                    || toolSelectNode.getToolNumber()
                                    != requiredNozzle.getNozzleReferenceNumber()))
                            {
                                //Need to create a new Tool Select Node
                                toolSelectNode = new ToolSelectNode();
                                toolSelectNode.setToolNumber(
                                        requiredNozzle.getNozzleReferenceNumber());
                                layerNode.addChildAtEnd(toolSelectNode);
                            } else if (blankToolSelect)
                            {
                                toolSelectNode.setToolNumber(
                                        requiredNozzle.getNozzleReferenceNumber());
                            }

                            sectionNodeBeingExamined.removeFromParent();
                            toolSelectNode.addChildAtEnd(sectionNodeBeingExamined);

                        } catch (UnableToFindSectionNodeException ex)
                        {
                            throw new RuntimeException(
                                    "Error attempting to insert nozzle control sections by task - "
                                    + ex.getMessage(), ex);
                        }

                        lastSectionNode = sectionNodeBeingExamined;
                    } else
                    {
                        if (toolSelectNode == null)
                        {
                            //We got here because a node is sitting outside a section

                        }
                        //Probably a travel node - move it over without changing it
                        childNode.removeFromParent();
                        toolSelectNode.addChildAtEnd(childNode);
                    }
                }

                if (!objectNode.getChildren().isEmpty())
                {
                    throw new RuntimeException("Transfer of children from object "
                            + objectNode.getObjectNumber() + " failed");
                }
                objectNode.removeFromParent();
            }
        }
        return lastObjectReferenceNumber;
    }

    protected int insertNozzleControlSectionsByObject_old(LayerNode layerNode,
            LayerPostProcessResult lastLayerResult,
            HeadType headType)
    {
        int lastObjectReferenceNumber = -1;
        //We'll need at least one of these per layer
        ToolSelectNode toolSelectNode = null;

        SectionNode lastSectionNode = null;

        Iterator<GCodeEventNode> layerChildIterator = layerNode.childIterator();
        List<ObjectDelineationNode> objectNodes = new ArrayList<>();

        while (layerChildIterator.hasNext())
        {
            GCodeEventNode potentialObjectNode = layerChildIterator.next();

            if (potentialObjectNode instanceof ObjectDelineationNode)
            {
                objectNodes.add((ObjectDelineationNode) potentialObjectNode);
            }
        }

        for (ObjectDelineationNode objectNode : objectNodes)
        {
            lastObjectReferenceNumber = objectNode.getObjectNumber();

            //Object numbers correspond to extruder numbers
            ObjectDelineationNode objectNodeBeingExamined = (ObjectDelineationNode) objectNode;

            int requiredToolNumber = -1;

            //TODO change if we ever get here with a single material head
            //Tool number corresponds to nozzle number
            requiredToolNumber = extruderToNozzleMap.get(objectNodeBeingExamined.getObjectNumber());

            if (toolSelectNode == null
                    || toolSelectNode.getToolNumber() != requiredToolNumber)
            {
                //Need to create a new Tool Select Node
                toolSelectNode = new ToolSelectNode();
                toolSelectNode.setToolNumber(requiredToolNumber);
                layerNode.addChildAtEnd(toolSelectNode);
            }

            objectNodeBeingExamined.removeFromParent();
            List<GCodeEventNode> sectionNodes = objectNodeBeingExamined.getChildren().stream().collect(
                    Collectors.toList());

            for (GCodeEventNode childNode : sectionNodes)
            {
                if (childNode instanceof SectionNode)
                {
                    SectionNode sectionNodeUnderExamination = (SectionNode) childNode;

                    if (sectionNodeUnderExamination instanceof OrphanSectionNode)
                    {
                        if (lastSectionNode == null)
                        {
                            //Try to get the section from the last layer
                            if (lastLayerResult != null && lastLayerResult.getLayerData() != null)
                            {
                                GCodeEventNode lastEventOnLastLayer = lastLayerResult.getLayerData().getAbsolutelyTheLastEvent();
                                if (lastEventOnLastLayer != null)
                                {
                                    Optional<GCodeEventNode> potentialLastSection = lastEventOnLastLayer.getParent();
                                    if (potentialLastSection.isPresent()
                                            && potentialLastSection.get() instanceof SectionNode)
                                    {
                                        lastSectionNode = (SectionNode) potentialLastSection.get();
                                    }
                                }
                            }

                            if (lastSectionNode == null)
                            {
                                throw new RuntimeException(
                                        "Failed to process orphan section on layer "
                                        + layerNode.getLayerNumber() + " as last section didn't exist");
                            }
                        }

                        try
                        {
                            SectionNode replacementSection = lastSectionNode.getClass().newInstance();

                            List<GCodeEventNode> sectionChildren = new ArrayList<>();
                            Iterator<GCodeEventNode> sectionChildrenIterator = sectionNodeUnderExamination.childIterator();

                            while (sectionChildrenIterator.hasNext())
                            {
                                GCodeEventNode sectionChildNode = sectionChildrenIterator.next();
                                sectionChildren.add(sectionChildNode);
                            }

                            for (GCodeEventNode sectionChildNode : sectionChildren)
                            {
                                sectionChildNode.removeFromParent();
                                replacementSection.addChildAtEnd(sectionChildNode);
                            }

                            sectionNodeUnderExamination.removeFromParent();
                            toolSelectNode.addChildAtEnd(replacementSection);
                            lastSectionNode = replacementSection;
                        } catch (InstantiationException | IllegalAccessException ex)
                        {
                            throw new RuntimeException("Failed to process orphan section on layer "
                                    + layerNode.getLayerNumber(), ex);
                        }
                    } else
                    {
                        sectionNodeUnderExamination.removeFromParent();
                        toolSelectNode.addChildAtEnd(sectionNodeUnderExamination);
                        lastSectionNode = sectionNodeUnderExamination;
                    }
                } else
                {
                    //Probably a travel node - move it over without changing it
                    childNode.removeFromParent();
                    toolSelectNode.addChildAtEnd(childNode);
                }
            }
        }
        return lastObjectReferenceNumber;
    }

    protected int insertNozzleControlSectionsByObject(LayerNode layerNode,
            LayerPostProcessResult lastLayerResult)
    {
        List<GCodeEventNode> nodesToRemove = new ArrayList<>();

        Iterator<GCodeEventNode> layerChildIterator = layerNode.childIterator();
        List<ObjectDelineationNode> objectNodes = new ArrayList<>();

        while (layerChildIterator.hasNext())
        {
            GCodeEventNode nodeBeingExamined = layerChildIterator.next();

            if (nodeBeingExamined instanceof ObjectDelineationNode)
            {
                objectNodes.add((ObjectDelineationNode) nodeBeingExamined);
            }
        }

        int objectReferenceNumber = -1;
        //We'll need at least one of these per layer
        ToolSelectNode toolSelectNode = null;

        SectionNode lastSectionNode = null;

        for (ObjectDelineationNode objectNode : objectNodes)
        {
            objectReferenceNumber = objectNode.getObjectNumber();

            //Object numbers correspond to extruder numbers
            ObjectDelineationNode objectNodeBeingExamined = (ObjectDelineationNode) objectNode;

            objectNodeBeingExamined.removeFromParent();

            List<GCodeEventNode> objectChildren = objectNodeBeingExamined.getChildren().stream().collect(
                    Collectors.toList());

            for (GCodeEventNode childNode : objectChildren)
            {
                if (childNode instanceof SectionNode)
                {
                    SectionNode sectionUnderConsideration = (SectionNode) childNode;

                    if (sectionUnderConsideration instanceof OrphanSectionNode)
                    {
                        if (lastSectionNode == null)
                        {
                            //Try to get the section from the last layer
                            if (lastLayerResult != null && lastLayerResult.getLayerData() != null)
                            {
                                GCodeEventNode lastEventOnLastLayer = lastLayerResult.getLayerData().getAbsolutelyTheLastEvent();
                                if (lastEventOnLastLayer != null)
                                {
                                    Optional<GCodeEventNode> potentialLastSection = lastEventOnLastLayer.getParent();
                                    if (potentialLastSection.isPresent()
                                            && potentialLastSection.get() instanceof SectionNode)
                                    {
                                        lastSectionNode = (SectionNode) potentialLastSection.get();
                                    }
                                }
                            }

                            if (lastSectionNode == null)
                            {
                                throw new RuntimeException(
                                        "Couldn't determine prior section for orphan on layer "
                                        + layerNode.getLayerNumber() + " as last section didn't exist");
                            } else
                            {
                                if (lastSectionNode.getParent() != null
                                        && (lastSectionNode.getParent().get() instanceof ObjectDelineationNode))
                                {
                                    objectReferenceNumber = ((ObjectDelineationNode) lastSectionNode.getParent().get()).getObjectNumber();
                                } else
                                {
                                    throw new RuntimeException(
                                            "Couldn't determine prior object for orphan section on layer "
                                            + layerNode.getLayerNumber() + " as last section didn't exist");
                                }
                            }
                        }

                        try
                        {
                            SectionNode replacementSection = lastSectionNode.getClass().newInstance();

                            List<GCodeEventNode> sectionChildren = new ArrayList<>();
                            Iterator<GCodeEventNode> sectionChildrenIterator = sectionUnderConsideration.childIterator();

                            while (sectionChildrenIterator.hasNext())
                            {
                                GCodeEventNode sectionChildNode = sectionChildrenIterator.next();
                                sectionChildren.add(sectionChildNode);
                            }

                            for (GCodeEventNode sectionChildNode : sectionChildren)
                            {
                                sectionChildNode.removeFromParent();
                                replacementSection.addChildAtEnd(sectionChildNode);
                            }

                            nodesToRemove.add(sectionUnderConsideration);
                            sectionUnderConsideration = replacementSection;
                        } catch (InstantiationException | IllegalAccessException ex)
                        {
                            throw new RuntimeException("Failed to process orphan section on layer "
                                    + layerNode.getLayerNumber(), ex);
                        }
                    }

                    int requiredToolNumber = -1;

                    //Tool number corresponds to nozzle number
                    if ((postProcessingMode == PostProcessingMode.SUPPORT_IN_FIRST_MATERIAL
                            || postProcessingMode == PostProcessingMode.SUPPORT_IN_SECOND_MATERIAL)
                            && ((sectionUnderConsideration instanceof SupportSectionNode)
                            || (sectionUnderConsideration instanceof SupportInterfaceSectionNode)
                            || (sectionUnderConsideration instanceof SkirtSectionNode)))
                    {
                        requiredToolNumber = (postProcessingMode == PostProcessingMode.SUPPORT_IN_FIRST_MATERIAL) ? 0 : 1;
                    } else
                    {
                        requiredToolNumber = extruderToNozzleMap.get(objectReferenceNumber);
                    }

                    if (toolSelectNode == null
                            || toolSelectNode.getToolNumber() != requiredToolNumber)
                    {
                        //Need to create a new Tool Select Node
                        toolSelectNode = new ToolSelectNode();
                        toolSelectNode.setToolNumber(requiredToolNumber);
                        layerNode.addChildAtEnd(toolSelectNode);
                    }

                    sectionUnderConsideration.removeFromParent();
                    toolSelectNode.addChildAtEnd(sectionUnderConsideration);
                    lastSectionNode = sectionUnderConsideration;
                } else
                {
                    //Probably a travel node - move it over without changing it
                    childNode.removeFromParent();

                    if (toolSelectNode == null)
                    {
                        //Need to create a new Tool Select Node
                        //At this stage all we can do is look at the object number
                        toolSelectNode = new ToolSelectNode();
                        toolSelectNode.setToolNumber(extruderToNozzleMap.get(objectReferenceNumber));
                        layerNode.addChildAtEnd(toolSelectNode);
                    }
                    toolSelectNode.addChildAtEnd(childNode);
                }
            }
            
            for (GCodeEventNode node : nodesToRemove)
            {
                node.removeFromParent();
            }
        }
        return objectReferenceNumber;
    }
}
