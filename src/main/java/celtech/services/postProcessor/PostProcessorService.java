/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.postProcessor;

import celtech.printerControl.model.Printer;
import celtech.services.ControllableService;
import celtech.services.slicer.RoboxProfile;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author Ian
 */
public class PostProcessorService extends Service<GCodePostProcessingResult> implements ControllableService
{
    private String printJobUUID = null;
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
    protected Task<GCodePostProcessingResult> createTask()
    {
        return new PostProcessorTask(printJobUUID, settings, printerToUse);
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
