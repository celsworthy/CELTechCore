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
import celtech.gcodetranslator.postprocessing.nodes.ToolSelectNode;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.modelcontrol.ModelContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.collections.ObservableList;

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
    private final List<Integer> objectToNozzleNumberMap;
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

        nozzleControlUtilities = new NozzleManagementUtilities(nozzleProxies, slicerParametersFile, headFile);

        extruderToNozzleMap = new HashMap<>();
        nozzleToExtruderMap = new HashMap<>();
        for (int extruderNumber = 0; extruderNumber < 2; extruderNumber++)
        {
            Optional<NozzleProxy> proxy = nozzleControlUtilities.chooseNozzleProxyByExtruderNumber(extruderNumber);
            if (proxy.isPresent())
            {
                extruderToNozzleMap.put(extruderNumber, proxy.get().getNozzleReferenceNumber());
                nozzleToExtruderMap.put(proxy.get().getNozzleReferenceNumber(), extruderNumber);
            }
        }

        objectToNozzleNumberMap = new ArrayList<>();
        for (ModelContainer model : project.getLoadedModels())
        {
            for (int extruderNumber : model.getMeshExtruderAssociationProperty())
            {
                objectToNozzleNumberMap.add(extruderToNozzleMap.get(extruderNumber));
            }
        }
