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
public class RetractDuringExtrusionEvent extends TravelEvent
{
    private double e;

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
    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 X" + String.format("%.3f", getX()) + " Y" + String.format("%.3f", getY()) + " E" + String.format("%.5f", e);
        
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
