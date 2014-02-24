/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.importers;

import celtech.modelcontrol.ModelContainer;
import celtech.coreUI.components.ProjectTab;
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
    private ProjectTab targetProjectTab = null;
    // File lines are only used for gcode at present
    private final ArrayList<String> fileLines = new ArrayList<>();
    private String fullFilename = null;

    public ModelLoadResult(boolean modelIsTooLarge, String fullFilename, String filename, ProjectTab targetProjectTab, ModelContainer modelContainer)
    {
        this.modelTooLargeForPrintbed = modelIsTooLarge;
        this.fullFilename = fullFilename;
        this.filename = filename;
        this.targetProjectTab = targetProjectTab;
        this.modelContainer = modelContainer;
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
    
    public void setModelContainer(ModelContainer modelGroup)
    {
        this.modelContainer = modelGroup;
        resultType = ModelLoadResultType.Mesh;
    }

    public ModelContainer getModelContainer()
    {
        return modelContainer;
    }
    
    public void setGroupedParts(Group groupedParts)
    {
        this.groupedParts = groupedParts;
        resultType = ModelLoadResultType.GroupedParts;
    }
    
    public Group getGroupedParts()
    {
        return groupedParts;
    }

    public ProjectTab getTargetProjectTab()
    {
        return targetProjectTab;
    }
    
    public void setFileLines(ArrayList<String> fileData)
    {
        this.fileLines.addAll(0, fileData);
    }

    public ArrayList<String> getFileLines()
    {
        return fileLines;
    }
    
    public ModelLoadResultType getResultType()
    {
        return resultType;
    }
}
