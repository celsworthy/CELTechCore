package celtech.gcodetranslator.postprocessing.nodes;

import static celtech.gcodetranslator.postprocessing.nodes.SkinSectionNode.designator;

/**
 *
 * @author Ian
 */
public class SupportSectionNode extends SectionNode
{
    public static final String designator ="???";
    
    public SupportSectionNode()
    {
    }

    @Override
    public String renderForOutput()
    {
        return designator + " " + super.renderForOutput();
    }
}
