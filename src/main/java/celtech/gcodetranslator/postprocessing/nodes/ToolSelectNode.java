package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class ToolSelectNode extends GCodeEventNode implements Renderable
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

        stringToReturn += getCommentText();

        return stringToReturn;
    }
}
