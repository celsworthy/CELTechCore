package celtech.appManager;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.ProgressDialog;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import celtech.utils.tasks.TaskResponder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.dialog.CommandLinksDialog;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author Ian
 */
public class SystemNotificationManagerJavaFX implements SystemNotificationManager
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        SystemNotificationManagerJavaFX.class.getName());
    private boolean errorDialogOnDisplay = false;

    /*
     * Error dialog
     */
    private static CommandLinksDialog.CommandLinksButtonType clearOnly = null;
    private static CommandLinksDialog.CommandLinksButtonType clearOnlyDefault = null;
    private static CommandLinksDialog.CommandLinksButtonType clearAndContinueDefault = null;
    private static CommandLinksDialog.CommandLinksButtonType abortJob = null;

    /*
     * Calibration dialog
     */
    private static CommandLinksDialog.CommandLinksButtonType okCalibrate = null;
    private static CommandLinksDialog.CommandLinksButtonType dontCalibrate = null;

    /*
     * Firmware upgrade dialog
     */
    protected CommandLinksDialog.CommandLinksButtonType firmwareUpdateOK = null;
    protected CommandLinksDialog.CommandLinksButtonType firmwareUpdateNotOK = null;

    /*
     * Firmware upgrade progress
     */
    protected ProgressDialog firmwareUpdateProgress = null;

    /*
     * Printer ID Dialog
     */
    protected PrinterIDDialog printerIDDialog = null;

    /*
     * SD card dialog
     */
    protected boolean sdDialogOnDisplay = false;

    /*
     * Door open dialog
     */
    private CommandLinksDialog.CommandLinksButtonType goAheadAndOpenTheLid = null;
    private CommandLinksDialog.CommandLinksButtonType dontOpenTheLid = null;

    /*
     * Model too big dialog
     */
    private CommandLinksDialog.CommandLinksButtonType shrinkTheModel = null;
    private CommandLinksDialog.CommandLinksButtonType dontLoadTheModel = null;

    /*
     * Application upgrade dialog
     */
    private CommandLinksDialog.CommandLinksButtonType upgradeApplication = null;
    private CommandLinksDialog.CommandLinksButtonType dontUpgradeApplication = null;

    private boolean programInvalidHeadDialogOnDisplay = false;

    private boolean headNotRecognisedDialogOnDisplay = false;

    private boolean reelNotRecognisedDialogOnDisplay = false;

    @Override
    public void showErrorNotification(String title, String message)
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Notifier.showErrorNotification(title, message);
        });
    }

    @Override
    public void showWarningNotification(String title, String message)
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Notifier.showWarningNotification(title, message);
        });
    }

    @Override
    public void showInformationNotification(String title, String message)
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Notifier.showInformationNotification(title, message);
        });
    }

    @Override
    public void processErrorPacketFromPrinter(FirmwareError error, Printer printer)
    {
        if (clearOnly == null)
        {
            clearOnly = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n(
                "dialogs.error.clearOnly"), false);
            clearOnlyDefault = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n(
                "dialogs.error.clearOnly"), true);
            clearAndContinueDefault = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n(
                "dialogs.error.clearAndContinue"), true);
            abortJob = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n(
                "dialogs.error.abortJob"), false);
        }

        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            if (!errorDialogOnDisplay)
            {
                errorDialogOnDisplay = true;

                Optional<ButtonType> errorHandlingResponse = null;

                if (printer.printerStatusProperty().get() != PrinterStatus.IDLE
                    && printer.printerStatusProperty().get() != PrinterStatus.ERROR)
                {
                    CommandLinksDialog printerErrorDialog = new CommandLinksDialog(
                        clearAndContinueDefault,
                        clearOnly,
                        abortJob
                    );
                    printerErrorDialog.setTitle(Lookup.i18n("dialogs.error.errorEncountered"));
                    printerErrorDialog.setContentText(error.getLocalisedErrorText());
                    errorHandlingResponse = printerErrorDialog.showAndWait();
                } else
                {
                    CommandLinksDialog printerErrorDialog = new CommandLinksDialog(clearOnlyDefault,
                                                                                   abortJob);
                    printerErrorDialog.setTitle(Lookup.i18n("dialogs.error.errorEncountered"));
                    printerErrorDialog.setContentText(error.getLocalisedErrorText());
                    errorHandlingResponse = printerErrorDialog.showAndWait();
                }

                try
                {
                    printer.transmitResetErrors();
                } catch (RoboxCommsException ex)
                {
                    steno.error("Couldn't reset errors after error detection");
                }

                if (errorHandlingResponse.get() == abortJob.getButtonType())
                {
                    try
                    {
                        if (printer.canPauseProperty().get())
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
            okCalibrate = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n(
                "dialogs.headUpdateCalibrationYes"), true);
            dontCalibrate = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n(
                "dialogs.headUpdateCalibrationNo"), false);
        }

        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            CommandLinksDialog calibrationDialog = new CommandLinksDialog(
                okCalibrate,
                dontCalibrate
            );
            calibrationDialog.setTitle(Lookup.i18n("dialogs.headUpdateCalibrationRequiredTitle"));
            calibrationDialog.setContentText(Lookup.i18n(
                "dialogs.headUpdateCalibrationRequiredInstruction"));
            Optional<ButtonType> calibrationResponse = calibrationDialog.showAndWait();

            if (calibrationResponse.get() == okCalibrate.getButtonType())
            {
                ApplicationStatus.getInstance().setMode(ApplicationMode.CALIBRATION_CHOICE);
            }
        });
    }

    @Override
    public void showHeadUpdatedNotification()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Notifier.showInformationNotification(
                Lookup.i18n("notification.headSettingsUpdatedTitle"),
                Lookup.i18n("notification.noActionRequired"));
        });
    }

    @Override
    public void showSDCardNotification()
    {
        if (!sdDialogOnDisplay)
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
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
        showErrorNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n(
                              "notification.sliceFailed"));
    }

    @Override
    public void showSliceSuccessfulNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n(
                                    "notification.sliceSuccessful"));
    }

    @Override
    public void showGCodePostProcessFailedNotification()
    {
        showErrorNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n(
                              "notification.gcodePostProcessFailed"));
    }

    @Override
    public void showGCodePostProcessSuccessfulNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n(
                                    "notification.gcodePostProcessSuccessful"));
    }

    @Override
    public void showPrintJobCancelledNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n(
                                    "notification.printJobCancelled"));
    }

    @Override
    public void showPrintJobFailedNotification()
    {
        showErrorNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n(
                              "notification.printJobFailed"));
    }

    @Override
    public void showPrintTransferSuccessfulNotification(String printerName)
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n(
                                    "notification.printTransferredSuccessfully")
                                    + " "
                                    + printerName + "\n"
                                    + Lookup.i18n("notification.printTransferredSuccessfullyEnd"));
    }

    @Override
    public void showPrintTransferInitiatedNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n(
                                    "notification.printTransferInitiated"));
    }

    @Override
    public void showReprintStartedNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n(
                                    "notification.reprintInitiated"));
    }

    @Override
    public void showDetectedPrintInProgressNotification()
    {
        showInformationNotification(Lookup.i18n("notification.PrintQueueTitle"), Lookup.i18n(
                                    "notification.activePrintDetected"));
    }

    @Override
    public void showFirmwareUpgradeStatusNotification(FirmwareLoadResult result)
    {
        switch (result.getStatus())
        {
            case FirmwareLoadResult.SDCARD_ERROR:
                showErrorNotification(Lookup.i18n("dialogs.firmwareUpdateFailedTitle"),
                                      Lookup.i18n("dialogs.sdCardError"));
                break;
            case FirmwareLoadResult.FILE_ERROR:
                showErrorNotification(Lookup.i18n("dialogs.firmwareUpdateFailedTitle"),
                                      Lookup.i18n("dialogs.firmwareFileError"));
                break;
            case FirmwareLoadResult.OTHER_ERROR:
                showErrorNotification(Lookup.i18n("dialogs.firmwareUpdateFailedTitle"),
                                      Lookup.i18n("dialogs.firmwareUpdateFailedMessage"));
                break;
            case FirmwareLoadResult.SUCCESS:
                showInformationNotification(Lookup.i18n("dialogs.firmwareUpdateSuccessTitle"),
                                            Lookup.i18n("dialogs.firmwareUpdateSuccessMessage"));
                break;
        }
    }

    /**
     * Returns 0 for no downgrade and 1 for downgrade
     *
     * @param requiredFirmwareVersion
     * @param actualFirmwareVersion
     * @return True if the user has agreed to update, otherwise false
     */
    @Override
    public boolean askUserToUpdateFirmware(int requiredFirmwareVersion, int actualFirmwareVersion)
    {
        if (firmwareUpdateOK == null)
        {
            firmwareUpdateOK = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n(
                "dialogs.firmwareUpdateOKTitle"),
                                                                             Lookup.i18n(
                                                                                 "dialogs.firmwareUpdateOKMessage"),
                                                                             true);
            firmwareUpdateNotOK = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n(
                "dialogs.firmwareUpdateNotOKTitle"),
                                                                                Lookup.i18n(
                                                                                    "dialogs.firmwareUpdateNotOKMessage"),
                                                                                false);
        }

        Callable<Boolean> askUserToUpgradeDialog = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                CommandLinksDialog firmwareUpdateDialog = new CommandLinksDialog(
                    firmwareUpdateOK,
                    firmwareUpdateNotOK
                );

