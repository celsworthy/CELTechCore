/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.importers.gcode;

import celtech.coreUI.components.ProjectTab;
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.importers.FloatArrayList;
import celtech.coreUI.visualisation.importers.IntegerArrayList;
import celtech.coreUI.visualisation.importers.ModelLoadResult;
import celtech.modelcontrol.ModelContainer;
import celtech.services.modelLoader.ModelLoaderTask;
import celtech.utils.gcode.representation.GCodeElement;
import celtech.utils.gcode.representation.GCodeMeshData;
import celtech.utils.gcode.representation.MovementType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import javafx.beans.property.DoubleProperty;
import javafx.scene.CacheHint;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Line;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class GCodeImporterLines
{

    private final Stenographer steno = StenographerFactory.getStenographer(GCodeImporterLines.class.getName());
    private final int VERTICES_PER_TRIANGLE = 3;
    private final int POINTS_PER_VERTEX = 3;
    private final int POINTS_PER_TRIANGLE = POINTS_PER_VERTEX * VERTICES_PER_TRIANGLE;
    private final int FACE_INDICES_PER_VERTEX = 2;
    private final int FACE_INDICES_PER_TRIANGLE = FACE_INDICES_PER_VERTEX * POINTS_PER_VERTEX;

    public ModelLoadResult loadFile(ModelLoaderTask parentTask, String modelFileToLoad, ProjectTab targetProjectTab, DoubleProperty percentProgressProperty)
    {
//        G result = new Node();
//        Node currentLayer = 
        Group outputMeshes = new Group();
        outputMeshes.setAutoSizeChildren(false);
        outputMeshes.setDepthTest(DepthTest.DISABLE);
        outputMeshes.setManaged(false);
        File fFile = new File(modelFileToLoad);
        int linesInFile = getNumberOfLines(fFile);
        ArrayList<String> fileLines = new ArrayList<>();
        HashMap<Integer, GCodeElement> referencedElements = new HashMap<>();
        HashMap<Integer, Group> referencedLayers = new HashMap<>();
        int lineNumber = 0;

        float currentX = 0;
        float currentY = 0;
        float currentZ = 0;

        float lastX = 0;
        float lastY = 0;
        float lastZ = 0;

        int g1Lines = 0;

        MovementType lastMovementType = null;
        MovementType currentMovementType = MovementType.TRAVEL;

        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(fFile));

            lineNumber = 1;
            int progressPercent = 0;

            String line;

            TriangleMesh meshToOutput = new TriangleMesh();
            FloatArrayList points = new FloatArrayList();
            IntegerArrayList faces = new IntegerArrayList();

            while ((line = reader.readLine()) != null)
            {
                int progressUpdate = (int) (((double) lineNumber / (double) linesInFile) * 100);
                if (progressUpdate != progressPercent)
                {
                    progressPercent = progressUpdate;
                    percentProgressProperty.set(progressPercent);
                }

                line = line.trim();
                fileLines.add(line);

//                steno.info("Processing " + line);
                if (line.startsWith("G1"))
                {
                    g1Lines++;
                    String[] strippedComments = line.split(";");
                    boolean extrusionDetected = false;
                    boolean supportDetected = false;

                    if (strippedComments.length > 1)
                    {
                        // We have a line that has both content and comments -- use this as a retract/unretract cue
                        if (strippedComments[1].toLowerCase().contains("unretract"))
                        {
                            currentMovementType = MovementType.UNRETRACT;
                        } else if (strippedComments[1].toLowerCase().contains("retract"))
                        {
                            currentMovementType = MovementType.RETRACT;
                        }

                        if (strippedComments[1].toLowerCase().contains("support"))
                        {
                            currentMovementType = MovementType.EXTRUDE_SUPPORT;
                        }
                    }

                    String[] lineParts = strippedComments[0].trim().split(" ");
                    currentX = lastX;
                    currentY = lastY;
                    currentZ = lastZ;

                    for (int index = 1; index < lineParts.length; index++)
                    {

                        if (lineParts[index].charAt(0) == 'X')
                        {
                            currentX = Float.valueOf(lineParts[index].substring(1, lineParts[index].length()));
                        } else if (lineParts[index].charAt(0) == 'Y')
                        {
                            currentZ = Float.valueOf(lineParts[index].substring(1, lineParts[index].length()));
                        } else if (lineParts[index].charAt(0) == 'Z')
                        {
                            currentY = -Float.valueOf(lineParts[index].substring(1, lineParts[index].length()));
                        } else if (lineParts[index].charAt(0) == 'E')
                        {
                            extrusionDetected = true;
                        }
                    }

//                    System.out.println("Last: " + lastX + ":" + lastY + ":" + lastZ);
//                    System.out.println("Current: " + currentX + ":" + currentY + ":" + currentZ);
                    if (currentY != lastY)
                    {
                        if (points.size() > 0)
                        {
                            FloatArrayList texCoords = new FloatArrayList();
                            texCoords.add(0f);
                            texCoords.add(0f);

                            meshToOutput.getPoints().addAll(points.toFloatArray());
                            meshToOutput.getTexCoords().addAll(texCoords.toFloatArray());
                            meshToOutput.getFaces().addAll(faces.toIntArray());
                            int[] smoothingGroups = new int[faces.size() / 6];
                            for (int i = 0; i < smoothingGroups.length; i++)
                            {
                                smoothingGroups[i] = 0;
                            }
                            meshToOutput.getFaceSmoothingGroups().addAll(smoothingGroups);

                            MeshView meshView = new MeshView();
                            meshView.setMesh(meshToOutput);
                            meshView.setMaterial(ApplicationMaterials.getGCodeMaterial(MovementType.EXTRUDE, false));
                            meshView.setCullFace(CullFace.BACK);
                            meshView.setCache(true);
                            meshView.setCacheHint(CacheHint.SCALE_AND_ROTATE);
                            meshView.setManaged(false);
                            meshView.setDepthTest(DepthTest.DISABLE);
//                            meshView.setDrawMode(DrawMode.LINE);
                            meshView.setId("Line " + lineNumber);
                            outputMeshes.getChildren().add(meshView);
                            steno.info("The mesh contains " + meshToOutput.getPoints().size()
                                    + " points, " + meshToOutput.getTexCoords().size() + " tex coords and "
                                    + meshToOutput.getFaces().size() + " faces");
                            meshToOutput = new TriangleMesh();
                            points.clear();
                            faces.clear();
                        }
                    }
//outputMeshes.getChildren().add(drawLine(lastX, lastY, lastZ,currentX, currentY, currentZ));
                    if (extrusionDetected == true)
                    {
                        makeCube(points, faces, currentX, currentY, currentZ);
                    }

//                    steno.info("Adding mesh for " +lineNumber);
                    lastX = currentX;
                    lastY = currentY;
                    lastZ = currentZ;

                }
                lineNumber++;
            }

            steno.info("there were " + g1Lines + " lines");
            steno.info("About to close file");
            reader.close();
        } catch (IOException ex)
        {
            steno.error("IO Exception whilst processing " + fFile.getName() + " : " + ex + " on line " + lineNumber);
        }

        steno.info("About to add models");

        GCodeMeshData gcodeData = new GCodeMeshData(outputMeshes, referencedElements, referencedLayers);
        ModelContainer container = new ModelContainer(modelFileToLoad, gcodeData, fileLines);
        return new ModelLoadResult(false, modelFileToLoad, fFile.getName(), targetProjectTab, container);

    }

    private String quote(String aText)
    {
        String QUOTE = "'";
        return QUOTE + aText + QUOTE;
    }

    @SuppressWarnings("empty-statement")
    private int getNumberOfLines(File aFile)
    {
        LineNumberReader reader = null;
        try
        {
            reader = new LineNumberReader(new FileReader(aFile));
            while ((reader.readLine()) != null);
            return reader.getLineNumber();
        } catch (IOException ex)
        {
            return -1;
        } finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (IOException ex)
                {
                    System.err.println("Failed to close file during line number read: " + ex);
                }
            }
        }
    }

    private Line drawLine(float lastX, float lastY, float lastZ, float currentX, float currentY, float currentZ)
    {
        Line theLine = new Line();
        theLine.setCache(true);
        theLine.setCacheHint(CacheHint.SPEED);
        theLine.setSmooth(false);
        theLine.setTranslateZ(lastZ);
        theLine.setFill(Color.GREEN);
//        theLine.setStrokeType(StrokeType.INSIDE);
//        theLine.setStrokeWidth(1);

        theLine.setStartX(lastX);
        theLine.setStartY(lastY);
        theLine.setEndX(currentX);
        theLine.setEndY(currentY);

        return theLine;
    }

    private void makeCube(FloatArrayList points, IntegerArrayList faces, float xPos, float yPos, float zPos)
    {
        float cubeSize = .1f;
        int pointSets = points.size() / 24;

        points.add(xPos - cubeSize);
        points.add(yPos + cubeSize);
        points.add(zPos - cubeSize);

        points.add(xPos + cubeSize);
        points.add(yPos + cubeSize);
        points.add(zPos - cubeSize);

        points.add(xPos - cubeSize);
        points.add(yPos - cubeSize);
        points.add(zPos - cubeSize);

        points.add(xPos + cubeSize);
        points.add(yPos - cubeSize);
        points.add(zPos - cubeSize);

        points.add(xPos - cubeSize);
        points.add(yPos + cubeSize);
        points.add(zPos + cubeSize);

        points.add(xPos + cubeSize);
        points.add(yPos + cubeSize);
        points.add(zPos + cubeSize);

        points.add(xPos - cubeSize);
        points.add(yPos - cubeSize);
        points.add(zPos + cubeSize);

        points.add(xPos + cubeSize);
        points.add(yPos - cubeSize);
        points.add(zPos + cubeSize);

        int faceOffset = pointSets * 8;

        faces.add(faceOffset + 0);
        faces.add(0);
        faces.add(faceOffset + 3);
        faces.add(0);
        faces.add(faceOffset + 1);
        faces.add(0);

        faces.add(faceOffset + 0);
        faces.add(0);
        faces.add(faceOffset + 2);
        faces.add(0);
        faces.add(faceOffset + 3);
        faces.add(0);

        faces.add(faceOffset + 3);
        faces.add(0);
        faces.add(faceOffset + 6);
        faces.add(0);
        faces.add(faceOffset + 7);
        faces.add(0);

        faces.add(faceOffset + 3);
        faces.add(0);
        faces.add(faceOffset + 2);
        faces.add(0);
        faces.add(faceOffset + 6);
        faces.add(0);

        faces.add(faceOffset + 1);
        faces.add(0);
        faces.add(faceOffset + 3);
        faces.add(0);
        faces.add(faceOffset + 7);
        faces.add(0);

        faces.add(faceOffset + 1);
        faces.add(0);
        faces.add(faceOffset + 7);
        faces.add(0);
        faces.add(faceOffset + 5);
        faces.add(0);

        faces.add(faceOffset + 7);
        faces.add(0);
        faces.add(faceOffset + 4);
        faces.add(0);
        faces.add(faceOffset + 5);
        faces.add(0);

        faces.add(faceOffset + 7);
        faces.add(0);
        faces.add(faceOffset + 6);
        faces.add(0);
        faces.add(faceOffset + 4);
        faces.add(0);

        faces.add(faceOffset + 6);
        faces.add(0);
        faces.add(faceOffset + 2);
        faces.add(0);
        faces.add(faceOffset + 0);
        faces.add(0);

        faces.add(faceOffset + 6);
        faces.add(0);
        faces.add(faceOffset + 0);
        faces.add(0);
        faces.add(faceOffset + 4);
        faces.add(0);

        faces.add(faceOffset + 1);
        faces.add(0);
        faces.add(faceOffset + 4);
        faces.add(0);
        faces.add(faceOffset + 0);
        faces.add(0);

        faces.add(faceOffset + 1);
        faces.add(0);
        faces.add(faceOffset + 5);
        faces.add(0);
        faces.add(faceOffset + 4);
        faces.add(0);
    }
}
