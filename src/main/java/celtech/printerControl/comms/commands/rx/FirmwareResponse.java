/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.rx;

import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;

/**
 *
 * @author ianhudson
 */
public class FirmwareResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";
    private String firmwareRevision = null;
    private final int firmwareRevisionBytes = 8;
    private int firmwareRevisionInt = 0;

    private NumberFormat numberFormatter = NumberFormat.getNumberInstance();

    /**
     *
     * @return
     */
    public String getFirmwareRevision()
    {
        return firmwareRevision;
    }

    /**
     *
     * @return
     */
    public int getFirmwareRevisionInt()
    {
        return firmwareRevisionInt;
    }

    /**
     *
     */
    public FirmwareResponse()
    {
        super(RxPacketTypeEnum.FIRMWARE_RESPONSE, false, false);
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
            this.firmwareRevision = new String(byteData, byteOffset, firmwareRevisionBytes, charsetToUse);
            byteOffset += firmwareRevisionBytes;

            //TODO hack related to bug 19 - remove once this is fixed.
            this.firmwareRevisionInt = Integer.valueOf(firmwareRevision.substring(1,4).trim());

            success = true;
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Failed to convert byte array to Status Response");
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
        outputString.append("Firmware: " + getFirmwareRevision());
        outputString.append("\n");
        outputString.append(">>>>>>>>>>\n");

        return outputString.toString();
    }
}
