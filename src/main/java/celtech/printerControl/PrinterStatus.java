package celtech.printerControl;

import celtech.Lookup;

/**
 *
 * @author ianhudson
 */
public enum PrinterStatus
{

    IDLE("printerStatus.idle"),
    PRINTING("printerStatus.printing"),
    PRINTING_GCODE("printerStatus.printingGCode"),
    RUNNING_TEST("printerStatus.runningTest"),
    RUNNING_MACRO("printerStatus.executingMacro"),
    REMOVING_HEAD("printerStatus.removingHead"),
    PURGING_HEAD("printerStatus.purging"),
    OPENING_DOOR("printerStatus.openingDoor"),
    CALIBRATING_NOZZLE_ALIGNMENT("printerStatus.calibratingNozzleAlignment"),
    CALIBRATING_NOZZLE_HEIGHT("printerStatus.calibratingNozzleHeight"),
    CALIBRATING_NOZZLE_OPENING("printerStatus.calibratingNozzleOpening");

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
