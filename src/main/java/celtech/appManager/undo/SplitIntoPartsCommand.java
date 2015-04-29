/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelContainer.State;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tony
 */
public class SplitIntoPartsCommand extends Command
{

    Project project;
    Set<ModelContainer> modelContainers;
    Set<ModelContainer> newModelContainers;
    Map<ModelContainer, State> states;

    public SplitIntoPartsCommand(Project project, Set<ModelContainer> modelContainers)
    {
        this.project = project;
        this.modelContainers = modelContainers;
    }
    
    @Override
    public void do_()
    {
        states = new HashMap<>();
        for (ModelContainer modelContainer : modelContainers)
        {
            states.put(modelContainer, modelContainer.getState());
        }
        newModelContainers = project.splitIntoParts(modelContainers);
    }

    @Override
    public void undo()
    {
        project.deleteModels(newModelContainers);
        for (ModelContainer modelContainer : modelContainers)
        {
           project.addModel(modelContainer); 
        }
    }

    @Override
    public void redo()
    {
        project.deleteModels(modelContainers);
        for (ModelContainer modelContainer : newModelContainers)
        {
           project.addModel(modelContainer); 
           modelContainer.setState(states.get(modelContainer));
        }
    }

    @Override
    public boolean canMergeWith(Command command)
    {
        return false;
    }

    @Override
    public void merge(Command command)
    {
        throw new UnsupportedOperationException("Should never be called");
    }

}
