package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class SupportSectionNode extends SectionNode
{
    public static final String designator =";TYPE:SUPPORT";
    
    public SupportSectionNode()
    {
    }

    @Override
    public String renderForOutput()
    {
        return designator + " " + super.renderForOutput();
    }
}
