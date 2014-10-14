package celtech.printerControl.comms;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.ProgressDialog;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.model.Printer;
import celtech.services.firmware.FirmwareLoadService;
import celtech.services.firmware.FirmwareLoadTask;
import static java.lang.Thread.sleep;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import jssc.SerialPort;
import libertysystems.configuration.ConfigNotLoadedException;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

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

        Platform.runLater(() ->
        {
            firmwareUpdateProgress = new ProgressDialog(firmwareLoadService);

            printerIDDialog = new PrinterIDDialog();

            noSDDialog = new ModalDialog();
            noSDDialog.setTitle(languageBundle.getString("dialogs.noSDCardTitle"));
            noSDDialog.setMessage(languageBundle.getString("dialogs.noSDCardMessage"));
            noSDDialog.addButton(languageBundle.getString("dialogs.noSDCardOK"));
            initialised = true;
        });

        firmwareLoadService.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                int firmwareUpgradeState = (int) t.getSource().getValue();

                Lookup.getSystemNotificationHandler().showFirmwareUpgradeFailedNotification(firmwareUpgradeState);
                firmwareCheckInProgress = false;
            }
        });

        firmwareLoadService.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                Lookup.getSystemNotificationHandler().showFirmwareUpgradeFailedNotification(FirmwareLoadTask.OTHER_ERROR);
                firmwareCheckInProgress = false;
            }
        });
    }

    @Override

    public void run()
    {
        while (!initialised)
        {
            try
            {
                sleep(100);
            } catch (InterruptedException ex)
            {

            }
        }

        while (keepRunning)
        {
            switch (commsState)
            {
                case FOUND:
                    steno.debug("Trying to connect to printer in " + portName);

                    boolean printerCommsOpen = connectToPrinter(portName);
                    if (printerCommsOpen)
                    {
                        steno.debug("Connected to Robox on " + portName);
                        commsState = RoboxCommsState.CHECKING_FIRMWARE;
                    } else
                    {
                        steno.debug("Failed to connect to Robox on " + portName);
                        controlInterface.failedToConnect(portName);
                        keepRunning = false;
                    }
                    break;
                    
                case POST:
                    try
                    {
                        StatusResponse response = (StatusResponse) writeToPrinter(
                            RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.STATUS_REQUEST));
                        printerToUse.processRoboxResponse(response);
                        commsState = RoboxCommsState.CHECKING_FIRMWARE;
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error whilst carrying out firmware POST");
                        disconnectSerialPort();
                    }

                    break;

                case CHECKING_FIRMWARE:

                    if (firmwareCheckInProgress == false && noSDDialog.isShowing() == false)
                    {
                        try
                        {
                            RoboxRxPacket response = writeToPrinter(
                                RoboxTxPacketFactory.createPacket(
                                    TxPacketTypeEnum.QUERY_FIRMWARE_VERSION));

                            if (response instanceof FirmwareResponse)
                            {
                                FirmwareResponse fwResponse = (FirmwareResponse) response;
                                steno.info("Firmware v " + fwResponse.getFirmwareRevision()
                                    + " returned");

                                if (fwResponse.getFirmwareRevisionInt() > requiredFirmwareVersion)
                                {
                                    //The firmware version is higher than that associated with AutoMaker
                                    // Tell the user to downgrade

                                    steno.warning("Firmware version is "
                                        + fwResponse.getFirmwareRevisionInt() + " and should be "
                                        + requiredFirmwareVersion);

                                    firmwareCheckInProgress = true;

                                    boolean firmwareDecision = Lookup.getSystemNotificationHandler().askUserToDowngradeFirmware(requiredFirmwareVersion, fwResponse.getFirmwareRevisionInt());

                                    if (firmwareDecision == false)
                                    {
                                        //Proceed at risk
                                        printerToUse.processRoboxResponse(response);
                                        if (suppressPrinterIDChecks == false)
                                        {
                                            commsState = RoboxCommsState.CHECKING_ID;
                                        } else
                                        {
                                            controlInterface.printerConnected(portName);
                                            commsState = RoboxCommsState.CONNECTED;
                                        }
                                        firmwareCheckInProgress = false;
                                    } else
                                    {
                                        firmwareLoadService.reset();
                                        firmwareLoadService.setPrinterToUse(printerToUse);
                                        firmwareLoadService.setFirmwareFileToLoad(
                                            ApplicationConfiguration.getCommonApplicationDirectory()
                                            + "robox_r" + requiredFirmwareVersion + ".bin");
                                        firmwareLoadService.start();
                                    }
                                } else if (fwResponse.getFirmwareRevisionInt()
                                    < requiredFirmwareVersion)
                                {
                                    firmwareCheckInProgress = true;
                                    steno.warning("Firmware version is "
                                        + fwResponse.getFirmwareRevisionInt() + " and should be "
                                        + requiredFirmwareVersion);

                                    boolean firmwareDecision = Lookup.getSystemNotificationHandler().askUserToUpgradeFirmware(requiredFirmwareVersion, fwResponse.getFirmwareRevisionInt());

                                    if (firmwareDecision == true)
                                    {
                                        firmwareLoadService.reset();
                                        firmwareLoadService.setPrinterToUse(printerToUse);
                                        firmwareLoadService.setFirmwareFileToLoad(
                                            ApplicationConfiguration.getCommonApplicationDirectory()
                                            + "robox_r" + requiredFirmwareVersion + ".bin");
                                        firmwareLoadService.start();
                                    } else
                                    {
                                        //Proceed at risk
                                        printerToUse.processRoboxResponse(response);
                                        if (suppressPrinterIDChecks == false)
                                        {
                                            commsState = RoboxCommsState.CHECKING_ID;
                                        } else
                                        {
                                            controlInterface.printerConnected(portName);
                                            commsState = RoboxCommsState.CONNECTED;
                                        }
                                        firmwareCheckInProgress = false;
                                    }
                                } else
                                {
                                    printerToUse.processRoboxResponse(response);
                                    if (suppressPrinterIDChecks == false)
                                    {
                                        commsState = RoboxCommsState.CHECKING_ID;
                                    } else
                                    {
                                        controlInterface.printerConnected(portName);
                                        commsState = RoboxCommsState.CONNECTED;
                                    }
                                }
                            } else
                            {
                                steno.error(
                                    "Wrong type of packet returned from firmware version request");
                                disconnectSerialPort();
                            }
                        } catch (RoboxCommsException ex)
                        {
                            steno.error("Failure during firmware version request. " + ex.toString());
                        }
                    } else
                    {
                        try
                        {
                            sleep(100);
                        } catch (InterruptedException ex)
                        {

                        }
                    }
                    break;

                case CHECKING_ID:

                    String printerID = null;
                    Color printerColour = null;
                    boolean success = false;

                    try
                    {
                        if (printerIDDialog.isShowing() || printerIDDialogWillShow)
                        {
                            if (printerIDDialog.isShowing())
                            {
                                printerIDDialogWasShowing = true;
                                printerIDDialogWillShow = false;
                            }
                            StatusResponse testStatusResp = (StatusResponse) writeToPrinter(
                                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.STATUS_REQUEST));
                            sleep(250);
                        } else
                        {
                            lastPrinterIDResponse = (PrinterIDResponse) writeToPrinter(
                                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID));
                            if (lastPrinterIDResponse != null)
                            {
                                printerID = lastPrinterIDResponse.getPrinterFriendlyName();

                                if (printerIDSetupAttempts < 3 && (printerID == null
                                    || printerID.length() > 0 && printerID.charAt(0) == '\0'))
                                {
                                    // The printer ID hasn't been set up
                                    printerIDDialogWillShow = true;
                                    printerIDSetupAttempts++;
                                    Platform.runLater(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            ApplicationStatus.getInstance().setMode(
                                                ApplicationMode.STATUS);
                                            printerIDDialog.setPrinterToUse(printerToUse);
                                            printerIDDialog.show();
                                        }
                                    });
                                } else
                                {
                                    success = true;
                                }
                            }
                        }
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Failure during printer ID checking");
                        disconnectSerialPort();
                    } catch (InterruptedException ex)
                    {
                        steno.debug("Comms interrupted");
                    }

                    if (success)
                    {
                        controlInterface.printerConnected(portName);
                        printerToUse.processRoboxResponse(lastPrinterIDResponse);
                        commsState = RoboxCommsState.CONNECTED;
                    }
                    break;

                case CONNECTED:
                    try
                    {
                        this.sleep(sleepBetweenStatusChecks);

                        RoboxRxPacket response = writeToPrinter(RoboxTxPacketFactory.createPacket(
                            TxPacketTypeEnum.STATUS_REQUEST));
                        if (response != null && response instanceof StatusResponse)
                        {
                            steno.trace("Got " + response.toString() + " from printer.");
                            printerToUse.processRoboxResponse(response);
                        } else
                        {
                            steno.warning("No valid response from printer");
                        }

                        AckResponse errors = (AckResponse) writeToPrinter(
                            RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.REPORT_ERRORS));
                        if (errors != null)
                        {
                            steno.trace(errors.toString());
                            printerToUse.processRoboxResponse(errors);
                        }
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Failure during printer status request. " + ex.toString());
                    } catch (InterruptedException ex)
                    {
                        steno.debug("Comms interrupted");
                    }
                    break;
            }
        }
        steno.info(
            "Handler for " + portName + " exiting");
    }
    
    public void shutdown()
    {
        if (firmwareLoadService.isRunning())
        {
            firmwareLoadService.cancel();
        }
        keepRunning = false;
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

    /**
     *
     * @param printer
     */
    public void setPrinter(Printer printer)
    {
        this.printerToUse = printer;
    }

    /**
     *
     * @param commsPortName
     * @return
     */
    protected abstract boolean connectToPrinter(String commsPortName);

    /**
     *
     */
    protected abstract void disconnectSerialPort();
}
