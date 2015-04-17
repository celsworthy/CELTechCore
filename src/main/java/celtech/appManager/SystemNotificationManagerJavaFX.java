package celtech.appManager;

import celtech.Lookup;
import celtech.appManager.errorHandling.SystemErrorHandlerOptions;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.fileRepresentation.HeadFile;
import celtech.coreUI.components.ChoiceLinkButton;
import celtech.coreUI.components.ChoiceLinkDialogBox;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.scene.control.ChoiceDialog;
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
    private boolean errorDialogOnDisplay = false;

    private HashMap<SystemErrorHandlerOptions, ChoiceLinkButton> errorToButtonMap = null;

    /*
     * Error dialog
     */

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

    private boolean programInvalidHeadDialogOnDisplay = false;

    private boolean headNotRecognisedDialogOnDisplay = false;

    private boolean reelNotRecognisedDialogOnDisplay = false;

    private boolean clearBedDialogOnDisplay = false;

    private ChoiceLinkDialogBox failedTransferDialogBox = null;

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
        Lookup.getTaskExecutor().runOnGUIThread(() ->
        {
            //Don't show B POSITION LOST errors for the moment...
            if (error != FirmwareError.B_POSITION_LOST)
            {
                if (!errorDialogOnDisplay)
                {
                    errorDialogOnDisplay = true;

                    setupErrorOptions();

                    ChoiceLinkDialogBox errorChoiceBox = new ChoiceLinkDialogBox();
                    errorChoiceBox.setTitle(error.getLocalisedErrorTitle());
                    errorChoiceBox.setMessage(error.getLocalisedErrorMessage());
                    error.getOptions()
                        .stream()
                        .forEach(option -> errorChoiceBox.
                            addChoiceLink(errorToButtonMap.get(option)));

                    Optional<ChoiceLinkButton> buttonPressed = errorChoiceBox.getUserInput();

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
                                    case OK_CONTINUE:
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
                                }
                                break;
                            }
                        }
                    }

                    errorDialogOnDisplay = false;
                }
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
            ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
            choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.headUpdateCalibrationRequiredTitle"));
            choiceLinkDialogBox.setMessage(Lookup.i18n(
                "dialogs.headUpdateCalibrationRequiredInstruction"));
            ChoiceLinkButton okCalibrateChoice = choiceLinkDialogBox.addChoiceLink(
                Lookup.i18n("dialogs.headUpdateCalibrationYes"));
            ChoiceLinkButton dontCalibrateChoice = choiceLinkDialogBox.addChoiceLink(
                Lookup.i18n("dialogs.headUpdateCalibrationNo"));

            Optional<ChoiceLinkButton> calibrationResponse = choiceLinkDialogBox.
                getUserInput();

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
                    failedTransferDialogBox = new ChoiceLinkDialogBox();
                    failedTransferDialogBox.setTitle(Lookup.i18n("notification.PrintQueueTitle"));
                    failedTransferDialogBox.setMessage(Lookup.i18n(
                        "notification.printTransferFailed"));

                    failedTransferDialogBox.addChoiceLink(Lookup.i18n("misc.OK"));

                    failedTransferDialogBox.getUserInput();
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
    public boolean askUserToUpdateFirmware()
    {
        Callable<Boolean> askUserToUpgradeDialog = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.noSDCardTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n(
                    "dialogs.noSDCardMessage"));
                ChoiceLinkButton openTheLidChoice = choiceLinkDialogBox.addChoiceLink(
                    Lookup.i18n("misc.OK"));

                choiceLinkDialogBox.getUserInput();

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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.purgeRequiredTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n(
                    "dialogs.purgeRequiredInstruction"));
                ChoiceLinkButton purge = choiceLinkDialogBox.addChoiceLink(
                    Lookup.i18n("dialogs.goForPurgeTitle"),
                    Lookup.i18n("dialogs.goForPurgeInstruction"));
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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
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

                choiceLinkDialogBox.getUserInput();

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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
                choiceLinkDialogBox.setTitle(title);
                choiceLinkDialogBox.setMessage(message);

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
                    choiceLinkDialogBox.addChoiceLink(continueChoice);
                }

                if (showAbortOption)
                {
                    choiceLinkDialogBox.addChoiceLink(abortChoice);
                }

                if (showRetryOption)
                {
                    choiceLinkDialogBox.addChoiceLink(retryChoice);
                }

                if (showOKOption)
                {
                    choiceLinkDialogBox.addChoiceLink(okChoice);
                }

                Optional<ChoiceLinkButton> response = choiceLinkDialogBox.getUserInput();

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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.reelNotRecognisedTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n("dialogs.reelNotRecognisedMessage1")
                    + " "
                    + printerName
                    + " "
                    + Lookup.i18n("dialogs.reelNotRecognisedMessage2")
                    + " "
                    + ApplicationConfiguration.getApplicationName());

                choiceLinkDialogBox.addChoiceLink(Lookup.i18n("misc.OK"));

                choiceLinkDialogBox.getUserInput();

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
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
                choiceLinkDialogBox.setTitle(Lookup.i18n("dialogs.clearBedTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n("dialogs.clearBedInstruction"));

                choiceLinkDialogBox.addChoiceLink(Lookup.i18n("misc.OK"));

                choiceLinkDialogBox.getUserInput();

                clearBedDialogOnDisplay = false;
            });
        }
    }

    /**
     * Returns 0 for no downgrade and 1 for downgrade
     *
     * @return True if the user has decided to switch to Advanced Mode, otherwise false
     */
    @Override
    public boolean confirmAdvancedMode()
    {
        Callable<Boolean> confirmAdvancedModeDialog = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
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
            steno.error("Error during advanced mode query");
            return false;
        }
    }
}
