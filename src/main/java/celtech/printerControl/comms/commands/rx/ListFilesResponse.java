/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.rx;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 *
 * @author ianhudson
 */
public class ListFilesResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";

    private final int lengthFieldBytes = 2;

    private ArrayList<String> printJobIDs = new ArrayList<String>();

    /**
     *
     */
    public ListFilesResponse()
    {
        super(RxPacketTypeEnum.LIST_FILES_RESPONSE, false, false);
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
            
            int numberOfIDs = 0;
            
            String lengthString = new String(byteData, byteOffset, lengthFieldBytes, charsetToUse);
            byteOffset += lengthFieldBytes;

            numberOfIDs = Integer.valueOf(lengthString, 16);
            
            for (int counter = 0; counter < numberOfIDs; counter++)
            {
                String jobID = new String(byteData, byteOffset, 16, charsetToUse);
                byteOffset += 16;
                printJobIDs.add(jobID);
            }

            success = true;
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Failed to extract print job IDs");
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
    public ArrayList<String> getPrintJobIDs()
    {
        return printJobIDs;
    }
}
