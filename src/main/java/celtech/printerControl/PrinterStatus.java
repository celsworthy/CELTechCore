package celtech.printerControl;

import celtech.Lookup;
import java.util.Optional;

/**
 *
 * @author ianhudson
 */
public enum PrinterStatus
{
    IDLE("printerStatus.idle"),
    SLICING("printerStatus.slicing"),
    POST_PROCESSING("printerStatus.postProcessing"),
    SENDING_TO_PRINTER("printerStatus.sendingToPrinter"),
    PRINTING("printerStatus.printing"),
    EXECUTING_MACRO("printerStatus.executingMacro"),
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
    ERROR("printerStatus.error");

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
