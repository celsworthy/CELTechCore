/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.Lookup;

/**
 *
 * @author ianhudson
 */
public enum PrinterStatus
{
    IDLE(0, "printerStatus.idle"),
    SLICING(5, "PrintQueue.Slicing"),
    POST_PROCESSING(6, "PrintQueue.PostProcessing"),
    SENDING_TO_PRINTER(10, "PrintQueue.SendingToPrinter"),
    PRINTING(15, "printerStatus.printing"),
    EXECUTING_MACRO(16, "PrintQueue.ExecutingMacro"),
    PAUSING(20, "printerStatus.pausing"),
    PAUSED(21, "printerStatus.paused"),
    RESUMING(22, "printerStatus.resuming"),
    REMOVING_HEAD(30, "printerStatus.removingHead"),
    PURGING_HEAD(40, "printerStatus.purging"),
    CANCELLING(50, "printerStatus.cancelling"),
    EJECTING_FILAMENT(60, "printerStatus.ejectingFilament"),
    ERROR(90, "PrintQueue.Error");

    private final int statusValue;
    private final String i18nString;

    private PrinterStatus(int statusValue, String i18nString)
    {
        this.statusValue = statusValue;
        this.i18nString = i18nString;
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
    public String getI18nString()
    {
        return Lookup.i18n(i18nString);
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return getI18nString();
    }
}
