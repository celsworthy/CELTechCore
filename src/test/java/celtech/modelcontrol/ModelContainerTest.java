/*
 * Copyright 2015 CEL UK
 */
package celtech.modelcontrol;

import celtech.JavaFXConfiguredTest;
import celtech.coreUI.visualisation.metaparts.ModelLoadResult;
import celtech.services.modelLoader.ModelLoadResults;
import celtech.services.modelLoader.ModelLoaderTask;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javafx.geometry.Point3D;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class ModelContainerTest extends JavaFXConfiguredTest
{

    @Test
    public void testRotationApplication()
    {

        Vector3D xAxis = new Vector3D(1, 0, 0);
        Rotation Rx180 = new Rotation(xAxis, Math.PI / 2d);
        Vector3D yAxis = new Vector3D(0, 1, 0);
        Rotation Ry180 = new Rotation(yAxis, Math.PI / 2d);
        printRotation(Ry180);

        Rotation Rx180y180 = Rx180.applyTo(Ry180);
        printRotation(Rx180y180);

        Rotation Rx180Inverse = new Rotation(xAxis, -Math.PI / 2d);
        printRotation(Rx180Inverse.applyTo(Rx180y180));

    }
    
    private ModelContainer loadSTL(String stlLocation) throws InterruptedException, ExecutionException {
        List<File> modelFiles = new ArrayList<>();
        URL statisticsFile = this.getClass().getResource(stlLocation);
        modelFiles.add(new File(statisticsFile.getFile()));
        ModelLoaderTask modelLoaderTask = new ModelLoaderTask(modelFiles, null, true);
        Thread th = new Thread(modelLoaderTask);
        th.setDaemon(true);
        th.start();
        ModelLoadResults modelLoadResults = modelLoaderTask.get();
        ModelLoadResult modelLoadResult = modelLoadResults.getResults().get(0);
        ModelContainer modelContainer = modelLoadResult.getModelContainer();
        return modelContainer;
    }
    
    @Test
    public void testSnapPyramidToGround() throws InterruptedException, ExecutionException
    {
        // this test just needs to get the Lean correct (no twist required)
        
        ModelContainer modelContainer = loadSTL("/stl/pyramid.stl");
        int faceIndex = 4;
        modelContainer.printTransforms();

//        modelContainer.setSnapFaceIndex(faceIndex); // should set lean to around 117 degrees

        Vector3D faceNormal = modelContainer.getFaceNormal(faceIndex);
        Vector3D faceCentre = modelContainer.getFaceCentre(faceIndex);
        System.out.println("face normal is " + faceNormal);
        System.out.println("face centre is " + faceCentre);

        modelContainer.setRotationLean(90);
        modelContainer.setRotationTwist(0);
        modelContainer.setRotationTurn(0);
        
        Point3D rotatedFaceNormal = modelContainer.getRotatedFaceNormal(faceIndex);

        System.out.println("rotated face normal  " + rotatedFaceNormal);

        assertEquals(117.0, modelContainer.getRotationLean(), 0.1);
        fail("");
    }
    
    private void printRotation(Rotation R)
    {
        System.out.println("Axis " + R.getAxis() + " Angle " + Math.toDegrees(R.getAngle()));
    }

}
