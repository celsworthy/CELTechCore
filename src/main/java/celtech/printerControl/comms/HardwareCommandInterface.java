package celtech.printerControl.comms;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.EEPROMState;
import celtech.printerControl.model.Printer;
import celtech.printerControl.comms.commands.exceptions.ConnectionLostException;
import celtech.printerControl.comms.commands.exceptions.InvalidCommandByteException;
import celtech.printerControl.comms.commands.exceptions.InvalidResponseFromPrinterException;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
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
import static java.lang.Thread.sleep;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialogs;
import org.controlsfx.dialog.Dialogs.CommandLink;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class HardwareCommandInterface extends CommandInterface
{
    public HardwareCommandInterface(PrinterStatusConsumer controlInterface, String portName,
        boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        super(controlInterface, portName, suppressPrinterIDChecks, sleepBetweenStatusChecks);
        this.setName("PrinterHandler:" + portName + " " + this.toString());
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
            switch (commsState)
            {
                case FOUND:
                    steno.debug("Trying to connect to printer in " + portName);

                    boolean printerCommsOpen = connectToPrinter(portName);
                    if (printerCommsOpen)
                    {
                        steno.debug("Connected to Robox on " + portName);
                        commsState = RoboxCommsState.CHECKING_HEAD;
                    } else
                    {
                        steno.debug("Failed to connect to Robox on " + portName);
                        controlInterface.failedToConnect(portName);
                        keepRunning = false;
                    }
                    break;

                case CHECKING_HEAD:
                    boolean doFormatHead = checkHead();
                    if (doFormatHead)
                    {
                        commsState = RoboxCommsState.FORMATTING_HEAD;
                    } else
                    {
                        commsState = RoboxCommsState.CHECKING_FIRMWARE;
                    }
                    break;

                case FORMATTING_HEAD:
                    try
                    {
                        formatHead();
                    } catch (RoboxCommsException ex)
                    {
                        steno.error("Error formatting head in POST " + ex.getMessage());
                    }
                    commsState = RoboxCommsState.CHECKING_FIRMWARE;
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
//                                                //Proceed at risk
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
                                        }
                                    });
                                } else if (fwResponse.getFirmwareRevisionInt()
                                    < requiredFirmwareVersion)
                                {
                                    firmwareCheckInProgress = true;
                                    steno.warning("Firmware version is "
                                        + fwResponse.getFirmwareRevisionInt() + " and should be "
                                        + requiredFirmwareVersion);

                                    Platform.runLater(new Runnable()
                                    {

                                        @Override
                                        public void run()
                                        {
                                            Action upgradeApplicationResponse = Dialogs.create().title(
                                                languageBundle.getString(
                                                    "dialogs.firmwareUpgradeTitle"))
                                                .message(languageBundle.getString(
                                                        "dialogs.firmwareVersionError1")
                                                    + fwResponse.getFirmwareRevisionInt()
                                                    + languageBundle.getString(
                                                        "dialogs.firmwareVersionError2")
                                                    + requiredFirmwareVersion + ".\n"
                                                    + languageBundle.getString(
                                                        "dialogs.firmwareVersionError3"))
                                                .masthead(null)
                                                .showCommandLinks(firmwareUpgradeOK,
                                                                  firmwareUpgradeOK,
                                                                  firmwareUpgradeNotOK);

                                            if (upgradeApplicationResponse == firmwareUpgradeOK)
                                            {
                                                firmwareLoadService.reset();
                                                firmwareLoadService.setPrinterToUse(printerToUse);
                                                firmwareLoadService.setFirmwareFileToLoad(
                                                    ApplicationConfiguration.getCommonApplicationDirectory()
                                                    + "robox_r" + requiredFirmwareVersion + ".bin");
                                                firmwareLoadService.start();
                                            } else if (upgradeApplicationResponse
                                                == firmwareUpgradeNotOK)
                                            {
//                                                //Proceed at risk
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
                                        }
                                    });
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
    @Override
    public synchronized RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        RoboxRxPacket receivedPacket = null;

        if (commsState == RoboxCommsState.CONNECTED || commsState == RoboxCommsState.CHECKING_FIRMWARE || commsState == RoboxCommsState.CHECKING_HEAD || commsState == RoboxCommsState.FORMATTING_HEAD
            || commsState == RoboxCommsState.POST || commsState == RoboxCommsState.CHECKING_ID && serialPort != null)
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

                    if (waitCounter >= 10)
                    {
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
                        throw new InvalidResponseFromPrinterException("Expected response of type "
                            + messageToWrite.getPacketType().getExpectedResponse().name());
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

                        printerToUse.processRoboxResponse(receivedPacket);
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
    @Override
    public void setSleepBetweenStatusChecks(int sleepMillis)
    {
        sleepBetweenStatusChecks = sleepMillis;
    }

    /**
     * Check the head and if it is unformatted then offer to format it. If it is not programmed then offer to reset it.
     */
    private boolean checkHead()
    {
        try
        {
            StatusResponse response = (StatusResponse) writeToPrinter(
                RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.STATUS_REQUEST));
            if (response != null)
            {

                if (response.getHeadEEPROMState() == EEPROMState.NOT_PROGRAMMED)
                {

                    return askIfFormatHead();

                }
            }
        } catch (RoboxCommsException ex)
        {
            try
            {
                sleep(100);
            } catch (InterruptedException ex1)
            {
                steno.info("Wait for printer to return status in checking head");
            }
        }
        return false;

    }

    /**
     * Ask the user if the unformatted head should be formatted.
     *
     * @return true if a format is requested, else false.
     */
    private boolean askIfFormatHead()
    {
        Callable<Boolean> askFormatDialog = new Callable()
        {

            @Override
            public Boolean call() throws Exception
            {
                Dialogs.CommandLink formatHeadOK = new CommandLink(
                    languageBundle.getString("dialogs.formatHeadOkQuestion"),
                    languageBundle.getString("dialogs.formatHeadOkMessage"));
                Dialogs.CommandLink formatHeadNotOK = new CommandLink(
                    languageBundle.getString("dialogs.formatHeadNotOkQuestion"),
                    languageBundle.getString("dialogs.formatHeadNotOkMessage"));
                Action formatHeadResponse = Dialogs.create().title(languageBundle.getString("dialogs.unformattedHeadDetected"))
                    .message(languageBundle.getString("dialogs.unformattedHead"))
                    .masthead(null)
                    .showCommandLinks(formatHeadOK, formatHeadOK,
                                      formatHeadNotOK);
                return formatHeadResponse.equals(formatHeadOK);
            }
        };
        FutureTask<Boolean> askFormatTask = new FutureTask<>(askFormatDialog);
        Platform.runLater(askFormatTask);
        try
        {
            return askFormatTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            ex.printStackTrace();
            System.out.println("XXX Exception " + ex);
            return false;
        }
    }

    /**
     * Format the head.
     *
     * @throws RoboxCommsException
     */
    private AckResponse formatHead() throws RoboxCommsException
    {
        FormatHeadEEPROM formatHead = (FormatHeadEEPROM) RoboxTxPacketFactory.createPacket(
            TxPacketTypeEnum.FORMAT_HEAD_EEPROM);
        return (AckResponse) writeToPrinter(formatHead);
    }

}
