package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class IntersectionResult
{
    private final GCodeEventNode closestNode;
    private final Vector2D intersectionPoint;

    public IntersectionResult(GCodeEventNode closestNode, Vector2D intersectionPoint)
    {
        this.closestNode = closestNode;
        this.intersectionPoint = intersectionPoint;
    }

    public GCodeEventNode getClosestNode()
    {
        return closestNode;
    }

    public Vector2D getIntersectionPoint()
    {
        return intersectionPoint;
    }
}
