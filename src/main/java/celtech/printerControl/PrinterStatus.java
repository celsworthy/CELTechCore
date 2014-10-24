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
    IDLE("printerStatus.idle"),
    SLICING("PrintQueue.Slicing"),
    POST_PROCESSING("PrintQueue.PostProcessing"),
    SENDING_TO_PRINTER("PrintQueue.SendingToPrinter"),
    PRINTING("printerStatus.printing"),
    EXECUTING_MACRO("PrintQueue.ExecutingMacro"),
    PAUSING("printerStatus.pausing"),
    PAUSED("printerStatus.paused"),
    RESUMING("printerStatus.resuming"),
    REMOVING_HEAD("printerStatus.removingHead"),
    PURGING_HEAD("printerStatus.purging"),
    CANCELLING("printerStatus.cancelling"),
    EJECTING_FILAMENT("printerStatus.ejectingFilament"),
    OPENING_DOOR("printerStatus.openingDoor"),
    CALIBRATING_NOZZLE_ALIGNMENT("printerStatus.calibratingNozzleAlignment"),
    CALIBRATING_NOZZLE_HEIGHT("printerStatus.calibratingNozzleHeight"),
    CALIBRATING_NOZZLE_OPENING("printerStatus.calibratingNozzleOpening"),
    ERROR("PrintQueue.Error");

    private final String i18nString;

    private PrinterStatus(String i18nString)
    {
        this.i18nString = i18nString;
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
