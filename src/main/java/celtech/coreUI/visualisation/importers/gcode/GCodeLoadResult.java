/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.coreUI.visualisation.importers.gcode;

import celtech.utils.gcode.representation.ExtrusionMode;
import celtech.utils.gcode.representation.GCodeMeshData;
import celtech.utils.gcode.representation.MovementMode;
import java.util.ArrayList;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class GCodeLoadResult
{
    private boolean modelTooLargeForPrintbed = false;
    private String filename = null;
    private ArrayList<String> fileLines = new ArrayList<>();
    private GCodeMeshData gcodeMeshData = null;

    public GCodeLoadResult(boolean modelIsTooLarge, String filename, GCodeMeshData gcodeMeshData, ArrayList<String> fileLines)
    {
        this.modelTooLargeForPrintbed = modelIsTooLarge;
        this.filename = filename;
        this.gcodeMeshData = gcodeMeshData;
        this.fileLines.addAll(0, fileLines);
    }

    public boolean isModelTooLarge()
    {
        return modelTooLargeForPrintbed;
    }
    
    public String getModelFilename()
    {
        return filename;
    }
    
    public GCodeMeshData getGCodeMeshData()
    {
        return gcodeMeshData;
    }
    
    public ArrayList<String> getFileLines()
    {
        return fileLines;
    }
}