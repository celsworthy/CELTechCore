/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import celtech.appManager.Project;
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
    private RoboxProfile settings = null;
    private Printer printerToUse = null;

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
     * @param project
     */
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
    public void setSettings(RoboxProfile settings)
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
