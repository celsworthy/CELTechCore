package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;

/**
 *
 * @author Ian
 */
public class SearchSegment
{
    private final MovementProvider startNode;
    private final MovementProvider endNode;

    public SearchSegment(MovementProvider startNode, MovementProvider endNode)
    {
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public MovementProvider getStartNode()
    {
        return startNode;
    }

    public MovementProvider getEndNode()
    {
        return endNode;
    }
}
