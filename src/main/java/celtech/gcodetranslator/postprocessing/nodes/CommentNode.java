package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class CommentNode extends CommentableNode
{

    public CommentNode(String comment)
    {
        super(comment);
    }

    @Override
    public String renderForOutput()
    {
        return renderComments();
    }
    
}
