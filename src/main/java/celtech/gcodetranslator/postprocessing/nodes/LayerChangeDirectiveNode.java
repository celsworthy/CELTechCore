package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class LayerChangeDirectiveNode extends GCodeEventNode
{
    @Override
    public String renderForOutput()
    {
        return toString();
    }
}
