package celtech.printerControl.comms.remote;

import celtech.comms.remote.Configuration;
import celtech.comms.remote.RoboxRxPacketRemote;
import celtech.comms.remote.RoboxTxPacketRemote;
import celtech.configuration.ApplicationConfiguration;
import celtech.printerControl.comms.DeviceDetector;
import celtech.printerControl.comms.RemoteDetectedPrinter;
import celtech.printerControl.comms.commands.exceptions.InvalidCommandByteException;
import celtech.printerControl.comms.commands.exceptions.UnableToGenerateRoboxPacketException;
import celtech.printerControl.comms.commands.exceptions.UnknownPacketTypeException;
import celtech.printerControl.comms.commands.rx.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.RoboxRxPacketFactory;
import celtech.printerControl.comms.commands.rx.RxPacketTypeEnum;
import celtech.printerControl.comms.commands.tx.RoboxTxPacket;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class RemoteClient implements LowLevelInterface
{

    private final Stenographer steno = StenographerFactory.getStenographer(RemoteClient.class.getName());
    private final RemoteDetectedPrinter remotePrinterHandle;

    private final String baseUrlString;
    private final String connectUrlString;
    private final String disconnectUrlString;
    private final String writeToPrinterUrlString;

    public RemoteClient(RemoteDetectedPrinter remotePrinterHandle)
    {
        this.remotePrinterHandle = remotePrinterHandle;
        baseUrlString = "http://" + remotePrinterHandle.getAddress().getHostAddress() + ":" + Configuration.remotePort;
        connectUrlString = baseUrlString + "/" + remotePrinterHandle.getConnectionHandle() + Configuration.lowLevelAPIService + Configuration.connectService;
        disconnectUrlString = baseUrlString + "/" + remotePrinterHandle.getConnectionHandle() + Configuration.lowLevelAPIService + Configuration.disconnectService;
        writeToPrinterUrlString = baseUrlString + "/" + remotePrinterHandle.getConnectionHandle() + Configuration.lowLevelAPIService + Configuration.writeDataService;
    }

    @Override
    public boolean connect(String printerID)
    {
        boolean success = false;
        RemoteWebHelper.postData(connectUrlString);
        return success;
    }

    @Override
    public void disconnect(String printerID)
    {
        RemoteWebHelper.postData(disconnectUrlString);
    }

    @Override
    public RoboxRxPacket writeToPrinter(String printerID, RoboxTxPacket messageToWrite)
    {
        RoboxRxPacket returnedPacket = null;
        byte[] content = messageToWrite.toByteArray();
        RoboxTxPacketRemote remoteTx = new RoboxTxPacketRemote(content);

        RoboxRxPacketRemote returnedData = (RoboxRxPacketRemote) RemoteWebHelper.postData(writeToPrinterUrlString, remoteTx, RoboxRxPacketRemote.class);

        try
        {
            returnedPacket = RoboxRxPacketFactory.createPacket(returnedData.getRawData());
        } catch (InvalidCommandByteException | UnableToGenerateRoboxPacketException | UnknownPacketTypeException ex)
        {
            steno.error("Failed to process returned data from remote " + remotePrinterHandle);
        }

        return returnedPacket;
    }

}
