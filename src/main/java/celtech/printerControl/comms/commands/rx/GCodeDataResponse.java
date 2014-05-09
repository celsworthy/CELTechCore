/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.rx;

import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 *
 * @author ianhudson
 */
public class GCodeDataResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";

    private final int lengthFieldBytes = 4;

    private String gCodeResponse = "";

    public GCodeDataResponse()
    {
        super(RxPacketTypeEnum.GCODE_RESPONSE, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
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

    public String getGCodeResponse()
    {
        return gCodeResponse;
    }
}
