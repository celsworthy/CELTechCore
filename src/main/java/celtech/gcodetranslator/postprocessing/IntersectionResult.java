package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class IntersectionResult
{
    private final ExtrusionNode closestNode;
    private final Vector2D intersectionPoint;

    public IntersectionResult(ExtrusionNode closestNode, Vector2D intersectionPoint)
    {
        this.closestNode = closestNode;
        this.intersectionPoint = intersectionPoint;
    }

    public ExtrusionNode getClosestNode()
    {
        return closestNode;
    }

    public Vector2D getIntersectionPoint()
    {
        return intersectionPoint;
    }
}
