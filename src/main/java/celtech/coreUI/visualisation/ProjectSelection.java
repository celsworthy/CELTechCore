/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.visualisation;

import celtech.appManager.Project;
import celtech.appManager.Project.ProjectChangesListener;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 * ProjectSelection captures all required state about the currently selected ModelContainers.
 *
 * @author tony
 */
public class ProjectSelection implements ProjectChangesListener
{

    private final ObservableSet<ModelContainer> modelContainers;
    private final PrimarySelectedModelDetails primarySelectedModelDetails;
    private final IntegerProperty numModelsSelected = new SimpleIntegerProperty(0);
    private final IntegerProperty numGroupsSelected = new SimpleIntegerProperty(0);
    private final Set<SelectedModelContainersListener> selectedModelContainersListeners;
    /**
     * If any of the current selection are a child of a group then value is true.
     */
    private final BooleanBinding selectionHasChildOfGroup;

    public ProjectSelection(Project project)
    {
        modelContainers = FXCollections.observableSet();
        primarySelectedModelDetails = new PrimarySelectedModelDetails();
        selectedModelContainersListeners = new HashSet<>();
        project.addProjectChangesListener(this);

        selectionHasChildOfGroup = new BooleanBinding()
        {
            {
                super.bind(modelContainers);
            }

            @Override
            protected boolean computeValue()
            {
                for (ModelContainer modelContainer : modelContainers)
                {
                    if (modelContainer.getParentModelContainer() != null)
                    {
                        return true;
                    }
                }
                return false;
            }

        };
    }

    public BooleanBinding getSelectionHasChildOfGroup()
    {
        return selectionHasChildOfGroup;
    }

    /**
     * Add the given modelContainer to the set of selected ModelContainers.
     */
    public void addModelContainer(ModelContainer modelContainer)
    {
        if (!modelContainers.contains(modelContainer))
        {
            modelContainers.add(modelContainer);
            modelContainer.setSelected(true);
            primarySelectedModelDetails.setTo(modelContainer);
            numModelsSelected.set(numModelsSelected.get() + 1);
            if (modelContainer instanceof ModelGroup)
            {
                numGroupsSelected.set(numGroupsSelected.get() + 1);
            }
            for (SelectedModelContainersListener selectedModelContainersListener : selectedModelContainersListeners)
            {
                selectedModelContainersListener.whenAdded(modelContainer);
            }
        }
    }

    /**
     * Remove the given modelContainer from the set of selected ModelContainers.
     */
    public void removeModelContainer(ModelContainer modelContainer)
    {
        if (modelContainers.contains(modelContainer))
        {
            modelContainers.remove(modelContainer);
            numModelsSelected.set(numModelsSelected.get() - 1);
            if (modelContainer instanceof ModelGroup)
            {
                numGroupsSelected.set(numGroupsSelected.get() - 1);
            }
            modelContainer.setSelected(false);
            for (SelectedModelContainersListener selectedModelContainersListener : selectedModelContainersListeners)
            {
                selectedModelContainersListener.whenRemoved(modelContainer);
            }
        }

    }

    /**
     * Return if the given ModelContainer is selected or not.
     *
     * @param modelContainer
     * @return
     */
    public boolean isSelected(ModelContainer modelContainer)
    {
        return modelContainers.contains(modelContainer);
    }

    /**
     * Deselect all ModelContainers in the set of ModelContainers.
     */
    public void deselectAllModels()
    {
        Set<ModelContainer> allSelectedModelContainers = new HashSet<>(modelContainers);
        for (ModelContainer modelContainer : allSelectedModelContainers)
        {
            removeModelContainer(modelContainer);
        }
    }

    /**
     * Return a copy of the set of selected models.
     */
    public Set<ModelContainer> getSelectedModelsSnapshot()
    {
        return new HashSet<>(modelContainers);
    }

    /**
     * Return the number of selected ModelContainers as an observable number.
     */
    public ReadOnlyIntegerProperty getNumModelsSelectedProperty()
    {
        return numModelsSelected;
    }

    /**
     * Return the number of selected ModelGroups as an observable number.
     */
    public ReadOnlyIntegerProperty getNumGroupsSelectedProperty()
    {
        return numGroupsSelected;
    }

    /**
     * Return the details of the primary selected ModelContainer.
     */
    public PrimarySelectedModelDetails getPrimarySelectedModelDetails()
    {
        return primarySelectedModelDetails;
    }

    /**
     * Call this method when the transformed geometry of the selected model have changed.
     */
    public void updateSelectedValues()
    {
        primarySelectedModelDetails.updateSelectedProperties();
    }

