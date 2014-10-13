/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.rx;

import celtech.printerControl.model.Head;
import celtech.printerControl.model.NozzleHeater;
import celtech.utils.FixedDecimalFloatFormat;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

/**
 *
 * @author ianhudson
 */
public class HeadEEPROMDataResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";

    private final int decimalFloatFormatBytes = 8;
    private final int headTypeCodeBytes = 16;
    private final int uniqueIDBytes = 24;

    private String headTypeCode;
    private String uniqueID;
    private float maximumTemperature = 0;
    private float thermistorBeta = 0;
    private float thermistorTCal = 0;

    private float nozzle1XOffset = 0;
    private float nozzle1YOffset = 0;
    private float nozzle1ZOffset = 0;
    private float nozzle1BOffset = 0;

    private float nozzle2XOffset = 0;
    private float nozzle2YOffset = 0;
    private float nozzle2ZOffset = 0;
    private float nozzle2BOffset = 0;

    private float lastFilamentTemperature = 0;
    private float hoursUsed = 0;

    /**
     *
     */
    public HeadEEPROMDataResponse()
    {
        super(RxPacketTypeEnum.HEAD_EEPROM_DATA, false, false);
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

        FixedDecimalFloatFormat decimalFloatFormatter = new FixedDecimalFloatFormat();

        try
        {
            int byteOffset = 1;

            headTypeCode = (new String(byteData, byteOffset, headTypeCodeBytes, charsetToUse)).trim();
            byteOffset += headTypeCodeBytes;

            uniqueID = (new String(byteData, byteOffset, uniqueIDBytes, charsetToUse)).trim();
            byteOffset += uniqueIDBytes;

            String maxTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                maximumTemperature = decimalFloatFormatter.parse(maxTempString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse maximum temperature - " + maxTempString);
            }

            String thermistorBetaString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                thermistorBeta = decimalFloatFormatter.parse(thermistorBetaString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse thermistor beta - " + thermistorBetaString);
            }

            String thermistorTCalString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                thermistorTCal = decimalFloatFormatter.parse(thermistorTCalString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse thermistor tcal - " + thermistorTCalString);
            }

            String nozzle1XOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                nozzle1XOffset = decimalFloatFormatter.parse(nozzle1XOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 1 X offset - " + nozzle1XOffsetString);
            }

            String nozzle1YOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle1YOffset = decimalFloatFormatter.parse(nozzle1YOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 1 Y offset - " + nozzle1YOffsetString);
            }

            String nozzle1ZOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle1ZOffset = decimalFloatFormatter.parse(nozzle1ZOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 1 Z offset - " + nozzle1ZOffsetString);
            }

            String nozzle1BOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle1BOffset = decimalFloatFormatter.parse(nozzle1BOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 1 B offset - " + nozzle1BOffsetString);
            }

            //Empty section
            byteOffset += 16;

            String nozzle2XOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                nozzle2XOffset = decimalFloatFormatter.parse(nozzle2XOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 X offset - " + nozzle2XOffsetString);
            }

            String nozzle2YOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle2YOffset = decimalFloatFormatter.parse(nozzle2YOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 Y offset - " + nozzle2YOffsetString);
            }

            String nozzle2ZOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle2ZOffset = decimalFloatFormatter.parse(nozzle2ZOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 Z offset - " + nozzle2ZOffsetString);
            }

            String nozzle2BOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle2BOffset = decimalFloatFormatter.parse(nozzle2BOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 B offset - " + nozzle2BOffsetString);
            }

            //Empty section
            byteOffset += 32;

            String lastFilamentTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                lastFilamentTemperature = decimalFloatFormatter.parse(lastFilamentTemperatureString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse last filament temperature - " + lastFilamentTemperatureString);
            }

            String hoursUsedString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                hoursUsed = decimalFloatFormatter.parse(hoursUsedString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse hours used - " + hoursUsedString);
            }

            success = true;
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Failed to convert byte array to Printer ID Response");
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
    public float getMaximumTemperature()
    {
        return maximumTemperature;
    }

    /**
     *
     * @return
     */
    public float getNozzle1XOffset()
    {
        return nozzle1XOffset;
    }

    /**
     *
     * @return
     */
    public float getNozzle1YOffset()
    {
        return nozzle1YOffset;
    }

    /**
     *
     * @return
     */
    public float getNozzle1ZOffset()
    {
        return nozzle1ZOffset;
    }

    /**
     *
     * @return
     */
    public float getNozzle1BOffset()
    {
        return nozzle1BOffset;
    }

    /**
     *
     * @return
     */
    public float getNozzle2XOffset()
    {
        return nozzle2XOffset;
    }

    /**
     *
     * @return
     */
    public float getNozzle2YOffset()
    {
        return nozzle2YOffset;
    }

    /**
     *
     * @return
     */
    public float getNozzle2ZOffset()
    {
        return nozzle2ZOffset;
    }

    /**
     *
     * @return
     */
    public float getNozzle2BOffset()
    {
        return nozzle2BOffset;
    }

    /**
     *
     * @return
     */
    public float getHeadHours()
    {
        return hoursUsed;
    }

    /**
     *
     * @return
     */
    public String getTypeCode()
    {
        return headTypeCode;
    }

    /**
     *
     * @return
     */
    public String getUniqueID()
    {
        return uniqueID;
    }

    /**
     *
     * @return
     */
    public float getBeta()
    {
        return thermistorBeta;
    }

    /**
     *
     * @return
     */
    public float getTCal()
    {
        return thermistorTCal;
    }

    /**
     *
     * @return
     */
    public float getLastFilamentTemperature()
    {
        return lastFilamentTemperature;
    }

    public void updateContents(Head attachedHead)
    {
        //TODO modify for multiple heaters
        headTypeCode = attachedHead.typeCodeProperty().get();
        uniqueID = attachedHead.uniqueIDProperty().get();

        NozzleHeater heater = attachedHead.getNozzleHeaters().get(0);
        maximumTemperature = heater.maximumTemperatureProperty().get();
        thermistorBeta = heater.betaProperty().get();
        thermistorTCal = heater.tCalProperty().get();
        lastFilamentTemperature = heater.lastFilamentTemperatureProperty().get();        
        hoursUsed = attachedHead.headHoursProperty().get();

        if (attachedHead.getNozzles().size() > 0)
        {
            nozzle1XOffset = attachedHead.getNozzles().get(0).xOffsetProperty().get();
            nozzle1YOffset = attachedHead.getNozzles().get(0).yOffsetProperty().get();
            nozzle1ZOffset = attachedHead.getNozzles().get(0).zOffsetProperty().get();
            nozzle1BOffset = attachedHead.getNozzles().get(0).bOffsetProperty().get();
        }

        if (attachedHead.getNozzles().size() > 1)
        {
            nozzle2XOffset = attachedHead.getNozzles().get(1).xOffsetProperty().get();
            nozzle2YOffset = attachedHead.getNozzles().get(1).yOffsetProperty().get();
            nozzle2ZOffset = attachedHead.getNozzles().get(1).zOffsetProperty().get();
            nozzle2BOffset = attachedHead.getNozzles().get(1).bOffsetProperty().get();
        }
    }
}
