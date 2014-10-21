package celtech.services.slicer;

import celtech.appManager.Project;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.fileRepresentation.SlicerParameters;
import celtech.printerControl.model.Printer;

/**
 *
 * @author ianhudson
 */
public class SliceResult
{
    private String printJobUUID = null;
    private Project project = null;
    private FilamentContainer filament = null;
    private PrintQualityEnumeration printQuality = null;
    private SlicerParameters settings = null;  
    private Printer printerToUse = null;
    private boolean success = false;

    /**
     *
     * @param printJobUUID
     * @param project
     * @param filament
     * @param printQuality
     * @param settings
     * @param printerToUse
     * @param success
     */
    public SliceResult(String printJobUUID, Project project, FilamentContainer filament, PrintQualityEnumeration printQuality, SlicerParameters settings, Printer printerToUse, boolean success)
    {
        this.printJobUUID = printJobUUID;
        this.project = project;
        this.filament = filament;
        this.printQuality = printQuality;
        this.settings = settings;
        this.printerToUse = printerToUse;
        this.success = success;
    }

    /**
     *
     * @return
     */
    public String getPrintJobUUID()
    {
        return printJobUUID;
    }

    /**
     *
     * @return
     */
    public Project getProject()
    {
        return project;
    }

    /**
     *
     * @return
     */
    public FilamentContainer getFilament()
    {
        return filament;
    }

    /**
     *
     * @return
     */
    public PrintQualityEnumeration getPrintQuality()
    {
        return printQuality;
    }

    /**
     *
     * @return
     */
    public SlicerParameters getSettings()
    {
        return settings;
    }

    /**
     *
     * @return
     */
    public Printer getPrinterToUse()
    {
        return printerToUse;
    }

    /**
     *
     * @return
     */
    public boolean isSuccess()
    {
        return success;
    }

    /**
     *
     * @param success
     */
    public void setSuccess(boolean success)
    {
        this.success = success;
    }
}
