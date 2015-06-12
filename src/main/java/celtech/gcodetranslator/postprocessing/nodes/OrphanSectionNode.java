package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class OrphanSectionNode extends SectionNode implements Renderable
{
    @Override
    public String renderForOutput()
    {
        return ";Orphan section";
    }
}
