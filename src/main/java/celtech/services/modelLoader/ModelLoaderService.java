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

    public final void setModelFileToLoad(String value)
    {
        modelFileToLoad.set(value);
    }

    public final String getModelFileToLoad()
    {
        return modelFileToLoad.get();
    }

    public final StringProperty modelFileToLoadProperty()
    {
        return modelFileToLoad;
    }

    public final void setShortModelName(String value)
    {
        shortModelName.set(value);
    }

    public final String getShortModelName()
    {
        return shortModelName.get();
    }

    public final StringProperty shortModelNameProperty()
    {
        return shortModelName;
    }

    @Override
    protected Task<ModelLoadResult> createTask()
    {
        return new ModelLoaderTask(getModelFileToLoad(), getShortModelName(), targetProjectTab);
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

    public void setTargetTab(ProjectTab targetProjectTab)
    {
        this.targetProjectTab = targetProjectTab;
    }
}
