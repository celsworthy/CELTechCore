package celtech.services.slicer;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.printerControl.model.Printer;
import javafx.concurrent.Task;

/**
 *
 * @author ianhudson
 */
public class SlicerService extends AbstractSlicerService
{

    private String printJobUUID = null;
    private Project project = null;
    private PrintQualityEnumeration printQuality = null;
    private SlicerParametersFile settings = null;
    private Printer printerToUse = null;

    /**
     *
     * @param printJobUUID
     */
    @Override
    public void setPrintJobUUID(String printJobUUID)
    {
        this.printJobUUID = printJobUUID;
    }
    
    /**
     *
     * @param project
     */
    @Override
    public void setProject(Project project)
    {
        this.project = project;
    }
        
    /**
     *
     * @param printQuality
     */
    public void setPrintQuality(PrintQualityEnumeration printQuality)
    {
        this.printQuality = printQuality;
    }
    
    /**
     *
     * @param settings
     */
    @Override
    public void setSettings(SlicerParametersFile settings)
    {
        this.settings = settings;
    }
    
    /**
     *
     * @param printerToUse
     */
    @Override
    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }
    
    @Override
    protected Task<SliceResult> createTask()
    {
        return new SlicerTask(printJobUUID, project, printQuality, settings, printerToUse);
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
