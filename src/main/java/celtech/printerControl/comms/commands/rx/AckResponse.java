package celtech.printerControl.comms.commands.rx;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianhudson
 */
public class AckResponse extends RoboxRxPacket
{
    private final String charsetToUse = "US-ASCII";

    private List<FirmwareError> firmwareErrors = new ArrayList<>();

    /**
     *
     * @return
     */
    public boolean isError()
    {
        return !firmwareErrors.isEmpty();
    }

    /*
     * Errors...
     */
    /**
     *
     */
    public AckResponse()
    {
        super(RxPacketTypeEnum.ACK_WITH_ERRORS, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        int byteOffset = 1;

        for (; byteOffset < this.getPacketType().getPacketSize(); byteOffset++)
        {
            if ((byteData[byteOffset] & 1) > 0)
            {
                FirmwareError error = FirmwareError.fromBytePosition(byteOffset);
                firmwareErrors.add(error);
            }
        }

        return !isError();
    }
    
    public List<FirmwareError> getFirmwareErrors()
    {
        return firmwareErrors;
    }

    /**
     *
     * @return
     */
    public String getErrorsAsString()
    {
        StringBuilder outputString = new StringBuilder();

        for (FirmwareError error : firmwareErrors)
        {
            outputString.append(error.getLocalisedErrorTitle());
            outputString.append("\n");
        }

        return outputString.toString();
    }

    /**
     *
     * @return
     */
    public String toString()
    {
        StringBuilder outputString = new StringBuilder();

        outputString.append("Error Report\n");
        outputString.append("Packet type:");
        outputString.append(getPacketType().name());
        outputString.append("\n");
        for (FirmwareError error : firmwareErrors)
        {
            outputString.append(error.getLocalisedErrorTitle());
            outputString.append("\n");
        }
        outputString.append(">>>>>>>>>>\n");

        return outputString.toString();
    }
}
