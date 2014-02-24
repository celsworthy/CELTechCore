/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

/**
 *
 * @author ianhudson
 */
public enum PrinterStatusEnumeration
{
    IDLE(0, "Idle"), SLICING(5, "Slicing"), PRINTING(10, "Printing"), PAUSED(15, "Paused"), ERROR(90, "Error");
    
    private final int statusValue;
    private final String description;

    private PrinterStatusEnumeration(int statusValue, String description)
    {
        this.statusValue = statusValue;
        this.description = description;
    }
    
    public int getStatusValue()
    {
        return statusValue;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    @Override
    public String toString()
    {
        return getDescription();
    }
}
