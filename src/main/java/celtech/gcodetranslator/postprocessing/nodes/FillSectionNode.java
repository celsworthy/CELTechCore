package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class FillSectionNode extends SectionNode
{

    public static final String designator = ";TYPE:FILL";

    public FillSectionNode()
    {
    }

    @Override
    public String renderForOutput()
    {
        return designator + " " + super.renderForOutput();
    }
}
