package celtech.printerControl.comms;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.StatusRequest;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import celtech.utils.PrinterUtils;
import java.util.Locale;
import javafx.concurrent.WorkerStateEvent;
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
    protected DeviceDetector.DetectedPrinter printerHandle = null;
    protected Printer printerToUse = null;
    protected String printerFriendlyName = "Robox";
    protected RoboxCommsState commsState = RoboxCommsState.FOUND;
    protected PrinterID printerID = new PrinterID();

    protected final FirmwareLoadService firmwareLoadService = new FirmwareLoadService();
    protected String requiredFirmwareVersionString = "";
    protected float requiredFirmwareVersion = 0;
    protected float firmwareVersionInUse = 0;

    protected boolean suppressPrinterIDChecks = false;
    protected int sleepBetweenStatusChecks = 1000;
    private boolean loadingFirmware = false;

    protected boolean suppressComms = false;

    private String printerName = null;

    private PrinterIDResponse lastPrinterIDResponse = null;

    private boolean isConnected = false;

    /**
     *
     * @param controlInterface
     * @param printerHandle
     * @param suppressPrinterIDChecks
     * @param sleepBetweenStatusChecks
     */
    public CommandInterface(PrinterStatusConsumer controlInterface,
            DeviceDetector.DetectedPrinter printerHandle,
            boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        this.controlInterface = controlInterface;
        this.printerHandle = printerHandle;
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
            disconnectPrinter();
        });

        firmwareLoadService.setOnFailed((WorkerStateEvent t) ->
        {
            FirmwareLoadResult result = (FirmwareLoadResult) t.getSource().getValue();
            Lookup.getSystemNotificationHandler().showFirmwareUpgradeStatusNotification(result);
            disconnectPrinter();
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
                    steno.debug("Trying to connect to printer in " + printerHandle);

                    boolean printerCommsOpen = connectToPrinter();
                    if (printerCommsOpen)
                    {
                        steno.debug("Connected to Robox on " + printerHandle);
                        commsState = RoboxCommsState.CHECKING_FIRMWARE;
                    } else
                    {
                        steno.error("Failed to connect to Robox on " + printerHandle);
                        controlInterface.failedToConnect(printerHandle);
                        keepRunning = false;
                    }
                    break;

                case CHECKING_FIRMWARE:
                    steno.debug("Check firmware " + printerHandle);
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
                            steno.warning("Firmware version is "
                                    + firmwareResponse.getFirmwareRevisionString() + " and should be "
                                    + requiredFirmwareVersionString);

                            //ROB-931 - don't check for presence of the SD card if firmware version earlier than 691
                            if (firmwareResponse.getFirmwareRevisionFloat() >= 691)
                            {
//                            Lookup.setFirmwareVersion()
                                // Is the SD card present?
                                try
                                {
                                    StatusRequest request = (StatusRequest) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.STATUS_REQUEST);
                                    firmwareVersionInUse = firmwareResponse.getFirmwareRevisionFloat();
                                    StatusResponse response = (StatusResponse) writeToPrinter(request, true);
                                    if (!response.isSDCardPresent())
                                    {
                                        steno.warning("SD Card not present");
                                        Lookup.getSystemNotificationHandler().processErrorPacketFromPrinter(FirmwareError.SD_CARD, printerToUse);
                                        disconnectPrinter();
                                        keepRunning = false;
                                        break;
                                    } else
                                    {
                                        Lookup.getSystemNotificationHandler().clearAllDialogsOnDisconnect();
                                    }
                                } catch (RoboxCommsException ex)
                                {
                                    steno.error("Failure during printer status request. " + ex.toString());
                                    break;
                                }
                            }

                            // Tell the user to update
                            loadRequiredFirmware = Lookup.getSystemNotificationHandler().
                                    askUserToUpdateFirmware();
                        }

                        if (loadRequiredFirmware)
                        {
                            loadingFirmware = true;
                            loadFirmware(ApplicationConfiguration.getCommonApplicationDirectory()
                                    + "robox_r" + requiredFirmwareVersionString + ".bin");
                        } else
                        {
                            firmwareVersionInUse = firmwareResponse.getFirmwareRevisionFloat();
                            moveOnFromFirmwareCheck(firmwareResponse);
                        }
                    } catch (PrinterException ex)
                    {
                        steno.error("Exception whilst checking firmware version: " + ex);
                        disconnectPrinter();
                    }
                    break;

                case CHECKING_ID:
                    steno.debug("Check id " + printerHandle);

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
                    steno.debug("Determining printer status on port " + printerHandle);

                    try
                    {
                        StatusResponse statusResponse = (StatusResponse) writeToPrinter(
                                RoboxTxPacketFactory.createPacket(
                                        TxPacketTypeEnum.STATUS_REQUEST), true);

                        determinePrinterStatus(statusResponse);

                        controlInterface.printerConnected(printerHandle);

                        //Stash the connected printer info
                        String printerIDToUse = null;
                        if (lastPrinterIDResponse != null
                                && lastPrinterIDResponse.getAsFormattedString() != null)
                        {
                            printerIDToUse = lastPrinterIDResponse.getAsFormattedString();
                        }
                        ApplicationConfiguration.setLastPrinterAttached(printerIDToUse, String.format(Locale.UK, "%.2f", firmwareVersionInUse));

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
                        disconnectPrinter();
                    }

                    break;

                case CONNECTED:
