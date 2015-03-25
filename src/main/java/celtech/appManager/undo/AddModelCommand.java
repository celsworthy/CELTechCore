/*
 * Copyright 2015 CEL UK
 */
package celtech.appManager.undo;

import celtech.appManager.Project;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import celtech.modelcontrol.ModelContainer;
import celtech.services.modelLoader.ModelLoadResults;
import celtech.services.modelLoader.ModelLoaderTask;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.concurrent.WorkerStateEvent;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class AddModelCommand extends Command
{
    
    private Stenographer steno = StenographerFactory.getStenographer(
        AddModelCommand.class.getName());

    Project project;
    ModelContainer modelContainer;
    
    public AddModelCommand(Project project, ModelContainer modelContainer)
    {
        this.project = project;
        this.modelContainer = modelContainer;
    }

    @Override
    public void do_()
    {
        project.addModel(modelContainer);
    }

    @Override
    public void undo()
    {
        modelContainer.clearMeshes();
        project.deleteModel(modelContainer);
    }

    @Override
    public void redo()
    {
        List<File> modelFiles = new ArrayList<>();
        modelFiles.add(modelContainer.getModelFile());
        ModelLoaderTask modelLoaderTask = new ModelLoaderTask(modelFiles, null, true);
        modelLoaderTask.setOnSucceeded((WorkerStateEvent event) ->
        {
            ModelLoadResults modelLoadResults = modelLoaderTask.getValue();
            ModelLoadResult modelLoadResult = modelLoadResults.getResults().get(0);
            ModelContainer loadedModelContainer = modelLoadResult.getModelContainer();
            modelContainer.addChildNodes(loadedModelContainer.getChildren());
            project.addModel(modelContainer);
        });
        modelLoaderTask.setOnFailed((WorkerStateEvent event) ->
        {
            steno.error("Unable to re-add the model");
        });
        modelLoaderTask.run();

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
