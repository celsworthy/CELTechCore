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
    private double estimatedDuration_ignoresFeedrate = 0;

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

    public void setEstimatedDuration(double estimatedDuration)
    {
        this.estimatedDuration_ignoresFeedrate = estimatedDuration;
    }

    public double getEstimatedDuration()
    {
        return estimatedDuration_ignoresFeedrate;
    }

    @Override
    public String renderForOutput()
    {
        String stringToReturn = "";

        if (!outputSuppressed)
        {
            stringToReturn += "T" + getToolNumber();
            stringToReturn += getCommentText();
            stringToReturn += " ; Tool Node duration: " + getEstimatedDuration();
        }

        return stringToReturn;
    }
}
