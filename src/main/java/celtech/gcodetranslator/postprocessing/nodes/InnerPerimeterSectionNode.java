package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class InnerPerimeterSectionNode extends GCodeEventNode
{

    public InnerPerimeterSectionNode()
    {
    }
        @Override
    public String renderForOutput()
    {
        return toString();
    }
}
