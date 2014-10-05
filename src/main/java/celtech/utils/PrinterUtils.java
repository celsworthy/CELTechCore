/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils;

import celtech.appManager.TaskController;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.printerControl.model.Printer;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.utils.tasks.Cancellable;
import java.util.ResourceBundle;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author Ian
 */
public class PrinterUtils
{

    private static final Stenographer steno = StenographerFactory.getStenographer(PrinterUtils.class.getName());
    private static PrinterUtils instance = null;
    private static ResourceBundle i18nBundle = null;
    private Dialogs.CommandLink goForPurge = null;
    private Dialogs.CommandLink dontGoForPurge = null;
    private boolean purgeDialogVisible = false;
    private SettingsScreenState settingsScreenState = null;

    private PrinterUtils()
    {
        i18nBundle = DisplayManager.getLanguageBundle();
        goForPurge = new Dialogs.CommandLink(i18nBundle.getString("dialogs.goForPurgeTitle"), i18nBundle.getString("dialogs.goForPurgeInstruction"));
        dontGoForPurge = new Dialogs.CommandLink(i18nBundle.getString("dialogs.dontGoForPurgeTitle"), i18nBundle.getString("dialogs.dontGoForPurgeInstruction"));

        settingsScreenState = SettingsScreenState.getInstance();
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

        if (task != null)
        {
            while (printerToCheck.getPrinterStatusProperty().get() != PrinterStatus.IDLE
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
            while (printerToCheck.getPrinterStatusProperty().get() != PrinterStatus.IDLE && !TaskController.
                isShuttingDown())
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

        while ((printerToCheck.getPrintQueue().isConsideringPrintRequest() == true
            || printerToCheck.getPrintQueue().getPrintStatus() != PrinterStatus.IDLE)
            && !TaskController.isShuttingDown())
        {
            try
            {
                Thread.sleep(100);
                
                if (cancellable.cancelled)
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

                if (cancellable.cancelled)
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
            targetNozzleTemperature = (float) printer.getReelNozzleTemperature().get();
        }

        // A reel is attached - check to see if the temperature is different from that stored on the head
        if (Math.abs(targetNozzleTemperature - printer.getLastFilamentTemperature().get()) > ApplicationConfiguration.maxPermittedTempDifferenceForPurge)
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
    public boolean offerPurgeIfNecessary(Printer printer)
    {
        boolean purgeConsent = false;
        if (isPurgeNecessary(printer) && purgeDialogVisible == false)
        {
            purgeDialogVisible = true;

            Action nozzleFlushResponse = Dialogs.create().title(i18nBundle.getString("dialogs.purgeRequiredTitle"))
                .message(i18nBundle.getString("dialogs.purgeRequiredInstruction"))
                .masthead(null)
                .showCommandLinks(goForPurge, goForPurge, dontGoForPurge);

            if (nozzleFlushResponse == goForPurge)
            {
                purgeConsent = true;
            }
            purgeDialogVisible = false;
        }

        return purgeConsent;
    }

    public static boolean waitUntilTemperatureIsReached(ReadOnlyIntegerProperty temperatureProperty, Task task, int temperature, int tolerance, int timeoutSec) throws InterruptedException
    {
        boolean failed = false;

        int minTemp = temperature - tolerance;
        int maxTemp = temperature + tolerance;
        long timestampAtStart = System.currentTimeMillis();
        long timeoutMillis = timeoutSec * 1000;

        if (task != null)
        {
            try
            {
                while ((temperatureProperty.get() < minTemp
                    || temperatureProperty.get() > maxTemp)
                    && task.isCancelled() == false)
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

}
