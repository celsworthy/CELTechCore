/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.visualisation;

import celtech.modelcontrol.ModelContainer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
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

    public SelectedModelContainers()
    {
        modelContainers = FXCollections.observableSet();
        primarySelectedModelDetails = new PrimarySelectedModelDetails();
    }

    public void addModelContainer(ModelContainer modelContainer)
    {
        modelContainers.add(modelContainer);
        primarySelectedModelDetails.setTo(modelContainer);
    }

    public void removeModelContainer(ModelContainer modelContainer)
    {
        modelContainers.remove(modelContainer);
    }

    public boolean isSelected(ModelContainer modelContainer)
    {
        return modelContainers.contains(modelContainer);
    }

    public ObservableSet<ModelContainer> getModelContainersProperty()
    {
        return modelContainers;
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

    public class PrimarySelectedModelDetails
    {

        ModelContainer boundModelContainer;

        private final DoubleProperty width = new SimpleDoubleProperty();
        private final DoubleProperty centreX = new SimpleDoubleProperty();
        private final DoubleProperty centreZ = new SimpleDoubleProperty();
        private final DoubleProperty height = new SimpleDoubleProperty();
        private final DoubleProperty depth = new SimpleDoubleProperty();
        private final DoubleProperty scale = new SimpleDoubleProperty();
        private final DoubleProperty rotationY = new SimpleDoubleProperty();

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
            width.set(boundModelContainer.getTotalWidth());
            centreX.set(boundModelContainer.getCentreX());
            centreZ.set(boundModelContainer.getCentreZ());
            height.set(boundModelContainer.getHeight());
            depth.set(boundModelContainer.getDepth());
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
