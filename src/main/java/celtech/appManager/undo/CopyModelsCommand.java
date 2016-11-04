/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.modelcontrol.TranslateableTwoD;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    Set<ProjectifiableThing> modelContainers;
    Set<ProjectifiableThing> newProjectifiableThings;

    public CopyModelsCommand(Project project, Set<ProjectifiableThing> modelContainers)
    {
        this.project = project;
        this.modelContainers = modelContainers;
    }

    @Override
    public void do_()
    {
        newProjectifiableThings = new HashSet<>();
        for (ProjectifiableThing modelContainer : modelContainers)
        {
            ProjectifiableThing newModel = modelContainer.makeCopy();
            newProjectifiableThings.add(newModel);
        }
        redo();
    }

    @Override
    public void undo()
    {
        project.removeModels(newProjectifiableThings);
    }

    @Override
    public void redo()
    {
        for (ProjectifiableThing modelContainer : newProjectifiableThings)
        {
            project.addModel(modelContainer);
        }
        List<ProjectifiableThing> thingsToLayout = new ArrayList<>(newProjectifiableThings);
        project.autoLayout(thingsToLayout);
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
