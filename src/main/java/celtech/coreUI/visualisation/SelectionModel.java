/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.visualisation;

import celtech.modelcontrol.ModelContainer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

/**
 * The SelectionModel captures all required state about the currently selected objects.
 *
 * @author tony
 */
public class SelectionModel
{
    
    private ObservableSet<ModelContainer> modelContainers;

    public SelectionModel()
    {
         modelContainers = FXCollections.observableSet();
    }

    public void addModelContainer(ModelContainer modelContainer)
    {
        modelContainers.add(modelContainer);
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

}
