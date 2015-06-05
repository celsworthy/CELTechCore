package celtech.configuration.fileRepresentation;

import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.fileRepresentation.SlicerParametersFile.SupportType;
import celtech.services.slicer.PrintQualityEnumeration;
import java.util.Date;


public class ProjectFile
{
    private int version = 1;
    private String projectName;
    private Date lastModifiedDate;
    private int brimOverride = 0;
    private float fillDensityOverride = 0;
    private SupportType printSupportOverride = SupportType.NO_SUPPORT;
    private boolean printRaft = false;
    private String extruder0FilamentID;
    private String extruder1FilamentID;
    private String lastPrintJobID = "";
    private String settingsName = ApplicationConfiguration.draftSettingsProfileName;
    private PrintQualityEnumeration printQuality = PrintQualityEnumeration.DRAFT;

    public String getSettingsName()
    {
        return settingsName;
    }

    public void setSettingsName(String settingsName)
    {
        this.settingsName = settingsName;
    }

    public PrintQualityEnumeration getPrintQuality()
    {
        return printQuality;
    }

    public void setPrintQuality(PrintQualityEnumeration printQuality)
    {
        this.printQuality = printQuality;
    }

    public Date getLastModifiedDate()
    {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate)
    {
        this.lastModifiedDate = lastModifiedDate;
    }

    public int getBrimOverride()
    {
        return brimOverride;
    }

    public void setBrimOverride(int brimOverride)
    {
        this.brimOverride = brimOverride;
    }

    public float getFillDensityOverride()
    {
        return fillDensityOverride;
    }

    public void setFillDensityOverride(float fillDensityOverride)
    {
        this.fillDensityOverride = fillDensityOverride;
    }

    public SupportType getPrintSupportOverride()
    {
        return printSupportOverride;
    }

    public void setPrintSupportOverride(SupportType printSupportOverride)
    {
        this.printSupportOverride = printSupportOverride;
    }

    public boolean getPrintRaft()
    {
        return printRaft;
    }

    public void setPrintRaft(boolean printRaft)
    {
        this.printRaft = printRaft;
    }

    public String getExtruder0FilamentID()
    {
        return extruder0FilamentID;
    }

    public void setExtruder0FilamentID(String extruder0FilamentID)
    {
        this.extruder0FilamentID = extruder0FilamentID;
    }

    public String getExtruder1FilamentID()
    {
        return extruder1FilamentID;
    }

    public void setExtruder1FilamentID(String extruder1FilamentID)
    {
        this.extruder1FilamentID = extruder1FilamentID;
    }

    public String getLastPrintJobID()
    {
        return lastPrintJobID;
    }

    public void setLastPrintJobID(String lastPrintJobID)
    {
        this.lastPrintJobID = lastPrintJobID;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }
    
    public void populateFromProject(Project project) {
        projectName = project.getProjectName();
        lastModifiedDate = project.getLastModifiedDate().get();
        lastPrintJobID = project.getLastPrintJobID();
        extruder0FilamentID = project.getExtruder0FilamentProperty().get().getFilamentID();
        extruder1FilamentID = project.getExtruder1FilamentProperty().get().getFilamentID();
        settingsName = project.getPrinterSettings().getSettingsName();
        printQuality = project.getPrinterSettings().getPrintQuality();
        brimOverride = project.getPrinterSettings().getBrimOverride();
        fillDensityOverride = project.getPrinterSettings().getFillDensityOverride();
        printSupportOverride = project.getPrinterSettings().getPrintSupportOverride();
        printRaft = project.getPrinterSettings().getRaftOverride();
    }
}
