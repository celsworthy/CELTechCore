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

    private String headTypeCode;
    private String headUniqueID;
    private float maximumTemperature;
    private float thermistorBeta;
    private float thermistorTCal;
    private float nozzle1XOffset;
    private float nozzle1YOffset;
    private float nozzle1ZOffset;
    private float nozzle1BOffset;
    private float nozzle2XOffset;
    private float nozzle2YOffset;
    private float nozzle2ZOffset;
    private float nozzle2BOffset;
    private float lastFilamentTemperature0;
    private float lastFilamentTemperature1;
    private float hourCounter;

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
     * @param lastFilamentTemperature0
     * @param hourCounter
     */
    public void populateEEPROM(String headTypeCode, String headUniqueID, float maximumTemperature,
        float thermistorBeta, float thermistorTCal,
        float nozzle1XOffset, float nozzle1YOffset, float nozzle1ZOffset, float nozzle1BOffset,
        float nozzle2XOffset, float nozzle2YOffset, float nozzle2ZOffset, float nozzle2BOffset,
        float lastFilamentTemperature0, float lastFilamentTemperature1, float hourCounter)
    {

        //TODO make work for DMH, lastTemp1 not being added to payload
        
        this.headTypeCode = headTypeCode;
        this.headUniqueID = headUniqueID;
        this.maximumTemperature = maximumTemperature;
        this.thermistorBeta = thermistorBeta;
        this.thermistorTCal = thermistorTCal;
        this.nozzle1XOffset = nozzle1XOffset;
        this.nozzle1YOffset = nozzle1YOffset;
        this.nozzle1ZOffset = nozzle1ZOffset;
        this.nozzle1BOffset = nozzle1BOffset;
        this.nozzle2XOffset = nozzle2XOffset;
        this.nozzle2YOffset = nozzle2YOffset;
        this.nozzle2ZOffset = nozzle2ZOffset;
        this.nozzle2BOffset = nozzle2BOffset;
        this.lastFilamentTemperature0 = lastFilamentTemperature0;
        this.lastFilamentTemperature1 = lastFilamentTemperature1;
        this.hourCounter = hourCounter;

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
        payload.append(decimalFloatFormatter.format(lastFilamentTemperature0));
        payload.append(decimalFloatFormatter.format(hourCounter));

        this.setMessagePayload(payload.toString());
    }

    public void populateEEPROM(Head head)
    {
        //TODO modify to cater for different number of nozzles/heaters

        NozzleHeater heater0 = head.getNozzleHeaters().get(0);
        float lastFilamentTemperature1 = 0;
        if (head.getNozzleHeaters().size() > 1) {
             NozzleHeater heater1 = head.getNozzleHeaters().get(1);
             lastFilamentTemperature1 = heater1.lastFilamentTemperatureProperty().get();
        }
       
        ArrayList<Nozzle> nozzles = head.getNozzles();

        populateEEPROM(head.typeCodeProperty().get(),
                       head.uniqueIDProperty().get(),
                       heater0.maximumTemperatureProperty().get(),
                       heater0.betaProperty().get(),
                       heater0.tCalProperty().get(),
                       nozzles.get(0).xOffsetProperty().get(),
                       nozzles.get(0).yOffsetProperty().get(),
                       nozzles.get(0).zOffsetProperty().get(),
                       nozzles.get(0).bOffsetProperty().get(),
                       nozzles.get(1).xOffsetProperty().get(),
                       nozzles.get(1).yOffsetProperty().get(),
                       nozzles.get(1).zOffsetProperty().get(),
                       nozzles.get(1).bOffsetProperty().get(),
                       heater0.lastFilamentTemperatureProperty().get(),
                       lastFilamentTemperature1,
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

    public String getHeadTypeCode()
    {
        return headTypeCode;
    }

    public String getHeadUniqueID()
    {
        return headUniqueID;
    }

    public float getMaximumTemperature()
    {
        return maximumTemperature;
    }

    public float getThermistorBeta()
    {
        return thermistorBeta;
    }

    public float getThermistorTCal()
    {
        return thermistorTCal;
    }

    public float getNozzle1XOffset()
    {
        return nozzle1XOffset;
    }

    public float getNozzle1YOffset()
    {
        return nozzle1YOffset;
    }

    public float getNozzle1ZOffset()
    {
        return nozzle1ZOffset;
    }

    public float getNozzle1BOffset()
    {
        return nozzle1BOffset;
    }

    public float getNozzle2XOffset()
    {
        return nozzle2XOffset;
    }

    public float getNozzle2YOffset()
    {
        return nozzle2YOffset;
    }

    public float getNozzle2ZOffset()
    {
        return nozzle2ZOffset;
    }

    public float getNozzle2BOffset()
    {
        return nozzle2BOffset;
    }

    public float getLastFilamentTemperature()
    {
        return lastFilamentTemperature0;
    }

    public float getHourCounter()
    {
        return hourCounter;
    }
    
    
}
