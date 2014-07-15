/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.visualisation;

import celtech.modelcontrol.ModelContainer;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 * The SelectionModel captures all required state about the currently selected objects.
 *
 * @author tony
 */
public class SelectedModelContainers
{

    private final ObservableSet<ModelContainer> modelContainers;
    private final PrimarySelectedModelDetails primarySelectedModelDetails;
    private final IntegerProperty numModelsSelected = new SimpleIntegerProperty(0);
    private Set<SelectedModelContainersListener> selectedModelContainersListeners;

    public SelectedModelContainers()
    {
        modelContainers = FXCollections.observableSet();
        primarySelectedModelDetails = new PrimarySelectedModelDetails();
        selectedModelContainersListeners = new HashSet<>();
    }

    public synchronized void addModelContainer(ModelContainer modelContainer)
    {
        modelContainers.add(modelContainer);
        modelContainer.setSelected(true);
        primarySelectedModelDetails.setTo(modelContainer);
        for (SelectedModelContainersListener selectedModelContainersListener : selectedModelContainersListeners)
        {
            selectedModelContainersListener.whenAdded(modelContainer);
        }
        numModelsSelected.set(numModelsSelected.get() + 1);
    }

    public synchronized void removeModelContainer(ModelContainer modelContainer)
    {
        modelContainers.remove(modelContainer);
        modelContainer.setSelected(false);
        for (SelectedModelContainersListener selectedModelContainersListener : selectedModelContainersListeners)
        {
            selectedModelContainersListener.whenRemoved(modelContainer);
        }    
        numModelsSelected.set(numModelsSelected.get() - 1);
    }

    public boolean isSelected(ModelContainer modelContainer)
    {
        return modelContainers.contains(modelContainer);
    }
    
    public synchronized void deselectAllModels() {
        Set<ModelContainer> allSelectedModelContainers = new HashSet<>(modelContainers);
        for (ModelContainer modelContainer : allSelectedModelContainers)
        {
            removeModelContainer(modelContainer);
        }
    }

    public ReadOnlyIntegerProperty getNumModelsSelectedProperty() {
        return numModelsSelected;
    }

    public PrimarySelectedModelDetails getPrimarySelectedModelDetails()
    {
        return primarySelectedModelDetails;
    }
    
    /**
     * Call this method when the transformed geometry of the selected model have changed.
     */
    public void updateSelectedValues() {
        primarySelectedModelDetails.updateSelectedProperties();
    }
    
    public void addListener(SelectedModelContainersListener selectedModelContainersListener) {
        selectedModelContainersListeners.add(selectedModelContainersListener);
    }
        
    
    public interface SelectedModelContainersListener {
        
        public void whenAdded(ModelContainer modelContainer);
        
        public void whenRemoved(ModelContainer modelContainer);
    }

    public class PrimarySelectedModelDetails
    {

        ModelContainer boundModelContainer;

        // initing values to -1 forces a change update when value first set to 0 (e.g. rotY)
        private final DoubleProperty width = new SimpleDoubleProperty(-1);
        private final DoubleProperty centreX = new SimpleDoubleProperty(-1);
        private final DoubleProperty centreZ = new SimpleDoubleProperty(-1);
        private final DoubleProperty height = new SimpleDoubleProperty(-1);
        private final DoubleProperty depth = new SimpleDoubleProperty(-1);
        private final DoubleProperty scale = new SimpleDoubleProperty(-1);
        private final DoubleProperty rotationY = new SimpleDoubleProperty(-1);

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
            width.set(boundModelContainer.getScaledWidth());
            centreX.set(boundModelContainer.getTransformedCentreX());
            centreZ.set(boundModelContainer.getTransformedCentreZ());
            height.set(boundModelContainer.getScaledHeight());
            depth.set(boundModelContainer.getScaledDepth());
            scale.set(boundModelContainer.getScale());
            rotationY.set(boundModelContainer.getRotationY());
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

        public DoubleProperty getScale()
        {
            return scale;
        }

        public DoubleProperty getRotationY()
        {
            return rotationY;
        }
    }

}
