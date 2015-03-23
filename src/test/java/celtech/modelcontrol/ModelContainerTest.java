/*
 * Copyright 2015 CEL UK
 */
package celtech.modelcontrol;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class ModelContainerTest
{

    @Test
    public void testRotationApplication()
    {
        
        Vector3D xAxis = new Vector3D(1,0,0);
        Rotation Rx180 = new Rotation(xAxis, Math.PI / 2d);
        Vector3D yAxis = new Vector3D(0,1,0);
        Rotation Ry180 = new Rotation(yAxis, Math.PI / 2d);
        printRotation(Ry180);
        
        Rotation Rx180y180 = Rx180.applyTo(Ry180);
        printRotation(Rx180y180);
        
        Rotation Rx180Inverse = new Rotation(xAxis, - Math.PI / 2d);
        printRotation(Rx180Inverse.applyTo(Rx180y180));
        
    }

    private void printRotation(Rotation R)
    {
        System.out.println("Axis " + R.getAxis() + " Angle " + Math.toDegrees(R.getAngle()));
    }
    
}
