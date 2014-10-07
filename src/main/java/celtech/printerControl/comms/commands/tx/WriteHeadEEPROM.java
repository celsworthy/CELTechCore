/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.tx;

import celtech.printerControl.model.Head;
import celtech.printerControl.model.Nozzle;
import celtech.printerControl.model.NozzleHeater;
import celtech.utils.FixedDecimalFloatFormat;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 *
 * @author ianhudson
 */
public class WriteHeadEEPROM extends RoboxTxPacket
{

    /**
     *
     */
    public WriteHeadEEPROM()
    {
        super(TxPacketTypeEnum.WRITE_HEAD_EEPROM, false, false);
    }

    /**
     *
     * @param headTypeCode
     * @param headUniqueID
     * @param maximumTemperature
     * @param thermistorBeta
     * @param thermistorTCal
     * @param nozzle1XOffset
     * @param nozzle1YOffset
     * @param nozzle1ZOffset
     * @param nozzle1BOffset
     * @param nozzle2XOffset
     * @param nozzle2YOffset
     * @param nozzle2ZOffset
     * @param nozzle2BOffset
     * @param lastFilamentTemperature
     * @param hourCounter
     */
    public void populateEEPROM(String headTypeCode, String headUniqueID, float maximumTemperature,
        float thermistorBeta, float thermistorTCal,
        float nozzle1XOffset, float nozzle1YOffset, float nozzle1ZOffset, float nozzle1BOffset,
        float nozzle2XOffset, float nozzle2YOffset, float nozzle2ZOffset, float nozzle2BOffset,
        float lastFilamentTemperature, float hourCounter)
    {
        StringBuilder payload = new StringBuilder();

        FixedDecimalFloatFormat decimalFloatFormatter = new FixedDecimalFloatFormat();

        payload.append(String.format("%1$-16s", headTypeCode));
        payload.append(String.format("%1$-24s", headUniqueID));
        payload.append(decimalFloatFormatter.format(maximumTemperature));
        payload.append(decimalFloatFormatter.format(thermistorBeta));
        payload.append(decimalFloatFormatter.format(thermistorTCal));
        payload.append(decimalFloatFormatter.format(nozzle1XOffset));
        payload.append(decimalFloatFormatter.format(nozzle1YOffset));
        payload.append(decimalFloatFormatter.format(nozzle1ZOffset));
        payload.append(decimalFloatFormatter.format(nozzle1BOffset));
        payload.append(String.format("%1$16s", " "));
        payload.append(decimalFloatFormatter.format(nozzle2XOffset));
        payload.append(decimalFloatFormatter.format(nozzle2YOffset));
        payload.append(decimalFloatFormatter.format(nozzle2ZOffset));
        payload.append(decimalFloatFormatter.format(nozzle2BOffset));
        payload.append(String.format("%1$32s", " "));
        payload.append(decimalFloatFormatter.format(lastFilamentTemperature));
        payload.append(decimalFloatFormatter.format(hourCounter));

        this.setMessagePayload(payload.toString());
    }

    public void populateEEPROM(Head head)
    {
        //TODO modify to cater for different number of nozzles/heaters

        NozzleHeater heater = head.getNozzleHeaters().get(0);
        ArrayList<Nozzle> nozzles = head.getNozzles();

        populateEEPROM(head.typeCodeProperty().get(),
                       head.uniqueIDProperty().get(),
                       heater.maximumTemperatureProperty().get(),
                       heater.betaProperty().get(),
                       heater.tCalProperty().get(),
                       nozzles.get(0).xOffsetProperty().get(),
                       nozzles.get(0).yOffsetProperty().get(),
                       nozzles.get(0).zOffsetProperty().get(),
                       nozzles.get(0).bOffsetProperty().get(),
                       nozzles.get(1).xOffsetProperty().get(),
                       nozzles.get(1).yOffsetProperty().get(),
                       nozzles.get(1).zOffsetProperty().get(),
                       nozzles.get(1).bOffsetProperty().get(),
                       heater.lastFilamentTemperatureProperty().get(),
                       head.headHoursProperty().get());
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
