/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.services.slicer;

import celtech.appManager.Project;
import celtech.configuration.FilamentContainer;
import celtech.printerControl.Printer;

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
    private SlicerSettings settings = null;  
    private Printer printerToUse = null;
    private boolean success = false;

    public SliceResult(String printJobUUID, Project project, FilamentContainer filament, PrintQualityEnumeration printQuality, SlicerSettings settings, Printer printerToUse, boolean success)
    {
        this.printJobUUID = printJobUUID;
        this.project = project;
        this.filament = filament;
        this.printQuality = printQuality;
        this.settings = settings;
        this.printerToUse = printerToUse;
        this.success = success;
    }

    public String getPrintJobUUID()
    {
        return printJobUUID;
    }

    public Project getProject()
    {
        return project;
    }

    public FilamentContainer getFilament()
    {
        return filament;
    }

    public PrintQualityEnumeration getPrintQuality()
    {
        return printQuality;
    }

    public SlicerSettings getSettings()
    {
        return settings;
    }

    public Printer getPrinterToUse()
    {
        return printerToUse;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }
}
