package celtech.configuration.fileRepresentation;

import celtech.appManager.Project;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Date;

//@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
//@JsonDeserialize(using = ProjectFileDeserialiser.class)
public abstract class ProjectFile
{
    private ProjectFileTypeEnum projectType;
    private int version = 4;
    private String projectName;
    private Date lastModifiedDate;
    private String lastPrintJobID = "";

    public ProjectFileTypeEnum getProjectType()
    {
        return projectType;
    }

    public void setProjectType(ProjectFileTypeEnum projectType)
    {
        this.projectType = projectType;
    }
    
    public final Date getLastModifiedDate()
    {
        return lastModifiedDate;
    }

    public final void setLastModifiedDate(Date lastModifiedDate)
    {
        this.lastModifiedDate = lastModifiedDate;
    }

    public final String getLastPrintJobID()
    {
        return lastPrintJobID;
    }

    public final void setLastPrintJobID(String lastPrintJobID)
    {
        this.lastPrintJobID = lastPrintJobID;
    }

    public final String getProjectName()
    {
        return projectName;
    }

    public final void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public final int getVersion()
    {
        return version;
    }

    public final void setVersion(int version)
    {
        this.version = version;
    }
           
    public abstract void implementationSpecificPopulate(Project project);
    
    public final void populateFromProject(Project project) {
        projectName = project.getProjectName();
        lastModifiedDate = project.getLastModifiedDate().get();
        lastPrintJobID = project.getLastPrintJobID();
        
        implementationSpecificPopulate(project);
    }
}
