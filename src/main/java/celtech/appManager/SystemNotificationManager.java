package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.PauseStatus;
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
    private static Dialogs.CommandLink clearOnly;
    private static Dialogs.CommandLink clearAndContinue;
    private static Dialogs.CommandLink abortJob;

    /*
     * Calibration dialog
     */
    private static Dialogs.CommandLink okCalibrate;
    private static Dialogs.CommandLink dontCalibrate;

    /*
     * Print Notifications
     */
    private String printTransferInitiatedNotification = null;
    private String printTransferSuccessfulNotification = null;
    private String printTransferSuccessfulNotificationEnd = null;
    private String printJobCancelledNotification = null;
    private String printJobFailedNotification = null;
    private String sliceSuccessfulNotification = null;
    private String sliceFailedNotification = null;
    private String gcodePostProcessSuccessfulNotification = null;
    private String gcodePostProcessFailedNotification = null;
    private String detectedPrintInProgressNotification = null;
    private String printQueueNotificationTitle = null;
    private String reprintInitiatedNotification = null;

    public SystemNotificationManager()
    {
        clearOnly = new Dialogs.CommandLink(Lookup.i18n("dialogs.error.clearOnly"), null);
        clearAndContinue = new Dialogs.CommandLink(Lookup.i18n("dialogs.error.clearAndContinue"), null);
        abortJob = new Dialogs.CommandLink(Lookup.i18n("dialogs.error.abortJob"), null);

        okCalibrate = new Dialogs.CommandLink(Lookup.i18n("dialogs.headUpdateCalibrationYes"), null);
        dontCalibrate = new Dialogs.CommandLink(Lookup.i18n("dialogs.headUpdateCalibrationNo"), null);

        printTransferInitiatedNotification = Lookup.i18n("notification.printTransferInitiated");
        printTransferSuccessfulNotification = Lookup.i18n("notification.printTransferredSuccessfully");
        printTransferSuccessfulNotificationEnd = Lookup.i18n("notification.printTransferredSuccessfullyEnd");
        printJobCancelledNotification = Lookup.i18n("notification.printJobCancelled");
        printJobFailedNotification = Lookup.i18n("notification.printJobFailed");
        sliceSuccessfulNotification = Lookup.i18n("notification.sliceSuccessful");
        sliceFailedNotification = Lookup.i18n("notification.sliceFailed");
        gcodePostProcessSuccessfulNotification = Lookup.i18n("notification.gcodePostProcessSuccessful");
        gcodePostProcessFailedNotification = Lookup.i18n("notification.gcodePostProcessFailed");
        printQueueNotificationTitle = Lookup.i18n("notification.PrintQueueTitle");
        detectedPrintInProgressNotification = Lookup.i18n("notification.activePrintDetected");
        reprintInitiatedNotification = Lookup.i18n("notification.reprintInitiated");
    }

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
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
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
            }
        });
    }

    public void showCalibrationDialogue()
    {
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
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
        showErrorNotification(printQueueNotificationTitle, sliceFailedNotification);
    }

    public void showSliceSuccessfulNotification()
    {
        showInformationNotification(printQueueNotificationTitle, sliceSuccessfulNotification);
    }

    public void showGCodePostProcessFailedNotification()
    {
        showErrorNotification(printQueueNotificationTitle, gcodePostProcessFailedNotification);
    }

    public void showGCodePostProcessSuccessfulNotification()
    {
        showInformationNotification(printQueueNotificationTitle, gcodePostProcessSuccessfulNotification);
    }

    public void showPrintJobCancelledNotification()
    {
        showInformationNotification(printQueueNotificationTitle, printJobCancelledNotification);
    }

    public void showPrintJobFailedNotification()
    {
        showErrorNotification(printQueueNotificationTitle, printJobFailedNotification);
    }

    public void showPrintTransferSuccessfulNotification(String printerName)
    {
        showInformationNotification(printQueueNotificationTitle,
                                    printTransferSuccessfulNotification + " "
                                    + printerName + "\n"
                                    + printTransferSuccessfulNotificationEnd);
    }

    public void showPrintTransferInitiatedNotification()
    {
        showInformationNotification(printQueueNotificationTitle, printTransferInitiatedNotification);
    }

    public void showReprintStartedNotification()
    {
        showInformationNotification(printQueueNotificationTitle, reprintInitiatedNotification);
    }

    public void showDetectedPrintInProgressNotification()
    {
        showInformationNotification(printQueueNotificationTitle, detectedPrintInProgressNotification);
    }
}
