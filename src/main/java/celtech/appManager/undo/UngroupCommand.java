/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import java.util.Set;

/**
 *
 * @author tony
 */
public class UngroupCommand extends Command
{

    Project project;
    Set<ModelContainer> modelContainers;
    ModelContainer group;

    public UngroupCommand(Project project, Set<ModelContainer> modelContainers)
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
    }

    @Override
    public void redo()
    {
        project.ungroup(modelContainers);
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
