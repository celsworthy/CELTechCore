/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.visualisation;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.appManager.undo.UndoableProject;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
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

    private boolean undoableMode = false;

    public ModelLoader()
    {
        modelLoaderService.setOnSucceeded((WorkerStateEvent t) ->
        {
            whenModelLoadSucceeded();
        });
    }

    private void whenModelLoadSucceeded()
    {
        offerShrinkAndAddToProject();
    }

    private boolean offerShrinkAndAddToProject()
    {
        ModelLoadResults loadResults = modelLoaderService.getValue();
        if (loadResults.getResults().isEmpty())
        {
            return true;
        }
        ModelLoadResult firstResult = loadResults.getResults().get(0);
        boolean projectIsEmpty = firstResult.getTargetProject().getTopLevelModels().isEmpty();
        for (ModelLoadResult loadResult : loadResults.getResults())
        {
            if (loadResult != null)
            {
                boolean shrinkModel = false;
                if (loadResult.isModelTooLarge())
                {
                    shrinkModel = Lookup.getSystemNotificationHandler().
                        showModelTooBigDialog(loadResult.getModelFilename());
                }
                Set<ModelContainer> modelContainers = loadResult.getModelContainers();
                if (shrinkModel)
                {
                    for (ModelContainer modelContainer : modelContainers)
                    {
                        modelContainer.shrinkToFitBed();
                    }

                }
                addToProject(loadResult, modelContainers);
            } else
            {
                steno.error("Error whilst attempting to load model");
            }
        }
        if (loadResults.isRelayout() && projectIsEmpty && loadResults.getResults().size() > 1)
        {
            Project project = loadResults.getResults().get(0).getTargetProject();
            project.autoLayout();
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
     * Load each model in modelsToLoad, do not lay them out on the bed. If there are already models
     * loaded in the project then do not relayout even if relayout=true;
     */
    public void loadExternalModels(Project project, List<File> modelsToLoad, boolean relayout)
    {
        modelLoaderService.reset();
        modelLoaderService.setModelFilesToLoad(modelsToLoad, relayout);
        modelLoaderService.setProject(project);
        modelLoaderService.start();
    }

    private void addToProject(ModelLoadResult loadResult, Set<ModelContainer> modelContainers)
    {
        UndoableProject undoableProject = new UndoableProject(loadResult.getTargetProject());
        Set<ModelContainer> splitModelContainers = new HashSet<>();
        for (ModelContainer modelContainer : modelContainers)
        {
            splitModelContainers.add(modelContainer.splitIntoParts());
        }
        if (splitModelContainers.size() == 1)
        {
            undoableProject.addModel(splitModelContainers.iterator().next());
        } else
        {
            ModelGroup modelGroup = new ModelGroup(splitModelContainers);
            undoableProject.addModel(modelGroup);
        }
    }

}
