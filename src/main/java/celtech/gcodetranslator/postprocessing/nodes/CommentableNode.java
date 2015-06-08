package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public abstract class CommentableNode extends GCodeEventNode
{
    //This comment field is only used if a subclass has an inline comment

    private String comment = "";

    public CommentableNode()
    {
    }
    
    public CommentableNode(String comment)
    {
        this.comment = comment;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }
    
    public void appendComment(String comment)
    {
        this.comment += " " + comment;
    }

    public String renderComments()
    {
        StringBuilder stringToReturn = new StringBuilder();
        if (getComment().length() > 0)
        {
            stringToReturn.append(" ; ");
            stringToReturn.append(getComment());
        }

        return stringToReturn.toString();
    }
}
