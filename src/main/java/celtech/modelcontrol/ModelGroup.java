/*
 * Copyright 2015 CEL UK
 */
package celtech.modelcontrol;

import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;

/**
 * ModelGroup is a ModelContainer that is a group of child ModelContainers or other ModelGroups.
 *
 * @author tony
 */
public class ModelGroup extends ModelContainer
{

    private final Group modelContainersGroup = new Group();
    private Set<ModelContainer> childModelContainers;

    public ModelGroup(Set<ModelContainer> modelContainers)
    {
        initialise(null);
        childModelContainers = new HashSet<>();
        getChildren().add(modelContainersGroup);
        childModelContainers.addAll(modelContainers);
        modelContainersGroup.getChildren().addAll(modelContainers);
        initialiseTransforms();
        for (ModelContainer modelContainer : modelContainers)
        {
            modelContainer.clearBedTransform();
        }
        lastTransformedBoundsInParent = calculateBoundsInParentCoordinateSystem();
        /**
         * for a group the originalModelBounds changes whenever a child changes
         */
        updateOriginalModelBounds();
        checkOffBed();
    }

    public ModelGroup(Set<ModelContainer> modelContainers, int groupModelId)
    {
        this(modelContainers);
        modelId = groupModelId;
        if (modelId >= nextModelId)
        {
            // avoid any duplicate ids
            nextModelId = modelId + 1;
        }
    }

    /**
     * Return the immediate children of this group.
     */
    @Override
    public Set<ModelContainer> getChildModelContainers()
    {
        return Collections.unmodifiableSet(childModelContainers);
    }

    @Override
    public Set<ModelContainer> getDescendentModelContainers()
    {
        Set<ModelContainer> modelContainers = new HashSet<>(childModelContainers);
        for (ModelContainer modelContainer : childModelContainers)
        {
            modelContainers.addAll(modelContainer.getDescendentModelContainers());
        }

        return modelContainers;
    }

    public void removeModel(ModelContainer modelContainer)
    {
        childModelContainers.remove(modelContainer);
    }

    @Override
    protected void initialiseTransforms()
    {
        super.initialiseTransforms();
        modelContainersGroup.getTransforms().addAll(transformScalePreferred);
    }

    /**
     * Return a set of all descendent ModelContainers that have MeshView children.
     */
    @Override
    public Set<ModelContainer> getModelsHoldingMeshViews()
    {
        Set<ModelContainer> modelsHoldingMeshViews = new HashSet<>();
        for (Node modelNode : modelContainersGroup.getChildren())
        {
            ModelContainer modelContainer = (ModelContainer) modelNode;
            modelsHoldingMeshViews.addAll(modelContainer.getModelsHoldingMeshViews());
        }
        return modelsHoldingMeshViews;
    }

    /**
     * Return a set of all descendent ModelContainers (and include this one) that have
     * ModelContainer children.
     */
    @Override
    public Collection<? extends ModelContainer> getModelsHoldingModels()
    {
        Set<ModelContainer> modelsHoldingModels = new HashSet<>();
        if (modelContainersGroup.getChildren().size() > 0)
        {
            modelsHoldingModels.add(this);
        }
        for (Node modelNode : modelContainersGroup.getChildren())
        {
            ModelContainer modelContainer = (ModelContainer) modelNode;
            modelsHoldingModels.addAll(modelContainer.getModelsHoldingModels());
        }
        return modelsHoldingModels;
    }

    /**
     * Update the given group structure with the details of this group.
     */
    @Override
    public void addGroupStructure(Map<Integer, Set<Integer>> groupStructure)
    {
        for (Node modelNode : modelContainersGroup.getChildren())
        {
            ModelContainer modelContainer = (ModelContainer) modelNode;
            if (groupStructure.get(modelId) == null)
            {
                groupStructure.put(modelId, new HashSet<>());
            }
            groupStructure.get(modelId).add(modelContainer.modelId);
        }
    }

    /**
     * Calculate max/min X,Y,Z before the transforms have been applied (ie the original model
     * dimensions before any transforms).
     */
    @Override
    ModelBounds calculateBoundsInLocal()
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (ModelContainer modelContainer : childModelContainers)
        {
            ModelBounds bounds = modelContainer.lastTransformedBoundsInParent; // parent of child is this model
            minX = Math.min(bounds.getMinX(), minX);
            minY = Math.min(bounds.getMinY(), minY);
            minZ = Math.min(bounds.getMinZ(), minZ);

            maxX = Math.max(bounds.getMaxX(), maxX);
            maxY = Math.max(bounds.getMaxY(), maxY);
            maxZ = Math.max(bounds.getMaxZ(), maxZ);
        }

        double newwidth = maxX - minX;
        double newdepth = maxZ - minZ;
        double newheight = maxY - minY;

        double newcentreX = minX + (newwidth / 2);
        double newcentreY = minY + (newheight / 2);
        double newcentreZ = minZ + (newdepth / 2);

        return new ModelBounds(minX, maxX, minY, maxY, minZ, maxZ, newwidth,
                               newheight, newdepth, newcentreX, newcentreY,
                               newcentreZ);
    }

    @Override
    public Set<MeshView> descendentMeshViews()
    {
        Set<MeshView> descendentMeshViews = new HashSet<>();

        for (ModelContainer modelContainer : childModelContainers)
        {
            descendentMeshViews.addAll(modelContainer.descendentMeshViews());
        }

        return descendentMeshViews;
    }

    /**
     * If this model is associated with the given extruder number then recolour it to the given
     * colour, also taking into account if it is misplaced (off the bed). Also call the same method
     * on any child ModelContainers.
     */
    @Override
    public void updateColour(final Color displayColourExtruder0, final Color displayColourExtruder1,
        boolean showMisplacedColour)
    {
        for (ModelContainer modelContainer : childModelContainers)
        {
            modelContainer.updateColour(displayColourExtruder0, displayColourExtruder1,
                                        showMisplacedColour);
        }
    }

    @Override
    public void setUseExtruder0(boolean useExtruder0)
    {
        for (ModelContainer modelContainer : childModelContainers)
        {
            modelContainer.setUseExtruder0(useExtruder0);
        }
    }

    @Override
    public ModelContainer makeCopy()
    {
        Set<ModelContainer> childModels = new HashSet<>();
        for (ModelContainer childModel : childModelContainers)
        {
            ModelContainer modelContainerCopy = childModel.makeCopy();
            modelContainerCopy.setState(childModel.getState());
            childModels.add(modelContainerCopy);
        }
        ModelGroup copy = new ModelGroup(childModels);
        copy.setXScale(this.getXScale());
        copy.setYScale(this.getYScale());
        copy.setZScale(this.getZScale());
        copy.setRotationLean(this.getRotationLean());
        copy.setRotationTwist(this.getRotationTwist());
        copy.setRotationTurn(this.getRotationTurn());
        return copy;
    }

}
