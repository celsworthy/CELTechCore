/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
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
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.comms.events.RoboxEvent;
import celtech.printerControl.comms.events.RoboxEventType;
import celtech.services.firmware.FirmwareLoadService;
import celtech.services.firmware.FirmwareLoadTask;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;
import jssc.SerialPort;
import jssc.SerialPortException;
import libertysystems.configuration.ConfigNotLoadedException;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterHandler extends Thread
{

    private boolean keepRunning = true;
    private boolean initialised = false;

    private Stenographer steno = StenographerFactory.getStenographer(PrinterHandler.class.getName());
    private PrinterControlInterface controlInterface = null;
    private String portName = null;
    private Printer printerToUse = null;
    private String printerFriendlyName = "Robox";
    private RoboxCommsState commsState = RoboxCommsState.FOUND;
    private SerialPort serialPort = null;
    private PrinterID printerID = new PrinterID();
    /*
     * 
     */
    private ModalDialog firmwareUpgradeQuery = null;
    private ModalDialog firmwareSuccessDialog = null;
    private ModalDialog firmwareFailureDialog = null;
    private int firmwareUpdateOK = -1;
    private int firmwareUpdateCancel = -1;
    private ProgressDialog firmwareUpdateProgress = null;
    private final FirmwareLoadService firmwareLoadService = new FirmwareLoadService();
    private ResourceBundle languageBundle = null;
    private int requiredFirmwareVersion = 0;

    private PrinterIDDialog printerIDDialog = null;
    private boolean printerIDDialogWillShow = false;
    private boolean printerIDDialogWasShowing = false;
    private PrinterIDResponse lastPrinterIDResponse = null;
    private int printerIDSetupAttempts = 0;

    private boolean firmwareCheckInProgress = false;

    private boolean suppressPrinterIDChecks = false;
    private int sleepBetweenStatusChecks = 1000;

    private ModalDialog noSDDialog = null;

    public PrinterHandler(PrinterControlInterface controlInterface, String portName, boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        this.controlInterface = controlInterface;
        this.portName = portName;
        this.suppressPrinterIDChecks = suppressPrinterIDChecks;
        this.sleepBetweenStatusChecks = sleepBetweenStatusChecks;
        this.setName("PrinterHandler:" + portName + " " + this.toString());

        languageBundle = DisplayManager.getLanguageBundle();
        try
        {
            Configuration applicationConfiguration = Configuration.getInstance();
            requiredFirmwareVersion = applicationConfiguration.getInt(ApplicationConfiguration.applicationConfigComponent, "requiredFirmwareVersion");
        } catch (ConfigNotLoadedException ex)
        {
            steno.error("Couldn't load configuration - will not be able to check firmware version");
        }

        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                firmwareUpgradeQuery = new ModalDialog();
                firmwareUpgradeQuery.setTitle(languageBundle.getString("dialogs.firmwareUpgradeTitle"));
                firmwareUpdateOK = firmwareUpgradeQuery.addButton(languageBundle.getString("dialogs.firmwareUpgradeOK"));
                firmwareUpdateCancel = firmwareUpgradeQuery.addButton(languageBundle.getString("dialogs.firmwareUpgradeCancel"));

                firmwareUpdateProgress = new ProgressDialog(firmwareLoadService);

                printerIDDialog = new PrinterIDDialog();

                noSDDialog = new ModalDialog();
                noSDDialog.setTitle(languageBundle.getString("dialogs.noSDCardTitle"));
                noSDDialog.setMessage(languageBundle.getString("dialogs.noSDCardMessage"));
                noSDDialog.addButton(languageBundle.getString("dialogs.noSDCardOK"));
                initialised = true;

                firmwareSuccessDialog = new ModalDialog();
                firmwareSuccessDialog.setTitle(languageBundle.getString("dialogs.firmwareUpgradeSuccessTitle"));
                firmwareSuccessDialog.setMessage(languageBundle.getString("dialogs.firmwareUpgradeSuccessMessage"));
                firmwareSuccessDialog.addButton(languageBundle.getString("dialogs.firmwareUpgradeSuccessOK"));

                firmwareFailureDialog = new ModalDialog();
                firmwareFailureDialog.setTitle(languageBundle.getString("dialogs.firmwareUpgradeFailedTitle"));
                firmwareFailureDialog.setMessage(languageBundle.getString("dialogs.firmwareUpgradeFailedMessage"));
                firmwareFailureDialog.addButton(languageBundle.getString("dialogs.firmwareUpgradeFailedOK"));
            }
        });

        firmwareLoadService.setOnSucceeded(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                int firmwareUpgradeSuccess = (int) (t.getSource().getValue());
                if (firmwareUpgradeSuccess == FirmwareLoadTask.SUCCESS)
                {
                    steno.info("Successfully updated firmware");

                    Platform.runLater(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            firmwareSuccessDialog.show();

                            disconnectSerialPort();
                        }
                    });
                } else
                {
                    if (firmwareUpgradeSuccess == FirmwareLoadTask.FILE_ERROR)
                    {
                        steno.error("File error when updating firmware");

                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                firmwareFailureDialog.show();
                            }
                        });
                    } else if (firmwareUpgradeSuccess == FirmwareLoadTask.SDCARD_ERROR)
                    {
                        steno.error("SD card error when updating firmware");

                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {

                                if (!noSDDialog.isShowing())
                                {
                                    Platform.runLater(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            noSDDialog.show();

                                            disconnectSerialPort();
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            }
        });

        firmwareLoadService.setOnFailed(new EventHandler<WorkerStateEvent>()
        {
            @Override
            public void handle(WorkerStateEvent t)
            {
                firmwareFailureDialog.show();

                steno.info("I'm here");
            }
        });

        // The printer ID starts as the port name and then is changed when it goes online 
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
                        StatusResponse response = (StatusResponse) writeToPrinter(RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.STATUS_REQUEST));

