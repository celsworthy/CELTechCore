/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.Lookup;
import celtech.configuration.Filament;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.Cancellable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javax.print.PrintException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class PurgeActions extends StateTransitionActions
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

    private PurgePrinterErrorHandler printerErrorHandler;

    private HeadEEPROMDataResponse savedHeadData;

    private float reelNozzleTemperature = 0;
    private final IntegerProperty lastDisplayTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty currentDisplayTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty purgeTemperature = new SimpleIntegerProperty(0);

    /**
     * The filament that will be used during the purge, either the filament on the current reel or a
     * custom filament loaded on the SettingsScreen that will be used for a print that has been
     * requested.
     */
    private Filament purgeFilament;
    
    private boolean failedActionPerformed = false;

    PurgeActions(Printer printer, Cancellable userCancellable, Cancellable errorCancellable)
    {
        super(userCancellable, errorCancellable);
        this.printer = printer;
        PrinterUtils.setCancelledIfPrinterDisconnected(printer, errorCancellable);
    }

    

    @Override
    public void initialise()
    {
        reelNozzleTemperature = 0;
        lastDisplayTemperature.set(0);
        currentDisplayTemperature.set(0);
        savedHeadData = null;
    }

    private void resetPrinter() throws PrinterException
    {
        printer.gotoNozzlePosition(0);
        printer.switchBedHeaterOff();
        switchHeatersAndHeadLightOff();
        
        PrinterUtils.waitOnBusy(printer, (Cancellable) null);
        try
        {
            // wait for above actions to complete so that AutoMaker does not return
            // to Status page until cancel/reset is complete
            Thread.sleep(2000);
        } catch (InterruptedException ex)
        {
            steno.error("Wait interrupted");
        }

    }

    public void doInitialiseAction() throws RoboxCommsException, PrintException
    {
        printer.setPrinterStatus(PrinterStatus.PURGING_HEAD);

        printerErrorHandler = new PurgePrinterErrorHandler(printer, errorCancellable);
        printerErrorHandler.registerForPrinterErrors();

        // put the write after the purge routine once the firmware no longer raises an error whilst connected to the host computer
        //TODO make PURGE work for dual material head
        savedHeadData = printer.readHeadEEPROM();
    }

    void doHeatingAction() throws InterruptedException, PurgeException
    {
        steno.debug("purge temperature set to " + purgeTemperature.get());
        //Set the bed to 90 degrees C
        int desiredBedTemperature = 90;
        printer.setBedTargetTemperature(desiredBedTemperature);
        printer.goToTargetBedTemperature();
        boolean bedHeatFailed = PrinterUtils.waitUntilTemperatureIsReached(
            printer.getPrinterAncillarySystems().bedTemperatureProperty(), null,
            desiredBedTemperature, 5, 600, userOrErrorCancellable);

        if (bedHeatFailed)
        {
            throw new PurgeException("Bed heat failed");
        }

        printer.setNozzleTargetTemperature(purgeTemperature.get());
        printer.goToTargetNozzleTemperature();
        //TODO modify to support multiple heaters
        boolean extruderHeatFailed = PrinterUtils.waitUntilTemperatureIsReached(
            printer.headProperty().get().getNozzleHeaters().get(0).
            nozzleTemperatureProperty(),
            null, purgeTemperature.get(), 5, 300, userOrErrorCancellable);

        if (extruderHeatFailed)
        {
            throw new PurgeException("Extruder heat failed");
        }
    }

    void doRunPurgeAction() throws PrinterException
    {
        printer.purgeMaterial(true, userOrErrorCancellable);
    }

    public void doFinishedAction() throws RoboxCommsException, PrinterException
    {
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
        resetPrinter();
        printer.setPrinterStatus(PrinterStatus.IDLE);
        openDoor();
        deregisterPrinterErrorHandler();
    }

    private void openDoor()
    {
        // needs to run on gui thread to make sure it is called after status set to idle
        Lookup.getTaskExecutor().
            runOnGUIThread(() -> {
                try
                {
                    printer.goToOpenDoorPosition(null);
                } catch (PrinterException ex)
                {
                    steno.warning("could not go to open door");
                }
            });
    }

    public void doFailedAction() throws RoboxCommsException, PrinterException
    {
        // this can be called twice if an error occurs
        if (failedActionPerformed) {
            return;
        }
        
        failedActionPerformed = true;
        
        try
        {
            abortAnyOngoingPrint();
            resetPrinter();
            deregisterPrinterErrorHandler();
        } catch (PrinterException ex)
        {
            System.out.println("Error running failed action");
        }
        printer.setPrinterStatus(PrinterStatus.IDLE);
        openDoor();
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

    private void switchHeatersAndHeadLightOff() throws PrinterException
    {
        printer.switchAllNozzleHeatersOff();
        printer.switchOffHeadLEDs();
    }

    public ReadOnlyIntegerProperty getLastMaterialTemperatureProperty()
    {
        return lastDisplayTemperature;
    }

    public ReadOnlyIntegerProperty getCurrentMaterialTemperatureProperty()
    {
        return currentDisplayTemperature;
    }

    public ReadOnlyIntegerProperty getPurgeTemperatureProperty()
    {
        return purgeTemperature;
    }

    public void setPurgeTemperature(int newPurgeTemperature)
    {
        purgeTemperature.set(newPurgeTemperature);
    }

    public void setPurgeFilament(Filament filament) throws PrintException
    {
        purgeFilament = filament;
        updatePurgeTemperature();
    }

    private void updatePurgeTemperature() throws PrintException
    {
        // The nozzle should be heated to a temperature halfway between the last
        //temperature stored on the head and the current required temperature stored
        // on the reel
        if (purgeFilament != null)
        {
            reelNozzleTemperature = purgeFilament.getNozzleTemperature();
        } else
        {
            throw new PrintException("The purge filament must be set");
        }

        if (savedHeadData != null)
        {
            float temperatureDifference = reelNozzleTemperature
                - savedHeadData.getLastFilamentTemperature();
            lastDisplayTemperature.set((int) savedHeadData.getLastFilamentTemperature());
            currentDisplayTemperature.set((int) reelNozzleTemperature);
            purgeTemperature.set((int) Math.min(savedHeadData.getMaximumTemperature(),
                                                Math.max(180.0,
                                                         savedHeadData.
                                                         getLastFilamentTemperature()
                                                         + (temperatureDifference / 2))));
        }

    }

    @Override
    /**
     * This is run immediately after the user presses the cancel button.
     */
    void whenUserCancelDetected()
    {
        abortAnyOngoingPrint();
    }

    @Override
    /**
     * This is run immediately after the printer error is detected.
     */
    void whenErrorDetected()
    {
        abortAnyOngoingPrint();
    }

    @Override
    /**
     * This is run after a Cancel or Error but not until any ongoing Action has completed / stopped.
     * We reset the printer here and not at the time of error/cancel detection because if done
     * immediately the ongoing Action could undo the effects of the reset.
     */
    void resetAfterCancelOrError()
    {
        try
        {
            doFailedAction();
        } catch (Exception ex)
        {
            ex.printStackTrace();
            steno.error("error resetting printer " + ex);
        }
    }

    private void abortAnyOngoingPrint()
    {
        try
        {
            if (printer.canCancelProperty().get())
            {
                printer.cancel(null);
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to abort purge print - " + ex.getMessage());
        }
    }

}
