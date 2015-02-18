/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.modelLoader;

import celtech.coreUI.components.ProjectTab;
import celtech.services.ControllableService;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
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
    private ProjectTab targetProjectTab = null;
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
        return new ModelLoaderTask(modelFilesToLoad, targetProjectTab, relayout);
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

    /**
     *
     * @param targetProjectTab
     */
    public void setTargetTab(ProjectTab targetProjectTab)
    {
        this.targetProjectTab = targetProjectTab;
    }

}
