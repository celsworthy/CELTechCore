/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.modelLoader;

import celtech.services.ControllableService;
import java.io.File;
import java.util.List;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author ianhudson
 */
public class ModelLoaderService extends Service<ModelLoadResults> implements
    ControllableService
{

    private List<File> modelFilesToLoad;

    public final void setModelFilesToLoad(List<File> modelFiles)
    {
        modelFilesToLoad = modelFiles;
    }

    @Override
    protected Task<ModelLoadResults> createTask()
    {
        return new ModelLoaderTask(modelFilesToLoad);
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

}
