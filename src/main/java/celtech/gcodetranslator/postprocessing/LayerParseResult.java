package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.NozzleProxy;
import celtech.gcodetranslator.postprocessing.nodes.LayerNode;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public class LayerParseResult
{
    private final Optional<NozzleProxy> nozzleStateAtEndOfLayer;
    private final LayerNode layerData;

    public LayerParseResult(Optional<NozzleProxy> nozzleStateAtEndOfLayer, LayerNode layerData)
    {
        this.nozzleStateAtEndOfLayer = nozzleStateAtEndOfLayer;
        this.layerData = layerData;
    }

    public Optional<NozzleProxy> getNozzleStateAtEndOfLayer()
    {
        return nozzleStateAtEndOfLayer;
    }

    public LayerNode getLayerData()
    {
        return layerData;
    }
}
