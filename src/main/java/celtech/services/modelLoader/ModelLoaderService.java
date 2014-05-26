/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.modelLoader;

import celtech.coreUI.components.ProjectTab;
import celtech.services.ControllableService;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author ianhudson
 */
public class ModelLoaderService extends Service<ModelLoadResult> implements ControllableService
{

    private StringProperty modelFileToLoad = new SimpleStringProperty();
    private StringProperty shortModelName = new SimpleStringProperty();
    private ProjectTab targetProjectTab = null;

    /**
     *
     * @param value
     */
    public final void setModelFileToLoad(String value)
    {
        modelFileToLoad.set(value);
    }

    /**
     *
     * @return
     */
    public final String getModelFileToLoad()
    {
        return modelFileToLoad.get();
    }

    /**
     *
     * @return
     */
    public final StringProperty modelFileToLoadProperty()
    {
        return modelFileToLoad;
    }

    /**
     *
     * @param value
     */
    public final void setShortModelName(String value)
    {
        shortModelName.set(value);
    }

    /**
     *
     * @return
     */
    public final String getShortModelName()
    {
        return shortModelName.get();
    }

    /**
     *
     * @return
     */
    public final StringProperty shortModelNameProperty()
    {
        return shortModelName;
    }

    @Override
    protected Task<ModelLoadResult> createTask()
    {
        return new ModelLoaderTask(getModelFileToLoad(), getShortModelName(), targetProjectTab);
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
