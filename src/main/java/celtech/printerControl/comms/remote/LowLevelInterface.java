package celtech.printerControl.comms.remote;

import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;

/**
 *
 * @author ianhudson
 */
public interface LowLevelInterface
{
    public boolean connect(String printerID);
    public void disconnect(String printerID);
    public RoboxRxPacket writeToPrinter(String printerID, RoboxTxPacket messageToWrite);

//    public boolean connect(int baudrate);
//
//    public void disconnect() throws LowLevelInterfaceException;
//
//    public void writeAndWaitForData(byte[] data) throws LowLevelInterfaceException;
//
//    public boolean writeBytes(byte[] data) throws LowLevelInterfaceException;
//
//    public byte[] readSerialPort(int lengthFieldSize) throws LowLevelInterfaceException;
//
//    public byte[] readAllDataOnBuffer() throws LowLevelInterfaceException;
}
