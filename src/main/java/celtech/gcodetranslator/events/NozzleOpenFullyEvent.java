package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class NozzleOpenFullyEvent extends GCodeParseEvent
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
        String stringToReturn = "G1 B1.0";
        
        if (e != 0)
        {
            stringToReturn += String.format(" E%.5f", e);
        }
        
        if (d != 0)
        {
            stringToReturn += String.format(" D%.5f", d);
        }
        
        if (e != 0 || d != 0)
        {
            stringToReturn += String.format(" F%.5f", 400f);
        }

        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
