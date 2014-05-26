/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.coreUI.DisplayManager;

/**
 *
 * @author ianhudson
 */
public enum PrinterStatusEnumeration
{

    /**
     *
     */
    IDLE(0, "PrintQueue.Idle"),

    /**
     *
     */
    SLICING(5, "PrintQueue.Slicing"),

    /**
     *
     */
    POST_PROCESSING(6, "PrintQueue.PostProcessing"),

    /**
     *
     */
    SENDING_TO_PRINTER(10, "PrintQueue.SendingToPrinter"),

    /**
     *
     */
    PRINTING(15, "PrintQueue.Printing"),

    /**
     *
     */
    EXECUTING_MACRO(16, "PrintQueue.ExecutingMacro"),

    /**
     *
     */
    PAUSED(20, "PrintQueue.Paused"),

    /**
     *
     */
    ERROR(90, "PrintQueue.Error");
    
    private final int statusValue;
    private final String description;

    private PrinterStatusEnumeration(int statusValue, String description)
    {
        this.statusValue = statusValue;
        this.description = description;
    }
    
    /**
     *
     * @return
     */
    public int getStatusValue()
    {
        return statusValue;
    }
    
    /**
     *
     * @return
     */
    public String getDescription()
    {
        return DisplayManager.getLanguageBundle().getString(description);
    }
    
    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return getDescription();
    }
}
