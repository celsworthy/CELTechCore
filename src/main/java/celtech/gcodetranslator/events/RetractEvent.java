package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class RetractEvent extends GCodeParseEvent
{

    private double e = 0;
    private double d = 0;

    public double getE()
    {
        return e;
    }

    public void setE(double value)
    {
        this.e = value;
    }

    public double getD()
    {
        return d;
    }

    public void setD(double value)
    {
        this.d = value;
    }
    
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
