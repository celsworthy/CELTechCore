package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.GCodeEventNode;
import java.util.List;

/**
 *
 * @author Ian
 */
public class InScopeEvents
{

    private final List<GCodeEventNode> inScopeEvents;
    private final double availableExtrusion;

    public InScopeEvents(List<GCodeEventNode> inScopeEvents, double availableExtrusion)
    {
        this.inScopeEvents = inScopeEvents;
        this.availableExtrusion = availableExtrusion;
    }

    public double getAvailableExtrusion()
    {
        return availableExtrusion;
    }

    public List<GCodeEventNode> getInScopeEvents()
    {
        return inScopeEvents;
    }
}
