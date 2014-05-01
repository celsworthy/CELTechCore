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
public class HeadEEPROMDataResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";
    private NumberFormat numberFormatter = NumberFormat.getNumberInstance();

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

    public HeadEEPROMDataResponse()
    {
        super(RxPacketTypeEnum.HEAD_EEPROM_DATA, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        boolean success = false;

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
                maximumTemperature = numberFormatter.parse(maxTempString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse maximum temperature - " + maxTempString);
            }

            String thermistorBetaString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                thermistorBeta = numberFormatter.parse(thermistorBetaString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse thermistor beta - " + thermistorBetaString);
            }

            String thermistorTCalString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                thermistorTCal = numberFormatter.parse(thermistorTCalString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse thermistor tcal - " + thermistorTCalString);
            }

            String nozzle1XOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                nozzle1XOffset = numberFormatter.parse(nozzle1XOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 1 X offset - " + nozzle1XOffsetString);
            }

            String nozzle1YOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle1YOffset = numberFormatter.parse(nozzle1YOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 1 Y offset - " + nozzle1YOffsetString);
            }

            String nozzle1ZOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle1ZOffset = numberFormatter.parse(nozzle1ZOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 1 Z offset - " + nozzle1ZOffsetString);
            }

            String nozzle1BOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle1BOffset = numberFormatter.parse(nozzle1BOffsetString.trim()).floatValue();
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
                nozzle2XOffset = numberFormatter.parse(nozzle2XOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 X offset - " + nozzle2XOffsetString);
            }

            String nozzle2YOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle2YOffset = numberFormatter.parse(nozzle2YOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 Y offset - " + nozzle2YOffsetString);
            }

            String nozzle2ZOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle2ZOffset = numberFormatter.parse(nozzle2ZOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 Z offset - " + nozzle2ZOffsetString);
            }

            String nozzle2BOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle2BOffset = numberFormatter.parse(nozzle2BOffsetString.trim()).floatValue();
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
                lastFilamentTemperature = numberFormatter.parse(lastFilamentTemperatureString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse last filament temperature - " + lastFilamentTemperatureString);
            }

            String hoursUsedString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                hoursUsed = numberFormatter.parse(hoursUsedString.trim()).intValue();
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

    public float getMaximumTemperature()
    {
        return maximumTemperature;
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

    public float getHoursUsed()
    {
        return hoursUsed;
    }

    public String getHeadTypeCode()
    {
        return headTypeCode;
    }

    public String getUniqueID()
    {
        return uniqueID;
    }

    public float getThermistorBeta()
    {
        return thermistorBeta;
    }

    public float getThermistorTCal()
    {
        return thermistorTCal;
    }
    
    public float getLastFilamentTemperature()
    {
        return lastFilamentTemperature;
    }
}
