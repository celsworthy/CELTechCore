
package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class NozzlePositionChangeEvent extends ExtrusionEvent
{

    private double b;
    private boolean noExtrusion = false;

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
    public boolean getNoExtrusionFlag()
    {
        return noExtrusion;
    }

    /*
     * This method indicates whether E values should be output or not
     */

    /**
     *
     * @param value
     */
    
    public void setNoExtrusionFlag(boolean value)
    {
        this.noExtrusion = value;
    }

    /**
     *
     * @return
     */
    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 X" + String.format("%.3f", getX()) + " Y" + String.format("%.3f", getY()) + " B" + String.format("%.3f", b);

        if (noExtrusion == false)
        {
            stringToReturn += " E" + String.format("%.5f", getE());
        }

        if (getFeedRate() > 0)
        {
            stringToReturn += " F" + String.format("%.3f", getFeedRate());
        }

        stringToReturn += " ; ->L" + getLength() + " ";
        
        if (noExtrusion != false)
        {
            stringToReturn += " ; ->E" + getE() + " ";
        }

        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