//                firmwareUpgradeDialog.getDialogPane().getStylesheets().add(ApplicationConfiguration.getDialogsCSSFile());
//                firmwareUpgradeDialog.getDialogPane().getStyleClass().add("dialog-commands");
                firmwareUpdateDialog.getDialogPane().setStyle(
                    "-fx-wrap-text: true; -fx-font-size: 13px; -fx-font-family: 'Source Sans Pro Regular';");
                firmwareUpdateDialog.setTitle(Lookup.i18n("dialogs.firmwareUpdateTitle"));
                firmwareUpdateDialog.setContentText(Lookup.i18n("dialogs.firmwareUpdateError"));
                Optional<ButtonType> firmwareUpgradeResponse = firmwareUpdateDialog.showAndWait();

                return firmwareUpgradeResponse.get() == firmwareUpdateOK.getButtonType();
            }
        };
        FutureTask<Boolean> askUserToUpgradeTask = new FutureTask<>(askUserToUpgradeDialog);
        Lookup.getTaskExecutor().runOnGUIThread(askUserToUpgradeTask);
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
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            firmwareUpdateProgress = new ProgressDialog(firmwareLoadService);
        });
    }

    @Override
    public void showNoSDCardDialog()
    {
        if (!sdDialogOnDisplay)
        {
            sdDialogOnDisplay = true;
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                Dialogs.create().title(
                    Lookup.i18n("dialogs.noSDCardTitle"))
                    .message(Lookup.i18n("dialogs.noSDCardMessage"))
                    .masthead(null).showError();
                sdDialogOnDisplay = false;
            });
        }
    }

    @Override
    public void showNoPrinterIDDialog(Printer printer)
    {
        if (printerIDDialog.isShowing() == false)
        {
            printerIDDialog.setPrinterToUse(printer);

            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                if (printerIDDialog == null)
                {
                    printerIDDialog = new PrinterIDDialog();
                }

                printerIDDialog.show();
            });
        }
    }

    @Override
    public boolean showOpenDoorDialog()
    {
        if (goAheadAndOpenTheLid == null)
        {
            goAheadAndOpenTheLid = new CommandLinksDialog.CommandLinksButtonType(
                Lookup.i18n("dialogs.openLidPrinterHotGoAheadHeading"),
                Lookup.i18n("dialogs.openLidPrinterHotGoAheadInfo"),
                true);
            dontOpenTheLid = new CommandLinksDialog.CommandLinksButtonType(
                Lookup.i18n("dialogs.openLidPrinterHotDontOpenHeading"),
                true);
        }

        Callable<Boolean> askUserWhetherToOpenDoorDialog = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                CommandLinksDialog doorOpenDialog = new CommandLinksDialog(
                    goAheadAndOpenTheLid,
                    dontOpenTheLid
                );
                doorOpenDialog.setTitle(Lookup.i18n("dialogs.openLidPrinterHotTitle"));

                Optional<ButtonType> doorOpenResponse = doorOpenDialog.showAndWait();

                return doorOpenResponse.get() == goAheadAndOpenTheLid.getButtonType();
            }
        };

        FutureTask<Boolean> askUserWhetherToOpenDoorTask = new FutureTask<>(
            askUserWhetherToOpenDoorDialog);
        Lookup.getTaskExecutor().runOnGUIThread(askUserWhetherToOpenDoorTask);
        try
        {
            return askUserWhetherToOpenDoorTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during door open query");
            return false;
        }
    }

    /**
     *
     * @param modelFilename
     * @return True if the user has opted to shrink the model
     */
    @Override
    public boolean showModelTooBigDialog(String modelFilename)
    {
        if (shrinkTheModel == null)
        {
            shrinkTheModel = new CommandLinksDialog.CommandLinksButtonType(
                Lookup.i18n("dialogs.ShrinkModelToFit"),
                true);
            dontLoadTheModel = new CommandLinksDialog.CommandLinksButtonType(
                Lookup.i18n("dialogs.ModelTooLargeNo"),
                false);
        }

        Callable<Boolean> askUserWhetherToLoadModel = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                CommandLinksDialog loadModelDialog = new CommandLinksDialog(
                    shrinkTheModel,
                    dontLoadTheModel
                );
                loadModelDialog.setTitle(Lookup.i18n("dialogs.ModelTooLargeTitle"));
                loadModelDialog.setContentText(modelFilename
                    + ": "
                    + Lookup.i18n("dialogs.ModelTooLargeDescription"));

                Optional<ButtonType> loadModelResponse = loadModelDialog.showAndWait();

                return loadModelResponse.get() == shrinkTheModel.getButtonType();
            }
        };

        FutureTask<Boolean> askUserWhetherToLoadModelTask = new FutureTask<>(
            askUserWhetherToLoadModel);
        Lookup.getTaskExecutor().runOnGUIThread(askUserWhetherToLoadModelTask);
        try
        {
            return askUserWhetherToLoadModelTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during model too large query");
            return false;
        }
    }

    /**
     *
     * @param applicationName
     * @return True if the user has elected to upgrade
     */
    @Override
    public boolean showApplicationUpgradeDialog(String applicationName)
    {
        if (upgradeApplication == null)
        {
            upgradeApplication = new CommandLinksDialog.CommandLinksButtonType(
                Lookup.i18n("misc.Yes"),
                Lookup.i18n("dialogs.updateExplanation"),
                true);
            dontUpgradeApplication = new CommandLinksDialog.CommandLinksButtonType(
                Lookup.i18n("misc.No"),
                Lookup.i18n("dialogs.updateContinueWithCurrent"),
                false);
        }

        Callable<Boolean> askUserWhetherToUpgrade = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                CommandLinksDialog upgradeApplicationDialog = new CommandLinksDialog(
                    upgradeApplication,
                    dontUpgradeApplication
                );
                upgradeApplicationDialog.setTitle(Lookup.i18n("dialogs.updateApplicationTitle"));
                upgradeApplicationDialog.setContentText(
                    Lookup.i18n("dialogs.updateApplicationMessagePart1")
                    + applicationName
                    + Lookup.i18n("dialogs.updateApplicationMessagePart2"));

                Optional<ButtonType> upgradeApplicationResponse = upgradeApplicationDialog.showAndWait();

                return upgradeApplicationResponse.get() == upgradeApplication.getButtonType();
            }
        };

        FutureTask<Boolean> askWhetherToUpgradeTask = new FutureTask<>(askUserWhetherToUpgrade);
        Lookup.getTaskExecutor().runOnGUIThread(askWhetherToUpgradeTask);
        try
        {
            return askWhetherToUpgradeTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during model too large query");
            return false;
        }
    }

    /**
     * @return True if the user has elected to purge
     */
    @Override
    public PurgeResponse showPurgeDialog()
    {
        Callable<PurgeResponse> askUserWhetherToPurge = new Callable()
        {
            @Override
            public PurgeResponse call() throws Exception
            {
                CommandLinksDialog.CommandLinksButtonType purge = new CommandLinksDialog.CommandLinksButtonType(
                    Lookup.i18n("dialogs.goForPurgeTitle"),
                    Lookup.i18n("dialogs.goForPurgeInstruction"),
                    true);
                CommandLinksDialog.CommandLinksButtonType dontPurge = new CommandLinksDialog.CommandLinksButtonType(
                    Lookup.i18n("dialogs.dontGoForPurgeTitle"),
                    Lookup.i18n("dialogs.dontGoForPurgeInstruction"),
                    false);
                CommandLinksDialog.CommandLinksButtonType dontPrint = new CommandLinksDialog.CommandLinksButtonType(
                    Lookup.i18n("dialogs.dontPrintTitle"),
                    Lookup.i18n("dialogs.dontPrintInstruction"),
                    false);
                CommandLinksDialog purgeDialog = new CommandLinksDialog(
                    purge,
                    dontPurge,
                    dontPrint
                );
                purgeDialog.setTitle(Lookup.i18n("dialogs.purgeRequiredTitle"));
                purgeDialog.setContentText(Lookup.i18n("dialogs.purgeRequiredInstruction"));

                Optional<ButtonType> purgeResponse = purgeDialog.showAndWait();

                PurgeResponse response = null;

                if (purgeResponse.get() == purge.getButtonType())
                {
                    response = PurgeResponse.PRINT_WITH_PURGE;
                } else if (purgeResponse.get() == dontPurge.getButtonType())
                {
                    response = PurgeResponse.PRINT_WITHOUT_PURGE;
                } else if (purgeResponse.get() == dontPrint.getButtonType())
                {
                    response = PurgeResponse.DONT_PRINT;
                }

                return response;
            }
        };

        FutureTask<PurgeResponse> askWhetherToPurgeTask = new FutureTask<>(askUserWhetherToPurge);
        Lookup.getTaskExecutor().runOnGUIThread(askWhetherToPurgeTask);
        try
        {
            return askWhetherToPurgeTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during purge query");
            return null;
        }
    }

    /**
     * @return True if the user has elected to shutdown
     */
    @Override
    public boolean showJobsTransferringShutdownDialog()
    {
        Callable<Boolean> askUserWhetherToShutdown = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                CommandLinksDialog.CommandLinksButtonType shutdown = new CommandLinksDialog.CommandLinksButtonType(
                    Lookup.i18n("dialogs.shutDownAndTerminateTitle"),
                    Lookup.i18n("dialogs.shutDownAndTerminateMessage"),
                    false);
                CommandLinksDialog.CommandLinksButtonType dontShutdown = new CommandLinksDialog.CommandLinksButtonType(
                    Lookup.i18n("dialogs.dontShutDownTitle"),
                    Lookup.i18n("dialogs.dontShutDownMessage"),
                    true);
                CommandLinksDialog shutdownDialog = new CommandLinksDialog(
                    shutdown,
                    dontShutdown
                );
                shutdownDialog.setTitle(Lookup.i18n("dialogs.printJobsAreStillTransferringTitle"));
                shutdownDialog.setContentText(Lookup.i18n(
                    "dialogs.printJobsAreStillTransferringMessage"));

                Optional<ButtonType> shutdownResponse = shutdownDialog.showAndWait();

                return shutdownResponse.get() == shutdown.getButtonType();
            }
        };

        FutureTask<Boolean> askWhetherToShutdownTask = new FutureTask<>(askUserWhetherToShutdown);
        Lookup.getTaskExecutor().runOnGUIThread(askWhetherToShutdownTask);
        try
        {
            return askWhetherToShutdownTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during shutdown whilst transferring query");
            return false;
        }
    }

    @Override
    public void showProgramInvalidHeadDialog(TaskResponder<HeadFile> responder)
    {
        if (!programInvalidHeadDialogOnDisplay)
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                programInvalidHeadDialogOnDisplay = true;
                ArrayList<HeadFile> headFiles = new ArrayList(HeadContainer.getCompleteHeadList());

                ChoiceDialog headRewriteDialog = new ChoiceDialog(headFiles.get(0), headFiles);

                headRewriteDialog.setTitle(Lookup.i18n("dialogs.headRepairTitle"));
                headRewriteDialog.setHeaderText(Lookup.i18n("dialogs.headRepairHeader"));
                headRewriteDialog.setContentText(Lookup.i18n("dialogs.headRepairInstruction"));

                Optional<HeadFile> chosenFileOption = headRewriteDialog.showAndWait();
                HeadFile chosenFile = null;
                if (chosenFileOption.isPresent())
                {
                    chosenFile = chosenFileOption.get();
                }

                Lookup.getTaskExecutor().respondOnGUIThread(responder, chosenFile != null,
                                                            "Head profile chosen", chosenFile);
                programInvalidHeadDialogOnDisplay = false;
            });
        }
    }

    @Override
    public void showHeadNotRecognisedDialog(String printerName)
    {
        if (!headNotRecognisedDialogOnDisplay)
        {
            headNotRecognisedDialogOnDisplay = true;
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                Dialogs.create().title(
                    Lookup.i18n("dialogs.headNotRecognisedTitle"))
                    .message(Lookup.i18n("dialogs.headNotRecognisedMessage1")
                        + " "
                        + printerName
                        + " "
                        + Lookup.i18n("dialogs.headNotRecognisedMessage2")
                        + " "
                        + ApplicationConfiguration.getApplicationName())
                    .masthead(null).showError();
                headNotRecognisedDialogOnDisplay = false;
            });
        }
    }

    @Override
    public Optional<PrinterErrorChoice> showPrinterErrorDialog(String title, String message,
        boolean showContinueOption, boolean showAbortOption, boolean showRetryOption)
    {
        if (!showContinueOption && !showAbortOption && !showRetryOption)
        {
            throw new RuntimeException("Must allow one option to be shown");
        }
        Callable<PrinterErrorChoice> askUserToRespondToPrinterError = new Callable()
        {
            @Override
            public PrinterErrorChoice call() throws Exception
            {
                CommandLinksDialog.CommandLinksButtonType continue_ = null;
                CommandLinksDialog.CommandLinksButtonType abort = null;
                CommandLinksDialog.CommandLinksButtonType retry = null;
                List<CommandLinksDialog.CommandLinksButtonType> choices = new ArrayList<>();
                if (showContinueOption)
                {
                    continue_
                        = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n("dialogs.error.continue"),
                                                                        Lookup.i18n("dialogs.error.clearAndContinue"),
                                                                        true);
                    choices.add(continue_);
                }
                if (showAbortOption)
                {
                    abort
                        = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n("dialogs.error.abort"),
                                                                        Lookup.i18n("dialogs.error.abortProcess"),
                                                                        true);
                    choices.add(abort);
                }
                if (showRetryOption)
                {
                    retry
                        = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n("dialogs.error.retry"),
                                                                        Lookup.i18n("dialogs.error.retryProcess"),
                                                                        true);
                    choices.add(retry);
                }
