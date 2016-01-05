package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.ExtrusionNode;

/**
 *
 * @author Ian
 */
public class CloseResult
{

    private final double nozzleStartPosition;
    private final double nozzleCloseOverVolume;
    private final ExtrusionNode nodeContainingFinalClose;

    public CloseResult(double nozzleStartPosition, double nozzleCloseOverVolume, ExtrusionNode nodeContainingFinalClose)
    {
        this.nozzleStartPosition = nozzleStartPosition;
        this.nozzleCloseOverVolume = nozzleCloseOverVolume;
        this.nodeContainingFinalClose = nodeContainingFinalClose;
    }

    public double getNozzleStartPosition()
    {
        return nozzleStartPosition;
    }

    public double getNozzleCloseOverVolume()
    {
        return nozzleCloseOverVolume;
    }

    public ExtrusionNode getNodeContainingFinalClose()
    {
        return nodeContainingFinalClose;
    }

}
