package celtech.printerControl.comms.commands.rx;

import java.io.UnsupportedEncodingException;

/**
 *
 * @author ianhudson
 */
public class GCodeDataResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";

    private final int lengthFieldBytes = 4;

    private String gCodeResponse = "";

    /**
     *
     */
    public GCodeDataResponse()
    {
        super(RxPacketTypeEnum.GCODE_RESPONSE, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData, float requiredFirmwareVersion)
    {
        boolean success = false;

        try
        {
            int byteOffset = 1;
            
            int lengthOfData = 0;
            
            String lengthString = new String(byteData, byteOffset, lengthFieldBytes, charsetToUse);
            byteOffset += lengthFieldBytes;

            lengthOfData = Integer.valueOf(lengthString, 16);

            gCodeResponse = new String(byteData, byteOffset, lengthOfData, charsetToUse);
            byteOffset += lengthOfData;

            success = true;
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Failed to convert byte array to GCode Response");
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
//        outputString.append("ID: " + getPrinterID());
        outputString.append("\n");
        outputString.append(">>>>>>>>>>\n");

        return outputString.toString();
    }

    /**
     *
     * @return
     */
    public String getGCodeResponse()
    {
        return gCodeResponse;
    }

    @Override
    public int packetLength(float requiredFirmwareVersion)
    {
        return 5;
    }
}
