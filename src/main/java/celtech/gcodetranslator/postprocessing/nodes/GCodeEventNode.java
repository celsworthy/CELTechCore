package celtech.gcodetranslator.postprocessing.nodes;

import org.parboiled.trees.MutableTreeNodeImpl;

/**
 *
 * @author Ian
 */
public abstract class GCodeEventNode extends MutableTreeNodeImpl<GCodeEventNode>
{
    public GCodeEventNode()
    {
    }

    /**
     *
     * @return
     */
    public abstract String renderForOutput();
}
