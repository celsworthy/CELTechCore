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
public class GCodeEvent extends GCodeParseEvent
{

    private int gNumber;

    public int getGNumber()
    {
        return gNumber;
    }

    public void setGNumber(int value)
    {
        this.gNumber = value;
    }

    @Override
    public String renderForOutput()
    {
        String stringToReturn = "G" + getGNumber();
        
        if (getComment() != null)
        {
            stringToReturn += " ; " + getComment();
        }

        return stringToReturn + "\n";
    }
}
