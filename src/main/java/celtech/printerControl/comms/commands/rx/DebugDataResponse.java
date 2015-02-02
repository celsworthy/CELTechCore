package celtech.printerControl.comms.commands.rx;

import java.io.UnsupportedEncodingException;

/**
 *
 * @author ianhudson
 */
public class DebugDataResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";

    private String debugResponse = "";

    /**
     *
     */
    public DebugDataResponse()
    {
        super(RxPacketTypeEnum.DEBUG_DATA, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        boolean success = false;

        try
        {
            int byteOffset = 1;
         
            int lengthOfData = 256;
            
            debugResponse = new String(byteData, byteOffset, lengthOfData, charsetToUse);
            byteOffset += lengthOfData;

            success = true;
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Failed to convert byte array to Debug Data Response");
        }

        return success;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        StringBuilder outputString = new StringBuilder();

        outputString.append(">>>>>>>>>>\n");
        outputString.append("Packet type:");
        outputString.append(getPacketType().name());
        outputString.append("\n");
        outputString.append("Debug data:" + debugResponse);
        outputString.append("\n");
        outputString.append(">>>>>>>>>>\n");

        return outputString.toString();
    }

    /**
     *
     * @return
     */
    public String getDebugData()
    {
        return debugResponse;
    }
}
