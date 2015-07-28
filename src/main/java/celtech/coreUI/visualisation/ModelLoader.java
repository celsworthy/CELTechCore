/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.configuration.PrintBed;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import celtech.coreUI.visualisation.modelDisplay.ModelBounds;
import celtech.modelcontrol.ModelContainer;
import celtech.modelcontrol.ModelGroup;
import celtech.services.modelLoader.ModelLoadResults;
import celtech.services.modelLoader.ModelLoaderService;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.concurrent.WorkerStateEvent;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * ModelLoader contains methods for loading models from a file.
 *
 * @author tony
 */
public class ModelLoader
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
        ModelLoader.class.getName());
    /*
     * Mesh Model loading
     */
    public static final ModelLoaderService modelLoaderService = new ModelLoaderService();

    private boolean offerShrinkAndAddToProject(Project project, boolean relayout)
    {
        ModelLoadResults loadResults = modelLoaderService.getValue();
        if (loadResults.getResults().isEmpty())
        {
            return true;
        }
        boolean projectIsEmpty = project.getTopLevelModels().isEmpty();
        Set<ModelContainer> allModelContainers = new HashSet<>();
        for (ModelLoadResult loadResult : loadResults.getResults())
        {
            if (loadResult != null)
            {
                allModelContainers.add(makeGroup(loadResult.getModelContainers()));
            } else
            {
                steno.error("Error whilst attempting to load model");
            }
        }
        addToProject(project, allModelContainers);
        if (relayout && projectIsEmpty && loadResults.getResults().size() > 1)
        {
//            project.autoLayout();
        }
        return false;
    }

    public ReadOnlyBooleanProperty modelLoadingProperty()
    {
        return modelLoaderService.runningProperty();
    }

    /**
     * Load each model in modelsToLoad, do not lay them out on the bed.
     */
    public void loadExternalModels(Project project, List<File> modelsToLoad)
    {
        loadExternalModels(project, modelsToLoad, false);
    }

    /**
     * Load each model in modelsToLoad and rwelayout if requested. If there are already models
     * loaded in the project then do not relayout even if relayout=true;
     */
    public void loadExternalModels(Project project, List<File> modelsToLoad, boolean relayout)
    {
        modelLoaderService.reset();
        modelLoaderService.setModelFilesToLoad(modelsToLoad, relayout);
        modelLoaderService.setOnSucceeded((WorkerStateEvent t) ->
        {
            offerShrinkAndAddToProject(project, relayout);
        });
        modelLoaderService.start();
    }

    /**
     * Add the given ModelContainers to the project. Some may be ModelGroups. If there is more than
     * one ModelContainer/Group then put them in one overarching group.
     */
    private void addToProject(Project project, Set<ModelContainer> modelContainers)
    {
        UndoableProject undoableProject = new UndoableProject(project);

        for (ModelContainer modelContainer : modelContainers)
        {
            modelContainer.applyDropToBedAtTopLevelOnly();
        }

        ModelContainer modelContainer;
        if (modelContainers.size() == 1)
        {
            modelContainer = modelContainers.iterator().next();
            undoableProject.addModel(modelContainer);
        } else
        {
            modelContainer = new ModelGroup(modelContainers);
//            modelContainer.applyMoveToCentreAtTopLevelOnly();
            undoableProject.addModel(modelContainer);
        }
        shrinkIfRequested(modelContainer);
    }

    private void shrinkIfRequested(ModelContainer modelContainer)
    {
        boolean shrinkModel = false;
        ModelBounds originalBounds = modelContainer.getOriginalModelBounds();
        boolean modelIsTooLarge = PrintBed.isBiggerThanPrintVolume(originalBounds);
        if (modelIsTooLarge)
        {
            shrinkModel = Lookup.getSystemNotificationHandler().
                showModelTooBigDialog(modelContainer.getModelName());
        }
        if (shrinkModel)
        {
            modelContainer.shrinkToFitBed();
        }
    }

    private ModelContainer makeGroup(Set<ModelContainer> modelContainers)
    {
        Set<ModelContainer> splitModelContainers = new HashSet<>();
        for (ModelContainer modelContainer : modelContainers)
        {
            splitModelContainers.add(modelContainer.splitIntoParts());
        }
        if (splitModelContainers.size() == 1)
        {
            return splitModelContainers.iterator().next();
        } else
        {
            return new ModelGroup(splitModelContainers);
        }
    }

}
