/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class NozzlePositionChangeEvent extends ExtrusionEvent
{

    private double b;
    private boolean noExtrusion = false;

    public double getB()
    {
        return b;
    }

    public void setB(double b)
    {
        this.b = b;
    }

    public boolean getNoExtrusionFlag()
    {
        return noExtrusion;
    }

    /*
     * This method indicates whether E values should be output or not
     */
    public void setNoExtrusionFlag(boolean value)
    {
        this.noExtrusion = value;
    }

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

        stringToReturn += " ; ->" + getLength() + " ";

        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
