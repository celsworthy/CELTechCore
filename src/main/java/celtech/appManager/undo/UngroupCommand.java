/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.modelcontrol.Groupable;
import celtech.modelcontrol.ItemState;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.modelcontrol.ProjectifiableThing;
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
    Map<Integer, Set<Groupable>> groupIds;
    private Set<ItemState> originalStates;
    private Set<ItemState> newStates;
    private Set<ProjectifiableThing> containersToRecentre = new HashSet<>();

    public UngroupCommand(Project project, Set<ProjectifiableThing> modelContainers)
    {
        this.project = project;
        groupIds = new HashMap<>();
        for (ProjectifiableThing modelContainer : modelContainers)
        {
            if (modelContainer instanceof ModelGroup)
            {
                containersToRecentre.addAll(((ModelContainer)modelContainer).getChildModelContainers());
                groupIds.put(modelContainer.getModelId(), (Set) ((ModelGroup) modelContainer).getChildModelContainers());
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
                project.ungroup(((ModelContainerProject)project).getModelContainersOfIds(groupIds.keySet()));
            } catch (ModelContainerProject.ProjectLoadException ex)
            {
                steno.exception("Could not ungroup", ex);
            }
//            Set<TranslateableTwoD> recentreThese = (Set) containersToRecentre;
//            Printer selectedPrinter = Lookup.getSelectedPrinterProperty().get();
//            if (selectedPrinter != null
//                    && selectedPrinter.printerConfigurationProperty().get() != null)
//            {
//                project.translateModelsTo(recentreThese, Lookup.getSelectedPrinterProperty().get().printerConfigurationProperty().get().getPrintVolumeWidth(),
//                        Lookup.getSelectedPrinterProperty().get().printerConfigurationProperty().get().getPrintVolumeDepth());
//            } else
//            {
//                PrinterDefinitionFile defaultPrinterConfiguration = PrinterContainer.getPrinterByID(PrinterContainer.defaultPrinterID);
//                project.translateModelsTo(recentreThese, defaultPrinterConfiguration.getPrintVolumeWidth(),
//                        defaultPrinterConfiguration.getPrintVolumeDepth());
//            }
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
