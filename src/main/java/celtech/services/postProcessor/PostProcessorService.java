package celtech.services.postProcessor;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.printerControl.model.Printer;
import celtech.services.ControllableService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author Ian
 */
public class PostProcessorService extends Service<GCodePostProcessingResult> implements ControllableService
{
    private String printJobUUID = null;
    private SlicerParametersFile settings = null;
    private Printer printerToUse = null;
    private Project project = null;

    /**
     *
     * @param printJobUUID
     */
    public void setPrintJobUUID(String printJobUUID)
    {
        this.printJobUUID = printJobUUID;
    }

    /**
     *
     * @param settings
     */
    public void setSettings(SlicerParametersFile settings)
    {
        this.settings = settings;
    }

    /**
     *
     * @param printerToUse
     */
    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }
    
    @Override
    protected Task<GCodePostProcessingResult> createTask()
    {
        return new PostProcessorTask(printJobUUID, settings, printerToUse, project);
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
