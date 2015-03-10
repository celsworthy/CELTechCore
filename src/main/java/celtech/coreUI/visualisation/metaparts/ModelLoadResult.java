package celtech.coreUI.visualisation.metaparts;

import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import java.util.ArrayList;
import javafx.scene.Group;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ModelLoadResult
{
    private ModelLoadResultType resultType = ModelLoadResultType.Mesh;
    private String filename = null;
    private Part loadedPart = null;
    private Project targetProject = null;
    private String fullFilename = null;

    /**
     *
     * @param fullFilename
     * @param filename
     * @param loadedPart
     * @param targetProject
     */
    public ModelLoadResult(String fullFilename, String filename, Project targetProject, Part loadedPart)
    {
        this.fullFilename = fullFilename;
        this.filename = filename;
        this.targetProject = targetProject;
        this.loadedPart = loadedPart;
    }

    /**
     *
     * @return
     */
    public String getModelFilename()
    {
        return filename;
    }
    
    /**
     *
     * @return
     */
    public String getFullFilename()
    {
        return fullFilename;
    }
    
    /**
     *
     * @return
     */
    public Part getLoadedPart()
    {
        return loadedPart;
    }
        
    /**
     *
     * @return
     */
    public Project getTargetProject()
    {
        return targetProject;
    }
        
    /**
     *
     * @return
     */
    public ModelLoadResultType getResultType()
    {
        return resultType;
    }
}
