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

    private ObservableSet<ModelContainer> modelContainers;
    private PrimarySelectedModelDetails primarySelectedModelDetails;

    public SelectedModelContainers()
    {
        modelContainers = FXCollections.observableSet();
        primarySelectedModelDetails = new PrimarySelectedModelDetails();
    }

    public void addModelContainer(ModelContainer modelContainer)
    {
        modelContainers.add(modelContainer);
        primarySelectedModelDetails.bind(modelContainer);
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
        if (primarySelectedModelDetails != null) {
            primarySelectedModelDetails.updateSelectedProperties();
        }    
    }

    public class PrimarySelectedModelDetails
    {

        ModelContainer boundModelContainer;

        private final DoubleProperty width = new SimpleDoubleProperty();
        private final DoubleProperty centreX = new SimpleDoubleProperty();
        private final DoubleProperty centreZ = new SimpleDoubleProperty();
        private DoubleProperty height= new SimpleDoubleProperty();
        private DoubleProperty depth= new SimpleDoubleProperty();
        private DoubleProperty scale= new SimpleDoubleProperty();
        private DoubleProperty rotationY= new SimpleDoubleProperty();

        private PrimarySelectedModelDetails()
        {
        }

        public DoubleProperty getWidth()
        {
            return width;
        }

        public void bind(ModelContainer modelContainer)
        {
            boundModelContainer = modelContainer;
            updateSelectedProperties();
        }

        public DoubleProperty getCentreX()
        {
            return centreX;
        }

        private void updateSelectedProperties()
        {
            width.set(boundModelContainer.getTotalWidth());
            System.out.println("set centre x to " + boundModelContainer.getCentreX());
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
