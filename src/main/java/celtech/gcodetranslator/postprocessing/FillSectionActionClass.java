package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.FillSectionNode;
import org.parboiled.Action;
import org.parboiled.Context;

/**
 *
 * @author Ian
 */
public class FillSectionActionClass implements Action
{
    private FillSectionNode node = null;
    
    @Override
    public boolean run(Context context)
    {
        node = new FillSectionNode();
        return true;
    }

    public FillSectionNode getNode()
    {
        return node;
    }
}
