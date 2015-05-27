package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.OuterPerimeterSectionNode;
import org.parboiled.Action;
import org.parboiled.Context;

/**
 *
 * @author Ian
 */
public class OuterPerimeterSectionActionClass implements Action
{
    private OuterPerimeterSectionNode node = null;
    
    @Override
    public boolean run(Context context)
    {
        node = new OuterPerimeterSectionNode();
        return true;
    }

    public OuterPerimeterSectionNode getNode()
    {
        return node;
    }
}
