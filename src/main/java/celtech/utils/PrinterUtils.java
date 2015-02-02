/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils;

import celtech.Lookup;
import celtech.appManager.PurgeResponse;
import celtech.appManager.TaskController;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.model.Printer;
import celtech.utils.tasks.Cancellable;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PrinterUtils
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
        PrinterUtils.class.getName());
    private static PrinterUtils instance = null;
    private boolean purgeDialogVisible = false;

    private PrinterUtils()
    {
    }

    /**
     *
     * @return
     */
    public static PrinterUtils getInstance()
    {
        if (instance == null)
        {
            instance = new PrinterUtils();
        }

        return instance;
    }

    /**
     *
     * @param printerToCheck
     * @param task
     * @return interrupted
     */
    public static boolean waitOnMacroFinished(Printer printerToCheck, Task task)
    {
        boolean interrupted = false;

        if (Platform.isFxApplicationThread())
        {
            throw new RuntimeException("Cannot call this function from the GUI thread");
        }
        // we need to wait here because it takes a little while before status changes
        // away from IDLE
        try
        {
            Thread.sleep(1500);
        } catch (InterruptedException ex)
        {
            interrupted = true;
            steno.error("Interrupted whilst waiting on Macro");
        }

        if (task != null && ! interrupted)
        {
            while (printerToCheck.printerStatusProperty().get() != PrinterStatus.IDLE
                && task.isCancelled() == false && !TaskController.isShuttingDown())
            {
                try
                {
                    Thread.sleep(100);
                } catch (InterruptedException ex)
                {
                    interrupted = true;
                    steno.error("Interrupted whilst waiting on Macro");
                }
            }
        } else
        {
            while (printerToCheck.printerStatusProperty().get() != PrinterStatus.IDLE
                && !TaskController.isShuttingDown())
            {
                try
                {
                    Thread.sleep(100);
                } catch (InterruptedException ex)
                {
                    interrupted = true;
                    steno.error("Interrupted whilst waiting on Macro");
                }
            }
        }
        return interrupted;
    }

    /**
     *
     * @param printerToCheck
     * @param cancellable
     * @return failed
     */
    public static boolean waitOnMacroFinished(Printer printerToCheck, Cancellable cancellable)
    {
        boolean failed = false;
        
        if (Platform.isFxApplicationThread())
        {
            throw new RuntimeException("Cannot call this function from the GUI thread");
        }
        // we need to wait here because it takes a little while before status changes
        // away from IDLE
        try
        {
            Thread.sleep(1500);
        } catch (InterruptedException ex)
        {
            failed = true;
            steno.error("Interrupted whilst waiting on Macro");
        }        

        while (printerToCheck.printJobIDIndicatesPrinting()
            && !TaskController.isShuttingDown())
        {
            try
            {
                Thread.sleep(100);

                if (cancellable != null && cancellable.cancelled)
                {
                    failed = true;
                    break;
                }
            } catch (InterruptedException ex)
            {
                failed = true;
                steno.error("Interrupted whilst waiting on Macro");
            }
        }

        return failed;
    }

    /**
     *
     * @param printerToCheck
     * @param task
     * @return failed
     */
    public static boolean waitOnBusy(Printer printerToCheck, Task task)
    {
        boolean failed = false;

        if (task != null)
        {
            try
            {
                StatusResponse response = printerToCheck.transmitStatusRequest();

                while (response.isBusyStatus() == true && !TaskController.isShuttingDown())
                {
                    Thread.sleep(100);
                    response = printerToCheck.transmitStatusRequest();

                    if (task.isCancelled())
                    {
                        failed = true;
                        break;
                    }
                }
            } catch (RoboxCommsException ex)
            {
                steno.error("Error requesting status");
                failed = true;
            } catch (InterruptedException ex)
            {
                steno.error("Interrupted during busy check");
                failed = true;
            }
        } else
        {
            try
            {
                StatusResponse response = printerToCheck.transmitStatusRequest();

                while (response.isBusyStatus() == true && !TaskController.isShuttingDown())
                {
                    Thread.sleep(100);
                    response = printerToCheck.transmitStatusRequest();
                }
            } catch (RoboxCommsException ex)
            {
                steno.error("Error requesting status");
                failed = true;
            } catch (InterruptedException ex)
            {
                steno.error("Interrupted during busy check");
                failed = true;
            }
        }

        return failed;
    }

    /**
     *
     * @param printerToCheck
     * @param cancellable
     * @return failed
     */
    public static boolean waitOnBusy(Printer printerToCheck, Cancellable cancellable)
    {
        boolean failed = false;

        try
        {
            StatusResponse response = printerToCheck.transmitStatusRequest();

            while (response.isBusyStatus() == true && !TaskController.isShuttingDown())
            {
                Thread.sleep(100);
                response = printerToCheck.transmitStatusRequest();

                if (cancellable != null && cancellable.cancelled)
                {
                    failed = true;
                    break;
                }
            }
        } catch (RoboxCommsException ex)
        {
            steno.error("Error requesting status");
            failed = true;
        } catch (InterruptedException ex)
        {
            steno.error("Interrupted during busy check");
            failed = true;
        }

        return failed;
    }

    /**
     *
     * @param printer
     * @return
     */
    public boolean isPurgeNecessary(Printer printer)
    {
        boolean purgeIsNecessary = false;
        float targetNozzleTemperature = 0;
        SettingsScreenState settingsScreenState = SettingsScreenState.getInstance();
        Filament settingsFilament = settingsScreenState.getFilament();

        if (settingsFilament != null)
        {
            targetNozzleTemperature = settingsFilament.getNozzleTemperature();
        } else
        {
            //TODO modify to work with multiple reels
            targetNozzleTemperature = (float) printer.reelsProperty().get(0).nozzleTemperatureProperty().get();
        }

        // A reel is attached - check to see if the temperature is different from that stored on the head
        //TODO modify to work with multiple heaters
        if (Math.abs(targetNozzleTemperature
            - printer.headProperty().get().getNozzleHeaters().get(0).lastFilamentTemperatureProperty().get())
            > ApplicationConfiguration.maxPermittedTempDifferenceForPurge)
        {
            purgeIsNecessary = true;
        }

        return purgeIsNecessary;
    }

    /**
     *
     * @param printer
     * @return
     */
    public PurgeResponse offerPurgeIfNecessary(Printer printer)
    {
        PurgeResponse purgeConsent = PurgeResponse.NOT_NECESSARY;
        if (isPurgeNecessary(printer) && purgeDialogVisible == false)
        {
            purgeDialogVisible = true;

            purgeConsent = Lookup.getSystemNotificationHandler().showPurgeDialog();
            
            purgeDialogVisible = false;
        }

        return purgeConsent;
    }
    
    public static boolean waitUntilTemperatureIsReached(ReadOnlyIntegerProperty temperatureProperty,
        Task task, int temperature, int tolerance, int timeoutSec) throws InterruptedException
    {
        return waitUntilTemperatureIsReached(temperatureProperty,
            task, temperature, tolerance, timeoutSec, (Cancellable) null);
    }

    public static boolean waitUntilTemperatureIsReached(ReadOnlyIntegerProperty temperatureProperty,
        Task task, int temperature, int tolerance, int timeoutSec, Cancellable cancellable) throws InterruptedException
    {
        boolean failed = false;

        int minTemp = temperature - tolerance;
        int maxTemp = temperature + tolerance;
        long timestampAtStart = System.currentTimeMillis();
        long timeoutMillis = timeoutSec * 1000;

        if (task != null || cancellable != null)
        {
            try
            {
                while ((temperatureProperty.get() < minTemp
                    || temperatureProperty.get() > maxTemp))
                {
                    if (task != null && task.isCancelled() ||
                       (cancellable != null && cancellable.cancelled)) {
                        break;
                    }
                    Thread.sleep(100);

                    long currentTimeMillis = System.currentTimeMillis();
                    if ((currentTimeMillis - timestampAtStart) >= timeoutMillis)
                    {
                        failed = true;
                        break;
                    }
                }
            } catch (InterruptedException ex)
            {
                steno.error("Interrupted during busy check");
                failed = true;
            }
        } else
        {
            try
            {
                while (temperatureProperty.get() < minTemp
                    || temperatureProperty.get() > maxTemp)
                {
                    Thread.sleep(100);

                    long currentTimeMillis = System.currentTimeMillis();
                    if ((currentTimeMillis - timestampAtStart) >= timeoutMillis)
                    {
                        failed = true;
                        break;
                    }
                }
            } catch (InterruptedException ex)
            {
                steno.error("Interrupted during busy check");
                failed = true;
            }
        }

        return failed;
    }

    public static float deriveNozzle1OverrunFromOffsets(float nozzle1Offset, float nozzle2Offset)
    {
        float delta = nozzle2Offset - nozzle1Offset;
        float halfdelta = delta / 2;

        float nozzle1Overrun = -(nozzle1Offset + halfdelta);
        float nozzle2Overrun = nozzle1Overrun + delta;

        return nozzle1Overrun;
    }

    public static float deriveNozzle2OverrunFromOffsets(float nozzle1Offset, float nozzle2Offset)
    {
        float delta = nozzle2Offset - nozzle1Offset;
        float halfdelta = delta / 2;

        float nozzle1Overrun = -(nozzle1Offset + halfdelta);
        float nozzle2Overrun = nozzle1Overrun + delta;

        return nozzle2Overrun;
    }

    public static float deriveNozzle1ZOffsetsFromOverrun(float nozzle1OverrunValue,
        float nozzle2OverrunValue)
    {
        float offsetAverage = -nozzle1OverrunValue;
        float delta = (nozzle2OverrunValue - nozzle1OverrunValue) / 2;
        float nozzle1Offset = offsetAverage - delta;
        float nozzle2Offset = offsetAverage + delta;

        return nozzle1Offset;
    }

    public static float deriveNozzle2ZOffsetsFromOverrun(float nozzle1OverrunValue,
        float nozzle2OverrunValue)
    {
        float offsetAverage = -nozzle1OverrunValue;
        float delta = (nozzle2OverrunValue - nozzle1OverrunValue) / 2;
        float nozzle1Offset = offsetAverage - delta;
        float nozzle2Offset = offsetAverage + delta;

        return nozzle2Offset;
    }

}
