/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import java.io.UnsupportedEncodingException;

/**
 *
 * @author ianhudson
 */
public class WriteHeadEEPROM extends RoboxTxPacket
{

    public WriteHeadEEPROM()
    {
        super(TxPacketTypeEnum.WRITE_HEAD_EEPROM, false, false);
    }

    public void populateEEPROM(String headTypeCode, String headUniqueID, float maximumTemperature,
            float thermistorBeta, float thermistorTCal,
            float nozzle1XOffset, float nozzle1YOffset, float nozzle1ZOffset, float nozzle1BOffset,
            float nozzle2XOffset, float nozzle2YOffset, float nozzle2ZOffset, float nozzle2BOffset,
            float hourCounter)
    {
        StringBuilder payload = new StringBuilder();

        payload.append(String.format("%1$-16s", headTypeCode));
        payload.append(String.format("%1$-24s", headUniqueID));
        payload.append(String.format("%08.2f", maximumTemperature));
        payload.append(String.format("%08.2f", thermistorBeta));
        payload.append(String.format("%08.2f", thermistorTCal));
        payload.append(String.format("%08.2f", nozzle1XOffset));
        payload.append(String.format("%08.2f", nozzle1YOffset));
        payload.append(String.format("%08.2f", nozzle1ZOffset));
        payload.append(String.format("%08.2f", nozzle1BOffset));
        payload.append(String.format("%1$16s", " "));
        payload.append(String.format("%08.2f", nozzle2XOffset));
        payload.append(String.format("%08.2f", nozzle2YOffset));
        payload.append(String.format("%08.2f", nozzle2ZOffset));
        payload.append(String.format("%08.2f", nozzle2BOffset));
        payload.append(String.format("%1$40s", " "));
        payload.append(String.format("%08.2f", hourCounter));

        this.setMessagePayload(payload.toString());
    }

    @Override
    public byte[] toByteArray()
    {
        byte[] outputArray = null;

        int bufferSize = 1; // 1 for the command

        bufferSize += 4;

        if (messagePayload != null)
        {
            bufferSize += messagePayload.length();
        }

        outputArray = new byte[bufferSize];

        outputArray[0] = TxPacketTypeEnum.WRITE_HEAD_EEPROM.getCommandByte();

        StringBuilder finalPayload = new StringBuilder();

        finalPayload.append("00");

        finalPayload.append(String.format("%02X", 192));

        if (getMessageData() != null)
        {
            finalPayload.append(messagePayload);
        }

        if (bufferSize > 1)
        {
            try
            {
                byte[] payloadBytes = finalPayload.toString().getBytes("US-ASCII");
                //TODO - replace this with a ByteBuffer or equivalent
                for (int i = 1; i <= payloadBytes.length; i++)
                {
                    outputArray[i] = payloadBytes[i - 1];
                }
            } catch (UnsupportedEncodingException ex)
            {
                steno.error("Couldn't encode message for output");
            }
        }

        return outputArray;
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
