/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import celtech.utils.FixedDecimalFloatFormat;
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

    /**
     *
     */
    public WriteReelEEPROM()
    {
        super(TxPacketTypeEnum.WRITE_REEL_EEPROM, false, false);
    }

    /**
     *
     * @param reelTypeCode
     * @param reelUniqueID
     * @param reelFirstLayerNozzleTemperature
     * @param reelNozzleTemperature
     * @param reelFirstLayerBedTemperature
     * @param reelBedTemperature
     * @param reelAmbientTemperature
     * @param reelFilamentDiameter
     * @param reelFilamentMultiplier
     * @param reelFeedRateMultiplier
     * @param reelRemainingFilament
     */
    public void populateEEPROM(String reelTypeCode, String reelUniqueID, float reelFirstLayerNozzleTemperature, float reelNozzleTemperature,
            float reelFirstLayerBedTemperature, float reelBedTemperature, float reelAmbientTemperature, float reelFilamentDiameter,
            float reelFilamentMultiplier, float reelFeedRateMultiplier, float reelRemainingFilament)
    {
        StringBuilder payload = new StringBuilder();

        FixedDecimalFloatFormat decimalFloatFormatter = new FixedDecimalFloatFormat();

        payload.append(String.format("%1$-16s", reelTypeCode));
        payload.append(String.format("%1$-24s", reelUniqueID));
        payload.append(decimalFloatFormatter.format(reelFirstLayerNozzleTemperature));
        payload.append(decimalFloatFormatter.format(reelNozzleTemperature));
        payload.append(decimalFloatFormatter.format(reelFirstLayerBedTemperature));
        payload.append(decimalFloatFormatter.format(reelBedTemperature));
        payload.append(decimalFloatFormatter.format(reelAmbientTemperature));
        payload.append(decimalFloatFormatter.format(reelFilamentDiameter));
        payload.append(decimalFloatFormatter.format(reelFilamentMultiplier));
        payload.append(decimalFloatFormatter.format(reelFeedRateMultiplier));
        payload.append(String.format("%1$80s", " "));
        String remainingFilamentValue = decimalFloatFormatter.format(reelRemainingFilament);
        if (remainingFilamentValue.length() > 8)
        {
            String oldValue = remainingFilamentValue;
            remainingFilamentValue = remainingFilamentValue.substring(0, 8);
            steno.warning("Truncated remaining filament value from " + oldValue + " to " + remainingFilamentValue);
        }
        payload.append(remainingFilamentValue);

        this.setMessagePayload(payload.toString());
    }

    /**
     *
     * @return
     */
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

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        return false;
    }
}
