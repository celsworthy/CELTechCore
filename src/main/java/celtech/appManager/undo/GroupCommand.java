/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.ModelContainerProject;
import celtech.modelcontrol.ModelContainer;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tony
 */
public class GroupCommand extends Command
{

    ModelContainerProject project;
    Set<ModelContainer> modelContainers;
    private Set<ModelContainer.State> states;
    ModelContainer group;

    public GroupCommand(ModelContainerProject project, Set<ModelContainer> modelContainers)
    {
        states = new HashSet<>();
        this.project = project;
        this.modelContainers = modelContainers;
    }

    @Override
    public void do_()
    {
        for (ModelContainer modelContainer : modelContainers)
        {
            states.add(modelContainer.getState());
        }
        doGroup();
    }

    @Override
    public void undo()
    {
        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(group);
        project.ungroup(modelContainers);
        project.setModelStates(states);
    }

    @Override
    public void redo()
    {
        doGroup();
    }
    
    private void doGroup() {
        if (modelContainers.size() == 1)
        {
            return;
        }
        group = project.group(modelContainers);
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
