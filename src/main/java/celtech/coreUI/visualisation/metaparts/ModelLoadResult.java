/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.metaparts;

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

    private ModelLoadResultType resultType = ModelLoadResultType.Mesh;
    private String filename = null;
    private Set<ModelContainer> modelContainers;
    // File lines are only used for gcode at present
    private final List<String> fileLines = new ArrayList<>();
    private String fullFilename;

    public ModelLoadResult(String fullFilename, String filename, Set<ModelContainer> modelContainers)
    {
        this.fullFilename = fullFilename;
        this.filename = filename;
        this.modelContainers = modelContainers;
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
