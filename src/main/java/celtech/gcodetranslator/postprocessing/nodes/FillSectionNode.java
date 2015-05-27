package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class FillSectionNode extends GCodeEventNode
{
    public FillSectionNode()
    {
    }
    
        @Override
    public String renderForOutput()
    {
        return toString();
    }
}
