package celtech.appManager;

import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.model.HardwarePrinter;
import celtech.printerControl.model.Printer;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;

/**
 *
 * @author Ian
 */
public interface SystemNotificationManager
{

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

    void processErrorPacketFromPrinter(AckResponse response, HardwarePrinter printer);

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

    public void showNoPrinterIDDialog(Printer printer);
}
