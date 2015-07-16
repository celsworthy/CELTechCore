/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.metaparts;

import celtech.appManager.Project;
import celtech.modelcontrol.ModelContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ModelLoadResult
{

    private boolean modelTooLargeForPrintbed = false;
    private ModelLoadResultType resultType = ModelLoadResultType.Mesh;
    private String filename = null;
    private Set<ModelContainer> modelContainers;
    private Project targetProject;
    // File lines are only used for gcode at present
    private final List<String> fileLines = new ArrayList<>();
    private String fullFilename;

    public ModelLoadResult(boolean modelIsTooLarge, String fullFilename, String filename, 
        Project targetProject, Set<ModelContainer> modelContainers)
    {
        this.modelTooLargeForPrintbed = modelIsTooLarge;
        this.fullFilename = fullFilename;
        this.filename = filename;
        this.targetProject = targetProject;
        this.modelContainers = modelContainers;
    }

    public boolean isModelTooLarge()
    {
        return modelTooLargeForPrintbed;
    }

    public String getModelFilename()
    {
        return filename;
    }

    public String getFullFilename()
    {
        return fullFilename;
    }

    public Set<ModelContainer> getModelContainers()
    {
        return modelContainers;
    }

    public Project getTargetProject()
    {
        return targetProject;
    }

    public void setFileLines(ArrayList<String> fileData)
    {
        this.fileLines.addAll(0, fileData);
    }

    public List<String> getFileLines()
    {
        return fileLines;
    }

    public ModelLoadResultType getResultType()
    {
        return resultType;
    }
}
