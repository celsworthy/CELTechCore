/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.roboxbase.configuration.PrintBed;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class UngroupCommand extends Command
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            UngroupCommand.class.getName());

    Project project;
    Map<Integer, Set<ModelContainer>> groupIds;
    private Set<ModelContainer.State> originalStates;
    private Set<ModelContainer.State> newStates;
    private Set<ModelContainer> containersToRecentre = new HashSet<>();

    public UngroupCommand(Project project, Set<ModelContainer> modelContainers)
    {
        this.project = project;
        groupIds = new HashMap<>();
        for (ModelContainer modelContainer : modelContainers)
        {
            if (modelContainer instanceof ModelGroup)
            {
                containersToRecentre.addAll(modelContainer.getChildModelContainers());
                groupIds.put(modelContainer.getModelId(), ((ModelGroup) modelContainer).getChildModelContainers());
            }
        }
    }

    @Override
    public void do_()
    {
        redo();
    }

    @Override
    public void undo()
    {
        for (int groupId : groupIds.keySet())
        {
            project.group(groupIds.get(groupId), groupId);
        }
        project.setModelStates(originalStates);
    }

    @Override
    public void redo()
    {
        originalStates = project.getModelStates();
        try
        {
            try
            {
                project.ungroup(project.getModelContainersOfIds(groupIds.keySet()));
            } catch (Project.ProjectLoadException ex)
            {
                steno.exception("Could not ungroup", ex);
            }
            project.translateModelsTo(containersToRecentre, PrintBed.getPrintVolumeCentre().getX(), PrintBed.getPrintVolumeCentre().getZ());
            newStates = project.getModelStates();
        } catch (Exception ex)
        {
            steno.exception("Failed running command ", ex);
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