    @Override
    public void whenModelAdded(ModelContainer modelContainer)
    {
    }

    @Override
    public void whenModelsRemoved(Set<ModelContainer> modelContainers)
    {
        for (ModelContainer modelContainer : modelContainers)
        {
            removeModelContainer(modelContainer);
        }
    }

    @Override
    public void whenAutoLaidOut()
    {
    }

    @Override
    public void whenModelsTransformed(Set<ModelContainer> modelContainers)
    {
        updateSelectedValues();
    }

    @Override
    public void whenModelChanged(ModelContainer modelContainer, String propertyName)
    {
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettings printerSettings)
    {
    }

    /**
     * Add a listener that will be notified whenever a ModelContainer is selected or deselected.
     */
    public void addListener(SelectedModelContainersListener selectedModelContainersListener)
    {
        selectedModelContainersListeners.add(selectedModelContainersListener);
    }

    /**
     * Remove a listener that will be notified whenever a ModelContainer is selected or deselected.
     */
    public void removeListener(SelectedModelContainersListener selectedModelContainersListener)
    {
        selectedModelContainersListeners.remove(selectedModelContainersListener);
    }

    public interface SelectedModelContainersListener
    {

        /**
         * Called when a ModelContainer is selected.
         */
        public void whenAdded(ModelContainer modelContainer);

        /**
         * Called when a ModelContainer is removed.
         */
        public void whenRemoved(ModelContainer modelContainer);
    }

    /**
     * PrimarySelectedModelDetails contains the details pertaining to the primary selected
     * ModelContainer.
     */
    public class PrimarySelectedModelDetails
    {

        ModelContainer boundModelContainer;

        // initing values to -1 forces a change update when value first set to 0 (e.g. rotY)
        private final DoubleProperty width = new SimpleDoubleProperty(-1);
        private final DoubleProperty centreX = new SimpleDoubleProperty(-1);
        private final DoubleProperty centreZ = new SimpleDoubleProperty(-1);
        private final DoubleProperty height = new SimpleDoubleProperty(-1);
        private final DoubleProperty depth = new SimpleDoubleProperty(-1);
        private final DoubleProperty scaleX = new SimpleDoubleProperty(-1);
        private final DoubleProperty scaleY = new SimpleDoubleProperty(-1);
        private final DoubleProperty scaleZ = new SimpleDoubleProperty(-1);
        private final DoubleProperty rotationLean = new SimpleDoubleProperty(-1);
        private final DoubleProperty rotationTwist = new SimpleDoubleProperty(-1);
        private final DoubleProperty rotationTurn = new SimpleDoubleProperty(-1);

        private PrimarySelectedModelDetails()
        {
        }

        public DoubleProperty getWidth()
        {
            return width;
        }

        public void setTo(ModelContainer modelContainer)
        {
            boundModelContainer = modelContainer;
            updateSelectedProperties();
        }

        private void updateSelectedProperties()
        {
            if (boundModelContainer != null)
            {
                width.set(boundModelContainer.getScaledWidth());
                centreX.set(boundModelContainer.getTransformedCentreX());
                centreZ.set(boundModelContainer.getTransformedCentreZ());
                height.set(boundModelContainer.getScaledHeight());
                depth.set(boundModelContainer.getScaledDepth());
                scaleX.set(boundModelContainer.getXScale());
                scaleY.set(boundModelContainer.getYScale());
                scaleZ.set(boundModelContainer.getZScale());
                rotationLean.set(boundModelContainer.getRotationLean());
                rotationTwist.set(boundModelContainer.getRotationTwist());
                rotationTurn.set(boundModelContainer.getRotationTurn());
            }
        }

        public DoubleProperty getCentreZ()
        {
            return centreZ;
        }

        public DoubleProperty getCentreX()
        {
            return centreX;
        }

        public DoubleProperty getHeight()
        {
            return height;
        }

        public DoubleProperty getDepth()
        {
            return depth;
        }

        public DoubleProperty getScaleX()
        {
            return scaleX;
        }

        public DoubleProperty getScaleY()
        {
            return scaleY;
        }

        public DoubleProperty getScaleZ()
        {
            return scaleZ;
        }

        public DoubleProperty getRotationLean()
        {
            return rotationLean;
        }

        public DoubleProperty getRotationTwist()
        {
            return rotationTwist;
        }

        public DoubleProperty getRotationTurn()
        {
            return rotationTurn;
        }
    }

}
