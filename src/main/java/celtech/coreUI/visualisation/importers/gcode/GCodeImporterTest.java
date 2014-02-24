/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.visualisation.importers.gcode;

import celtech.coreUI.components.ProjectTab;
import celtech.coreUI.visualisation.ApplicationMaterials;
import celtech.coreUI.visualisation.Xform;
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
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Line;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.StrokeType;
import javafx.scene.shape.TriangleMesh;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class GCodeImporterTest
{

    private final Stenographer steno = StenographerFactory.getStenographer(GCodeImporterTest.class.getName());
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

//            BufferedReader reader = new BufferedReader(new FileReader(fFile));
        lineNumber = 1;
        int progressPercent = 0;

        String line;

        TriangleMesh triangleMesh = new TriangleMesh();
        FloatArrayList points = new FloatArrayList();
        IntegerArrayList faces = new IntegerArrayList();

        makeCube(points, faces, currentX, currentY, currentZ);

        FloatArrayList texCoords = new FloatArrayList();
        texCoords.add(0f);
        texCoords.add(0f);

        triangleMesh.getPoints().addAll(points.toFloatArray());
        triangleMesh.getTexCoords().addAll(texCoords.toFloatArray());
        triangleMesh.getFaces().addAll(faces.toIntArray());
        int[] smoothingGroups = new int[faces.size() / 6];
        for (int i = 0; i < smoothingGroups.length; i++)
        {
            smoothingGroups[i] = 0;
        }
        triangleMesh.getFaceSmoothingGroups().addAll(smoothingGroups);

        MeshView meshView = new MeshView();
        meshView.setMesh(triangleMesh);
        meshView.setMaterial(ApplicationMaterials.getGCodeMaterial(MovementType.EXTRUDE, false));
        meshView.setCullFace(CullFace.BACK);
        meshView.setCache(true);
        meshView.setCacheHint(CacheHint.SPEED);
        meshView.setManaged(false);
        meshView.setDepthTest(DepthTest.DISABLE);
        meshView.setId("Line " + lineNumber);
        outputMeshes.getChildren().add(meshView);

        triangleMesh = new TriangleMesh();
        points.clear();
        faces.clear();

        steno.info("About to add models");

        steno.info("The mesh contains " + triangleMesh.getPoints().size()
                + " points, " + triangleMesh.getTexCoords().size() + " tex coords and "
                + triangleMesh.getFaces().size() + " faces");

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
