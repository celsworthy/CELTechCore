package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class GCodeEvent extends GCodeParseEvent
{

    private int gNumber;

    /**
     *
     * @return
     */
    public int getGNumber()
    {
        return gNumber;
    }

    /**
     *
     * @param value
     */
    public void setGNumber(int value)
    {
        this.gNumber = value;
    }

    /**
     *
     * @return
     */
    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G" + getGNumber();
        
        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
