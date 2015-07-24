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
    private String printJobUUID;
    private Printer printerToUse;
    private Project project;
    private SlicerParametersFile settings;

    public void setPrintJobUUID(String printJobUUID)
    {
        this.printJobUUID = printJobUUID;
    }

    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }
    
    public void setSettings(SlicerParametersFile settings)
    {
        this.settings = settings;
    }
    
    @Override
    protected Task<GCodePostProcessingResult> createTask()
    {
        return new PostProcessorTask(printJobUUID, printerToUse, project, settings);
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

}
