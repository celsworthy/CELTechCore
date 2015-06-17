package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public class CloseResult
{

    private final boolean succeeded;
    private final double elidedExtrusion;
    private Optional<GCodeEventNode> closestNode = Optional.empty();

    public CloseResult(boolean succeeded, double elidedExtrusion)
    {
        this.succeeded = succeeded;
        this.elidedExtrusion = elidedExtrusion;
    }

    public CloseResult(boolean succeeded,
            double elidedExtrusion,
            GCodeEventNode closestNode)
    {
        this.succeeded = succeeded;
        this.elidedExtrusion = elidedExtrusion;
        this.closestNode = Optional.of(closestNode);
    }

    public boolean hasSucceeded()
    {
        return succeeded;
    }

    public double getElidedExtrusion()
    {
        return elidedExtrusion;
    }

    public void setClosestNode(Optional<GCodeEventNode> closestNode)
    {
        this.closestNode = closestNode;
    }

    public Optional<GCodeEventNode> getClosestNode()
    {
        return closestNode;
    }
}
