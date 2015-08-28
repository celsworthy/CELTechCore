package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Feedrate;
import celtech.gcodetranslator.postprocessing.nodes.providers.FeedrateProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Movement;
import celtech.gcodetranslator.postprocessing.nodes.providers.MovementProvider;

/**
 *
 * @author Ian
 */
public class LayerChangeDirectiveNode extends GCodeEventNode implements MovementProvider, FeedrateProvider
{
    private int layerNumber = -1;
    private final Movement movement = new Movement();
    private final Feedrate feedrate = new Feedrate();

    @Override
    public Movement getMovement()
    {
        return movement;
    }

    @Override
    public Feedrate getFeedrate()
    {
        return feedrate;
    }

    public int getLayerNumber()
    {
        return layerNumber;
    }

    public void setLayerNumber(int layerNumber)
    {
        this.layerNumber = layerNumber;
    }
}
