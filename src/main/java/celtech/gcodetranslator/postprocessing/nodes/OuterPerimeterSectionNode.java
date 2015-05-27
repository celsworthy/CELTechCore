package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class OuterPerimeterSectionNode extends GCodeEventNode
{

    public OuterPerimeterSectionNode()
    {
    }
        @Override
    public String renderForOutput()
    {
        return toString();
    }
}
