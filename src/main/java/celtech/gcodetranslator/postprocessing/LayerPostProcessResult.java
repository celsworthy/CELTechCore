package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public class LayerPostProcessResult
{

    private final Optional<NozzleProxy> nozzleStateAtEndOfLayer;
    private final float eVolume;
    private final float dVolume;
    private final double timeForLayer_secs;
    private final LayerNode layerData;
    private Optional<Integer> lastObjectNumber = Optional.empty();
    private int lastFeedrateInForce = -1;

    public LayerPostProcessResult(Optional<NozzleProxy> nozzleStateAtEndOfLayer,
            LayerNode layerData,
            float eVolume,
            float dVolume,
            double timeForLayer_secs)
    {
        this.nozzleStateAtEndOfLayer = nozzleStateAtEndOfLayer;
        this.layerData = layerData;
        this.eVolume = eVolume;
        this.dVolume = dVolume;
        this.timeForLayer_secs = timeForLayer_secs;
    }

    public LayerPostProcessResult(Optional<NozzleProxy> nozzleStateAtEndOfLayer,
            LayerNode layerData,
            float eVolume,
            float dVolume,
            double timeForLayer_secs,
            int lastObjectNumber)
    {
        this.nozzleStateAtEndOfLayer = nozzleStateAtEndOfLayer;
        this.layerData = layerData;
        this.eVolume = eVolume;
        this.dVolume = dVolume;
        this.timeForLayer_secs = timeForLayer_secs;
        this.lastObjectNumber = Optional.of(lastObjectNumber);
    }

    public Optional<NozzleProxy> getNozzleStateAtEndOfLayer()
    {
        return nozzleStateAtEndOfLayer;
    }

    public LayerNode getLayerData()
    {
        return layerData;
    }

    public float getEVolume()
    {
        return eVolume;
    }

    public float getDVolume()
    {
        return dVolume;
    }

    public double getTimeForLayer()
    {
        return timeForLayer_secs;
    }

    public Optional<Integer> getLastObjectNumber()
    {
        return lastObjectNumber;
    }

    public void setLastObjectNumber(int lastObjectNumber)
    {
        this.lastObjectNumber = Optional.of(lastObjectNumber);
    }

    /**
     * This is the last feedrate that the parser saw 
     * @param feedrate
     */
    public void setLastFeedrateInForce(int feedrate)
    {
        this.lastFeedrateInForce = feedrate;
    }

    /**
     * This is the last feedrate that the parser saw
     * @return 
     */
    public int getLastFeedrateInForce()
    {
        return lastFeedrateInForce;
    }
}
