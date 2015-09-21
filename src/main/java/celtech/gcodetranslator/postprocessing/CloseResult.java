package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public class CloseResult
{

    private final double nozzleStartPosition;
    private final double nozzleCloseOverVolume;

    public CloseResult(double nozzleStartPosition, double nozzleCloseOverVolume)
    {
        this.nozzleStartPosition = nozzleStartPosition;
        this.nozzleCloseOverVolume = nozzleCloseOverVolume;
    }

    public double getNozzleStartPosition()
    {
        return nozzleStartPosition;
    }

    public double getNozzleCloseOverVolume()
    {
        return nozzleCloseOverVolume;
    }
}
