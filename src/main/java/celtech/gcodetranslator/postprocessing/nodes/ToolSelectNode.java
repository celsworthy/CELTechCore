package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class ToolSelectNode extends GCodeEventNode implements Renderable
{

    private int toolNumber = -1;
    private boolean outputSuppressed = false;

    public int getToolNumber()
    {
        return toolNumber;
    }

    public void setToolNumber(int toolNumber)
    {
        this.toolNumber = toolNumber;
    }

    public void suppressNodeOutput(boolean suppress)
    {
        outputSuppressed = suppress;
    }
    
    public boolean isNodeOutputSuppressed()
    {
        return outputSuppressed;
    }

    @Override
    public String renderForOutput()
    {
        String stringToReturn = "";

        if (!outputSuppressed)
        {
            stringToReturn += "T" + getToolNumber();
            stringToReturn += getCommentText();
        }

        return stringToReturn;
    }
}
