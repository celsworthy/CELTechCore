package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class IslandNode extends GCodeEventNode
{

    public IslandNode()
    {
    }

    @Override
    public String toString()
    {
        return "Island";
    }

    @Override
    public String renderForOutput()
    {
        return toString();
    }
}
