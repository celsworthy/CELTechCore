package celtech.appManager;

import celtech.Lookup;
import celtech.appManager.errorHandling.SystemErrorHandlerOptions;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.coreUI.components.ChoiceLinkButton;
import celtech.coreUI.components.ChoiceLinkDialogBox;
import celtech.coreUI.components.ChoiceLinkDialogBox.PrinterDisconnectedException;
import celtech.coreUI.components.Notifications.NotificationDisplay.NotificationType;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.ProgressDialog;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import celtech.utils.tasks.TaskResponder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ListView;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class SystemNotificationManagerJavaFX implements SystemNotificationManager
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            SystemNotificationManagerJavaFX.class.getName());

    private HashMap<SystemErrorHandlerOptions, ChoiceLinkButton> errorToButtonMap = null;

    /*
     * Error dialog
     */
    /*
     * SD card dialog
     */
    protected boolean sdDialogOnDisplay = false;

    private boolean programInvalidHeadDialogOnDisplay = false;

    private boolean headNotRecognisedDialogOnDisplay = false;

    private boolean reelNotRecognisedDialogOnDisplay = false;

    private boolean clearBedDialogOnDisplay = false;

    /*
     * Firmware upgrade progress
     */
    protected ProgressDialog firmwareUpdateProgress = null;

    /*
     * Printer ID Dialog
     */
    protected PrinterIDDialog printerIDDialog = null;

    private ChoiceLinkDialogBox keepPushingFilamentDialogBox = null;

    private ChoiceLinkDialogBox failedTransferDialogBox = null;

    private ChoiceLinkDialogBox failedEjectDialogBox = null;

    private ChoiceLinkDialogBox filamentMotionCheckDialogBox = null;

    private ChoiceLinkDialogBox filamentStuckDialogBox = null;

    private ChoiceLinkDialogBox loadFilamentNowDialogBox = null;

    private ChoiceLinkDialogBox errorChoiceBox = null;

    @Override
    public void showErrorNotification(String title, String message)
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Lookup.getNotificationDisplay().displayTimedNotification(title, message, NotificationType.CAUTION);
        });
    }

    @Override
    public void showWarningNotification(String title, String message)
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Lookup.getNotificationDisplay().displayTimedNotification(title, message, NotificationType.WARNING);
        });
    }

    @Override
    public void showInformationNotification(String title, String message)
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            Lookup.getNotificationDisplay().displayTimedNotification(title, message, NotificationType.NOTE);
        });
    }

    @Override
    public void processErrorPacketFromPrinter(FirmwareError error, Printer printer)
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {

            switch (error)
            {
                case B_POSITION_LOST:
                    steno.warning("B Position Lost error detected");
                    break;

                default:
                    if (errorChoiceBox == null)
                    {
                        setupErrorOptions();

                        errorChoiceBox = new ChoiceLinkDialogBox(true);
                        errorChoiceBox.setTitle(error.getLocalisedErrorTitle());
                        errorChoiceBox.setMessage(error.getLocalisedErrorMessage());
                        error.getOptions()
                                .stream()
                                .forEach(option -> errorChoiceBox.
                                        addChoiceLink(errorToButtonMap.get(option)));

                        Optional<ChoiceLinkButton> buttonPressed;
                        try
                        {
                            buttonPressed = errorChoiceBox.getUserInput();
                        } catch (PrinterDisconnectedException ex)
                        {
                            return;
                        }

                        if (buttonPressed.isPresent())
                        {
                            for (Entry<SystemErrorHandlerOptions, ChoiceLinkButton> mapEntry : errorToButtonMap.
                                    entrySet())
                            {
                                if (buttonPressed.get() == mapEntry.getValue())
                                {
                                    switch (mapEntry.getKey())
                                    {
                                        case ABORT:
                                        case OK_ABORT:
                                            try
                                            {
                                                if (printer.canPauseProperty().get())
                                                {
                                                    printer.pause();
                                                }
                                                printer.cancel(null);
                                            } catch (PrinterException ex)
                                            {
                                                steno.error(
                                                        "Error whilst cancelling print from error dialog");
                                            }
                                            break;
                                        case CLEAR_CONTINUE:
                                            try
                                            {
                                                if (printer.canResumeProperty().get())
                                                {
                                                    printer.resume();
                                                }
                                            } catch (PrinterException ex)
                                            {
                                                steno.error(
                                                        "Error whilst resuming print from error dialog");
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                    break;
                                }
                            }
                        }

                        errorChoiceBox = null;
                    }
                    break;
            }
        });
    }

    private void setupErrorOptions()
    {
        if (errorToButtonMap == null)
        {
            errorToButtonMap = new HashMap<>();
            for (SystemErrorHandlerOptions option : SystemErrorHandlerOptions.values())
            {
                ChoiceLinkButton buttonToAdd = new ChoiceLinkButton();
                buttonToAdd.setTitle(option.getLocalisedErrorTitle());
                buttonToAdd.setMessage(option.getLocalisedErrorMessage());
                errorToButtonMap.put(option, buttonToAdd);
            }
        }
    }

    @Override
    public void showCalibrationDialogue()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(true);
            choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.headUpdateCalibrationRequiredTitle"));
            choiceLinkDialogBox.setMessage(Lookup.i18n(
                    "dialogs.headUpdateCalibrationRequiredInstruction"));
            ChoiceLinkButton okCalibrateChoice = choiceLinkDialogBox.addChoiceLink(
                    Lookup.i18n("dialogs.headUpdateCalibrationYes"));
            ChoiceLinkButton dontCalibrateChoice = choiceLinkDialogBox.addChoiceLink(
                    Lookup.i18n("dialogs.headUpdateCalibrationNo"));

            Optional<ChoiceLinkButton> calibrationResponse;
            try
            {
                calibrationResponse = choiceLinkDialogBox.getUserInput();
            } catch (PrinterDisconnectedException ex)
            {
                return;
            }

            if (calibrationResponse.isPresent())
            {
                if (calibrationResponse.get() == okCalibrateChoice)
                {
                    ApplicationStatus.getInstance().setMode(ApplicationMode.CALIBRATION_CHOICE);
                }
            }
        });
    }

    @Override
    public void showHeadUpdatedNotification()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            showInformationNotification(
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
                showErrorNotification(Lookup.i18n("dialogs.noSDCardTitle"),
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

    /**
     *
     * @param printerName
     */
    @Override
    public void showPrintTransferFailedNotification(String printerName)
    {
        if (failedTransferDialogBox == null)
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                if (failedTransferDialogBox == null)
                {
                    failedTransferDialogBox = new ChoiceLinkDialogBox(false);
                    failedTransferDialogBox.setTitle(Lookup.i18n("notification.PrintQueueTitle"));
                    failedTransferDialogBox.setMessage(Lookup.i18n(
                            "notification.printTransferFailed"));

                    failedTransferDialogBox.addChoiceLink(Lookup.i18n("misc.OK"));

                    try
                    {
                        failedTransferDialogBox.getUserInput();
                    } catch (PrinterDisconnectedException ex)
                    {
                        // this should never happen
                        steno.error("Print job transfer failed to printer " + printerName);
                    }
                    failedTransferDialogBox = null;
                    steno.error("Print job transfer failed to printer " + printerName);
                }
            });
        }
    }

    @Override
    public void removePrintTransferFailedNotification()
    {
        if (failedTransferDialogBox != null)
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                failedTransferDialogBox.close();
                failedTransferDialogBox = null;
            });
        }
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
    public void showFirmwareUpgradeStatusNotification(FirmwareLoadResult result)
    {
        if (result != null)
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
        } else
        {
            showErrorNotification(Lookup.i18n("dialogs.firmwareUpdateFailedTitle"),
                    Lookup.i18n("dialogs.firmwareUpdateFailedMessage"));
        }
    }

    /**
     * Returns 0 for no downgrade and 1 for downgrade
     *
     * @return True if the user has agreed to update, otherwise false
     */
    @Override
    public boolean askUserToUpdateFirmware()
    {
        Callable<Boolean> askUserToUpgradeDialog = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(true);
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.firmwareUpdateTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n("dialogs.firmwareUpdateError"));
                ChoiceLinkButton updateFirmwareChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.firmwareUpdateOKTitle"),
                        Lookup.i18n("dialogs.firmwareUpdateOKMessage"));
                ChoiceLinkButton dontUpdateFirmwareChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.firmwareUpdateNotOKTitle"),
                        Lookup.i18n("dialogs.firmwareUpdateNotOKMessage"));

                Optional<ChoiceLinkButton> firmwareUpgradeResponse = choiceLinkDialogBox.
                        getUserInput();

                boolean updateConfirmed = false;

                if (firmwareUpgradeResponse.isPresent())
                {
                    updateConfirmed = firmwareUpgradeResponse.get() == updateFirmwareChoice;
                }

                return updateConfirmed;
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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(false);
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.noSDCardTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n(
                        "dialogs.noSDCardMessage"));
                ChoiceLinkButton openTheLidChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("misc.OK"));

                try
                {
                    choiceLinkDialogBox.getUserInput();
                } catch (PrinterDisconnectedException ex)
                {
                    steno.error("this should never happen");
                }

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
        Callable<Boolean> askUserWhetherToOpenDoorDialog = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(true);
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.openLidPrinterHotTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n(
                        "dialogs.openLidPrinterHotInfo"));
                ChoiceLinkButton openTheLidChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.openLidPrinterHotGoAheadHeading"),
                        Lookup.i18n("dialogs.openLidPrinterHotGoAheadInfo"));
                ChoiceLinkButton dontOpenTheLidChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.openLidPrinterHotDontOpenHeading"));

                Optional<ChoiceLinkButton> doorOpenResponse = choiceLinkDialogBox.
                        getUserInput();

                boolean openTheLid = false;

                if (doorOpenResponse.isPresent())
                {
                    openTheLid = doorOpenResponse.get() == openTheLidChoice;
                }

                return openTheLid;
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
        Callable<Boolean> askUserWhetherToLoadModel = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(false);
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.ModelTooLargeTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n(
                        "dialogs.ModelTooLargeDescription"));
                ChoiceLinkButton shrinkChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.ShrinkModelToFit"));
                ChoiceLinkButton dontShrinkChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.dontShrink"));

                Optional<ChoiceLinkButton> shrinkResponse = choiceLinkDialogBox.
                        getUserInput();

                boolean shrinkModel = false;

                if (shrinkResponse.isPresent())
                {
                    shrinkModel = shrinkResponse.get() == shrinkChoice;
                }

                return shrinkModel;
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
        Callable<Boolean> askUserWhetherToUpgrade = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(false);
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.updateApplicationTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n("dialogs.updateApplicationMessagePart1")
                        + applicationName
                        + Lookup.i18n("dialogs.updateApplicationMessagePart2"));
                ChoiceLinkButton upgradeChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("misc.Yes"),
                        Lookup.i18n("dialogs.updateExplanation"));
                ChoiceLinkButton dontUpgradeChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("misc.No"),
                        Lookup.i18n("dialogs.updateContinueWithCurrent"));

                Optional<ChoiceLinkButton> upgradeResponse = choiceLinkDialogBox.
                        getUserInput();

                boolean upgradeApplication = false;

                if (upgradeResponse.isPresent())
                {
                    upgradeApplication = upgradeResponse.get() == upgradeChoice;
                }

                return upgradeApplication;
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

    @Override
    public PurgeResponse showPurgeDialog()
    {
        return showPurgeDialog(true);
    }

    /**
     * @return True if the user has elected to purge
     */
    @Override
    public PurgeResponse showPurgeDialog(boolean allowAutoPrint)
    {
        Callable<PurgeResponse> askUserWhetherToPurge = new Callable()
        {
            @Override
            public PurgeResponse call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(true);
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.purgeRequiredTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n(
                        "dialogs.purgeRequiredInstruction"));

                ChoiceLinkButton purge = null;
                if (allowAutoPrint)
                {
                    purge = choiceLinkDialogBox.addChoiceLink(
                            Lookup.i18n("dialogs.goForPurgeTitle"),
                            Lookup.i18n("dialogs.goForPurgeInstruction"));
                }
                ChoiceLinkButton dontPurge = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.dontGoForPurgeTitle"),
                        Lookup.i18n("dialogs.dontGoForPurgeInstruction"));
                ChoiceLinkButton dontPrint = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.dontPrintTitle"),
                        Lookup.i18n("dialogs.dontPrintInstruction"));

                Optional<ChoiceLinkButton> purgeResponse = choiceLinkDialogBox.
                        getUserInput();

                PurgeResponse response = null;

                if (purgeResponse.get() == purge)
                {
                    response = PurgeResponse.PRINT_WITH_PURGE;
                } else if (purgeResponse.get() == dontPurge)
                {
                    response = PurgeResponse.PRINT_WITHOUT_PURGE;
                } else if (purgeResponse.get() == dontPrint)
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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(true);
                choiceLinkDialogBox.setTitle(Lookup.i18n(
                        "dialogs.printJobsAreStillTransferringTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n(
                        "dialogs.printJobsAreStillTransferringMessage"));
                ChoiceLinkButton shutdown = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.shutDownAndTerminateTitle"),
                        Lookup.i18n("dialogs.shutDownAndTerminateMessage"));
                ChoiceLinkButton dontShutdown = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.dontShutDownTitle"),
                        Lookup.i18n("dialogs.dontShutDownMessage"));

                Optional<ChoiceLinkButton> shutdownResponse = choiceLinkDialogBox.
                        getUserInput();

                return shutdownResponse.get() == shutdown;
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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(true);
                choiceLinkDialogBox.setTitle(Lookup.i18n(
                        "dialogs.headNotRecognisedTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n("dialogs.headNotRecognisedMessage1")
                        + " "
                        + printerName
                        + " "
                        + Lookup.i18n("dialogs.headNotRecognisedMessage2")
                        + " "
                        + ApplicationConfiguration.getApplicationName());

                ChoiceLinkButton openTheLidChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("misc.OK"));

                try
                {
                    choiceLinkDialogBox.getUserInput();
                } catch (PrinterDisconnectedException ex)
                {
                    steno.error("printer disconnected");
                }

                headNotRecognisedDialogOnDisplay = false;
            });
        }
    }

    @Override
    public Optional<PrinterErrorChoice> showPrinterErrorDialog(String title, String message,
            boolean showContinueOption, boolean showAbortOption, boolean showRetryOption,
            boolean showOKOption)
    {
        if (!showContinueOption && !showAbortOption && !showRetryOption && !showOKOption)
        {
            throw new RuntimeException("Must allow one option to be shown");
        }
        Callable<Optional<PrinterErrorChoice>> askUserToRespondToPrinterError = new Callable()
        {
            @Override
            public Optional<PrinterErrorChoice> call() throws Exception
            {
                ChoiceLinkDialogBox printerErrorDialogBox = new ChoiceLinkDialogBox(true);
                printerErrorDialogBox.setTitle(title);
                printerErrorDialogBox.setMessage(message);

                ChoiceLinkButton continueChoice = new ChoiceLinkButton();
                continueChoice.setTitle(Lookup.i18n("dialogs.error.continue"));
                continueChoice.setMessage(Lookup.i18n("dialogs.error.clearAndContinue"));

                ChoiceLinkButton abortChoice = new ChoiceLinkButton();
                abortChoice.setTitle(Lookup.i18n("dialogs.error.abort"));
                abortChoice.setMessage(Lookup.i18n("dialogs.error.abortProcess"));

                ChoiceLinkButton retryChoice = new ChoiceLinkButton();
                retryChoice.setTitle(Lookup.i18n("dialogs.error.retry"));
                retryChoice.setMessage(Lookup.i18n("dialogs.error.retryProcess"));

                ChoiceLinkButton okChoice = new ChoiceLinkButton();
                okChoice.setTitle(Lookup.i18n("error.handler.OK.title"));

                if (showContinueOption)
                {
                    printerErrorDialogBox.addChoiceLink(continueChoice);
                }

                if (showAbortOption)
                {
                    printerErrorDialogBox.addChoiceLink(abortChoice);
                }

                if (showRetryOption)
                {
                    printerErrorDialogBox.addChoiceLink(retryChoice);
                }

                if (showOKOption)
                {
                    printerErrorDialogBox.addChoiceLink(okChoice);
                }

                Optional<ChoiceLinkButton> response = printerErrorDialogBox.getUserInput();

                Optional<PrinterErrorChoice> userResponse = Optional.empty();

                if (response.isPresent())
                {
                    if (response.get() == continueChoice)
                    {
                        userResponse = Optional.of(PrinterErrorChoice.CONTINUE);
                    } else if (response.get() == abortChoice)
                    {
                        userResponse = Optional.of(PrinterErrorChoice.ABORT);
                    } else if (response.get() == okChoice)
                    {
                        userResponse = Optional.of(PrinterErrorChoice.OK);
                    } else if (response.get() == retryChoice)
                    {
                        userResponse = Optional.of(PrinterErrorChoice.RETRY);
                    }
                }

                return userResponse;
            }
        };

        FutureTask<Optional<PrinterErrorChoice>> askContinueAbortTask = new FutureTask<>(
                askUserToRespondToPrinterError);
        Lookup.getTaskExecutor().runOnGUIThread(askContinueAbortTask);
        try
        {
            return askContinueAbortTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during printer error query");
            return Optional.empty();
        }

    }

    @Override
    public void showReelUpdatedNotification()
    {
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            showInformationNotification(Lookup.i18n("notification.reelDataUpdatedTitle"),
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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(true);
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.reelNotRecognisedTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n("dialogs.reelNotRecognisedMessage1")
                        + " "
                        + printerName
                        + " "
                        + Lookup.i18n("dialogs.reelNotRecognisedMessage2")
                        + " "
                        + ApplicationConfiguration.getApplicationName());

                choiceLinkDialogBox.addChoiceLink(Lookup.i18n("misc.OK"));

                try
                {
                    choiceLinkDialogBox.getUserInput();
                } catch (PrinterDisconnectedException ex)
                {
                    steno.error("printer disconnected");
                }

                reelNotRecognisedDialogOnDisplay = false;
            });
        }
    }

    @Override
    public void askUserToClearBed()
    {
        if (!clearBedDialogOnDisplay)
        {
            clearBedDialogOnDisplay = true;
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(false);
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.clearBedTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n("dialogs.clearBedInstruction"));

                choiceLinkDialogBox.addChoiceLink(Lookup.i18n("misc.OK"));

                try
                {
                    choiceLinkDialogBox.getUserInput();
                } catch (PrinterDisconnectedException ex)
                {
                    steno.error("this should never happen");
                }

                clearBedDialogOnDisplay = false;
            });
        }
    }

    /**
     * Returns 0 for no downgrade and 1 for downgrade
     *
     * @return True if the user has decided to switch to Advanced Mode,
     * otherwise false
     */
    @Override
    public boolean confirmAdvancedMode()
    {
        Callable<Boolean> confirmAdvancedModeDialog = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(false);
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.goToAdvancedModeTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n("dialogs.goToAdvancedModeMessage"));
                ChoiceLinkButton goToAdvancedModeChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.goToAdvancedModeYesTitle"),
                        Lookup.i18n("dialogs.goToAdvancedModeYesMessage"));
                ChoiceLinkButton dontGoToAdvancedModeChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.goToAdvancedModeNoTitle"),
                        Lookup.i18n("dialogs.goToAdvancedModeNoMessage"));

                Optional<ChoiceLinkButton> goToAdvancedModeResponse
                        = choiceLinkDialogBox.getUserInput();

                boolean goToAdvancedMode = false;

                if (goToAdvancedModeResponse.isPresent())
                {
                    goToAdvancedMode = goToAdvancedModeResponse.get() == goToAdvancedModeChoice;
                }

                return goToAdvancedMode;
            }
        };

        FutureTask<Boolean> confirmAdvancedModeTask = new FutureTask<>(confirmAdvancedModeDialog);
        Lookup.getTaskExecutor().runOnGUIThread(confirmAdvancedModeTask);
        try
        {
            return confirmAdvancedModeTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during advanced mode query: " + ex);
            return false;
        }
    }

    /**
     *
     */
    @Override
    public void showKeepPushingFilamentNotification()
    {
        if (keepPushingFilamentDialogBox == null)
        {
            Platform.runLater(() ->
            {
                if (keepPushingFilamentDialogBox == null)
                {
                    keepPushingFilamentDialogBox = new ChoiceLinkDialogBox(true);
                    keepPushingFilamentDialogBox.setTitle(Lookup.i18n(
                            "notification.keepPushingFilamentTitle"));
                    keepPushingFilamentDialogBox.setMessage(Lookup.i18n(
                            "notification.keepPushingFilament"));
                    try
                    {
                        keepPushingFilamentDialogBox.getUserInput();
                    } catch (PrinterDisconnectedException ex)
                    {
                        steno.error("printer disconnected");
                    }
                }
            });
        }
    }

    @Override
    public void hideKeepPushingFilamentNotification()
    {
        if (keepPushingFilamentDialogBox != null)
        {
            Platform.runLater(() ->
            {
                if (keepPushingFilamentDialogBox != null)
                {
                    keepPushingFilamentDialogBox.close();
                    keepPushingFilamentDialogBox = null;
                }
            });
        }
    }

    /**
     *
     * @param printer
     */
    @Override
    public void showEjectFailedDialog(Printer printer, int nozzleNumber)
    {
        if (failedEjectDialogBox == null)
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                failedEjectDialogBox = new ChoiceLinkDialogBox(true);
                failedEjectDialogBox.setTitle(Lookup.i18n("error.ERROR_UNLOAD"));
                failedEjectDialogBox.setMessage(Lookup.i18n(
                        "error.ERROR_UNLOAD.message"));

                ChoiceLinkButton ejectStuckMaterial = failedEjectDialogBox.addChoiceLink(
                        Lookup.i18n("error.ERROR_UNLOAD.action.title"));
                failedEjectDialogBox.addChoiceLink(
                        Lookup.i18n("error.ERROR_UNLOAD.noaction.title"));

                boolean runEjectStuckMaterial = false;

                Optional<ChoiceLinkButton> choice;
                try
                {
                    choice = failedEjectDialogBox.getUserInput();
                } catch (PrinterDisconnectedException ex)
                {
                    return;
                }
                if (choice.isPresent())
                {
                    if (choice.get() == ejectStuckMaterial)
                    {
                        runEjectStuckMaterial = true;
                    }
                }
                failedEjectDialogBox = null;

                if (runEjectStuckMaterial)
                {
                    steno.error("Eject failed - user chose to eject stuck material");
                    try
                    {
                        printer.ejectStuckMaterial(nozzleNumber, false, null);
                    } catch (PrinterException ex)
                    {
                        steno.error("Error when automatically invoking eject stuck material");
                    }
                } else
                {
                    steno.error("Eject failed - user chose not to run eject stuck material");
                }
            });
        }
    }

    @Override
    public void showFilamentStuckMessage()
    {
        if (filamentStuckDialogBox == null)
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                filamentStuckDialogBox = new ChoiceLinkDialogBox(true);
                filamentStuckDialogBox.
                        setTitle(Lookup.i18n("dialogs.filamentStuck.title"));
                filamentStuckDialogBox.setMessage(
                        Lookup.i18n("dialogs.filamentStuck.message"));

                ChoiceLinkButton ok = filamentStuckDialogBox.addChoiceLink(
                        Lookup.i18n("misc.OK"));

                try
                {
                    Optional<ChoiceLinkButton> choice = filamentStuckDialogBox.getUserInput();
                } catch (PrinterDisconnectedException ex)
                {
                    return;
                }

                filamentStuckDialogBox.close();
                filamentStuckDialogBox = null;
            });
        }
    }

    @Override
    public void showLoadFilamentNowMessage()
    {
        if (loadFilamentNowDialogBox == null)
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                loadFilamentNowDialogBox = new ChoiceLinkDialogBox(true);
                loadFilamentNowDialogBox.
                        setTitle(Lookup.i18n("dialogs.loadFilamentNow.title"));
                loadFilamentNowDialogBox.setMessage(
                        Lookup.i18n("dialogs.loadFilamentNow.message"));

                ChoiceLinkButton ok = loadFilamentNowDialogBox.addChoiceLink(
                        Lookup.i18n("misc.OK"));

                try
                {
                    Optional<ChoiceLinkButton> choice = loadFilamentNowDialogBox.getUserInput();
                } catch (PrinterDisconnectedException ex)
                {
                    steno.error("printer disconnected");
                }

                loadFilamentNowDialogBox.close();
                loadFilamentNowDialogBox = null;
            });
        }
    }

    @Override
    public void showFilamentMotionCheckBanner()
    {
        if (filamentMotionCheckDialogBox == null)
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                filamentMotionCheckDialogBox = new ChoiceLinkDialogBox(true);
                filamentMotionCheckDialogBox.
                        setTitle(Lookup.i18n("notification.printManagement.title"));
                filamentMotionCheckDialogBox.setMessage(
                        Lookup.i18n("notification.filamentMotionCheck"));

                try
                {
                    filamentMotionCheckDialogBox.getUserInput();
                } catch (PrinterDisconnectedException ex)
                {
                    steno.error("printer disconnected");
                }
            });
        }
    }

    @Override
    public void hideFilamentMotionCheckBanner()
    {
        if (filamentMotionCheckDialogBox != null)
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                if (filamentMotionCheckDialogBox != null)
                {
                    filamentMotionCheckDialogBox.close();
                    filamentMotionCheckDialogBox = null;
                }
            });
        }
    }

    @Override
    public boolean showModelIsInvalidDialog(Set<String> modelNames)
    {
        Callable<Boolean> askUserWhetherToLoadModel = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox(false);
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.modelInvalidTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n(
                        "dialogs.modelInvalidDescription"));

                ListView problemModels = new ListView();
                problemModels.getItems().addAll(modelNames);
                choiceLinkDialogBox.addControl(problemModels);

                problemModels.setMaxHeight(200);

                ChoiceLinkButton loadChoice = choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("dialogs.loadModel"));
                choiceLinkDialogBox.addChoiceLink(Lookup.i18n("dialogs.dontLoadModel"));

                Optional<ChoiceLinkButton> loadResponse = choiceLinkDialogBox.getUserInput();

                boolean loadModel = false;

                if (loadResponse.isPresent())
                {
                    loadModel = loadResponse.get() == loadChoice;
                }

                return loadModel;
            }
        };

        FutureTask<Boolean> askInvalidModelTask = new FutureTask<>(askUserWhetherToLoadModel);
        Lookup.getTaskExecutor().runOnGUIThread(askInvalidModelTask);
        try
        {
            return askInvalidModelTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during model invalid query");
            return false;
        }
    }

    @Override
    public void clearAllDialogsOnDisconnect()
    {
        if (errorChoiceBox != null)
        {
            Lookup.getTaskExecutor().runOnGUIThread(() ->
            {
                errorChoiceBox.closeDueToPrinterDisconnect();
            });
        }
    }
}
