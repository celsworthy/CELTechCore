/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils.threed;

import celtech.modelcontrol.ModelContainer;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author Ian
 */
public class ThreeDUtils
{
    public static final Point3D X_AXIS_JFX = new Point3D(1, 0, 0);
    public static final Point3D Y_AXIS_JFX = new Point3D(0, 1, 0);
    public static final Point3D Z_AXIS_JFX = new Point3D(0, 0, 1);

    public static Vector3D calculateCentre(List<ModelContainer> models)
    {
        double minX = 999;
        double minY = 999;
        double minZ = 999;
        double maxX = 0;
        double maxY = 0;
        double maxZ = 0;

        for (ModelContainer model : models)
        {
            Bounds modelBounds = model.getBoundsInParent();

            minX = Math.min(modelBounds.getMinX(), minX);
            minY = Math.min(modelBounds.getMinY(), minY);
            minZ = Math.min(modelBounds.getMinZ(), minZ);

            maxX = Math.max(modelBounds.getMaxX(), maxX);
            maxY = Math.max(modelBounds.getMaxY(), maxY);
            maxZ = Math.max(modelBounds.getMaxZ(), maxZ);
        }

        double width = maxX - minX;
        double depth = maxZ - minZ;
        double height = maxY - minY;

        double centreX = minX + (width / 2);
        double centreY = maxY - (height / 2);
        double centreZ = minZ + (depth / 2);

        return new Vector3D(centreX, centreY, centreZ);
    }
}
