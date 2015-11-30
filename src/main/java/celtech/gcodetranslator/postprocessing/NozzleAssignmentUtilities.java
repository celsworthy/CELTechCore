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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.scene.shape.MeshView;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class NozzleAssignmentUtilities
{

    private final Stenographer steno = StenographerFactory.getStenographer(NozzleAssignmentUtilities.class.getName());
    private final List<NozzleProxy> nozzleProxies;
    private final SlicerParametersFile slicerParametersFile;
    private final HeadFile headFile;
    private final PostProcessorFeatureSet featureSet;
    private final Project project;
    private final PostProcessingMode postProcessingMode;

    private final NozzleManagementUtilities nozzleControlUtilities;
    private final Map<Integer, Integer> objectToNozzleNumberMap;
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

        objectToNozzleNumberMap = new HashMap<>();
        int objectIndex = 0;
        for (ModelContainer model : project.getTopLevelModels())
        {
            for (MeshView meshView : model.descendentMeshViews())
            {
                int extruderNumber = ((ModelContainer) meshView.getParent()).getAssociateWithExtruderNumberProperty().get();
                objectToNozzleNumberMap.put(objectIndex, extruderToNozzleMap.get(extruderNumber));
                objectIndex++;
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
                    int requiredToolNumber = -1;

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
                                if (layerNode.getLayerNumber() == 0)
                                {
                                    //Special case - set tool to 0
                                    requiredToolNumber = 0;
                                    steno.info("Layer 0 - special case for nozzle assignment");
                                } else
                                {
                                    throw new RuntimeException(
                                            "Couldn't determine prior section for orphan on layer "
                                            + layerNode.getLayerNumber() + " as last section didn't exist");
                                }
                            } else
                            {
                                if (lastSectionNode.getParent() != null
                                        && (lastSectionNode.getParent().get() instanceof ObjectDelineationNode))
                                {
                                    objectReferenceNumber = ((ObjectDelineationNode) lastSectionNode.getParent().get()).getObjectNumber();
                                } else if (lastSectionNode.getParent() != null
                                        && (lastSectionNode.getParent().get() instanceof ToolSelectNode))
                                {
                                    requiredToolNumber = ((ToolSelectNode) lastSectionNode.getParent().get()).getToolNumber();
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

                    if (requiredToolNumber < 0)
                    {
                        //Tool number corresponds to nozzle number
                        if (postProcessingMode == PostProcessingMode.TASK_BASED_NOZZLE_SELECTION)
                        {
                            //Assuming that we'll only be here with a single material nozzle
                            //In this case nozzle 0 corresponds to tool 0
                            try
                            {
                                NozzleProxy requiredNozzle = nozzleControlUtilities.chooseNozzleProxyByTask(sectionUnderConsideration);
                                requiredToolNumber = requiredNozzle.getNozzleReferenceNumber();
                            } catch (UnableToFindSectionNodeException ex)
                            {
                                throw new RuntimeException("Failed to determine correct nozzle - single material mode");
                            }
                        } else if ((postProcessingMode == PostProcessingMode.SUPPORT_IN_FIRST_MATERIAL
                                || postProcessingMode == PostProcessingMode.SUPPORT_IN_SECOND_MATERIAL)
                                && ((sectionUnderConsideration instanceof SupportSectionNode)
                                || (sectionUnderConsideration instanceof SupportInterfaceSectionNode)
                                || (sectionUnderConsideration instanceof SkirtSectionNode)))
                        {
                            requiredToolNumber = (postProcessingMode == PostProcessingMode.SUPPORT_IN_FIRST_MATERIAL) ? 1 : 0;
                        } else
                        {
                            requiredToolNumber = objectToNozzleNumberMap.get(objectReferenceNumber);
                        }
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
                        toolSelectNode.setToolNumber(objectToNozzleNumberMap.get(objectReferenceNumber));
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
