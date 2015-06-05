package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class OrphanSectionNode extends SectionNode
{

    public OrphanSectionNode()
    {
    }

    @Override
    public String renderForOutput()
    {
        return ";Orphan section " + super.renderForOutput();
    }
}
