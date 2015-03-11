package celtech.utils.threed;

import celtech.coreUI.visualisation.metaparts.Part;
import java.util.List;
import javafx.geometry.Bounds;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author Ian
 */
public class ThreeDUtils
{

    public static Vector3D calculateCentre(final List<Part> parts)
    {
        double minX = 999;
        double minY = 999;
        double minZ = 999;
        double maxX = 0;
        double maxY = 0;
        double maxZ = 0;

        for (Part part : parts)
        {
            Bounds modelBounds = part.getRealWorldBounds();

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
