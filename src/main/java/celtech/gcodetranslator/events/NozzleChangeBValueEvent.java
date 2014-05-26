package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class NozzleChangeBValueEvent extends GCodeParseEvent
{

    private double b;

    /**
     *
     * @return
     */
    public double getB()
    {
        return b;
    }

    /**
     *
     * @param b
     */
    public void setB(double b)
    {
        this.b = b;
    }

    /**
     *
     * @return
     */
    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G0 B" + String.format("%.3f", b);

        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
