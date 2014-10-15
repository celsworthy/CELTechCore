package celtech.printerControl.comms;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
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
import celtech.printerControl.model.PrinterException;
import celtech.services.firmware.FirmwareLoadResult;
import celtech.services.firmware.FirmwareLoadService;
import static java.lang.Thread.sleep;
import javafx.concurrent.WorkerStateEvent;
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

    protected Stenographer steno = StenographerFactory.getStenographer(HardwareCommandInterface.class.getName());
    protected PrinterStatusConsumer controlInterface = null;
    protected String portName = null;
    protected Printer printerToUse = null;
    protected String printerFriendlyName = "Robox";
    protected RoboxCommsState commsState = RoboxCommsState.FOUND;
    protected SerialPort serialPort = null;
    protected PrinterID printerID = new PrinterID();

    protected final FirmwareLoadService firmwareLoadService = new FirmwareLoadService();
    protected int requiredFirmwareVersion = 0;

    protected boolean suppressPrinterIDChecks = false;
    protected int sleepBetweenStatusChecks = 1000;

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
            requiredFirmwareVersion = applicationConfiguration.getInt(
                ApplicationConfiguration.applicationConfigComponent, "requiredFirmwareVersion");
        } catch (ConfigNotLoadedException ex)
        {
            steno.error("Couldn't load configuration - will not be able to check firmware version");
        }

        firmwareLoadService.setOnSucceeded((WorkerStateEvent t) ->
        {
            FirmwareLoadResult result = (FirmwareLoadResult) t.getSource().getValue();

            Lookup.getSystemNotificationHandler().showFirmwareUpgradeStatusNotification(result);
            moveOnFromFirmwareCheck(null);
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
                    FirmwareResponse firmwareResponse = null;
                    boolean loadRequiredFirmware = false;

                    try
                    {
                        firmwareResponse = printerToUse.readFirmwareVersion();

                        if (firmwareResponse.getFirmwareRevisionInt() > requiredFirmwareVersion)
                        {
                            //The firmware version is higher than that associated with AutoMaker
                            // Tell the user to downgrade

                            steno.warning("Firmware version is "
                                + firmwareResponse.getFirmwareRevisionInt() + " and should be "
                                + requiredFirmwareVersion);

                            loadRequiredFirmware = Lookup.getSystemNotificationHandler().askUserToDowngradeFirmware(requiredFirmwareVersion, firmwareResponse.getFirmwareRevisionInt());
                        } else if (firmwareResponse.getFirmwareRevisionInt() < requiredFirmwareVersion)
                        {
                            //The firmware version is lower than that associated with AutoMaker
                            // Tell the user to upgrade

                            steno.warning("Firmware version is "
                                + firmwareResponse.getFirmwareRevisionInt() + " and should be "
                                + requiredFirmwareVersion);

                            loadRequiredFirmware = Lookup.getSystemNotificationHandler().askUserToUpgradeFirmware(requiredFirmwareVersion, firmwareResponse.getFirmwareRevisionInt());
                        }

                        if (loadRequiredFirmware)
                        {
                            loadFirmware(requiredFirmwareVersion);
                        } else
                        {
                            moveOnFromFirmwareCheck(firmwareResponse);
                        }
                    } catch (PrinterException ex)
                    {
                        steno.error("Exception whilst checking firmware version");
                        disconnectSerialPort();
                    }
                    break;

                case CHECKING_ID:
                    String printerID = null;

                    PrinterIDResponse lastPrinterIDResponse = null;

                    try
                    {
                        lastPrinterIDResponse = printerToUse.readPrinterID();

                        printerID = lastPrinterIDResponse.getPrinterFriendlyName();

                        if (printerID == null
                            || (printerID.length() > 0
                            && printerID.charAt(0) == '\0'))
                        {
                            Lookup.getSystemNotificationHandler().showNoPrinterIDDialog(printerToUse);
                            lastPrinterIDResponse = printerToUse.readPrinterID();
                        }
                    } catch (PrinterException ex)
                    {
                        steno.error("Error whilst checking printer ID");
                    }

                    controlInterface.printerConnected(portName);
                    printerToUse.processRoboxResponse(lastPrinterIDResponse);
                    commsState = RoboxCommsState.CONNECTED;
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

    private void moveOnFromFirmwareCheck(FirmwareResponse firmwareResponse)
    {
        printerToUse.processRoboxResponse(firmwareResponse);
        if (suppressPrinterIDChecks == false)
        {
            commsState = RoboxCommsState.CHECKING_ID;
        } else
        {
            controlInterface.printerConnected(portName);
            commsState = RoboxCommsState.CONNECTED;
        }
    }

    private void loadFirmware(int versionToLoad)
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