//        project.getLoadedModels().stream()
//                .map(ModelContainer::getMeshExtruderAssociationProperty)
//                .map(ObservableList<Integer>::get)
//                .forEach(extruderNumber ->
//                        {
//                            objectToNozzleNumberMap.add(extruderToNozzleMap.get(extruderNumber));
//                });
    }

    protected
            void assignExtrusionToCorrectExtruder(LayerNode layerNode)
    {
        // Don't change anything if we're in task-based selection as this always uses extruder E
        if (postProcessingMode != PostProcessingMode.TASK_BASED_NOZZLE_SELECTION)
        {
            layerNode.stream()
                    .filter(node -> node instanceof ToolSelectNode)
                    .map(ToolSelectNode.class::cast)
                    .forEach(toolSelectNode ->
                            {
                                switch (nozzleToExtruderMap.get(toolSelectNode.getToolNumber()))
                                {
                                    case 0:
                                        toolSelectNode.stream()
                                        .filter(node -> node instanceof ExtrusionProvider)
                                        .map(ExtrusionProvider.class::cast)
                                        .map(ExtrusionProvider::getExtrusion)
                                        .forEach(extrusionNode ->
                                                {
                                                    extrusionNode.extrudeUsingEOnly();
                                        });
                                        break;
                                    case 1:
                                        toolSelectNode.stream()
                                        .filter(node -> node instanceof ExtrusionProvider)
                                        .map(ExtrusionProvider.class::cast)
                                        .map(ExtrusionProvider::getExtrusion)
                                        .forEach(extrusionNode ->
                                                {
                                                    extrusionNode.extrudeUsingDOnly();
                                        });
                                        break;
                                }
                    });
        }
    }

    /**
     *
     * @param layerNode
     * @return The reference number of the last object in the layer
     */
    protected int insertNozzleControlSectionsByTask(LayerNode layerNode)
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

            // Find all of the objects in this layer
            List<ObjectDelineationNode> objectNodes = layerNode.stream()
                    .filter(node -> node instanceof ObjectDelineationNode)
                    .map(ObjectDelineationNode.class::cast)
                    .collect(Collectors.toList());

            if (objectNodes.size()
                    > 0)
            {
                lastObjectReferenceNumber = objectNodes.get(objectNodes.size() - 1).getObjectNumber();
            }

            SectionNode lastSectionNode = null;

            for (ObjectDelineationNode objectNode : objectNodes)
            {
                List<GCodeEventNode> childNodes = objectNode.getChildren().stream().collect(Collectors.toList());

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
                                    throw new RuntimeException("Failed to process orphan section on layer " + layerNode.getLayerNumber() + " as last section didn't exist");
                                }

                                requiredNozzle = nozzleControlUtilities.chooseNozzleProxyByTask(lastSectionNode);
                                try
                                {
                                    SectionNode replacementSection = lastSectionNode.getClass().newInstance();

                                    // Move the child nodes to the replacement section
                                    List<GCodeEventNode> sectionChildren = sectionNodeBeingExamined.stream().collect(Collectors.toList());
                                    for (GCodeEventNode child : sectionChildren)
                                    {
                                        child.removeFromParent();
                                        replacementSection.addChildAtEnd(child);
                                    }

                                    sectionNodeBeingExamined.removeFromParent();
                                    lastSectionNode.addSiblingAfter(replacementSection);
                                    sectionNodeBeingExamined = replacementSection;
                                } catch (InstantiationException | IllegalAccessException ex)
                                {
                                    throw new RuntimeException("Failed to process orphan section on layer " + layerNode.getLayerNumber(), ex);
                                }
                            } else
                            {
                                requiredNozzle = nozzleControlUtilities.chooseNozzleProxyByTask(sectionNodeBeingExamined);
                            }

                            if (toolSelectNode == null
                                    || toolSelectNode.getToolNumber() != requiredNozzle.getNozzleReferenceNumber())
                            {
                                //Need to create a new Tool Select Node
                                toolSelectNode = new ToolSelectNode();
                                toolSelectNode.setToolNumber(requiredNozzle.getNozzleReferenceNumber());
                                layerNode.addChildAtEnd(toolSelectNode);
                            }

                            sectionNodeBeingExamined.removeFromParent();
                            toolSelectNode.addChildAtEnd(sectionNodeBeingExamined);

                        } catch (UnableToFindSectionNodeException ex)
                        {
                            throw new RuntimeException("Error attempting to insert nozzle control sections by task - " + ex.getMessage(), ex);
                        }

                        lastSectionNode = sectionNodeBeingExamined;
                    } else
                    {
                        //Probably a travel node - move it over without changing it
                        childNode.removeFromParent();
                        toolSelectNode.addChildAtEnd(childNode);
                    }
                }

                if (!objectNode.getChildren().isEmpty())
                {
                    throw new RuntimeException("Transfer of children from object " + objectNode.getObjectNumber() + " failed");
                }
                objectNode.removeFromParent();
            }
        }

        return lastObjectReferenceNumber;
    }

    protected int insertNozzleControlSectionsByObject(LayerNode layerNode)
    {
        int lastObjectReferenceNumber = -1;

        // Find all of the objects in this layer
        List<ObjectDelineationNode> objectNodes = layerNode.stream()
                .filter(node -> node instanceof ObjectDelineationNode)
                .map(ObjectDelineationNode.class::cast)
                .collect(Collectors.toList());

        if (objectNodes.size()
                > 0)
        {
            lastObjectReferenceNumber = objectNodes.get(objectNodes.size() - 1).getObjectNumber();
        }

        //We'll need at least one of these per layer
        ToolSelectNode toolSelectNode = null;

        SectionNode lastSectionNode = null;

        for (GCodeEventNode objectNode : objectNodes)
        {
            ObjectDelineationNode objectNodeBeingExamined = (ObjectDelineationNode) objectNode;

            NozzleProxy requiredNozzle = nozzleProxies.get(objectToNozzleNumberMap.get(objectNodeBeingExamined.getObjectNumber()));

            if (toolSelectNode == null
                    || toolSelectNode.getToolNumber() != requiredNozzle.getNozzleReferenceNumber())
            {
                //Need to create a new Tool Select Node
                toolSelectNode = new ToolSelectNode();
                toolSelectNode.setToolNumber(requiredNozzle.getNozzleReferenceNumber());
                layerNode.addChildAtEnd(toolSelectNode);
            }

            objectNodeBeingExamined.removeFromParent();
            List<GCodeEventNode> sectionNodes = objectNodeBeingExamined.getChildren().stream().collect(Collectors.toList());

            for (GCodeEventNode childNode : sectionNodes)
            {
                if (childNode instanceof SectionNode)
                {
                    SectionNode sectionNodeUnderExamination = (SectionNode) childNode;

                    if (sectionNodeUnderExamination instanceof OrphanSectionNode)
                    {
                        if (lastSectionNode == null)
                        {
                            throw new RuntimeException("Failed to process orphan section on layer " + layerNode.getLayerNumber() + " as last section didn't exist");
                        }

                        try
                        {
                            requiredNozzle = nozzleControlUtilities.chooseNozzleProxyByTask(lastSectionNode);

                            SectionNode replacementSection = lastSectionNode.getClass().newInstance();

                            // Move the child nodes to the replacement section
                            List<GCodeEventNode> sectionChildren = sectionNodes.stream().collect(Collectors.toList());
                            for (GCodeEventNode child : sectionChildren)
                            {
                                child.removeFromParent();
                                replacementSection.addChildAtEnd(child);
                            }

                            sectionNodeUnderExamination.removeFromParent();
                            lastSectionNode.addSiblingAfter(replacementSection);
                            sectionNodeUnderExamination = replacementSection;
                        } catch (InstantiationException | IllegalAccessException | UnableToFindSectionNodeException ex)
                        {
                            throw new RuntimeException("Failed to process orphan section on layer " + layerNode.getLayerNumber(), ex);
                        }
                    }

                    sectionNodeUnderExamination.removeFromParent();
                    toolSelectNode.addChildAtEnd(sectionNodeUnderExamination);

                    lastSectionNode = sectionNodeUnderExamination;
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
}
