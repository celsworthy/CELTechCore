package celtech.gcodetranslator.postprocessing.nodes;

/**
 *
 * @author Ian
 */
public class MCodeNode extends CommentableNode
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
        String stringToReturn = "M" + getMNumber();

        if (sNumberPresent)
        {
            stringToReturn += " S" + sNumber;
        }

        stringToReturn += renderComments();

        return stringToReturn;
    }
}
