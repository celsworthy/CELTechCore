/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils;

import celtech.appManager.Project;
import celtech.appManager.TaskController;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.Head;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.services.purge.PurgeTask;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import java.util.ResourceBundle;
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
            while ((printerToCheck.getPrintQueue().isConsideringPrintRequest() == true || printerToCheck.getPrintQueue().getPrintStatus() != PrinterStatusEnumeration.IDLE) && task.isCancelled() == false && !TaskController.isShuttingDown())
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
            while ((printerToCheck.getPrintQueue().isConsideringPrintRequest() == true || printerToCheck.getPrintQueue().getPrintStatus() != PrinterStatusEnumeration.IDLE) && !TaskController.isShuttingDown())
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

                while (response.isBusyStatus() == true && !task.isCancelled() && !TaskController.isShuttingDown())
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
     * @param printer
     * @return
     */
    public boolean isPurgeNecessary(Printer printer)
    {
        boolean purgeIsNecessary = false;

        // A reel is attached - check to see if the temperature is different from that stored on the head
        if (Math.abs(printer.getNozzleTargetTemperature() - printer.getLastFilamentTemperature().get()) > ApplicationConfiguration.maxPermittedTempDifferenceForPurge)
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

    /**
     *
     * @param project
     * @param filament
     * @param printQuality
     * @param settings
     * @param printerToUse
     */
    public static void runPurge(Project project, Filament filament, PrintQualityEnumeration printQuality, RoboxProfile settings, Printer printerToUse)
    {
        PurgeTask purgeTask = new PurgeTask(project, filament, printQuality, settings, printerToUse);
        TaskController.getInstance().manageTask(purgeTask);
        Thread purgeThread = new Thread(purgeTask);
        purgeThread.setName("Purge and Print Task");
        purgeThread.start();
    }

    /**
     *
     * @param printerToUse
     */
    public static void runPurge(Printer printerToUse)
    {
        PurgeTask purgeTask = new PurgeTask(printerToUse);
        TaskController.getInstance().manageTask(purgeTask);
        Thread purgeThread = new Thread(purgeTask);
        purgeThread.setName("Purge Task");
        purgeThread.start();
    }

    /**
     *
     * @param printerToUse
     * @param macroName
     */
    public static void runPurge(Printer printerToUse, String macroName)
    {
        PurgeTask purgeTask = new PurgeTask(printerToUse, macroName);
        TaskController.getInstance().manageTask(purgeTask);
        Thread purgeThread = new Thread(purgeTask);
        purgeThread.setName("Purge Task");
        purgeThread.start();
    }
}
