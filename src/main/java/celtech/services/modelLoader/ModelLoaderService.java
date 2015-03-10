package celtech.services.modelLoader;

import celtech.appManager.Project;
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
    private Project targetProject;
    private boolean relayout;

    /**
     *
     * @param value
     */
    public final void setModelFilesToLoad(List<File> modelFiles, boolean relayout)
    {
        modelFilesToLoad = modelFiles;
        this.relayout = relayout;
    }

    @Override
    protected Task<ModelLoadResults> createTask()
    {
        return new ModelLoaderTask(modelFilesToLoad, targetProject, relayout);
    }
    
    public void setProject(Project project) {
        this.targetProject = project;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

}
