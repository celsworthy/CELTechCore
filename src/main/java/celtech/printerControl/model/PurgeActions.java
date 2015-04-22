/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.configuration.Filament;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.services.purge.PurgePrinterErrorHandler;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.Cancellable;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class PurgeActions
{

    public class PurgeException extends Exception
    {

        public PurgeException(String message)
        {
            super(message);
        }
    }

    private final Stenographer steno = StenographerFactory.getStenographer(
        PurgeActions.class.getName());

    private final Printer printer;
    private final Cancellable cancellable = new Cancellable();
    private PurgePrinterErrorHandler printerErrorHandler;

    private HeadEEPROMDataResponse savedHeadData;

    private float reelNozzleTemperature = 0;
    private int lastDisplayTemperature = 0;
    private int currentDisplayTemperature = 0;
    private int purgeTemperature;

    /**
     * The filament that will be used during the purge, either the filament on the current reel or a
     * custom filament loaded on the SettingsScreen that will be used for a print that has been
     * requested.
     */
    private Filament purgeFilament;

    PurgeActions(Printer printer)
    {
        this.printer = printer;
    }

    private void resetPrinter() throws PrinterException
    {
        printer.gotoNozzlePosition(0);
        printer.switchBedHeaterOff();
        switchHeatersAndHeadLightOff();
    }

    public void doInitialiseAction() throws RoboxCommsException
    {

        printerErrorHandler = new PurgePrinterErrorHandler(printer, cancellable);
        printerErrorHandler.registerForPrinterErrors();

        // put the write after the purge routine once the firmware no longer raises an error whilst connected to the host computer
        //TODO make PURGE work for dual material head
        savedHeadData = printer.readHeadEEPROM();

        // The nozzle should be heated to a temperature halfway between the last
        //temperature stored on the head and the current required temperature stored
        // on the reel
        if (purgeFilament != null)
        {
            reelNozzleTemperature = purgeFilament.getNozzleTemperature();
        } else
        {
            //TODO modify for multiple reels
            reelNozzleTemperature = (float) printer.reelsProperty().get(0).
                nozzleTemperatureProperty().get();
        }

        float temperatureDifference = reelNozzleTemperature
            - savedHeadData.getLastFilamentTemperature();
        lastDisplayTemperature = (int) savedHeadData.getLastFilamentTemperature();
        currentDisplayTemperature = (int) reelNozzleTemperature;
        purgeTemperature = (int) Math.min(savedHeadData.getMaximumTemperature(),
                                          Math.max(180.0,
                                                   savedHeadData.
                                                   getLastFilamentTemperature()
                                                   + (temperatureDifference / 2)));

    }

    void doHeatingAction() throws InterruptedException, PurgeException
    {
        //Set the bed to 90 degrees C
        int desiredBedTemperature = 90;
        printer.setBedTargetTemperature(desiredBedTemperature);
        printer.goToTargetBedTemperature();
        boolean bedHeatFailed = PrinterUtils.waitUntilTemperatureIsReached(
            printer.getPrinterAncillarySystems().bedTemperatureProperty(), null,
            desiredBedTemperature, 5, 600, cancellable);

        if (bedHeatFailed)
        {
            throw new PurgeException("Bed heat failed");
        }

        printer.setNozzleTargetTemperature(purgeTemperature);
        printer.goToTargetNozzleTemperature();
        //TODO modify to support multiple heaters
        boolean extruderHeatFailed = PrinterUtils.waitUntilTemperatureIsReached(
                printer.headProperty().get().getNozzleHeaters().get(0).
                nozzleTemperatureProperty(),
                null, purgeTemperature, 5, 300, cancellable);

        if (extruderHeatFailed)
        {
            throw new PurgeException("Extruder heat failed");
        }
    }

    void doRunPurgeAction() throws PrinterException
    {
        printer.executeMacro("Purge Material");
        PrinterUtils.waitOnMacroFinished(printer, cancellable);
    }

    public void doFinishedAction() throws RoboxCommsException, PrinterException
    {
        resetPrinter();
        printer.transmitWriteHeadEEPROM(
            savedHeadData.getTypeCode(),
            savedHeadData.getUniqueID(),
            savedHeadData.getMaximumTemperature(),
            savedHeadData.getBeta(),
            savedHeadData.getTCal(),
            savedHeadData.getNozzle1XOffset(),
            savedHeadData.getNozzle1YOffset(),
            savedHeadData.getNozzle1ZOffset(),
            savedHeadData.getNozzle1BOffset(),
            savedHeadData.getNozzle2XOffset(),
            savedHeadData.getNozzle2YOffset(),
            savedHeadData.getNozzle2ZOffset(),
            savedHeadData.getNozzle2BOffset(),
            reelNozzleTemperature,
            savedHeadData.getHeadHours());
        printer.readHeadEEPROM();
        printer.setPrinterStatus(PrinterStatus.IDLE);
        deregisterPrinterErrorHandler();
    }

    public void doFailedAction() throws RoboxCommsException, PrinterException
    {
        resetPrinter();
        try
        {
            if (printer.canCancelProperty().get())
            {
                printer.cancel(null);
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to cancel print - " + ex.getMessage());
        }
        deregisterPrinterErrorHandler();
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    private void deregisterPrinterErrorHandler()
    {
        try
        {
            printerErrorHandler.deregisterForPrinterErrors();
        } catch (Exception ex)
        {
            steno.error("Error deregistering printer handler");
        }
    }

    public void cancel() throws RoboxCommsException, PrinterException
    {
        deregisterPrinterErrorHandler();
        cancellable.cancelled.set(true);
        try
        {
            // wait for any current actions to respect cancelled flag
            Thread.sleep(500);
        } catch (InterruptedException ex)
        {
            steno.warning("interrupted during wait of cancel");
        }
        doFailedAction();
    }

    private void switchHeatersAndHeadLightOff() throws PrinterException
    {
        printer.switchAllNozzleHeatersOff();
        printer.switchOffHeadLEDs();
    }

    public int getLastMaterialTemperature()
    {
        return lastDisplayTemperature;
    }

    public int getCurrentMaterialTemperature()
    {
        return currentDisplayTemperature;
    }

    public int getPurgeTemperature()
    {
        return purgeTemperature;
    }

    public void setPurgeTemperature(int newPurgeTemperature)
    {
        purgeTemperature = newPurgeTemperature;
    }

}
