/*
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
public class MCodeEvent extends GCodeParseEvent
{

    private int mNumber;
    private boolean sNumberPresent = false;
    private int sNumber;

    public int getMNumber()
    {
        return mNumber;
    }

    public void setMNumber(int value)
    {
        this.mNumber = value;
    }

    public int getSNumber()
    {
        return sNumber;
    }

    public void setSNumber(int value)
    {
        sNumberPresent = true;
        this.sNumber = value;
    }

    @Override
    public String renderForOutput()
    {
        String stringToReturn = "M" + getMNumber();

        if (sNumberPresent)
        {
            stringToReturn += " S" + sNumber;
        }
        
        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
