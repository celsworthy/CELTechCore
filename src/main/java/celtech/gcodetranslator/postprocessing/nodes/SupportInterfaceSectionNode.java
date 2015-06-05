package celtech.gcodetranslator.postprocessing.nodes;

import static celtech.gcodetranslator.postprocessing.nodes.SkinSectionNode.designator;

/**
 *
 * @author Ian
 */
public class SupportInterfaceSectionNode extends SectionNode
{
    public static final String designator =";TYPE:SUPPORT-INTERFACE";
    
    public SupportInterfaceSectionNode()
    {
    }

    @Override
    public String renderForOutput()
    {
        return designator + " " + super.renderForOutput();
    }
}
