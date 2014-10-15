/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import celtech.appManager.Project;
import celtech.configuration.FilamentContainer;
import celtech.printerControl.model.HardwarePrinter;

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
    private RoboxProfile settings = null;  
    private HardwarePrinter printerToUse = null;
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
    public SliceResult(String printJobUUID, Project project, FilamentContainer filament, PrintQualityEnumeration printQuality, RoboxProfile settings, HardwarePrinter printerToUse, boolean success)
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
    public RoboxProfile getSettings()
    {
        return settings;
    }

    /**
     *
     * @return
     */
    public HardwarePrinter getPrinterToUse()
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