//                        if (response.isSDCardPresent() == false)
//                        {
//                            steno.error("The SD card is missing. Printing cannot continue");
//                            if (!noSDDialog.isShowing())
//                            {
//                                Platform.runLater(new Runnable()
//                                {
//                                    @Override
//                                    public void run()
//                                    {
//                                        noSDDialog.show();
//                                    }
//                                });
//                            }
//                        } else
//                        {
                        controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.PRINTER_ACK, response));
                        commsState = RoboxCommsState.CHECKING_FIRMWARE;
//                        }
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
                            RoboxRxPacket response = writeToPrinter(RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.QUERY_FIRMWARE_VERSION));

                            if (response instanceof FirmwareResponse)
                            {
                                FirmwareResponse fwResponse = (FirmwareResponse) response;
                                steno.info("Firmware v " + fwResponse.getFirmwareRevision() + " returned");

//                                if (fwResponse.getFirmwareRevisionInt() > requiredFirmwareVersion)
//                                {
//                                    //The firmware version is higher than that associated with AutoMaker
//                                    // Tell the user to upgrade
//
//                                    firmwareCheckInProgress = true;
//                                    Platform.runLater(new Runnable()
//                                    {
//                                        @Override
//                                        public void run()
//                                        {
//                                            firmwareResetDialog.setTitle(languageBundle.getString("dialogs.firmwareVersionTooLowTitle"));
//                                            firmwareResetDialog.setMessage(languageBundle.getString("dialogs.firmwareVersionTooLowMessage"));
//                                            firmwareResetDialog.addButton(languageBundle.getString("dialogs.firmwareVersionTooLowOK"));
//                                            firmwareResetDialog.show();
//                                            Platform.exit();
//                                        }
//                                    });
//                                } else 
                                if (fwResponse.getFirmwareRevisionInt() < requiredFirmwareVersion)
                                {
                                    firmwareCheckInProgress = true;
                                    steno.warning("Firmware version is " + fwResponse.getFirmwareRevisionInt() + " and should be " + requiredFirmwareVersion);
                                    firmwareUpgradeQuery.setMessage(languageBundle.getString("dialogs.firmwareVersionError1") + fwResponse.getFirmwareRevisionInt() + languageBundle.getString("dialogs.firmwareVersionError2") + requiredFirmwareVersion + ".\n" + languageBundle.getString("dialogs.firmwareVersionError3"));

                                    Platform.runLater(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            ApplicationStatus.getInstance().setMode(ApplicationMode.STATUS);
                                            int choice = firmwareUpgradeQuery.show();
                                            if (choice == firmwareUpdateOK)
                                            {
                                                firmwareLoadService.reset();
                                                firmwareLoadService.setPrinterToUse(printerToUse);
                                                firmwareLoadService.setFirmwareFileToLoad(ApplicationConfiguration.getApplicationInstallDirectory(this.getClass()) + "robox_r" + requiredFirmwareVersion + ".bin");
                                                firmwareLoadService.start();
                                            } else if (choice == firmwareUpdateCancel)
                                            {
//                                                //Proceed at risk
//                                                controlInterface.printerConnected(portName);
//                                                controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.FIRMWARE_VERSION_INFO, response));
//                                                if (suppressPrinterIDChecks == false)
//                                                {
//                                                    commsState = RoboxCommsState.CHECKING_ID;
//                                                } else
//                                                {
//                                                    commsState = RoboxCommsState.CONNECTED;
//                                                }
                                                firmwareCheckInProgress = false;
                                            }
                                        }
                                    });
                                } else
                                {
                                    controlInterface.printerConnected(portName);
                                    controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.FIRMWARE_VERSION_INFO, response));
                                    if (suppressPrinterIDChecks == false)
                                    {
                                        commsState = RoboxCommsState.CHECKING_ID;
                                    } else
                                    {
                                        commsState = RoboxCommsState.CONNECTED;
                                    }
                                }
                            } else
                            {
                                steno.error("Wrong type of packet returned from firmware version request");
                                disconnectSerialPort();
                            }
                        } catch (RoboxCommsException ex)
                        {
                            controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.PRINTER_COMMS_ERROR));
                            steno.error("Failure during printer status request. " + ex.toString());
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
                            StatusResponse testStatusResp = (StatusResponse) writeToPrinter(RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.STATUS_REQUEST));
                            sleep(250);
                        } else
                        {
                            lastPrinterIDResponse = (PrinterIDResponse) writeToPrinter(RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.READ_PRINTER_ID));
                            if (lastPrinterIDResponse != null)
                            {
                                printerID = lastPrinterIDResponse.getPrinterFriendlyName();

                                if (printerIDSetupAttempts < 3 && (printerID == null || printerID.length() > 0 && printerID.charAt(0) == '\0'))
                                {
                                    // The printer ID hasn't been set up
                                    printerIDDialogWillShow = true;
                                    printerIDSetupAttempts++;
                                    Platform.runLater(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            ApplicationStatus.getInstance().setMode(ApplicationMode.STATUS);
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
                        controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.PRINTER_ID_INFO, lastPrinterIDResponse));
                        commsState = RoboxCommsState.CONNECTED;
                    }
                    break;

                case CONNECTED:
                    try
                    {
                        this.sleep(sleepBetweenStatusChecks);

                        RoboxRxPacket response = writeToPrinter(RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.STATUS_REQUEST));
                        if (response != null && response instanceof StatusResponse)
                        {
                            steno.trace("Got " + response.toString() + " from printer.");
                            controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.PRINTER_STATUS_UPDATE, response));
                        } else
                        {
                            controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.PRINTER_INVALID_RESPONSE));
                            steno.warning("No valid response from printer");
                        }
                    } catch (RoboxCommsException ex)
                    {
                        controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.PRINTER_COMMS_ERROR));
                        steno.error("Failure during printer status request. " + ex.toString());
                    } catch (InterruptedException ex)
                    {
                        steno.debug("Comms interrupted");
                    }
                    break;
            }
        }
        steno.info("Handler for " + portName + " exiting");
    }

    private boolean connectToPrinter(String commsPortName)
    {
        boolean portSetupOK = false;

        steno.trace("About to open serial port");
        serialPort = new SerialPort(commsPortName);

        try
        {
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            portSetupOK = true;
            steno.trace("Finished opening serial port");
        } catch (SerialPortException ex)
        {
            steno.error("Error setting up serial port");
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

        if (firmwareSuccessDialog != null)
        {
            if (firmwareSuccessDialog.isShowing())
            {
                Platform.runLater(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        firmwareSuccessDialog.close();
                    }
                });
            }
        }

        if (firmwareFailureDialog != null)
        {
            if (firmwareFailureDialog.isShowing())
            {
                Platform.runLater(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        firmwareFailureDialog.close();
                    }
                });
            }
        }

        if (firmwareUpgradeQuery != null)
        {
            if (firmwareUpgradeQuery.isShowing())
            {
                Platform.runLater(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        firmwareUpgradeQuery.close();
                    }
                });
            }
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

    public synchronized RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        RoboxRxPacket receivedPacket = null;

        if (commsState == RoboxCommsState.CONNECTED || commsState == RoboxCommsState.CHECKING_FIRMWARE || commsState == RoboxCommsState.POST || commsState == RoboxCommsState.CHECKING_ID)
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
                        throw new SerialPortException(serialPort.getPortName(), "Check availability", "Printer did not respond");
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
                        throw new InvalidResponseFromPrinterException("Expected response of type " + messageToWrite.getPacketType().getExpectedResponse().name());
                    }
