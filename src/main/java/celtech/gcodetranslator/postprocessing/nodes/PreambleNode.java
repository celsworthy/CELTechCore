package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class PreambleNode extends GCodeEventNode
{

    public PreambleNode()
    {
    }

    @Override
    public String toString()
    {
        return "Preamble";
    }

    @Override
    public String renderForOutput()
    {
        return toString();
    }
}
