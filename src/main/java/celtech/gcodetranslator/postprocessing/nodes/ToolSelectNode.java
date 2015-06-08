package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class ToolSelectNode extends CommentableNode
{
    private int toolNumber = -1;

    public int getToolNumber()
    {
        return toolNumber;
    }

    public void setToolNumber(int toolNumber)
    {
        this.toolNumber = toolNumber;
    }
     
    @Override
    public String renderForOutput()
    {
        String stringToReturn = "T" + getToolNumber();

        stringToReturn += getComment();

        return stringToReturn;
    }
}
