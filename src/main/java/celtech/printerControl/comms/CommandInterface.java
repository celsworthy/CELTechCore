/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms;

import celtech.appManager.Notifier;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.ProgressDialog;
import celtech.printerControl.model.Printer;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.services.firmware.FirmwareLoadService;
import celtech.services.firmware.FirmwareLoadTask;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import jssc.SerialPort;
import libertysystems.configuration.ConfigNotLoadedException;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author Ian
 */
public abstract class CommandInterface extends Thread
{

    protected boolean keepRunning = true;
    protected boolean initialised = false;

    protected Stenographer steno = StenographerFactory.getStenographer(HardwareCommandInterface.class.getName());
    protected PrinterStatusConsumer controlInterface = null;
    protected String portName = null;
    protected Printer printerToUse = null;
    protected String printerFriendlyName = "Robox";
    protected RoboxCommsState commsState = RoboxCommsState.FOUND;
    protected SerialPort serialPort = null;
    protected PrinterID printerID = new PrinterID();
    /*
     * 
     */
    protected Dialogs.CommandLink firmwareUpgradeOK = null;
    protected Dialogs.CommandLink firmwareUpgradeNotOK = null;
    protected Dialogs.CommandLink firmwareDowngradeOK = null;
    protected Dialogs.CommandLink firmwareDowngradeNotOK = null;

    protected ProgressDialog firmwareUpdateProgress = null;
    protected final FirmwareLoadService firmwareLoadService = new FirmwareLoadService();
    protected ResourceBundle languageBundle = null;
    protected int requiredFirmwareVersion = 0;

    protected PrinterIDDialog printerIDDialog = null;
    protected boolean printerIDDialogWillShow = false;
    protected boolean printerIDDialogWasShowing = false;
    protected PrinterIDResponse lastPrinterIDResponse = null;
    protected int printerIDSetupAttempts = 0;

    protected boolean firmwareCheckInProgress = false;

    protected boolean suppressPrinterIDChecks = false;
    protected int sleepBetweenStatusChecks = 1000;

    protected ModalDialog noSDDialog = null;
    
    private 

    /**
     *
     * @param controlInterface
     * @param portName
     * @param suppressPrinterIDChecks
     * @param sleepBetweenStatusChecks
     */
    public CommandInterface(PrinterStatusConsumer controlInterface, String portName,
        boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        this.controlInterface = controlInterface;
        this.portName = portName;
        this.suppressPrinterIDChecks = suppressPrinterIDChecks;
        this.sleepBetweenStatusChecks = sleepBetweenStatusChecks;

        languageBundle = DisplayManager.getLanguageBundle();
        try
        {
            Configuration applicationConfiguration = Configuration.getInstance();
            requiredFirmwareVersion = applicationConfiguration.getInt(
                ApplicationConfiguration.applicationConfigComponent, "requiredFirmwareVersion");
        } catch (ConfigNotLoadedException ex)
        {
            steno.error("Couldn't load configuration - will not be able to check firmware version");
        }

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                firmwareUpdateProgress = new ProgressDialog(firmwareLoadService);

                printerIDDialog = new PrinterIDDialog();

                noSDDialog = new ModalDialog();
                noSDDialog.setTitle(languageBundle.getString("dialogs.noSDCardTitle"));
                noSDDialog.setMessage(languageBundle.getString("dialogs.noSDCardMessage"));
                noSDDialog.addButton(languageBundle.getString("dialogs.noSDCardOK"));
                initialised = true;
            }
        });

        firmwareUpgradeOK = new Dialogs.CommandLink(languageBundle.getString(
            "dialogs.firmwareUpgradeOKTitle"), languageBundle.getString(
                                                        "dialogs.firmwareUpgradeOKMessage"));
        firmwareUpgradeNotOK = new Dialogs.CommandLink(languageBundle.getString(
            "dialogs.firmwareUpgradeNotOKTitle"), languageBundle.getString(
                                                           "dialogs.firmwareUpgradeNotOKMessage"));
        firmwareDowngradeOK = new Dialogs.CommandLink(languageBundle.getString(
            "dialogs.firmwareDowngradeOKTitle"), languageBundle.getString(
                                                          "dialogs.firmwareUpgradeOKMessage"));
        firmwareDowngradeNotOK = new Dialogs.CommandLink(languageBundle.getString(
            "dialogs.firmwareDowngradeNotOKTitle"), languageBundle.getString(
                                                             "dialogs.firmwareUpgradeNotOKMessage"));

        firmwareLoadService.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                int firmwareUpgradeState = (int) t.getSource().getValue();

                switch (firmwareUpgradeState)
                {
                    case FirmwareLoadTask.SDCARD_ERROR:
                        Notifier.showErrorNotification(languageBundle.getString(
                            "dialogs.firmwareUpgradeFailedTitle"),
                                                       languageBundle.getString(
                                                           "dialogs.sdCardError"));
                        break;
                    case FirmwareLoadTask.FILE_ERROR:
                        Notifier.showErrorNotification(languageBundle.getString(
                            "dialogs.firmwareUpgradeFailedTitle"),
                                                       languageBundle.getString(
                                                           "dialogs.firmwareFileError"));
                        break;
                    case FirmwareLoadTask.OTHER_ERROR:
                        Notifier.showErrorNotification(languageBundle.getString(
                            "dialogs.firmwareUpgradeFailedTitle"),
                                                       languageBundle.getString(
                                                           "dialogs.firmwareUpgradeFailedMessage"));
                        break;
                    case FirmwareLoadTask.SUCCESS:
                        Notifier.showInformationNotification(languageBundle.getString(
                            "dialogs.firmwareUpgradeSuccessTitle"),
                                                             languageBundle.getString(
                                                                 "dialogs.firmwareUpgradeSuccessMessage"));
                        break;
                }
                firmwareCheckInProgress = false;
            }
        });

        firmwareLoadService.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {

                Notifier.showErrorNotification(DisplayManager.getLanguageBundle().getString(
                    "dialogs.firmwareUpgradeFailedTitle"),
                                               DisplayManager.getLanguageBundle().getString(
                                                   "dialogs.firmwareUpgradeFailedMessage"));
                firmwareCheckInProgress = false;
            }
        });
    }

    /**
     *
     * @param sleepMillis
     */
    protected abstract void setSleepBetweenStatusChecks(int sleepMillis);

    /**
     *
     * @param messageToWrite
     * @return
     * @throws RoboxCommsException
     */
    public abstract RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException;

}
