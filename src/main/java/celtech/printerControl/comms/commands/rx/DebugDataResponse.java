package celtech.printerControl.comms.commands.rx;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianhudson
 */
public class DebugDataResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";

    private List<Integer> debugResponse = new ArrayList<>();

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

        int byteOffset = 1;

        int lengthOfData = 256;

        while (byteOffset <= lengthOfData)
        {
            if ((byteData[byteOffset] & 1) > 0)
            {
                debugResponse.add(byteOffset - 1);
            }
            byteOffset++;
        }

        success = true;

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
    public List<Integer> getDebugData()
    {
        return debugResponse;
    }
}
