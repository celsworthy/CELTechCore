package celtech.appManager;

import celtech.Lookup;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.utilityPanels.MaintenancePanelController;
import celtech.printerControl.model.Printer;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.model.PrinterException;
import javafx.application.Platform;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author Ian
 */
public class SystemNotificationManager
{

    private Stenographer steno = StenographerFactory.getStenographer(SystemNotificationManager.class.getName());
    private boolean errorDialogOnDisplay = false;
    private static boolean sdDialogOnDisplay = false;

    /*
     * Error dialog
     */
    private static Dialogs.CommandLink clearOnly = null;
    private static Dialogs.CommandLink clearAndContinue = null;
    private static Dialogs.CommandLink abortJob = null;

    /*
     * Calibration dialog
     */
    private static Dialogs.CommandLink okCalibrate = null;
    private static Dialogs.CommandLink dontCalibrate = null;

    private void showErrorNotification(String title, String message)
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                Notifier.showErrorNotification(title, message);
            }
        });
    }

    private void showWarningNotification(String title, String message)
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                Notifier.showWarningNotification(title, message);
            }
        });
    }

    private void showInformationNotification(String title, String message)
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                Notifier.showInformationNotification(title, message);
            }
        });
    }

    public void processErrorPacketFromPrinter(AckResponse response, Printer printer)
    {
        if (clearOnly == null)
        {
            clearOnly = new Dialogs.CommandLink(Lookup.i18n("dialogs.error.clearOnly"), null);
            clearAndContinue = new Dialogs.CommandLink(Lookup.i18n("dialogs.error.clearAndContinue"), null);
            abortJob = new Dialogs.CommandLink(Lookup.i18n("dialogs.error.abortJob"), null);
        }

        Platform.runLater(() ->
        {
            if (response.isError()
                && !errorDialogOnDisplay)
            {
                errorDialogOnDisplay = true;
                
                Action errorHandlingResponse = null;
                
                if (printer.printerStatusProperty().get() != PrinterStatus.IDLE
                    && printer.printerStatusProperty().get() != PrinterStatus.ERROR)
                {
                    errorHandlingResponse = Dialogs.create().title(
                        DisplayManager.getLanguageBundle().getString(
                            "dialogs.error.errorEncountered"))
                        .message(response.getErrorsAsString())
                        .masthead(null)
                        .showCommandLinks(clearAndContinue, clearAndContinue, abortJob);
                } else
                {
                    errorHandlingResponse = Dialogs.create().title(
                        DisplayManager.getLanguageBundle().getString(
                            "dialogs.error.errorEncountered"))
                        .message(response.getErrorsAsString())
                        .masthead(null)
                        .showCommandLinks(clearOnly, clearOnly);
                }
                
                try
                {
                    printer.transmitResetErrors();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Couldn't reset errors after error detection");
                }
                
                if (errorHandlingResponse == abortJob)
                {
                    try
                    {
                        if (printer.getCanPauseProperty().get())
                        {
                            printer.pause();
                        }
                        printer.cancel(null);
                    } catch (PrinterException ex)
                    {
                        steno.error("Error whilst cancelling print from error dialog");
                    }
                }
                
                errorDialogOnDisplay = false;
            }
        });
    }

    public void showCalibrationDialogue()
    {
        if (okCalibrate == null)
        {
            okCalibrate = new Dialogs.CommandLink(Lookup.i18n("dialogs.headUpdateCalibrationYes"), null);
            dontCalibrate = new Dialogs.CommandLink(Lookup.i18n("dialogs.headUpdateCalibrationNo"), null);
        }

        Platform.runLater(() ->
        {
            Action calibrationResponse = Dialogs.create().title(Lookup.i18n("dialogs.headUpdateCalibrationRequiredTitle"))
                .message(Lookup.i18n("dialogs.headUpdateCalibrationRequiredInstruction"))
                .masthead(null)
                .showCommandLinks(okCalibrate, okCalibrate, dontCalibrate);
            
            if (calibrationResponse == okCalibrate)
            {
                MaintenancePanelController.calibrateBAction();
                MaintenancePanelController.calibrateZOffsetAction();
            }
        });
    }

    public void showHeadUpdatedNotification()
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                Notifier.showInformationNotification(Lookup.i18n("notification.headSettingsUpdatedTitle"),
                                                     Lookup.i18n("notification.noActionRequired"));
            }
        });
    }

    public void showSDCardNotification()
    {
        if (!sdDialogOnDisplay)
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    sdDialogOnDisplay = true;
                    Notifier.showErrorNotification(Lookup.i18n("dialogs.noSDCardTitle"),
                                                   Lookup.i18n("dialogs.noSDCardMessage"));
                    sdDialogOnDisplay = false;
                }
            });
        }
    }

    public void showSliceFailedNotification()
    {
        showErrorNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.sliceFailed"));
    }

    public void showSliceSuccessfulNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.sliceSuccessful"));
    }

    public void showGCodePostProcessFailedNotification()
    {
        showErrorNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.gcodePostProcessFailed"));
    }

    public void showGCodePostProcessSuccessfulNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.gcodePostProcessSuccessful"));
    }

    public void showPrintJobCancelledNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.printJobCancelled"));
    }

    public void showPrintJobFailedNotification()
    {
        showErrorNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.printJobFailed"));
    }

    public void showPrintTransferSuccessfulNotification(String printerName)
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.printTransferredSuccessfully")
                                    + " "
                                    + printerName + "\n"
                                    + Lookup.i18n("notification.printTransferredSuccessfullyEnd"));
    }

    public void showPrintTransferInitiatedNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.printTransferInitiated"));
    }

    public void showReprintStartedNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.reprintInitiated"));
    }

    public void showDetectedPrintInProgressNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.activePrintDetected"));
    }
}
