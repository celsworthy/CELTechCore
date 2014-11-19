package celtech.appManager;

import celtech.configuration.fileRepresentation.HeadFile;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.model.Printer;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import celtech.utils.tasks.TaskResponder;

/**
 *
 * @author Ian
 */
public interface SystemNotificationManager
{

    void showInformationNotification(String title, String message);

    void showWarningNotification(String title, String message);

    void showErrorNotification(String title, String message);

    /**
     *
     * @param requiredFirmwareVersion
     * @param actualFirmwareVersion
     * @return True if the user has agreed to downgrade, otherwise false
     */
    boolean askUserToDowngradeFirmware(int requiredFirmwareVersion, int actualFirmwareVersion);

    /**
     * Returns 0 for no downgrade and 1 for downgrade
     *
     * @param requiredFirmwareVersion
     * @param actualFirmwareVersion
     * @return True if the user has agreed to upgrade, otherwise false
     */
    boolean askUserToUpgradeFirmware(int requiredFirmwareVersion, int actualFirmwareVersion);

    void processErrorPacketFromPrinter(FirmwareError error, Printer printer);

    void showCalibrationDialogue();

    void showDetectedPrintInProgressNotification();

    /**
     *
     * @param result
     */
    void showFirmwareUpgradeStatusNotification(FirmwareLoadResult result);

    void showGCodePostProcessFailedNotification();

    void showGCodePostProcessSuccessfulNotification();

    void showHeadUpdatedNotification();

    void showPrintJobCancelledNotification();

    void showPrintJobFailedNotification();

    void showPrintTransferInitiatedNotification();

    void showPrintTransferSuccessfulNotification(String printerName);

    void showReprintStartedNotification();

    void showSDCardNotification();

    void showSliceFailedNotification();

    void showSliceSuccessfulNotification();

    void configureFirmwareProgressDialog(FirmwareLoadService firmwareLoadService);

    public void showNoSDCardDialog();

    void showNoPrinterIDDialog(Printer printer);

    boolean showOpenDoorDialog();

    boolean showModelTooBigDialog(String modelFilename);
    
    boolean showApplicationUpgradeDialog(String applicationName);

    public boolean showPurgeDialog();

    public boolean showJobsTransferringShutdownDialog();

    public void showProgramInvalidHeadDialog(TaskResponder<HeadFile> taskResponse);

    public void showHeadNotRecognisedDialog(String printerName);
}
