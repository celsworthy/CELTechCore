package celtech.appManager;

import celtech.Lookup;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.ProgressDialog;
import celtech.coreUI.controllers.utilityPanels.MaintenancePanelController;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.model.HardwarePrinter;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author Ian
 */
public class SystemNotificationManagerJavaFX implements SystemNotificationManager
{

    private final Stenographer steno = StenographerFactory.getStenographer(SystemNotificationManagerJavaFX.class.getName());
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

    /*
     * Firmware upgrade dialog
     */
    protected Dialogs.CommandLink firmwareUpgradeOK = null;
    protected Dialogs.CommandLink firmwareUpgradeNotOK = null;
    protected Dialogs.CommandLink firmwareDowngradeOK = null;
    protected Dialogs.CommandLink firmwareDowngradeNotOK = null;

    /*
     * Firmware upgrade progress
     */
    protected ProgressDialog firmwareUpdateProgress = null;

    /*
     * HardwarePrinter ID Dialog
     */
    protected PrinterIDDialog printerIDDialog = null;

    /*
     * SD card dialog
     */
    protected ModalDialog noSDDialog = null;

    private void showErrorNotification(String title, String message)
    {
        Platform.runLater(() ->
        {
            Notifier.showErrorNotification(title, message);
        });
    }

    private void showWarningNotification(String title, String message)
    {
        Platform.runLater(() ->
        {
            Notifier.showWarningNotification(title, message);
        });
    }

    private void showInformationNotification(String title, String message)
    {
        Platform.runLater(() ->
        {
            Notifier.showInformationNotification(title, message);
        });
    }

    @Override
    public void processErrorPacketFromPrinter(AckResponse response, HardwarePrinter printer)
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

    @Override
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

    @Override
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

    @Override
    public void showSDCardNotification()
    {
        if (!sdDialogOnDisplay)
        {
            Platform.runLater(() ->
            {
                sdDialogOnDisplay = true;
                Notifier.showErrorNotification(Lookup.i18n("dialogs.noSDCardTitle"),
                                               Lookup.i18n("dialogs.noSDCardMessage"));
                sdDialogOnDisplay = false;
            });
        }
    }

    @Override
    public void showSliceFailedNotification()
    {
        showErrorNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.sliceFailed"));
    }

