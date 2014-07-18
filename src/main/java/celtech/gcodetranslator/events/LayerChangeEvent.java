package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class LayerChangeEvent extends GCodeParseEvent
{
    private double z;

    /**
     *
     * @return
     */
    public double getZ()
    {
        return z;
    }

    /**
     *
     * @param value
     */
    public void setZ(double value)
    {
        this.z = value;
    }

    /**
     *
     * @return
     */
    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 Z" + String.format("%.3f", z);

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
