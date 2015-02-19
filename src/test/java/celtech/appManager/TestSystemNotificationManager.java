package celtech.appManager;

import celtech.configuration.fileRepresentation.HeadFile;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.model.Printer;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import celtech.utils.tasks.TaskResponder;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public class TestSystemNotificationManager implements SystemNotificationManager
{

    @Override
    public boolean askUserToUpdateFirmware()
    {
        return false;
    }

    @Override
    public void processErrorPacketFromPrinter(FirmwareError response, Printer printer)
    {
    }

    @Override
    public void showCalibrationDialogue()
    {
    }

    @Override
    public void showDetectedPrintInProgressNotification()
    {
    }

    @Override
    public void showFirmwareUpgradeStatusNotification(FirmwareLoadResult result)
    {
    }

    @Override
    public void showGCodePostProcessFailedNotification()
    {
    }

    @Override
    public void showGCodePostProcessSuccessfulNotification()
    {
    }

    @Override
    public void showHeadUpdatedNotification()
    {
    }

    @Override
    public void showPrintJobCancelledNotification()
    {
    }

    @Override
    public void showPrintJobFailedNotification()
    {
    }

    @Override
    public void showPrintTransferInitiatedNotification()
    {
    }

    @Override
    public void showPrintTransferSuccessfulNotification(String printerName)
    {
    }

    @Override
    public void showReprintStartedNotification()
    {
    }

    @Override
    public void showSDCardNotification()
    {
    }

    @Override
    public void showSliceFailedNotification()
    {
    }

    @Override
    public void showSliceSuccessfulNotification()
    {
    }

    @Override
    public void configureFirmwareProgressDialog(FirmwareLoadService firmwareLoadService)
    {
    }

    @Override
    public void showNoSDCardDialog()
    {
    }

    @Override
    public void showNoPrinterIDDialog(Printer printer)
    {
    }

    @Override
    public void showInformationNotification(String title, String message)
    {
    }

    @Override
    public void showWarningNotification(String title, String message)
    {
    }

    @Override
    public void showErrorNotification(String title, String message)
    {
    }

    @Override
    public boolean showOpenDoorDialog()
    {
        return false;
    }

    @Override
    public boolean showModelTooBigDialog(String modelFilename)
    {
        return false;
    }

    @Override
    public boolean showApplicationUpgradeDialog(String applicationName)
    {
        return false;
    }

    @Override
    public boolean showJobsTransferringShutdownDialog()
    {
        return false;
    }

    @Override
    public void showProgramInvalidHeadDialog(TaskResponder<HeadFile> taskResponse)
    {
    }

    @Override
    public void showHeadNotRecognisedDialog(String printerName)
    {
    }

    @Override
    public PurgeResponse showPurgeDialog()
    {
        return PurgeResponse.PRINT_WITH_PURGE;
    }

    @Override
    public void showReelNotRecognisedDialog(String printerName)
    {
    }

    @Override
    public void showReelUpdatedNotification()
    {
    }

    @Override
    public Optional<PrinterErrorChoice> showPrinterErrorDialog(String title, String message,
        boolean showContinueOption, boolean showAbortOption, boolean showRetryOption, boolean showOKOption)
    {
        return Optional.empty();
    }

    @Override
    public void askUserToClearBed()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
