/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    private boolean modelTooLargeForPrintbed = false;
    private ModelLoadResultType resultType = ModelLoadResultType.Mesh;
    private String filename = null;
    // STL files create a single mesh - change to match GCode/Obj
    private ModelContainer modelContainer = null;
    // GCode and obj create a group
    private Group groupedParts = null;
    private Project targetProject = null;
    // File lines are only used for gcode at present
    private final ArrayList<String> fileLines = new ArrayList<>();
    private String fullFilename = null;

    /**
     *
     * @param modelIsTooLarge
     * @param fullFilename
     * @param filename
     * @param targetProjectTab
     * @param modelContainer
     */
    public ModelLoadResult(boolean modelIsTooLarge, String fullFilename, String filename, Project targetProject, ModelContainer modelContainer)
    {
        this.modelTooLargeForPrintbed = modelIsTooLarge;
        this.fullFilename = fullFilename;
        this.filename = filename;
        this.targetProject = targetProject;
        this.modelContainer = modelContainer;
    }

    /**
     *
     * @return
     */
    public boolean isModelTooLarge()
    {
        return modelTooLargeForPrintbed;
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
     * @param modelGroup
     */
    public void setModelContainer(ModelContainer modelGroup)
    {
        this.modelContainer = modelGroup;
        resultType = ModelLoadResultType.Mesh;
    }

    /**
     *
     * @return
     */
    public ModelContainer getModelContainer()
    {
        return modelContainer;
    }
    
    /**
     *
     * @param groupedParts
     */
    public void setGroupedParts(Group groupedParts)
    {
        this.groupedParts = groupedParts;
        resultType = ModelLoadResultType.GroupedParts;
    }
    
    /**
     *
     * @return
     */
    public Group getGroupedParts()
    {
        return groupedParts;
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
     * @param fileData
     */
    public void setFileLines(ArrayList<String> fileData)
    {
        this.fileLines.addAll(0, fileData);
    }

    /**
     *
     * @return
     */
    public ArrayList<String> getFileLines()
    {
        return fileLines;
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
