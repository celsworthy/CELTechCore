package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class UnretractEvent extends GCodeParseEvent
{

    private double e = 0;
    private double d = 0;

    /**
     *
     * @return
     */
    public double getE()
    {
        return e;
    }

    /**
     *
     * @param value
     */
    public void setE(double value)
    {
        this.e = value;
    }

    /**
     *
     * @return
     */
    public double getD()
    {
        return d;
    }

    /**
     *
     * @param value
     */
    public void setD(double value)
    {
        this.d = value;
    }

    /**
     *
     * @return
     */
    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 " + String.format("E%.5f", e) + String.format(" D%.5f", d);

        if (getFeedRate() > 0)
        {
            stringToReturn += " F" + String.format("%.3f", getFeedRate());
        }

        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
