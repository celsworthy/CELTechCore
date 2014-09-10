/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Notifier;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.coreUI.components.ProgressDialog;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.commands.exceptions.BadCommandException;
import celtech.printerControl.comms.commands.exceptions.ConnectionLostException;
import celtech.printerControl.comms.commands.exceptions.InvalidCommandByteException;
import celtech.printerControl.comms.commands.exceptions.InvalidResponseFromPrinterException;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.exceptions.SDCardErrorException;
import celtech.printerControl.comms.commands.exceptions.UnableToGenerateRoboxPacketException;
import celtech.printerControl.comms.commands.exceptions.UnknownPacketTypeException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.FirmwareResponse;
import celtech.printerControl.comms.commands.rx.PrinterIDResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.RoboxRxPacketFactory;
import celtech.printerControl.comms.commands.rx.RxPacketTypeEnum;
import celtech.printerControl.comms.commands.rx.StatusResponse;
import celtech.printerControl.comms.commands.tx.FormatHeadEEPROM;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.comms.events.RoboxEvent;
import celtech.printerControl.comms.events.RoboxEventType;
import celtech.services.firmware.FirmwareLoadService;
import celtech.services.firmware.FirmwareLoadTask;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import jssc.SerialPort;
import jssc.SerialPortException;
import libertysystems.configuration.ConfigNotLoadedException;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.dialog.Dialogs.CommandLink;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterHandler extends Thread
{

    private static Set<String> dontCheckFirmwareForPortName = new HashSet<>();

    private boolean keepRunning = true;
    private boolean initialised = false;

    private Stenographer steno = StenographerFactory.getStenographer(PrinterHandler.class.getName());
    private PrinterControlInterface controlInterface = null;
    private String portName = null;
    private Printer printerToUse = null;
    private volatile RoboxCommsState commsState = RoboxCommsState.FOUND;
    private SerialPort serialPort = null;

    private ProgressDialog firmwareUpdateProgress = null;
    private final FirmwareLoadService firmwareLoadService = new FirmwareLoadService();
    private ResourceBundle languageBundle = null;
    private int requiredFirmwareVersion = 0;

    private PrinterIDDialog printerIDDialog = null;
    private boolean printerIDDialogWillShow = false;
    private PrinterIDResponse lastPrinterIDResponse = null;
    private int printerIDSetupAttempts = 0;

    private volatile boolean firmwareCheckInProgress = false;

    private boolean suppressPrinterIDChecks = false;
    private int sleepBetweenStatusChecks = 1000;

    private ModalDialog noSDDialog = null;

    /**
     *
     * @param controlInterface
     * @param portName
     * @param suppressPrinterIDChecks
     * @param sleepBetweenStatusChecks
     */
    public PrinterHandler(PrinterControlInterface controlInterface, String portName,
        boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        System.out.println("PORT NAME IS " + portName);
        this.controlInterface = controlInterface;
        this.portName = portName;
        this.suppressPrinterIDChecks = suppressPrinterIDChecks;
        this.sleepBetweenStatusChecks = sleepBetweenStatusChecks;
        this.setName("PrinterHandler:" + portName + " " + this.toString());

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
                dontCheckFirmwareForPortName.add(portName);
                disconnectSerialPort();
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
                disconnectSerialPort();
            }
        });
    }

    /**
     *
     */
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
//            System.out.println("STATE IS " + commsState);
            switch (commsState)
            {
                case FOUND:
                    steno.debug("Trying to connect to printer in " + portName);

                    boolean printerCommsOpen = connectToPrinter(portName);
                    if (printerCommsOpen)
                    {
                        steno.debug("Connected to Robox on " + portName);
                        setCommsState(RoboxCommsState.CHECKING_FIRMWARE);
                    } else
                    {
                        steno.debug("Failed to connect to Robox on " + portName);
                        controlInterface.failedToConnect(portName);
                        keepRunning = false;
                    }
                    break;

                case CHECKING_FIRMWARE:
                    System.out.println("CHECK FIRMWARE FOR PORT NAME " + portName);
                    if (dontCheckFirmwareForPortName.contains(portName))
                    {
                        System.out.println("SKIP CHECK");
                        setCommsState(RoboxCommsState.CHECKING_ID);
                        break;
                    }
                    if (firmwareCheckInProgress == false && noSDDialog.isShowing() == false)
                    {
                        checkFirmwareVersion();
                    } else
                    {
                        try
                        {
//                            System.out.println("Firmware check GUI in progress");
                            sleep(100);
                        } catch (InterruptedException ex)
                        {

                        }
                    }
                    break;

                case CHECKING_ID:
                    if (suppressPrinterIDChecks)
                    {
                        setCommsState(RoboxCommsState.CONNECTED);
                    } else
                    {
                        checkPrinterID();
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
                            controlInterface.publishEvent(portName, new RoboxEvent(
                                                          RoboxEventType.PRINTER_STATUS_UPDATE,
                                                          response));
                        } else
                        {
                            controlInterface.publishEvent(portName, new RoboxEvent(
                                                          RoboxEventType.PRINTER_INVALID_RESPONSE));
                            steno.warning("No valid response from printer");
                        }

                        AckResponse errors = (AckResponse) writeToPrinter(
                            RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.REPORT_ERRORS));
                        if (errors != null)
                        {
                            steno.trace(errors.toString());
                            controlInterface.publishEvent(portName, new RoboxEvent(
                                                          RoboxEventType.PRINTER_ACK, errors));
                        }
                    } catch (RoboxCommsException ex)
                    {
                        controlInterface.publishEvent(portName, new RoboxEvent(
                                                      RoboxEventType.PRINTER_COMMS_ERROR));
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

    private void checkPrinterID()
    {
        String printerID = null;
        boolean success = false;

        try
        {
            if (printerIDDialog.isShowing() || printerIDDialogWillShow)
            {
                if (printerIDDialog.isShowing())
                {
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
//            controlInterface.publishEvent(portName, new RoboxEvent(
//                                          RoboxEventType.PRINTER_ID_INFO,
//                                          lastPrinterIDResponse));
            setCommsState(RoboxCommsState.CONNECTED);
        }
    }

    private void checkFirmwareVersion()
    {
        try
        {
            RoboxRxPacket response = writeToPrinter(
                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.QUERY_FIRMWARE_VERSION));

            if (response instanceof FirmwareResponse)
            {
                FirmwareResponse fwResponse = (FirmwareResponse) response;
                steno.info("Firmware v " + fwResponse.getFirmwareRevision()
                    + " returned");

                if (fwResponse.getFirmwareRevisionInt() > requiredFirmwareVersion)
                {
                    offerFirmwareDowngrade(fwResponse, response);
                } else if (fwResponse.getFirmwareRevisionInt() < requiredFirmwareVersion)
                {
                    offerFirmwareUpgrade(fwResponse, response);
                } else
                {

//                    controlInterface.publishEvent(portName, new RoboxEvent(
//                                                  RoboxEventType.FIRMWARE_VERSION_INFO,
//                                                  response));
                    setCommsState(RoboxCommsState.CHECKING_ID);

                }
            } else
            {
                steno.error(
                    "Wrong type of packet returned from firmware version request");
                disconnectSerialPort();
            }
        } catch (RoboxCommsException ex)
        {
//            controlInterface.publishEvent(portName, new RoboxEvent(
//                                          RoboxEventType.PRINTER_COMMS_ERROR));
            steno.error("Failure during firmware version request. " + ex.toString());
        }
    }

    private void offerFirmwareDowngrade(FirmwareResponse fwResponse, RoboxRxPacket response)
    {
        //The firmware version is higher than that associated with AutoMaker
        // Tell the user to upgrade

        steno.warning("Firmware version is "
            + fwResponse.getFirmwareRevisionInt() + " and should be "
            + requiredFirmwareVersion);

        firmwareCheckInProgress = true;
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {

                Dialogs.CommandLink firmwareDowngradeOK = new CommandLink(languageBundle.getString(
                    "dialogs.firmwareDowngradeOKTitle"), languageBundle.getString(
                                                                              "dialogs.firmwareUpgradeOKMessage"));
                Dialogs.CommandLink firmwareDowngradeNotOK = new CommandLink(
                    languageBundle.getString(
                        "dialogs.firmwareDowngradeNotOKTitle"), languageBundle.getString(
                        "dialogs.firmwareUpgradeNotOKMessage"));

                Action upgradeApplicationResponse = Dialogs.create().title(
                    languageBundle.getString(
                        "dialogs.firmwareVersionTooLowTitle"))
                    .message(languageBundle.getString(
                            "dialogs.firmwareVersionError1")
                        + fwResponse.getFirmwareRevisionInt()
                        + languageBundle.getString(
                            "dialogs.firmwareVersionError2")
                        + requiredFirmwareVersion + ".\n"
                        + languageBundle.getString(
                            "dialogs.firmwareVersionError3"))
                    .masthead(null)
                    .showCommandLinks(firmwareDowngradeOK,
                                      firmwareDowngradeOK,
                                      firmwareDowngradeNotOK);

                if (upgradeApplicationResponse == firmwareDowngradeOK)
                {
                    firmwareLoadService.reset();
                    firmwareLoadService.setPrinterToUse(printerToUse);
                    firmwareLoadService.setFirmwareFileToLoad(
                        ApplicationConfiguration.getCommonApplicationDirectory()
                        + "robox_r" + requiredFirmwareVersion + ".bin");
                    firmwareLoadService.start();
                } else if (upgradeApplicationResponse
                    == firmwareDowngradeNotOK)
                {
                    //Proceed at risk
//                    controlInterface.publishEvent(portName,
//                                                  new RoboxEvent(
//                                                      RoboxEventType.FIRMWARE_VERSION_INFO,
//                                                      response));
                    firmwareCheckInProgress = false;
                    setCommsState(RoboxCommsState.CHECKING_ID);

                }
            }
        });
    }

    private void offerFirmwareUpgrade(FirmwareResponse fwResponse, RoboxRxPacket response)
    {
        firmwareCheckInProgress = true;
        steno.warning("Firmware version is "
            + fwResponse.getFirmwareRevisionInt() + " and should be "
            + requiredFirmwareVersion);

        int firmwareRevision = fwResponse.getFirmwareRevisionInt();
        boolean upgradeFirmware = askIfUpgradeFirmware(firmwareRevision);

        if (upgradeFirmware)
        {
            firmwareLoadService.reset();
            firmwareLoadService.setPrinterToUse(printerToUse);
            firmwareLoadService.setFirmwareFileToLoad(
                ApplicationConfiguration.getCommonApplicationDirectory()
                + "robox_r" + requiredFirmwareVersion + ".bin");
            firmwareLoadService.start();
        } else
        {
            firmwareCheckInProgress = false;
            setCommsState(RoboxCommsState.CHECKING_ID);

        }
    }

    private boolean connectToPrinter(String commsPortName)
    {
        boolean portSetupOK = false;

        steno.trace("About to open serial port");
        serialPort = new SerialPort(commsPortName);

        try
        {
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8,
                                 SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
//            serialPort.purgePort(PURGE_RXCLEAR | PURGE_TXCLEAR);
            portSetupOK = true;
            steno.trace("Finished opening serial port");
        } catch (SerialPortException ex)
        {
            steno.error("Error setting up serial port " + ex.getMessage());
        }

        return portSetupOK;
    }

    private void disconnectSerialPort()
    {

        steno.info("Disconnecting port " + portName);

        if (serialPort != null)
        {
            try
            {
                serialPort.closePort();
            } catch (SerialPortException ex)
            {
                steno.error("Error closing serial port");
            }
        }
        serialPort = null;

        if (noSDDialog.isShowing())
        {
            Platform.runLater(new Runnable()
            {

                @Override
                public void run()
                {
                    noSDDialog.close();
                }
            });
        }

        if (printerIDDialog != null)
        {
            if (printerIDDialog.isShowing())
            {
                Platform.runLater(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        printerIDDialog.close();
                    }
                });
            }
        }

        controlInterface.disconnected(portName);
    }

    /**
     *
     * @param messageToWrite
     * @return
     * @throws RoboxCommsException
     */
    public synchronized RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        steno.info("WRITE " + messageToWrite);
