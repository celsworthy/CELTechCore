/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import java.util.HashSet;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class CopyModelsCommand extends Command
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CopyModelsCommand.class.getName());

    Project project;
    Set<ModelContainer> modelContainers;
    Set<ModelContainer> newModelContainers;

    public CopyModelsCommand(Project project, Set<ModelContainer> modelContainers)
    {
        this.project = project;
        this.modelContainers = modelContainers;
    }

    @Override
    public void do_()
    {
        newModelContainers = new HashSet<>();
        for (ModelContainer modelContainer : modelContainers)
        {
            ModelContainer newModel = modelContainer.makeCopy();
            newModelContainers.add(newModel);
        }
        redo();
    }

    @Override
    public void undo()
    {
        for (ModelContainer modelContainer : newModelContainers)
        {
            project.deleteModel(modelContainer);
        }
    }

    @Override
    public void redo()
    {
        for (ModelContainer modelContainer : newModelContainers)
        {
            project.addModel(modelContainer);
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
        throw new UnsupportedOperationException("Should never be called.");
    }

}
