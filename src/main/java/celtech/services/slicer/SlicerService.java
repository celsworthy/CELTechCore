/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import celtech.appManager.Project;
import celtech.printerControl.Printer;
import celtech.services.ControllableService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author ianhudson
 */
public class SlicerService extends Service<SliceResult> implements ControllableService
{

    private String printJobUUID = null;
    private Project project = null;
    private PrintQualityEnumeration printQuality = null;
    private SlicerSettings settings = null;
    private Printer printerToUse = null;

    public void setPrintJobUUID(String printJobUUID)
    {
        this.printJobUUID = printJobUUID;
    }
    
    public void setProject(Project project)
    {
        this.project = project;
    }
        
    public void setPrintQuality(PrintQualityEnumeration printQuality)
    {
        this.printQuality = printQuality;
    }
    
    public void setSettings(SlicerSettings settings)
    {
        this.settings = settings;
    }
    
    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }
    
    @Override
    protected Task<SliceResult> createTask()
    {
        return new SlicerTask(printJobUUID, project, printQuality, settings, printerToUse);
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }
}
