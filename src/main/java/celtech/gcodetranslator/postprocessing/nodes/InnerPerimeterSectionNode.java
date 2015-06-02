package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class InnerPerimeterSectionNode extends SectionNode
{

    public static final String designator = ";TYPE:WALL-INNER";

    public InnerPerimeterSectionNode()
    {
    }

    @Override
    public String renderForOutput()
    {
        return designator + " " + super.renderForOutput();
    }
}
