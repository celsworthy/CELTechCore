/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class DeleteModelsCommand extends Command
{    
    private final Stenographer steno = StenographerFactory.getStenographer(DeleteModelsCommand.class.getName());

    Project project;
    Set<ModelContainer> modelContainers;
    
    public DeleteModelsCommand(Project project, Set<ModelContainer> modelContainers)
    {
        this.project = project;
        this.modelContainers = modelContainers;
    }

    @Override
    public void do_()
    {
        project.deleteModels(modelContainers);
    }

    @Override
    public void undo()
    {
        System.out.println("Undo delete model, readd " + modelContainers);
        for (ModelContainer modelContainer : modelContainers)
        {
            project.addModel(modelContainer);
        }
        
    }

    @Override
    public void redo()
    {
        project.deleteModels(modelContainers);
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