    @Override
    public void showSliceSuccessfulNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.sliceSuccessful"));
    }

    @Override
    public void showGCodePostProcessFailedNotification()
    {
        showErrorNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.gcodePostProcessFailed"));
    }

    @Override
    public void showGCodePostProcessSuccessfulNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.gcodePostProcessSuccessful"));
    }

    @Override
    public void showPrintJobCancelledNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.printJobCancelled"));
    }

    @Override
    public void showPrintJobFailedNotification()
    {
        showErrorNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.printJobFailed"));
    }

    @Override
    public void showPrintTransferSuccessfulNotification(String printerName)
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.printTransferredSuccessfully")
                                    + " "
                                    + printerName + "\n"
                                    + Lookup.i18n("notification.printTransferredSuccessfullyEnd"));
    }

    @Override
    public void showPrintTransferInitiatedNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.printTransferInitiated"));
    }

    @Override
    public void showReprintStartedNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.reprintInitiated"));
    }

    @Override
    public void showDetectedPrintInProgressNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n("notification.activePrintDetected"));
    }

    @Override
    public void showFirmwareUpgradeStatusNotification(FirmwareLoadResult result)
    {
        switch (result.getStatus())
        {
            case FirmwareLoadResult.SDCARD_ERROR:
                showErrorNotification(Lookup.i18n("dialogs.firmwareUpgradeFailedTitle"),
                                      Lookup.i18n("dialogs.sdCardError"));
                break;
            case FirmwareLoadResult.FILE_ERROR:
                showErrorNotification(Lookup.i18n("dialogs.firmwareUpgradeFailedTitle"),
                                      Lookup.i18n("dialogs.firmwareFileError"));
                break;
            case FirmwareLoadResult.OTHER_ERROR:
                showErrorNotification(Lookup.i18n("dialogs.firmwareUpgradeFailedTitle"),
                                      Lookup.i18n("dialogs.firmwareUpgradeFailedMessage"));
                break;
            case FirmwareLoadResult.SUCCESS:
                showInformationNotification(Lookup.i18n("dialogs.firmwareUpgradeSuccessTitle"),
                                            Lookup.i18n("dialogs.firmwareUpgradeSuccessMessage"));
                break;
        }
    }

    /**
     *
     * @param requiredFirmwareVersion
     * @param actualFirmwareVersion
     * @return True if the user has agreed to downgrade, otherwise false
     */
    @Override
    public boolean askUserToDowngradeFirmware(int requiredFirmwareVersion, int actualFirmwareVersion)
    {
        if (firmwareDowngradeOK == null)
        {
            firmwareDowngradeOK = new Dialogs.CommandLink(Lookup.i18n("dialogs.firmwareDowngradeOKTitle"),
                                                          Lookup.i18n("dialogs.firmwareUpgradeOKMessage"));
            firmwareDowngradeNotOK = new Dialogs.CommandLink(Lookup.i18n("dialogs.firmwareDowngradeNotOKTitle"),
                                                             Lookup.i18n("dialogs.firmwareUpgradeNotOKMessage"));
        }

        Callable<Boolean> askUserToDowngradeDialog = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                Action downgradeApplicationResponse = Dialogs.create().title(
                    Lookup.i18n(
                        "dialogs.firmwareVersionTooLowTitle"))
                    .message(Lookup.i18n(
                            "dialogs.firmwareVersionError1")
                        + actualFirmwareVersion
                        + Lookup.i18n(
                            "dialogs.firmwareVersionError2")
                        + requiredFirmwareVersion + ".\n"
                        + Lookup.i18n(
                            "dialogs.firmwareVersionError3"))
                    .masthead(null)
                    .showCommandLinks(firmwareDowngradeOK,
                                      firmwareDowngradeOK,
                                      firmwareDowngradeNotOK);

                return downgradeApplicationResponse.equals(firmwareDowngradeOK);
            }
        };
        FutureTask<Boolean> askUserToUpgradeTask = new FutureTask<>(askUserToDowngradeDialog);
        Platform.runLater(askUserToUpgradeTask);
        try
        {
            return askUserToUpgradeTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during firmware upgrade query");
            return false;
        }

    }

    /**
     * Returns 0 for no downgrade and 1 for downgrade
     *
     * @param requiredFirmwareVersion
     * @param actualFirmwareVersion
     * @return True if the user has agreed to upgrade, otherwise false
     */
    @Override
    public boolean askUserToUpgradeFirmware(int requiredFirmwareVersion, int actualFirmwareVersion)
    {
        if (firmwareUpgradeOK == null)
        {
            firmwareUpgradeOK = new Dialogs.CommandLink(Lookup.i18n("dialogs.firmwareUpgradeOKTitle"),
                                                        Lookup.i18n("dialogs.firmwareUpgradeOKMessage"));
            firmwareUpgradeNotOK = new Dialogs.CommandLink(Lookup.i18n("dialogs.firmwareUpgradeNotOKTitle"),
                                                           Lookup.i18n("dialogs.firmwareUpgradeNotOKMessage"));
        }

        Callable<Boolean> askUserToUpgradeDialog = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                Action upgradeApplicationResponse = Dialogs.create().title(
                    Lookup.i18n("dialogs.firmwareUpgradeTitle"))
                    .message(Lookup.i18n("dialogs.firmwareVersionError1")
                        + actualFirmwareVersion
                        + Lookup.i18n("dialogs.firmwareVersionError2")
                        + requiredFirmwareVersion + ".\n"
                        + Lookup.i18n("dialogs.firmwareVersionError3"))
                    .masthead(null)
                    .showCommandLinks(firmwareUpgradeOK,
                                      firmwareUpgradeOK,
                                      firmwareUpgradeNotOK);
                return upgradeApplicationResponse.equals(firmwareUpgradeOK);
            }
        };
        FutureTask<Boolean> askUserToUpgradeTask = new FutureTask<>(askUserToUpgradeDialog);
        Platform.runLater(askUserToUpgradeTask);
        try
        {
            return askUserToUpgradeTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during firmware upgrade query");
            return false;
        }
    }

    @Override
    public void configureFirmwareProgressDialog(FirmwareLoadService firmwareLoadService)
    {
        Platform.runLater(() ->
        {
            if (firmwareUpdateProgress == null)
            {
                firmwareUpdateProgress = new ProgressDialog(firmwareLoadService);
            }
        });
    }

    @Override
    public void showNoSDCardDialog()
    {
        Platform.runLater(() ->
        {
            if (noSDDialog == null)
            {
                noSDDialog = new ModalDialog();
                noSDDialog.setTitle(Lookup.i18n("dialogs.noSDCardTitle"));
                noSDDialog.setMessage(Lookup.i18n("dialogs.noSDCardMessage"));
                noSDDialog.addButton(Lookup.i18n("dialogs.noSDCardOK"));
            }
        });
    }

    @Override
    public void showNoPrinterIDDialog(Printer printer)
    {
        if (printerIDDialog.isShowing() == false)
        {
            printerIDDialog.setPrinterToUse(printer);

            Platform.runLater(() ->
            {
                if (printerIDDialog == null)
                {
                    printerIDDialog = new PrinterIDDialog();
                }

                printerIDDialog.show();
            });
        }
    }
}
