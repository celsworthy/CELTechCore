/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package celtech.gcodetranslator.events;

/**
 *
 * @author Ian
 */
public class ExtrusionEvent extends TravelEvent
{
    private double e;
    private double d;

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
     * @param e
     */
    public void setE(double e)
    {
        this.e = e;
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
     * @param d
     */
    public void setD(double d)
    {
        this.d = d;
    }

    /**
     *
     * @return
     */
    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 X" + String.format("%.3f", getX()) + " Y" + String.format("%.3f", getY()) + " E" + String.format("%.5f", e) + " D" + String.format("%.5f", d);
        
        if (getFeedRate() > 0)
        {
            stringToReturn += " F" + String.format("%.3f", getFeedRate());
        }
        
        stringToReturn += " ; ->L" + getLength() + " ->E" + getE() + " ->D" + getD();
        
        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }
        
        return stringToReturn + "\n";
    }
}
