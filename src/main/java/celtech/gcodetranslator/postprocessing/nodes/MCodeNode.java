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
    private boolean sNumberOnly = false;
    private int sNumber;
    private boolean tNumberPresent = false;
    private boolean tNumberOnly = false;
    private int tNumber;

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
        sNumberOnly = false;
        sNumberPresent = true;
        this.sNumber = value;
    }

    /**
     *
     * @param value
     */
    public void setSOnly(boolean sOnly)
    {
        sNumberOnly = true;
        sNumberPresent = false;
    }
    
    /**
     *
     * @return
     */
    public int getTNumber()
    {
        return tNumber;
    }

    /**
     *
     * @param value
     */
    public void setTNumber(int value)
    {
        tNumberOnly = false;
        tNumberPresent = true;
        this.tNumber = value;
    }

    /**
     *
     * @param value
     */
    public void setTOnly(boolean tOnly)
    {
        tNumberOnly = true;
        tNumberPresent = false;
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
        else if (sNumberOnly)
        {            
            stringToOutput.append(" S");
        }

        if (tNumberPresent)
        {
            stringToOutput.append(" T");
            stringToOutput.append(tNumber);
        }
        else if (tNumberOnly)
        {            
            stringToOutput.append(" T");
        }

        stringToOutput.append(' ');
        stringToOutput.append(getCommentText());

        return stringToOutput.toString().trim();
    }
}
