package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class GCodeDirectiveNode extends GCodeEventNode
{
    @Override
    public String renderForOutput()
    {
        return toString();
    }
}
