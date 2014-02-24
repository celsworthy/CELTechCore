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
public class StatusResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";
    private String runningPrintJobID = null;
    private final int runningPrintJobIDBytes = 16;
    private String printJobLineNumberString = null;
    private int printJobLineNumber = 0;
    private final int printJobLineNumberBytes = 8;
    private boolean xSwitchStatus = false;
    private boolean ySwitchStatus = false;
    private boolean zSwitchStatus = false;
    private boolean pauseStatus = false;
    private boolean busyStatus = false;
    private boolean filament1SwitchStatus = false;
    private boolean filament2SwitchStatus = false;
    private boolean nozzleSwitchStatus = false;
    private boolean lidSwitchStatus = false;
    private boolean reelButtonStatus = false;
    private boolean EIndexStatus = false;
    private boolean DIndexStatus = false;
    private boolean topZSwitchStatus = false;
    private boolean nozzleHeaterOn = false;
    private String extruderTemperatureString = null;
    private int nozzleTemperature = 0;
    private String extruderTargetTemperatureString = null;
    private int nozzleTargetTemperature = 0;
    private String extruderFirstLayerTargetTemperatureString = null;
    private int nozzleFirstLayerTargetTemperature = 0;
    private boolean bedHeaterOn = false;
    private String bedTemperatureString = null;
    private int bedTemperature = 0;
    private String bedTargetTemperatureString = null;
    private int bedTargetTemperature = 0;
    private String bedFirstLayerTargetTemperatureString = null;
    private int bedFirstLayerTargetTemperature = 0;
    private boolean ambientFanOn = false;
    private int ambientTemperature = 0;
    private String ambientTemperatureString = null;
    private String ambientTargetTemperatureString = null;
    private int ambientTargetTemperature = 0;
    private boolean headFanOn = false;
    private boolean headEEPROMPresent = false;
    private boolean reelEEPROMPresent = false;
    private boolean sdCardPresent = false;
    private final int decimalFloatFormatBytes = 8;
    private float headXPosition = 0;
    private float headYPosition = 0;
    private float headZPosition = 0;
    private float EPosition = 0;
    private float DPosition = 0;
    private float BPosition = 0;

    private NumberFormat numberFormatter = NumberFormat.getNumberInstance();

    public String getRunningPrintJobID()
    {
        return runningPrintJobID;
    }

    public int getPrintJobLineNumber()
    {
        return printJobLineNumber;
    }

    public boolean isxSwitchStatus()
    {
        return xSwitchStatus;
    }

    public boolean isySwitchStatus()
    {
        return ySwitchStatus;
    }

    public boolean iszSwitchStatus()
    {
        return zSwitchStatus;
    }

    public boolean isPauseStatus()
    {
        return pauseStatus;
    }
    public boolean isBusyStatus()
    {
        return busyStatus;
    }
    public boolean isFilament1SwitchStatus()
    {
        return filament1SwitchStatus;
    }

    public boolean isFilament2SwitchStatus()
    {
        return filament2SwitchStatus;
    }

    public boolean isNozzleSwitchStatus()
    {
        return nozzleSwitchStatus;
    }

    public boolean isLidSwitchStatus()
    {
        return lidSwitchStatus;
    }

    public boolean isReelButtonStatus()
    {
        return reelButtonStatus;
    }

    public boolean isEIndexStatus()
    {
        return EIndexStatus;
    }

    public boolean isDIndexStatus()
    {
        return DIndexStatus;
    }
    
        public boolean isTopZSwitchStatus()
    {
        return topZSwitchStatus;
    }

    public boolean isNozzleHeaterOn()
    {
        return nozzleHeaterOn;
    }

    public int getNozzleTemperature()
    {
        return nozzleTemperature;
    }

    public int getNozzleTargetTemperature()
    {
        return nozzleTargetTemperature;
    }

    public int getNozzleFirstLayerTargetTemperature()
    {
        return nozzleFirstLayerTargetTemperature;
    }

    public boolean isBedHeaterOn()
    {
        return bedHeaterOn;
    }

    public int getBedTemperature()
    {
        return bedTemperature;
    }

    public int getBedTargetTemperature()
    {
        return bedTargetTemperature;
    }

    public int getBedFirstLayerTargetTemperature()
    {
        return bedFirstLayerTargetTemperature;
    }

    public boolean isAmbientFanOn()
    {
        return ambientFanOn;
    }

    public int getAmbientTemperature()
    {
        return ambientTemperature;
    }

    public int getAmbientTargetTemperature()
    {
        return ambientTargetTemperature;
    }

    public boolean isHeadFanOn()
    {
        return headFanOn;
    }

    public boolean isHeadEEPROMPresent()
    {
        return headEEPROMPresent;
    }

    public boolean isReelEEPROMPresent()
    {
        return reelEEPROMPresent;
    }
    
    public boolean isSDCardPresent()
    {
        return sdCardPresent;
    }
    
    public float getHeadXPosition()
    {
        return headXPosition;
    }

    public float getHeadYPosition()
    {
        return headYPosition;
    }

    public float getHeadZPosition()
    {
        return headZPosition;
    }

    public float getEPosition()
    {
        return EPosition;
    }

    public float getBPosition()
    {
        return BPosition;
    }

    /*
     * Errors...
     */
    public StatusResponse()
    {
        super(RxPacketTypeEnum.STATUS_RESPONSE, false, false);
    }

    @Override
    public boolean populatePacket(byte[] byteData)
    {
        boolean success = false;

        try
        {
            int byteOffset = 1;

            this.runningPrintJobID = new String(byteData, byteOffset, runningPrintJobIDBytes, charsetToUse);
            byteOffset += runningPrintJobIDBytes;

            this.printJobLineNumberString = new String(byteData, byteOffset, printJobLineNumberBytes, charsetToUse);
            byteOffset += printJobLineNumberBytes;

            this.printJobLineNumber = Integer.valueOf(printJobLineNumberString, 16);

            this.pauseStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.busyStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.xSwitchStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.ySwitchStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.zSwitchStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.filament1SwitchStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.filament2SwitchStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.nozzleSwitchStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.lidSwitchStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.reelButtonStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.EIndexStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.DIndexStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.topZSwitchStatus = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.nozzleHeaterOn = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.extruderTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzleTemperature = numberFormatter.parse(extruderTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse extruder temperature - " + extruderTemperatureString);
            }

            this.extruderTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzleTargetTemperature = numberFormatter.parse(extruderTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse extruder target temperature - " + extruderTargetTemperatureString);
            }

            this.extruderFirstLayerTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzleFirstLayerTargetTemperature = numberFormatter.parse(extruderFirstLayerTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse extruder first layer target temperature - " + extruderFirstLayerTargetTemperatureString);
            }

            this.bedHeaterOn = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.bedTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.bedTemperature = numberFormatter.parse(bedTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse bed temperature - " + bedTemperatureString);
            }

            this.bedTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.bedTargetTemperature = numberFormatter.parse(bedTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse bed target temperature - " + bedTargetTemperatureString);
            }

            this.bedFirstLayerTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.bedFirstLayerTargetTemperature = numberFormatter.parse(bedFirstLayerTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse bed first layer target temperature - " + bedFirstLayerTargetTemperatureString);
            }

            this.ambientFanOn = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.ambientTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.ambientTemperature = numberFormatter.parse(ambientTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse ambient temperature - " + ambientTemperatureString);
            }

            this.ambientTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.ambientTargetTemperature = numberFormatter.parse(ambientTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse ambient target temperature - " + ambientTargetTemperatureString);
            }

            this.headFanOn = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.headEEPROMPresent = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.reelEEPROMPresent = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.sdCardPresent = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            String headXPositionString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.headXPosition = numberFormatter.parse(headXPositionString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse head X position - " + headXPositionString);
            }

            String headYPositionString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.headYPosition = numberFormatter.parse(headYPositionString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse head Y position - " + headYPositionString);
            }

            String headZPositionString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.headZPosition = numberFormatter.parse(headZPositionString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse head Z position - " + headZPositionString);
            }

            String EPositionString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.EPosition = numberFormatter.parse(EPositionString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse E position - " + EPositionString);
            }

            String DPositionString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.DPosition = numberFormatter.parse(DPositionString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse D position - " + DPositionString);
            }

            String BPositionString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.BPosition = numberFormatter.parse(BPositionString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse B position - " + BPositionString);
            }

            success = true;
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Failed to convert byte array to Status Response");
        }

        return success;
    }

    public String toString()
    {
        StringBuilder outputString = new StringBuilder();

        outputString.append(">>>>>>>>>>\n");
        outputString.append("Packet type:");
        outputString.append(getPacketType().name());
        outputString.append("\n");
        outputString.append("Print job ID: " + getRunningPrintJobID());
        outputString.append("\n");
        outputString.append("Print line number: " + getPrintJobLineNumber());
        outputString.append("\n");
        outputString.append("Pause status: " + isPauseStatus());
        outputString.append("\n");
        outputString.append("Busy status: " + isBusyStatus());
        outputString.append("\n");
        outputString.append("X switch status: " + isxSwitchStatus());
        outputString.append("\n");
        outputString.append("Y switch status: " + isySwitchStatus());
        outputString.append("\n");
        outputString.append("Z switch status: " + iszSwitchStatus());
        outputString.append("\n");
        outputString.append("Filament 1 switch status: " + isFilament1SwitchStatus());
        outputString.append("\n");
        outputString.append("Filament 2 switch status: " + isFilament2SwitchStatus());
        outputString.append("\n");
        outputString.append("Nozzle switch status: " + isNozzleSwitchStatus());
        outputString.append("\n");
        outputString.append("Lid switch status: " + isLidSwitchStatus());
        outputString.append("\n");
        outputString.append("Reel button status: " + isReelButtonStatus());
        outputString.append("\n");
        outputString.append("E index status: " + isEIndexStatus());
        outputString.append("\n");
        outputString.append("D index status: " + isDIndexStatus());
        outputString.append("\n");
        outputString.append("Top Z switch status: " + isTopZSwitchStatus());
        outputString.append("\n");
        outputString.append("Extruder heater on: " + isNozzleHeaterOn());
        outputString.append("\n");
        outputString.append("Extruder temperature: " + getNozzleTemperature());
        outputString.append("\n");
        outputString.append("Extruder target temperature: " + getNozzleTargetTemperature());
        outputString.append("\n");
        outputString.append("Extruder first layer target temperature: " + getNozzleFirstLayerTargetTemperature());
        outputString.append("\n");
        outputString.append("Bed heater on: " + isBedHeaterOn());
        outputString.append("\n");
        outputString.append("Bed temperature: " + getBedTemperature());
        outputString.append("\n");
        outputString.append("Bed target temperature: " + getBedTargetTemperature());
        outputString.append("\n");
        outputString.append("Bed first layer target temperature: " + getBedFirstLayerTargetTemperature());
        outputString.append("\n");
        outputString.append("Ambient fan on: " + isAmbientFanOn());
        outputString.append("\n");
        outputString.append("Ambient temperature: " + getAmbientTemperature());
        outputString.append("\n");
        outputString.append("Ambient target temperature: " + getAmbientTargetTemperature());
        outputString.append("\n");
        outputString.append("Head fan on: " + isHeadFanOn());
        outputString.append("\n");
        outputString.append("Head EEPROM present: " + isHeadEEPROMPresent());
        outputString.append("\n");
        outputString.append("Reel EEPROM present: " + isReelEEPROMPresent());
        outputString.append("\n");
        outputString.append("SD card present: " + isSDCardPresent());
        outputString.append("\n");
        outputString.append(">>>>>>>>>>\n");

        return outputString.toString();
    }
}
