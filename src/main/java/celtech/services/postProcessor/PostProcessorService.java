package celtech.services.postProcessor;

import celtech.configuration.fileRepresentation.SlicerParameters;
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
    private SlicerParameters settings = null;
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
    public void setSettings(SlicerParameters settings)
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
