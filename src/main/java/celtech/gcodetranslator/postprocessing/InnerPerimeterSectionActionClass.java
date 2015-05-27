package celtech.gcodetranslator.postprocessing;

import celtech.gcodetranslator.postprocessing.nodes.InnerPerimeterSectionNode;
import org.parboiled.Action;
import org.parboiled.Context;

/**
 *
 * @author Ian
 */
public class InnerPerimeterSectionActionClass implements Action
{
    private InnerPerimeterSectionNode node = null;
    
    @Override
    public boolean run(Context context)
    {
        node = new InnerPerimeterSectionNode();
        return true;
    }

    public InnerPerimeterSectionNode getNode()
    {
        return node;
    }
}
