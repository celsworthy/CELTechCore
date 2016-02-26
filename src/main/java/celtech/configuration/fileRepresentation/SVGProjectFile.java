package celtech.configuration.fileRepresentation;

import celtech.appManager.Project;
import celtech.appManager.SVGProject;

public class SVGProjectFile extends ProjectFile
{

    private int subVersion = 1;

    public int getSubVersion()
    {
        return subVersion;
    }

    public void setSubVersion(int version)
    {
        this.subVersion = version;
    }

    public void populateFromProject(SVGProject project)
    {
    }

    @Override
    public void implementationSpecificPopulate(Project project)
    {
    }
}
