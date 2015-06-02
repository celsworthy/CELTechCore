package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class SkinSectionNode extends SectionNode
{

    public static final String designator = ";TYPE:SKIN";

    public SkinSectionNode()
    {
    }

    @Override
    public String renderForOutput()
    {
        return designator + " " + super.renderForOutput();
    }
}
