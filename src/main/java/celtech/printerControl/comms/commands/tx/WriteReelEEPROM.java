/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import java.io.UnsupportedEncodingException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class WriteReelEEPROM extends RoboxTxPacket
{

    private Stenographer steno = StenographerFactory.getStenographer(WriteReelEEPROM.class.getName());
    
    public WriteReelEEPROM()
    {
        super(TxPacketTypeEnum.WRITE_REEL_EEPROM, false, false);
    }

    public void populateEEPROM(String reelTypeCode, String reelUniqueID, float reelFirstLayerNozzleTemperature, float reelNozzleTemperature,
            float reelFirstLayerBedTemperature, float reelBedTemperature, float reelAmbientTemperature, float reelFilamentDiameter,
            float reelMaxExtrusionRate, float reelExtrusionMultiplier, float reelRemainingFilament)
    {
        StringBuilder payload = new StringBuilder();

        payload.append(String.format("%1$-16s", reelTypeCode));
        payload.append(String.format("%1$-24s", reelUniqueID));
        payload.append(String.format("%08.2f", reelNozzleTemperature));
        payload.append(String.format("%08.2f", reelFirstLayerNozzleTemperature));
        payload.append(String.format("%08.2f", reelBedTemperature));
        payload.append(String.format("%08.2f", reelFirstLayerBedTemperature));
        payload.append(String.format("%08.2f", reelAmbientTemperature));
        payload.append(String.format("%08.2f", reelFilamentDiameter));
        payload.append(String.format("%08.2f", reelMaxExtrusionRate));
        payload.append(String.format("%08.2f", reelExtrusionMultiplier));
        payload.append(String.format("%1$80s", " "));
        payload.append(String.format("%08.2f", reelRemainingFilament));

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

        outputArray[0] = TxPacketTypeEnum.WRITE_REEL_EEPROM.getCommandByte();

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
