/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils;

import celtech.appManager.Project;
import celtech.appManager.TaskController;
import celtech.configuration.Filament;
import celtech.coreUI.DisplayManager;
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

    private PrinterUtils()
    {
        i18nBundle = DisplayManager.getLanguageBundle();
        goForPurge = new Dialogs.CommandLink(i18nBundle.getString("dialogs.goForPurgeTitle"), i18nBundle.getString("dialogs.goForPurgeInstruction"));
        dontGoForPurge = new Dialogs.CommandLink(i18nBundle.getString("dialogs.dontGoForPurgeTitle"), i18nBundle.getString("dialogs.dontGoForPurgeInstruction"));
    }

    public static PrinterUtils getInstance()
    {
        if (instance == null)
        {
            instance = new PrinterUtils();
        }

        return instance;
    }

    public static void waitOnMacroFinished(Printer printerToCheck, Task task)
    {
        if (task != null)
        {
            while ((printerToCheck.getPrintQueue().isConsideringPrintRequest() == true || printerToCheck.getPrintQueue().getPrintStatus() != PrinterStatusEnumeration.IDLE) && task.isCancelled() == false && !TaskController.isShuttingDown())
            {
                try
                {
                    Thread.sleep(100);
                } catch (InterruptedException ex)
                {
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
                    steno.error("Interrupted whilst waiting on Macro");
                }
            }
        }
    }

    public static void waitOnBusy(Printer printerToCheck, Task task)
    {
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
            } catch (InterruptedException ex)
            {
                steno.error("Interrupted during busy check");
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
            } catch (InterruptedException ex)
            {
                steno.error("Interrupted during busy check");
            }
        }

    }

    public boolean isPurgeNecessary(Printer printer)
    {
        return Math.abs(printer.getReelNozzleTemperature().get() - printer.getLastFilamentTemperature().get()) > 5;
    }

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

    public static void runPurge(Project project, Filament filament, PrintQualityEnumeration printQuality, RoboxProfile settings, Printer printerToUse)
    {
        PurgeTask purgeTask = new PurgeTask(project, filament, printQuality, settings, printerToUse);
        TaskController.getInstance().manageTask(purgeTask);
        Thread purgeThread = new Thread(purgeTask);
        purgeThread.setName("Purge and Print Task");
        purgeThread.start();
    }

    public static void runPurge(Printer printerToUse)
    {
        PurgeTask purgeTask = new PurgeTask(printerToUse);
        TaskController.getInstance().manageTask(purgeTask);
        Thread purgeThread = new Thread(purgeTask);
        purgeThread.setName("Purge Task");
        purgeThread.start();
    }
}
