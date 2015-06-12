package celtech.printerControl;

import celtech.Lookup;

/**
 *
 * @author ianhudson
 */
public enum PrinterStatus
{

    IDLE("printerStatus.idle"),
    HEATING_BED("printerStatus.heatingBed"),
    HEATING_NOZZLE("printerStatus.heatingNozzle"),
    SLICING("printerStatus.slicing"),
    POST_PROCESSING("printerStatus.postProcessing"),
    //    SENDING_TO_PRINTER("printerStatus.sendingToPrinter"),
    PRINTING("printerStatus.printing"),
    PRINTING_GCODE("printerStatus.printingGCode"),
    //Macros
    RUNNING_TEST("printerStatus.runningTest"),
    HOMING("printerStatus.macro.homing"),
    EJECTING_STUCK_MATERIAL("printerStatus.macro.ejectStuckMaterial"),
    LEVELLING_GANTRY("printerStatus.macro.levellingGantry"),
    TEST_X("printerStatus.macro.testX"),
    TEST_Y("printerStatus.macro.testY"),
    TEST_Z("printerStatus.macro.testZ"),
    LEVELLING_Y("printerStatus.macro.levellingY"),
    SPEED_TEST("printerStatus.macro.speedTest"),
    NOZZLE_CLEAN("printerStatus.macro.nozzleClean"),
    //
    PAUSING("printerStatus.pausing"),
    PAUSED("printerStatus.paused"),
    RESUMING("printerStatus.resuming"),
    REMOVING_HEAD("printerStatus.removingHead"),
    PURGING_HEAD("printerStatus.purging"),
    CANCELLING("printerStatus.cancelling"),
    OPENING_DOOR("printerStatus.openingDoor"),
    CALIBRATING_NOZZLE_ALIGNMENT("printerStatus.calibratingNozzleAlignment"),
    CALIBRATING_NOZZLE_HEIGHT("printerStatus.calibratingNozzleHeight"),
    CALIBRATING_NOZZLE_OPENING("printerStatus.calibratingNozzleOpening"),
    LOADING_FILAMENT("printerStatus.loadingFilament"),
    EJECTING_FILAMENT("printerStatus.ejectingFilament"),
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
