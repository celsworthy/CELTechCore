package celtech.services.slicer;

import celtech.appManager.Project;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.printerControl.model.Printer;

/**
 *
 * @author ianhudson
 */
public class SliceResult
{
    private String printJobUUID = null;
    private Project project = null;
    private PrintQualityEnumeration printQuality = null;
    private SlicerParametersFile settings = null;  
    private Printer printerToUse = null;
    private boolean success = false;

    public SliceResult(String printJobUUID, Project project, PrintQualityEnumeration printQuality, SlicerParametersFile settings, Printer printerToUse, boolean success)
    {
        this.printJobUUID = printJobUUID;
        this.project = project;
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

    public PrintQualityEnumeration getPrintQuality()
    {
        return printQuality;
    }

    public SlicerParametersFile getSettings()
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
