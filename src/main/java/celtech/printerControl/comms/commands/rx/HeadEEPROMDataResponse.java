/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl.comms.commands.rx;

import celtech.printerControl.comms.commands.tx.WriteHeadEEPROM;
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
    
    private String filament0ID = "";
    private String filament1ID = "";

    private float nozzle2XOffset = 0;
    private float nozzle2YOffset = 0;
    private float nozzle2ZOffset = 0;
    private float nozzle2BOffset = 0;

    private float lastFilamentTemperature0 = 0;
    private float lastFilamentTemperature1 = 0;
    private float hoursUsed = 0;

    public HeadEEPROMDataResponse()
    {
        super(RxPacketTypeEnum.HEAD_EEPROM_DATA, false, false);
    }

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

            String maxTempString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                              charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                maximumTemperature = decimalFloatFormatter.parse(maxTempString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse maximum temperature - " + maxTempString);
            }

            String thermistorBetaString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                     charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                thermistorBeta = decimalFloatFormatter.parse(thermistorBetaString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse thermistor beta - " + thermistorBetaString);
            }

            String thermistorTCalString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                     charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                thermistorTCal = decimalFloatFormatter.parse(thermistorTCalString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse thermistor tcal - " + thermistorTCalString);
            }

            String nozzle1XOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                     charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                nozzle1XOffset = decimalFloatFormatter.parse(nozzle1XOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 1 X offset - " + nozzle1XOffsetString);
            }

            String nozzle1YOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                     charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle1YOffset = decimalFloatFormatter.parse(nozzle1YOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 1 Y offset - " + nozzle1YOffsetString);
            }

            String nozzle1ZOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                     charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle1ZOffset = decimalFloatFormatter.parse(nozzle1ZOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 1 Z offset - " + nozzle1ZOffsetString);
            }

            String nozzle1BOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                     charsetToUse);
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

            String nozzle2XOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                     charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                nozzle2XOffset = decimalFloatFormatter.parse(nozzle2XOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 X offset - " + nozzle2XOffsetString);
            }

            String nozzle2YOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                     charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle2YOffset = decimalFloatFormatter.parse(nozzle2YOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 Y offset - " + nozzle2YOffsetString);
            }

            String nozzle2ZOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                     charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle2ZOffset = decimalFloatFormatter.parse(nozzle2ZOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 Z offset - " + nozzle2ZOffsetString);
            }

            String nozzle2BOffsetString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                     charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                nozzle2BOffset = decimalFloatFormatter.parse(nozzle2BOffsetString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle 2 B offset - " + nozzle2BOffsetString);
            }

            //Empty section
            byteOffset += 24;

            String lastFilamentTemperatureString1 = new String(byteData, byteOffset,
                                                              decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                lastFilamentTemperature1 = decimalFloatFormatter.parse(
                    lastFilamentTemperatureString1.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse last filament temperature 1 - "
                    + lastFilamentTemperatureString1);
            }
            
            String lastFilamentTemperatureString0 = new String(byteData, byteOffset,
                                                              decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                lastFilamentTemperature0 = decimalFloatFormatter.parse(
                    lastFilamentTemperatureString0.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse last filament temperature 0 - "
                    + lastFilamentTemperatureString0);
            }            

            String hoursUsedString = new String(byteData, byteOffset, decimalFloatFormatBytes,
                                                charsetToUse);
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
            steno.error("Failed to convert byte array to Head EEPROM Response");
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

    public float getHeadHours()
    {
        return hoursUsed;
    }

    public String getTypeCode()
    {
        return headTypeCode;
    }

    public String getUniqueID()
    {
        return uniqueID;
    }

    public float getBeta()
    {
        return thermistorBeta;
    }

    public float getTCal()
    {
        return thermistorTCal;
    }

    public float getLastFilamentTemperature(int nozzleHeaterNumber)
    {
        if (nozzleHeaterNumber == 0)
        {
            return lastFilamentTemperature0;
        } else if (nozzleHeaterNumber == 1)
        {
            return lastFilamentTemperature1;
        } else
        {
            throw new RuntimeException("unrecognised nozzle heater number: " + nozzleHeaterNumber);
        }
    }
    
    public String getFilamentID(int nozzleHeaterNumber) {
         if (nozzleHeaterNumber == 0)
        {
            return filament0ID;
        } else if (nozzleHeaterNumber == 1)
        {
            return filament1ID;
        } else
        {
            throw new RuntimeException("unrecognised nozzle heater number: " + nozzleHeaterNumber);
        }
    }

    public void updateContents(Head attachedHead)
    {
        //TODO modify for multiple heaters
        headTypeCode = attachedHead.typeCodeProperty().get();
        uniqueID = attachedHead.uniqueIDProperty().get();

        if (attachedHead.getNozzleHeaters().size() > 0)
        {
            NozzleHeater heater = attachedHead.getNozzleHeaters().get(0);
            maximumTemperature = heater.maximumTemperatureProperty().get();
            thermistorBeta = heater.betaProperty().get();
            thermistorTCal = heater.tCalProperty().get();
            lastFilamentTemperature0 = heater.lastFilamentTemperatureProperty().get();
        }
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

    public void setHeadTypeCode(String headTypeCode)
    {
        this.headTypeCode = headTypeCode;
    }

    public void setUniqueID(String uniqueID)
    {
        this.uniqueID = uniqueID;
    }

    public void setMaximumTemperature(float maximumTemperature)
    {
        this.maximumTemperature = maximumTemperature;
    }

    public void setThermistorBeta(float thermistorBeta)
    {
        this.thermistorBeta = thermistorBeta;
    }

    public void setThermistorTCal(float thermistorTCal)
    {
        this.thermistorTCal = thermistorTCal;
    }

    public void setNozzle1XOffset(float nozzle1XOffset)
    {
        this.nozzle1XOffset = nozzle1XOffset;
    }

    public void setNozzle1YOffset(float nozzle1YOffset)
    {
        this.nozzle1YOffset = nozzle1YOffset;
    }

    public void setNozzle1ZOffset(float nozzle1ZOffset)
    {
        this.nozzle1ZOffset = nozzle1ZOffset;
    }

    public void setNozzle1BOffset(float nozzle1BOffset)
    {
        this.nozzle1BOffset = nozzle1BOffset;
    }

    public void setNozzle2XOffset(float nozzle2XOffset)
    {
        this.nozzle2XOffset = nozzle2XOffset;
    }

    public void setNozzle2YOffset(float nozzle2YOffset)
    {
        this.nozzle2YOffset = nozzle2YOffset;
    }

    public void setNozzle2ZOffset(float nozzle2ZOffset)
    {
        this.nozzle2ZOffset = nozzle2ZOffset;
    }

    public void setNozzle2BOffset(float nozzle2BOffset)
    {
        this.nozzle2BOffset = nozzle2BOffset;
    }

    public void setLastFilamentTemperature0(float lastFilamentTemperature)
    {
        this.lastFilamentTemperature0 = lastFilamentTemperature;
    }
    
    public void setLastFilamentTemperature1(float lastFilamentTemperature)
    {
        this.lastFilamentTemperature1 = lastFilamentTemperature;
    }    

    public void setHoursUsed(float hoursUsed)
    {
        this.hoursUsed = hoursUsed;
    }

    /**
     * This method is used to populate the response data prior to head update It should be used for
     * test purposes ONLY.
     *
     * @param headWriteCommand
     */
    public void updateFromWrite(WriteHeadEEPROM headWriteCommand)
    {
        //TODO ensure this copes with all data
        headTypeCode = headWriteCommand.getHeadTypeCode();
        uniqueID = headWriteCommand.getHeadUniqueID();

        maximumTemperature = headWriteCommand.getMaximumTemperature();
        thermistorBeta = headWriteCommand.getThermistorBeta();
        thermistorTCal = headWriteCommand.getThermistorTCal();
        lastFilamentTemperature0 = headWriteCommand.getLastFilamentTemperature0();
        lastFilamentTemperature1 = headWriteCommand.getLastFilamentTemperature1();

        nozzle1XOffset = headWriteCommand.getNozzle1XOffset();
        nozzle1YOffset = headWriteCommand.getNozzle1YOffset();
        nozzle1ZOffset = headWriteCommand.getNozzle1ZOffset();
        nozzle1BOffset = headWriteCommand.getNozzle1BOffset();
        
        filament0ID = headWriteCommand.getFilament0ID();
        filament1ID = headWriteCommand.getFilament1ID();

        nozzle2XOffset = headWriteCommand.getNozzle2XOffset();
        nozzle2YOffset = headWriteCommand.getNozzle2YOffset();
        nozzle2ZOffset = headWriteCommand.getNozzle2ZOffset();
        nozzle2BOffset = headWriteCommand.getNozzle2BOffset();
    }
}
