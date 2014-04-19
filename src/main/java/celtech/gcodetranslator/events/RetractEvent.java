package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class RetractEvent extends GCodeParseEvent
{

    private double e;

    public double getE()
    {
        return e;
    }

    public void setE(double value)
    {
        this.e = value;
    }

    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 E" + String.format("%.5f", e);

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
