package celtech.gcodetranslator.postprocessing.nodes;

import celtech.gcodetranslator.postprocessing.nodes.providers.Comment;
import celtech.gcodetranslator.postprocessing.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class MCodeNode extends GCodeEventNode implements Renderable
{

    private int mNumber;
    private boolean sNumberPresent = false;
    private int sNumber;

    public MCodeNode()
    {
    }

    public MCodeNode(int mNumber)
    {
        this.mNumber = mNumber;
    }

    /**
     *
     * @return
     */
    public int getMNumber()
    {
        return mNumber;
    }

    /**
     *
     * @param value
     */
    public void setMNumber(int value)
    {
        this.mNumber = value;
    }

    /**
     *
     * @return
     */
    public int getSNumber()
    {
        return sNumber;
    }

    /**
     *
     * @param value
     */
    public void setSNumber(int value)
    {
        sNumberPresent = true;
        this.sNumber = value;
    }

    /**
     *
     * @return
     */
    @Override
    public String renderForOutput()
    {
        StringBuilder stringToOutput = new StringBuilder();

        stringToOutput.append("M");
        stringToOutput.append(getMNumber());

        if (sNumberPresent)
        {
            stringToOutput.append(" S");
            stringToOutput.append(sNumber);
        }
        stringToOutput.append(' ');
        stringToOutput.append(getCommentText());

        return stringToOutput.toString().trim();
    }
}
