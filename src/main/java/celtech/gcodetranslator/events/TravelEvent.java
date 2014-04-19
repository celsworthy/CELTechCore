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
public class TravelEvent extends GCodeParseEvent
{

    private double x;
    private double y;

    public double getX()
    {
        return x;
    }

    public void setX(double x)
    {
        this.x = x;
    }

    public double getY()
    {
        return y;
    }

    public void setY(double y)
    {
        this.y = y;
    }

    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G1 X" + String.format("%.3f", x) + " Y" + String.format("%.3f", y);

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