//                
//                CommandLinksDialog.CommandLinksButtonType test = new CommandLinksDialog.CommandLinksButtonType(Lookup.i18n("dialogs.error.retry"),
//                                                                        Lookup.i18n("dialogs.error.retryProcess"),
//                                                                        true);
//                choices.add(test);

                CommandLinksDialog errorDialog = new CommandLinksDialog(choices);
                errorDialog.setTitle(title);
                errorDialog.setContentText(message);

                Optional<ButtonType> errorResponse = errorDialog.showAndWait();

                if (continue_ != null && errorResponse.get().equals(continue_.getButtonType()))
                {
                    return PrinterErrorChoice.CONTINUE;
                } else if (abort != null && errorResponse.get().equals(abort.getButtonType()))
                {
                    return PrinterErrorChoice.ABORT;
                }
                return PrinterErrorChoice.RETRY;
            }
        };

        FutureTask<PrinterErrorChoice> askContinueAbortTask = new FutureTask<>(
            askUserToRespondToPrinterError);
        Lookup.getTaskExecutor().runOnGUIThread(askContinueAbortTask);
        try
        {
            return Optional.of(askContinueAbortTask.get());
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during printer error query");
            return Optional.empty();
        }

    }

    public void showCantPrintNoFilamentDialog()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Dialogs.create().title(
                Lookup.i18n("dialogs.cantPrintNoFilamentTitle"))
                .message(Lookup.i18n("dialogs.cantPrintNoFilamentMessage"))
                .masthead(null).showInformation();
        });
    }

    @Override
    public void showCantPrintDoorIsOpenDialog()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Dialogs.create().title(
                Lookup.i18n("dialogs.cantPrintDoorIsOpenTitle"))
                .message(Lookup.i18n("dialogs.cantPrintDoorIsOpenMessage"))
                .masthead(null).showInformation();
        });
    }

    @Override
    public void showReelUpdatedNotification()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Notifier.showInformationNotification(Lookup.i18n("notification.reelDataUpdatedTitle"),
                                                 Lookup.i18n("notification.noActionRequired"));
        });
    }

    @Override
    public void showReelNotRecognisedDialog(String printerName)
    {
        if (!reelNotRecognisedDialogOnDisplay)
        {
            reelNotRecognisedDialogOnDisplay = true;
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                Dialogs.create().title(
                    Lookup.i18n("dialogs.reelNotRecognisedTitle"))
                    .message(Lookup.i18n("dialogs.reelNotRecognisedMessage1")
                        + " "
                        + printerName
                        + " "
                        + Lookup.i18n("dialogs.reelNotRecognisedMessage2")
                        + " "
                        + ApplicationConfiguration.getApplicationName())
                    .masthead(null).showError();
                reelNotRecognisedDialogOnDisplay = false;
            });
        }
    }

    @Override
    public void showSelectAFilamentDialog()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Dialogs.create().title(
                Lookup.i18n("dialogs.cantPrintNoFilamentSelectedTitle"))
                .message(Lookup.i18n("dialogs.cantPrintNoFilamentSelectedMessage"))
                .masthead(null).showInformation();
        });
    }
}
