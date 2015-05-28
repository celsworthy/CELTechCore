/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.configuration.Filament;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
import celtech.utils.PrinterUtils;
import celtech.utils.tasks.Cancellable;
import java.util.ArrayList;
import java.util.List;
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

    private final List<Float> reelNozzleTemperature;
    private final List<IntegerProperty> lastDisplayTemperature;
    private final List<IntegerProperty> currentDisplayTemperature;
    private final List<IntegerProperty> purgeTemperature;

    /**
     * The filament that will be used during the purge, either the filament on the current reel or a
     * custom filament loaded on the SettingsScreen that will be used for a print that has been
     * requested.
     */
    private final List<Filament> purgeFilament;

    PurgeActions(Printer printer, Cancellable userCancellable, Cancellable errorCancellable)
    {
        super(userCancellable, errorCancellable);
        this.printer = printer;
        PrinterUtils.setCancelledIfPrinterDisconnected(printer, errorCancellable);
        
        purgeTemperature = new ArrayList<>();
        lastDisplayTemperature = new ArrayList<>();
        currentDisplayTemperature = new ArrayList<>();
        reelNozzleTemperature = new ArrayList<>();
        purgeFilament = new ArrayList<>();
        for (int i = 0; i < getNumNozzleHeaters(); i++)
        {
            purgeTemperature.add(new SimpleIntegerProperty(0));
            lastDisplayTemperature.add(new SimpleIntegerProperty(0));
            currentDisplayTemperature.add(new SimpleIntegerProperty(0));
            reelNozzleTemperature.add(new Float(0));
            purgeFilament.add(null);
        }
        
    }

    @Override
    public void initialise()
    {
        
        for (int i = 0; i < getNumNozzleHeaters(); i++)
        {
            lastDisplayTemperature.get(i).set(0);
            currentDisplayTemperature.get(i).set(0);
            reelNozzleTemperature.set(i, 0f);
        }
        savedHeadData = null;
    }

    private void resetPrinter() throws PrinterException
    {
        printer.gotoNozzlePosition(0);
        printer.switchBedHeaterOff();
        switchHeatersAndHeadLightOff();
        printer.goToOpenDoorPosition(null);
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

    private int getNumNozzleHeaters()
    {
        return printer.headProperty().get().getNozzleHeaters().size();
    }

    void doHeatingAction() throws InterruptedException, PurgeException
    {
        
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

        for (int i = 0; i < getNumNozzleHeaters(); i++)
        {
            steno.debug("purge temperature set to " + purgeTemperature.get(i).get() + " for heater " + i);
            printer.setNozzleHeaterTargetTemperature(i, purgeTemperature.get(i).get());
            printer.goToTargetNozzleHeaterTemperature(i);
        }

        for (int i = 0; i < getNumNozzleHeaters(); i++)
        {
            boolean extruderHeatFailed = PrinterUtils.waitUntilTemperatureIsReached(
                printer.headProperty().get().getNozzleHeaters().get(i).
                nozzleTemperatureProperty(),
                null, purgeTemperature.get(i).get(), 5, 300, userOrErrorCancellable);

            if (extruderHeatFailed)
            {
                throw new PurgeException("Extruder heat failed");
            }
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
            reelNozzleTemperature.get(0),
            reelNozzleTemperature.get(1),
            savedHeadData.getHeadHours());
        printer.readHeadEEPROM();
        resetPrinter();
        printer.setPrinterStatus(PrinterStatus.IDLE);
        deregisterPrinterErrorHandler();
    }

    public void doFailedAction() throws RoboxCommsException, PrinterException
    {
        try
        {
            abortAnyOngoingPrint();
            resetPrinter();
        } catch (PrinterException ex)
        {
            steno.error("Error running failed action");
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

    private void switchHeatersAndHeadLightOff() throws PrinterException
    {
        printer.switchAllNozzleHeatersOff();
        printer.switchOffHeadLEDs();
    }

    public ReadOnlyIntegerProperty getLastMaterialTemperatureProperty(int nozzleHeaterNumber)
    {
        return lastDisplayTemperature.get(nozzleHeaterNumber);
    }

    public ReadOnlyIntegerProperty getCurrentMaterialTemperatureProperty(int nozzleHeaterNumber)
    {
        return currentDisplayTemperature.get(nozzleHeaterNumber);
    }

    public ReadOnlyIntegerProperty getPurgeTemperatureProperty(int nozzleHeaterNumber)
    {
        return purgeTemperature.get(nozzleHeaterNumber);
    }

    public void setPurgeTemperature(int nozzleHeaterNumber, int newPurgeTemperature)
    {
        purgeTemperature.get(nozzleHeaterNumber).set(newPurgeTemperature);
    }

    public void setPurgeFilament(int nozzleHeaterNumber, Filament filament) throws PrintException
    {
        purgeFilament.set(nozzleHeaterNumber, filament);
        updatePurgeTemperature(nozzleHeaterNumber);
    }

    private void updatePurgeTemperature(int nozzleHeaterNumber) throws PrintException
    {
        // The nozzle should be heated to a temperature halfway between the last
        //temperature stored on the head and the current required temperature stored
        // on the reel
        if (purgeFilament.get(nozzleHeaterNumber) != null)
        {
            reelNozzleTemperature.set(nozzleHeaterNumber, (float) purgeFilament.get(0).getNozzleTemperature());
        } else
        {
            throw new PrintException("The purge filament must be set");
        }

        if (savedHeadData != null)
        {
            float temperatureDifference = reelNozzleTemperature.get(nozzleHeaterNumber)
                - savedHeadData.getLastFilamentTemperature(nozzleHeaterNumber);
            lastDisplayTemperature.get(nozzleHeaterNumber).set(
                (int) savedHeadData.getLastFilamentTemperature(nozzleHeaterNumber));
            currentDisplayTemperature.get(nozzleHeaterNumber).set(
                reelNozzleTemperature.get(nozzleHeaterNumber).intValue());
            purgeTemperature.get(nozzleHeaterNumber).set((int) Math.min(savedHeadData.getMaximumTemperature(),
                    Math.max(180.0,
                             savedHeadData.getLastFilamentTemperature(nozzleHeaterNumber)
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
            resetPrinter();
            deregisterPrinterErrorHandler();
        } catch (PrinterException ex)
        {
            steno.error("Error resetting printer");
        }
        printer.setPrinterStatus(PrinterStatus.IDLE);
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
