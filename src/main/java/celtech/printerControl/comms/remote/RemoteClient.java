package celtech.printerControl.comms.remote;

import celtech.comms.remote.Configuration;
import celtech.comms.remote.RoboxRxPacketRemote;
import celtech.comms.remote.RoboxTxPacketRemote;
import celtech.printerControl.comms.RemoteDetectedPrinter;
import celtech.printerControl.comms.commands.exceptions.InvalidCommandByteException;
import celtech.printerControl.comms.commands.exceptions.UnableToGenerateRoboxPacketException;
import celtech.printerControl.comms.commands.exceptions.UnknownPacketTypeException;
import celtech.comms.remote.RoboxRxPacket;
import celtech.printerControl.comms.commands.rx.RoboxRxPacketFactory;
import celtech.comms.remote.RoboxTxPacket;
import java.io.IOException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.codehaus.jackson.map.ObjectMapper;

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
    private final ObjectMapper mapper = new ObjectMapper();

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

        try
        {
            String dataToOutput = mapper.writeValueAsString(messageToWrite);
            returnedPacket = RemoteWebHelper.postData(writeToPrinterUrlString, dataToOutput, RoboxRxPacketRemote.class);
        } catch (IOException ex)
        {
            steno.error("Failed to write to remote printer (" + messageToWrite.getPacketType().name() + ") " + remotePrinterHandle);
        }

        return returnedPacket;
    }

}
