package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Extrusion;
import celtech.gcodetranslator.postprocessing.nodes.providers.ExtrusionProvider;
import celtech.gcodetranslator.postprocessing.nodes.providers.Feedrate;
import celtech.gcodetranslator.postprocessing.nodes.providers.FeedrateProvider;

/**
 *
 * @author Ian
 */
public class UnretractNode extends GCodeEventNode implements ExtrusionProvider, FeedrateProvider
{
    private final Feedrate feedrate = new Feedrate();
    private final Extrusion extrusion = new Extrusion();

    @Override
    public Extrusion getExtrusion()
    {
        return extrusion;
    }

    @Override
    public Feedrate getFeedrate()
    {
        return feedrate;
    }
}