//        System.out.println("WRITE");
        RoboxRxPacket receivedPacket = null;

        if (commsState == RoboxCommsState.CONNECTED || commsState
            == RoboxCommsState.CHECKING_FIRMWARE || commsState == RoboxCommsState.CHECKING_HEAD
            || commsState == RoboxCommsState.FORMATTING_HEAD || commsState == RoboxCommsState.POST
            || commsState == RoboxCommsState.CHECKING_ID && serialPort != null)
        {
            try
            {
                byte[] outputBuffer = messageToWrite.toByteArray();

                serialPort.writeBytes(outputBuffer);

                int len = -1;

                int waitCounter = 0;
                while (serialPort.getInputBufferBytesCount() <= 0)
                {
//                    steno.trace("Avail is " + dataInputStream.available());
                    try
                    {
                        this.sleep(50);
                    } catch (InterruptedException ex)
                    {
                    }

                    if (waitCounter >= 20)
                    {
//                        System.out.println("TIMEOUT on printer");
                        steno.error("No response from printer - disconnecting");
                        throw new SerialPortException(serialPort.getPortName(), "Check availability",
                                                      "Printer did not respond");
                    }
                    waitCounter++;

                }

                byte[] respCommand = serialPort.readBytes(1);

//                steno.info("Got response:" + String.format("0x%02X", respCommand) + " for message: " + messageToWrite);
                RxPacketTypeEnum packetType = RxPacketTypeEnum.getEnumForCommand(respCommand[0]);
                if (packetType != null)
                {
                    if (packetType != messageToWrite.getPacketType().getExpectedResponse())
                    {
//                        System.out.println("INVALID RESPONSE");
                        throw new InvalidResponseFromPrinterException("Expected response of type "
                            + messageToWrite.getPacketType().getExpectedResponse().name()
                            + " but got type " + packetType);
                    }
//                    steno.info("Got a response packet back of type: " + packetType.toString());
                    byte[] inputBuffer = null;
                    if (packetType.containsLengthField())
                    {
                        byte[] lengthData = serialPort.readBytes(packetType.getLengthFieldSize());

                        int payloadSize = Integer.valueOf(new String(lengthData), 16);
                        if (packetType == RxPacketTypeEnum.LIST_FILES_RESPONSE)
                        {
                            payloadSize = payloadSize * 16;
                        }

                        inputBuffer = new byte[1 + packetType.getLengthFieldSize() + payloadSize];
                        for (int i = 0; i < packetType.getLengthFieldSize(); i++)
                        {
                            inputBuffer[1 + i] = lengthData[i];
                        }

                        byte[] payloadData = serialPort.readBytes(payloadSize);
                        for (int i = 0; i < payloadSize; i++)
                        {
                            inputBuffer[1 + packetType.getLengthFieldSize() + i] = payloadData[i];
                        }
                    } else
                    {
                        inputBuffer = new byte[packetType.getPacketSize()];
                        int bytesToRead = packetType.getPacketSize() - 1;
                        byte[] payloadData = serialPort.readBytes(bytesToRead);
                        for (int i = 0; i < bytesToRead; i++)
                        {
                            inputBuffer[1 + i] = payloadData[i];
                        }
                    }

                    inputBuffer[0] = respCommand[0];

                    try
                    {
                        receivedPacket = RoboxRxPacketFactory.createPacket(inputBuffer);
//                        steno.info("Got packet of type " + receivedPacket.getPacketType().name());

                        switch (receivedPacket.getPacketType())
                        {
                            case STATUS_RESPONSE:
                                break;
                            case ACK_WITH_ERRORS:
                                AckResponse ackResponse = (AckResponse) receivedPacket;
                                if (ackResponse.isError())
                                {
                                    RoboxCommsException exception = null;
                                    if (ackResponse.isSdCardError())
                                    {
                                        exception = new SDCardErrorException("No SD card inserted");
                                        steno.error("The SD card is missing.");
                                        throw exception;
                                    } else if (ackResponse.isBadCommandError())
                                    {
                                        exception = new BadCommandException("In response to "
                                            + messageToWrite);
                                        throw exception;
                                    } else
                                    {
//                                        exception = new RoboxCommsException("Unspecified cause");
//                                        steno.error("Got error in response packet:\n" + ackResponse.toString());
                                    }
                                }
                                break;
                            case FIRMWARE_RESPONSE:
                                break;
                            case HEAD_EEPROM_DATA:
                                controlInterface.publishEvent(portName, new RoboxEvent(
                                                              RoboxEventType.HEAD_EEPROM_DATA,
                                                              receivedPacket));
                                break;
                            case REEL_EEPROM_DATA:
                                controlInterface.publishEvent(portName, new RoboxEvent(
                                                              RoboxEventType.REEL_EEPROM_DATA,
                                                              receivedPacket));
                                break;
                            case PRINTER_ID_RESPONSE:
                                controlInterface.publishEvent(portName, new RoboxEvent(
                                                              RoboxEventType.PRINTER_ID_INFO,
                                                              receivedPacket));
                                break;
                            default:
                                break;
                        }
                    } catch (InvalidCommandByteException ex)
                    {
                        steno.error("Command byte of " + String.format("0x%02X", inputBuffer[0])
                            + " is invalid.");
                    } catch (UnknownPacketTypeException ex)
                    {
                        steno.error("Packet type unknown for command byte "
                            + String.format("0x%02X", inputBuffer[0]) + " is invalid.");
                    } catch (UnableToGenerateRoboxPacketException ex)
                    {
                        steno.error("A packet that appeared to be of type " + packetType.name()
                            + " could not be unpacked.");
                    }
                } else
                {
                    // Attempt to drain the crud from the input
                    // There shouldn't be anything here but just in case...                    
                    byte[] storage = serialPort.readBytes();

                    try
                    {
                        String received = new String(storage);

                        steno.warning("Invalid packet received from firmware: " + received);
                    } catch (Exception e)
                    {
                        steno.warning(
                            "Invalid packet received from firmware - couldn't print contents");
                    }

                    //TODO Reinstate exception inhibited as a result of issue 23 (firmware fault on M190 command)
//                    InvalidResponseFromPrinterException exception = new InvalidResponseFromPrinterException("Invalid response - got: " + received);
//                    throw exception;
                }

            } catch (SerialPortException ex)
            {
                //If we get an exception then abort and treat
                steno.debug("Error during write to printer");
                disconnectSerialPort();
                keepRunning = false;
                throw new ConnectionLostException();
            }
        }
        return receivedPacket;
    }

    void shutdown()
    {
        if (firmwareLoadService.isRunning())
        {
            firmwareLoadService.cancel();
        }
        keepRunning = false;
    }

    void setPrinterToUse(Printer newPrinter)
    {
        this.printerToUse = newPrinter;
    }

    /**
     *
     * @param sleepMillis
     */
    public void setSleepBetweenStatusChecks(int sleepMillis)
    {
        sleepBetweenStatusChecks = sleepMillis;
    }
    
    private boolean askIfUpgradeFirmware(int firmwareRevision)
    {
        Callable<Boolean> askUpgradeFirmware = new Callable()
        {

            @Override
            public Object call() throws Exception
            {
                Dialogs.CommandLink firmwareUpgradeOK = new CommandLink(languageBundle.getString(
                    "dialogs.firmwareUpgradeOKTitle"), languageBundle.getString(
                                                                            "dialogs.firmwareUpgradeOKMessage"));
                Dialogs.CommandLink firmwareUpgradeNotOK = new CommandLink(languageBundle.getString(
                    "dialogs.firmwareUpgradeNotOKTitle"), languageBundle.getString(
                                                                               "dialogs.firmwareUpgradeNotOKMessage"));

                Action upgradeApplicationResponse = Dialogs.create().title(
                    languageBundle.getString(
                        "dialogs.firmwareUpgradeTitle"))
                    .message(languageBundle.getString(
                            "dialogs.firmwareVersionError1")
                        + firmwareRevision
                        + languageBundle.getString(
                            "dialogs.firmwareVersionError2")
                        + requiredFirmwareVersion + ".\n"
                        + languageBundle.getString(
                            "dialogs.firmwareVersionError3"))
                    .masthead(null)
                    .showCommandLinks(firmwareUpgradeOK,
                                      firmwareUpgradeOK,
                                      firmwareUpgradeNotOK);

                return (upgradeApplicationResponse == firmwareUpgradeOK);
            }
        };
        
        FutureTask<Boolean> askFirmwareUpgradeTask = new FutureTask<>(askUpgradeFirmware);
        Platform.runLater(askFirmwareUpgradeTask);
        try
        {
            return askFirmwareUpgradeTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            return false;
        }
    }

    private synchronized void setCommsState(RoboxCommsState roboxCommsState)
    {

//        System.out.println("SET STATE TO " + roboxCommsState);
        commsState = roboxCommsState;
        if (commsState == RoboxCommsState.CONNECTED)
        {
            controlInterface.printerConnected(portName);
        }
    }

}