//                    steno.info("Got a response packet back of type: " + packetType.toString());
                    byte[] inputBuffer = null;
                    if (packetType.containsLengthField())
                    {
                        byte[] lengthData = serialPort.readBytes(packetType.getLengthFieldSize());
                        int payloadSize = Integer.valueOf(new String(lengthData), 16);
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
                                    steno.error("Got errors in response: " + ackResponse.toString());

                                    RoboxCommsException exception = null;
                                    if (ackResponse.isSdCardError())
                                    {
                                        exception = new SDCardErrorException("No SD card inserted");
                                        steno.error("The SD card is missing.");
                                        throw exception;
                                    } else if (ackResponse.isBadCommandError())
                                    {
                                        exception = new BadCommandException("In response to " + messageToWrite);
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
                                controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.HEAD_EEPROM_DATA, receivedPacket));
                                break;
                            case REEL_EEPROM_DATA:
                                controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.REEL_EEPROM_DATA, receivedPacket));
                                break;
                            case PRINTER_ID_RESPONSE:
                                controlInterface.publishEvent(portName, new RoboxEvent(RoboxEventType.PRINTER_ID_INFO, receivedPacket));
                                break;
                            default:
                                break;
                        }
                    } catch (InvalidCommandByteException ex)
                    {
                        steno.error("Command byte of " + String.format("0x%02X", inputBuffer[0]) + " is invalid.");
                    } catch (UnknownPacketTypeException ex)
                    {
                        steno.error("Packet type unknown for command byte " + String.format("0x%02X", inputBuffer[0]) + " is invalid.");
                    } catch (UnableToGenerateRoboxPacketException ex)
                    {
                        steno.error("A packet that appeared to be of type " + packetType.name() + " could not be unpacked.");
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
                        steno.warning("Invalid packet received from firmware - couldn't print contents");
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
        keepRunning = false;
    }

    void setPrinterToUse(Printer newPrinter)
    {
        this.printerToUse = newPrinter;
    }

    public void setSleepBetweenStatusChecks(int sleepMillis)
    {
        sleepBetweenStatusChecks = sleepMillis;
    }

}
