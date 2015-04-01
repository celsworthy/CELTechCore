package celtech.printerControl.comms;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PauseStatus;
import celtech.printerControl.PrintJob;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.SendFile;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.commands.tx.ReadSendFileReport;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import celtech.utils.PrinterUtils;
import java.io.IOException;
import javafx.concurrent.WorkerStateEvent;
import jssc.SerialPort;
import libertysystems.configuration.ConfigItemIsAnArray;
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

    protected Stenographer steno = StenographerFactory.getStenographer(
        HardwareCommandInterface.class.getName());
    protected PrinterStatusConsumer controlInterface = null;
    protected String portName = null;
    protected Printer printerToUse = null;
    protected String printerFriendlyName = "Robox";
    protected RoboxCommsState commsState = RoboxCommsState.FOUND;
    protected PrinterID printerID = new PrinterID();

    protected final FirmwareLoadService firmwareLoadService = new FirmwareLoadService();
    protected String requiredFirmwareVersionString = "";
    protected float requiredFirmwareVersion = 0;

    protected boolean suppressPrinterIDChecks = false;
    protected int sleepBetweenStatusChecks = 1000;
    private boolean loadingFirmware = false;

    private String printerName = null;

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

        try
        {
            Configuration applicationConfiguration = Configuration.getInstance();
            try
            {
                requiredFirmwareVersionString = applicationConfiguration.getString(
                    ApplicationConfiguration.applicationConfigComponent, "requiredFirmwareVersion").
                    trim();
                requiredFirmwareVersion = Float.valueOf(requiredFirmwareVersionString);
            } catch (ConfigItemIsAnArray ex)
            {
                steno.error("Firmware version was an array... can't interpret firmware version");
            }
        } catch (ConfigNotLoadedException ex)
        {
            steno.error("Couldn't load configuration - will not be able to check firmware version");
        }

        firmwareLoadService.setOnSucceeded((WorkerStateEvent t) ->
        {
            FirmwareLoadResult result = (FirmwareLoadResult) t.getSource().getValue();
            Lookup.getSystemNotificationHandler().showFirmwareUpgradeStatusNotification(result);
            disconnectSerialPort();
        });

        firmwareLoadService.setOnFailed((WorkerStateEvent t) ->
        {
            FirmwareLoadResult result = (FirmwareLoadResult) t.getSource().getValue();
            Lookup.getSystemNotificationHandler().showFirmwareUpgradeStatusNotification(result);
            disconnectSerialPort();
        });

        Lookup.getSystemNotificationHandler().configureFirmwareProgressDialog(firmwareLoadService);
    }

    @Override

    public void run()
    {
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

                case CHECKING_FIRMWARE:
                    steno.debug("Check firmware " + portName);
                    if (loadingFirmware)
                    {
                        try
                        {
                            Thread.sleep(200);
                        } catch (InterruptedException ex)
                        {
                            steno.error("Interrupted while waiting for firmware to be loaded " + ex);
                        }
                        break;
                    }

                    FirmwareResponse firmwareResponse = null;
                    boolean loadRequiredFirmware = false;

                    try
                    {
                        firmwareResponse = printerToUse.readFirmwareVersion();

                        if (firmwareResponse.getFirmwareRevisionFloat() != requiredFirmwareVersion)
                        {
                            // The firmware version is different to that associated with AutoMaker
                            // Tell the user to update

                            steno.warning("Firmware version is "
                                + firmwareResponse.getFirmwareRevisionString() + " and should be "
                                + requiredFirmwareVersionString);

                            loadRequiredFirmware = Lookup.getSystemNotificationHandler().
                                askUserToUpdateFirmware();
                        }
                        if (loadRequiredFirmware)
                        {
                            loadingFirmware = true;
                            loadFirmware(requiredFirmwareVersionString);
                        } else
                        {
                            moveOnFromFirmwareCheck(firmwareResponse);
                        }
                    } catch (PrinterException ex)
                    {
                        steno.error("Exception whilst checking firmware version: " + ex);
                        disconnectSerialPort();
                    }
                    break;

                case CHECKING_ID:
                    steno.debug("Check id " + portName);

                    PrinterIDResponse lastPrinterIDResponse = null;

                    try
                    {
                        lastPrinterIDResponse = printerToUse.readPrinterID();

                        printerName = lastPrinterIDResponse.getPrinterFriendlyName();

                        if (printerName == null
                            || (printerName.length() > 0
                            && printerName.charAt(0) == '\0'))
                        {
                            steno.info("Connected to unknown printer");
                            Lookup.getSystemNotificationHandler().
                                showNoPrinterIDDialog(printerToUse);
                            lastPrinterIDResponse = printerToUse.readPrinterID();
                        } else
                        {
                            steno.info("Connected to printer " + printerName);
                        }
                    } catch (PrinterException ex)
                    {
                        steno.error("Error whilst checking printer ID");
                    }

                    commsState = RoboxCommsState.DETERMINING_PRINTER_STATUS;
                    break;

                case DETERMINING_PRINTER_STATUS:
                    steno.debug("Determining printer status on port " + portName);

                    try
                    {
                        StatusResponse statusResponse = (StatusResponse) writeToPrinter(
                            RoboxTxPacketFactory.createPacket(
                                TxPacketTypeEnum.STATUS_REQUEST), true);

                        determinePrinterStatus(statusResponse);

                        controlInterface.printerConnected(portName);
                        commsState = RoboxCommsState.CONNECTED;
                    } catch (RoboxCommsException ex)
                    {
                        if (printerFriendlyName != null)
                        {
                            steno.error("Failed to determine printer status on "
                                + printerFriendlyName);
                        } else
                        {
                            steno.error("Failed to determine printer status on unknown printer");
                        }
                        disconnectSerialPort();
                    }

                    break;

                case CONNECTED:
//                    steno.debug("CONNECTED " + portName);
                    try
                    {
                        this.sleep(sleepBetweenStatusChecks);

//                        steno.debug("STATUS REQUEST: " + portName);
                        writeToPrinter(RoboxTxPacketFactory.createPacket(
                            TxPacketTypeEnum.STATUS_REQUEST));

                        writeToPrinter(
                            RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.REPORT_ERRORS));
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

    private void moveOnFromFirmwareCheck(FirmwareResponse firmwareResponse)
    {
        if (suppressPrinterIDChecks == false)
        {
            commsState = RoboxCommsState.CHECKING_ID;
        } else
        {
            commsState = RoboxCommsState.DETERMINING_PRINTER_STATUS;
        }
        loadingFirmware = false;
    }

    private void loadFirmware(String versionToLoad)
    {
        firmwareLoadService.reset();
        firmwareLoadService.setPrinterToUse(printerToUse);
        firmwareLoadService.setFirmwareFileToLoad(
            ApplicationConfiguration.getCommonApplicationDirectory()
            + "robox_r" + versionToLoad + ".bin");
        firmwareLoadService.start();
    }

    public void shutdown()
    {
        steno.info("Shutdown command interface...");
        if (firmwareLoadService.isRunning())
        {
            steno.info("Shutdown command interface firmware service...");
            firmwareLoadService.cancel();
        }
        steno.info("Shutdown command interface complete");
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
     * @param messageToWrite
     * @param dontPublishResult
     * @return
     * @throws RoboxCommsException
     */
    public abstract RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite,
        boolean dontPublishResult) throws RoboxCommsException;

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

    private void determinePrinterStatus(StatusResponse statusResponse)
    {
        if (PrinterUtils.printJobIDIndicatesPrinting(statusResponse.getRunningPrintJobID()))
        {
            if (printerFriendlyName != null)
            {
                steno.error(printerFriendlyName + " is printing");
            } else
            {
                steno.error("Connected to an unknown printer that is printing");
            }

            ReadSendFileReport sendFileReport = (ReadSendFileReport) RoboxTxPacketFactory.
                createPacket(
                    TxPacketTypeEnum.READ_SEND_FILE_REPORT);

            try
            {
                SendFile sendFileData = (SendFile) writeToPrinter(sendFileReport, true);

                if (sendFileData.getFileID() != null && !sendFileData.getFileID().equals(""))
                {
                    steno.info("The printer is printing an incomplete job: File ID: "
                        + sendFileData.getFileID()
                        + " Expected sequence number: " + sendFileData.getExpectedSequenceNumber());

                    printerToUse.getPrintEngine().reEstablishTransfer(sendFileData.getFileID(),
                                                                      sendFileData.
                                                                      getExpectedSequenceNumber());
                }
            } catch (RoboxCommsException ex)
            {
                steno.error(
                    "Error determining whether the printer has a partially transferred job in progress");
            }

            String printJobID = printerToUse.printJobIDProperty().get();

            printerToUse.getPrintEngine().makeETCCalculatorForJobOfUUID(printJobID);
            Lookup.getSystemNotificationHandler().
                showDetectedPrintInProgressNotification();

            if (printerToUse.pauseStatusProperty().get() == PauseStatus.PAUSED)
            {
                printerToUse.setPrinterStatus(PrinterStatus.PAUSED);
            } else
            {
                printerToUse.setPrinterStatus(PrinterStatus.PRINTING);
            }
        }
    }
}
