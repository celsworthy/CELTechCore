package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class ToolSelectNode extends GCodeEventNode
{
        @Override
    public String renderForOutput()
    {
        return toString();
    }
}
