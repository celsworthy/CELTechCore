/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tony
 */
public class GroupCommand extends Command
{

    Project project;
    Set<ModelContainer> modelContainers;
    ModelContainer group;

    public GroupCommand(Project project, Set<ModelContainer> modelContainers)
    {
        this.project = project;
        this.modelContainers = modelContainers;
    }

    @Override
    public void do_()
    {
        redo();
    }

    @Override
    public void undo()
    {
        Set<ModelContainer> modelContainers = new HashSet<>();
        modelContainers.add(group);
        project.ungroup(modelContainers);
    }

    @Override
    public void redo()
    {
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
