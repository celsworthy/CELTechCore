package celtech.printerControl.comms;

import java.io.UnsupportedEncodingException;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class SerialPortManager implements SerialPortEventListener
{

    private String serialPortToConnectTo = null;
    protected SerialPort serialPort = null;
    private final Stenographer steno = StenographerFactory.getStenographer(SerialPortManager.class.
        getName());
    // timeout is required on the read particularly for when the firmware is out of date
    // and the returned status report is then too short see issue ROB-453
    private final static int READ_TIMEOUT = 5000;

    public SerialPortManager(String portToConnectTo)
    {
        this.serialPortToConnectTo = portToConnectTo;
    }

    public boolean connect(int baudrate)
    {
        boolean portSetupOK = false;

        steno.info("About to open serial port " + serialPortToConnectTo);
        serialPort = new SerialPort(serialPortToConnectTo);

        try
        {
            serialPort.openPort();
            serialPort.setParams(baudrate, SerialPort.DATABITS_8,
                                 SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            portSetupOK = true;
            steno.info("Finished opening serial port " + serialPortToConnectTo);
        } catch (SerialPortException ex)
        {
            steno.error("Error setting up serial port " + ex.getMessage());
        }

        return portSetupOK;
    }

    public void disconnect() throws SerialPortException
    {
        steno.info("Disconnecting port " + serialPortToConnectTo);

        checkSerialPortOK();

        if (serialPort != null)
        {
            try
            {
                serialPort.closePort();
                steno.info("Port " + serialPortToConnectTo + " disconnected");
            } catch (SerialPortException ex)
            {
                steno.error("Error closing serial port");
            }
        }
        serialPort = null;
    }

    public boolean writeBytes(byte[] data) throws SerialPortException
    {
        boolean wroteOK = false;

        checkSerialPortOK();

        wroteOK = serialPort.writeBytes(data);

        return wroteOK;
    }

    public int getInputBufferBytesCount() throws SerialPortException
    {
        checkSerialPortOK();
        return serialPort.getInputBufferBytesCount();
    }

    public void writeAndWaitForData(byte[] data) throws SerialPortException
    {
        checkSerialPortOK();

        boolean wroteOK = writeBytes(data);

        if (wroteOK)
        {
            int len = -1;

            int waitCounter = 0;
            while (getInputBufferBytesCount() <= 0)
            {
                try
                {
                    Thread.sleep(0, 100000);
                } catch (InterruptedException ex)
                {
                }

                if (waitCounter >= 5000)
                {
                    steno.error("No response from device - disconnecting");
                    throw new SerialPortException(serialPort.getPortName(),
                                                  "Check availability",
                                                  "Printer did not respond");
                }
                waitCounter++;
            }
        } else
        {
            throw new SerialPortException(serialPort.getPortName(),
                                          "Failure during write",
                                          "");
        }
    }

    public byte[] readSerialPort(int numBytes) throws SerialPortException
    {
        checkSerialPortOK();

        byte[] returnData = null;
        try
        {
            returnData = serialPort.readBytes(numBytes, READ_TIMEOUT);
        } catch (SerialPortTimeoutException ex)
        {
            throw new SerialPortException(serialPort.getPortName(),
                                          "Check availability",
                                          "Printer did not respond in time");
        }
        return returnData;
    }

    public byte[] readAllDataOnBuffer() throws SerialPortException
    {
        checkSerialPortOK();
        return serialPort.readBytes();
    }

    public boolean writeASCIIString(String string) throws SerialPortException
    {
        checkSerialPortOK();

        try
        {
            return serialPort.writeString(string, "US-ASCII");
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Strange error with encoding");
            ex.printStackTrace();
            throw new SerialPortException(serialPortToConnectTo,
                                          "Encoding error whilst writing ASCII string", "");
        }
    }

    public String readString() throws SerialPortException
    {
        checkSerialPortOK();

        return serialPort.readString();
    }

    private void checkSerialPortOK() throws SerialPortException
    {
        if (serialPort == null)
        {
            throw new SerialPortException(serialPortToConnectTo,
                                          "Serial port not open",
                                          "");
        }
    }

    public void callback() throws SerialPortException
    {
        serialPort.addEventListener(this, SerialPort.MASK_RXCHAR);
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent)
    {
        if (serialPortEvent.isRXCHAR())
        {
            int numberOfBytesReceived = serialPortEvent.getEventValue();
            steno.info("Got " + numberOfBytesReceived + " bytes");
            try
            {
                serialPort.readBytes(numberOfBytesReceived, READ_TIMEOUT);
            } catch (SerialPortTimeoutException | SerialPortException ex)
            {
                steno.exception("Error whilst auto reading from port " + serialPortToConnectTo, ex);
            }
        } else
        {
            steno.info("Got serial event of type " + serialPortEvent.getEventType()
                + " that I didn't understand");
        }
    }
}