//                    steno.debug("CONNECTED " + portName);
                    try
                    {
                        this.sleep(sleepBetweenStatusChecks);

                        if (!suppressComms && isConnected)
                        {
                            try
                            {
//                        steno.debug("STATUS REQUEST: " + portName);
                                CommandInterface.this.writeToPrinter(RoboxTxPacketFactory.createPacket(
                                        TxPacketTypeEnum.STATUS_REQUEST));

                                CommandInterface.this.writeToPrinter(RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.REPORT_ERRORS));
                            } catch (RoboxCommsException ex)
                            {
                                if (isConnected)
                                {
                                    steno.exception("Failure during printer status request.", ex);
                                }
                            }
                        }
                    } catch (InterruptedException ex)
                    {
                        steno.info("Comms interrupted");
                    }
                    break;

                case DISCONNECTED:
                    steno.debug("state is disconnected");
                    break;
            }
        }
        steno.info(
                "Handler for " + printerHandle + " exiting");
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

    public void loadFirmware(String firmwareFilePath)
    {
        suppressComms = true;
        this.interrupt();
        firmwareLoadService.reset();
        firmwareLoadService.setPrinterToUse(printerToUse);
        firmwareLoadService.setFirmwareFileToLoad(firmwareFilePath);
        firmwareLoadService.start();
    }

    public void shutdown()
    {
        steno.info("Shutdown command interface...");
        suppressComms = true;
        if (firmwareLoadService.isRunning())
        {
            steno.info("Shutdown command interface firmware service...");
            firmwareLoadService.cancel();
        }
        steno.debug("set state to disconnected");
        commsState = RoboxCommsState.DISCONNECTED;
        disconnectPrinter();
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
    public final synchronized RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        if (isConnected)
        {
            return writeToPrinter(messageToWrite, false);
        } else
        {
            return null;
        }
    }

    /**
     *
     * @param messageToWrite
     * @param dontPublishResult
     * @return
     * @throws RoboxCommsException
     */
    public final synchronized RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite, boolean dontPublishResult) throws RoboxCommsException
    {
        if (isConnected)
        {
            return writeToPrinterImpl(messageToWrite, dontPublishResult);
        } else
        {
            return null;
        }
    }

    public abstract RoboxRxPacket writeToPrinterImpl(RoboxTxPacket messageToWrite,
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
     * @return
     */
    public final boolean connectToPrinter()
    {
        isConnected = connectToPrinterImpl();
        return isConnected;
    }

    /**
     *
     * @return
     */
    protected abstract boolean connectToPrinterImpl();

    /**
     *
     */
    protected final void disconnectPrinter()
    {
        isConnected = false;
        disconnectPrinterImpl();
    }

    /**
     *
     */
    protected abstract void disconnectPrinterImpl();

    private void determinePrinterStatus(StatusResponse statusResponse)
    {
        if (PrinterUtils.printJobIDIndicatesPrinting(statusResponse.getRunningPrintJobID()))
        {
            if (printerFriendlyName != null)
            {
                steno.info(printerFriendlyName + " is printing");
            } else
            {
                steno.error("Connected to an unknown printer that is printing");
            }

        }
    }
}
