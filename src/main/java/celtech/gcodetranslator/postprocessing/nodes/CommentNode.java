package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class CommentNode extends GCodeEventNode implements Renderable
{

    //This comment field is only used if a subclass has an inline comment

    public CommentNode(String comment)
    {
        setCommentText(comment);
    }

    @Override
    public String renderForOutput()
    {
        return getCommentText();
    }
}
