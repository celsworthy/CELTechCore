package celtech.printerControl.comms;

import celtech.printerControl.comms.commands.exceptions.ConnectionLostException;
import celtech.printerControl.comms.commands.exceptions.InvalidCommandByteException;
import celtech.printerControl.comms.commands.exceptions.InvalidResponseFromPrinterException;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.exceptions.UnableToGenerateRoboxPacketException;
import celtech.printerControl.comms.commands.exceptions.UnknownPacketTypeException;
import celtech.printerControl.comms.commands.rx.AckResponse;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.RoboxRxPacketFactory;
import celtech.printerControl.comms.commands.rx.RxPacketTypeEnum;
import celtech.printerControl.comms.commands.tx.FormatHeadEEPROM;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacketFactory;
import celtech.printerControl.comms.commands.tx.TxPacketTypeEnum;
import celtech.printerControl.model.Printer;
import javafx.application.Platform;
import jssc.SerialPort;
import jssc.SerialPortException;

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

    @Override
    protected boolean connectToPrinter(String commsPortName)
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

    @Override
    protected void disconnectSerialPort()
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

        if (commsState == RoboxCommsState.CONNECTED
            || commsState == RoboxCommsState.CHECKING_FIRMWARE
            || commsState == RoboxCommsState.POST
            || commsState == RoboxCommsState.CHECKING_ID
            && serialPort != null)
        {
            try
            {
                byte[] outputBuffer = messageToWrite.toByteArray();

                boolean wroteOK = serialPort.writeBytes(outputBuffer);

                if (wroteOK)
                {
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
