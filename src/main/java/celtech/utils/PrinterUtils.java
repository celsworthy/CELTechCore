/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.utils;

import celtech.coreUI.DisplayManager;
import celtech.printerControl.Printer;
import celtech.printerControl.PrinterStatusEnumeration;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.HeadEEPROMDataResponse;
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
            while ((printerToCheck.getPrintQueue().isConsideringPrintRequest() == true || printerToCheck.getPrintQueue().getPrintStatus() != PrinterStatusEnumeration.IDLE) && task.isCancelled() == false)
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
            while ((printerToCheck.getPrintQueue().isConsideringPrintRequest() == true || printerToCheck.getPrintQueue().getPrintStatus() != PrinterStatusEnumeration.IDLE))
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
    
    public boolean isPurgeNecessary(Printer printer)
    {
        return Math.abs(printer.getReelNozzleTemperature().get() - printer.getLastFilamentTemperature().get()) > 5;
    }

    public void offerPurgeIfNecessary(Printer printer)
    {
        if (isPurgeNecessary(printer) && purgeDialogVisible == false)
        {
            purgeDialogVisible = true;

            Action nozzleFlushResponse = Dialogs.create().title(i18nBundle.getString("dialogs.purgeRequiredTitle"))
                    .message(i18nBundle.getString("dialogs.purgeRequiredInstruction"))
                    .masthead(null)
                    .showCommandLinks(goForPurge, goForPurge, dontGoForPurge);
            try
            {
                if (nozzleFlushResponse == goForPurge)
                {
                    printer.transmitStoredGCode("Purge Material");
                    HeadEEPROMDataResponse savedHeadData = printer.transmitReadHeadEEPROM();
                    printer.transmitWriteHeadEEPROM(savedHeadData.getTypeCode(),
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
                                                    (float) (printer.getReelNozzleTemperature().get()),
                                                    savedHeadData.getHeadHours());
                }
                purgeDialogVisible = false;
            } catch (RoboxCommsException ex)
            {
                purgeDialogVisible = false;
                steno.error("Error running purge routine");
            }
        }
    }
}
