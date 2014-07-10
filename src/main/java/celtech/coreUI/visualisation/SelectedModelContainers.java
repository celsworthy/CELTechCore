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

        private DoubleProperty width = new SimpleDoubleProperty();
        private DoubleProperty centreX = new SimpleDoubleProperty();

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
        }
    }

}
