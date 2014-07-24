package celtech.printerControl.comms.commands.rx;

import celtech.configuration.EEPROMState;
import celtech.configuration.HeaterMode;
import celtech.configuration.PauseStatus;
import celtech.configuration.WhyAreWeWaitingState;
import celtech.utils.FixedDecimalFloatFormat;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;

/**
 *
 * @author ianhudson
 */
public class StatusResponse extends RoboxRxPacket
{
    /* Current spec of status response as of v677 firmware */

    /*
     status: <0xe1> iiiiiiiiiiiiiiii llllllll p b x y z e d b g h i j a k mmmmmmmm nnnnnnnn cccccccc o pppppppp qqqqqqqq aaaaaaaa r ssssssss tttttttt u c v w s xxxxxxxx yyyyyyyy zzzzzzzz bbbbbbbb eeeeeeee gggggggg ffffffff
     iiiiiiiiiiiiiiii = id of running job
     llllllll = line # of running job in hex
     p = pause ('0'->normal, '1'->pause pending, '2'->paused, '3'->resume pending)
     b = busy
     x = X switch state
     y = Y switch state
     z = Z switch state
     e = E switch state
     d = D switch state
     b = nozzle switch state
     g = lid switch state
     h = eject switch state
     i = E index wheel state
     j = D index wheel state
     a = Z top switch state
     k = extruder heater mode ('0'->off, '1'->normal, '2'->first layer)
     mmmmmmmm = extruder temperature (decimal float format)
     nnnnnnnn = extruder target (decimal float format)
     cccccccc = extruder first layer target (decimal float format)
     o = bed heater mode ('0'->off, '1'->normal, '2'->first layer)
     pppppppp = bed temperature (decimal float format)
     qqqqqqqq = bed target (decimal float format)
     aaaaaaaa = bed first layer target (decimal float format)
     r = ambient controller on
     ssssssss = ambient temperature (decimal float format)
     tttttttt = ambient target (decimal float format)
     u = head fan on
     c = why are we waiting ('0'->not waiting, '1'->waiting for bed to cool, '2'->waiting for bed to reach target, '3'->waiting for extruder to reach target
     v = head EEPROM state ('0'->none, '1'->not valid, '2'->valid)
     w = reel EEPROM state ('0'->none, '1'->not valid, '2'->valid
     s = SD card present
     xxxxxxxx = X position (decimal float format)
     yyyyyyyy = Y position (decimal float format)
     zzzzzzzz = Z position (decimal float format)
     bbbbbbbb = B position (decimal float format)
     eeeeeeee = E filament diameter (decimal float format)
     gggggggg = E filament multiplier (decimal float format)
     ffffffff = Feed rate multiplier (decimal float format)
     total length = 166
     */
    private final String charsetToUse = "US-ASCII";
    private String runningPrintJobID = null;
    private final int runningPrintJobIDBytes = 16;
    private String printJobLineNumberString = null;
    private int printJobLineNumber = 0;
    private final int printJobLineNumberBytes = 8;
    private boolean xSwitchStatus = false;
    private boolean ySwitchStatus = false;
    private boolean zSwitchStatus = false;
    private PauseStatus pauseStatus = null;
    private boolean busyStatus = false;
    private boolean filament1SwitchStatus = false;
    private boolean filament2SwitchStatus = false;
    private boolean nozzleSwitchStatus = false;
    private boolean lidSwitchStatus = false;
    private boolean reelButtonStatus = false;
    private boolean EIndexStatus = false;
    private boolean DIndexStatus = false;
    private boolean topZSwitchStatus = false;
    private HeaterMode nozzleHeaterMode = HeaterMode.OFF;
    private String nozzleHeaterModeString = null;
    private String nozzleTemperatureString = null;
    private int nozzleTemperature = 0;
    private String nozzleTargetTemperatureString = null;
    private int nozzleTargetTemperature = 0;
    private String nozzleFirstLayerTargetTemperatureString = null;
    private int nozzleFirstLayerTargetTemperature = 0;
    private HeaterMode bedHeaterMode = HeaterMode.OFF;
    private String bedHeaterModeString = null;
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
    private EEPROMState headEEPROMState = EEPROMState.NOT_PRESENT;
    private EEPROMState reelEEPROMState = EEPROMState.NOT_PRESENT;
    private boolean sdCardPresent = false;
    private final int decimalFloatFormatBytes = 8;
    private float headXPosition = 0;
    private float headYPosition = 0;
    private float headZPosition = 0;
    private float BPosition = 0;
    private float filamentDiameter = 0;
    private float filamentMultiplier = 0;
    private float feedRateMultiplier = 0;
    private WhyAreWeWaitingState whyAreWeWaitingState = WhyAreWeWaitingState.NOT_WAITING;

    /**
     *
     * @return
     */
    public String getRunningPrintJobID()
    {
        return runningPrintJobID;
    }

    /**
     *
     * @return
     */
    public int getPrintJobLineNumber()
    {
        return printJobLineNumber;
    }

    /**
     *
     * @return
     */
    public boolean isxSwitchStatus()
    {
        return xSwitchStatus;
    }

    /**
     *
     * @return
     */
    public boolean isySwitchStatus()
    {
        return ySwitchStatus;
    }

    /**
     *
     * @return
     */
    public boolean iszSwitchStatus()
    {
        return zSwitchStatus;
    }

    /**
     *
     * @return
     */
    public PauseStatus getPauseStatus()
    {
        return pauseStatus;
    }

    /**
     *
     * @return
     */
    public boolean isBusyStatus()
    {
        return busyStatus;
    }

    /**
     *
     * @return
     */
    public boolean isFilament1SwitchStatus()
    {
        return filament1SwitchStatus;
    }

    /**
     *
     * @return
     */
    public boolean isFilament2SwitchStatus()
    {
        return filament2SwitchStatus;
    }

    /**
     *
     * @return
     */
    public boolean isNozzleSwitchStatus()
    {
        return nozzleSwitchStatus;
    }

    /**
     *
     * @return
     */
    public boolean isLidSwitchStatus()
    {
        return lidSwitchStatus;
    }

    /**
     *
     * @return
     */
    public boolean isReelButtonStatus()
    {
        return reelButtonStatus;
    }

    /**
     *
     * @return
     */
    public boolean isEIndexStatus()
    {
        return EIndexStatus;
    }

    /**
     *
     * @return
     */
    public boolean isDIndexStatus()
    {
        return DIndexStatus;
    }

    /**
     *
     * @return
     */
    public boolean isTopZSwitchStatus()
    {
        return topZSwitchStatus;
    }

    /**
     *
     * @return
     */
    public HeaterMode getNozzleHeaterMode()
    {
        return nozzleHeaterMode;
    }

    /**
     *
     * @return
     */
    public int getNozzleTemperature()
    {
        return nozzleTemperature;
    }

    /**
     *
     * @return
     */
    public int getNozzleTargetTemperature()
    {
        return nozzleTargetTemperature;
    }

    /**
     *
     * @return
     */
    public int getNozzleFirstLayerTargetTemperature()
    {
        return nozzleFirstLayerTargetTemperature;
    }

    /**
     *
     * @return
     */
    public HeaterMode getBedHeaterMode()
    {
        return bedHeaterMode;
    }

    /**
     *
     * @return
     */
    public int getBedTemperature()
    {
        return bedTemperature;
    }

    /**
     *
     * @return
     */
    public int getBedTargetTemperature()
    {
        return bedTargetTemperature;
    }

    /**
     *
     * @return
     */
    public int getBedFirstLayerTargetTemperature()
    {
        return bedFirstLayerTargetTemperature;
    }

    /**
     *
     * @return
     */
    public boolean isAmbientFanOn()
    {
        return ambientFanOn;
    }

    /**
     *
     * @return
     */
    public int getAmbientTemperature()
    {
        return ambientTemperature;
    }

    /**
     *
     * @return
     */
    public int getAmbientTargetTemperature()
    {
        return ambientTargetTemperature;
    }

    /**
     *
     * @return
     */
    public boolean isHeadFanOn()
    {
        return headFanOn;
    }

    /**
     *
     * @return
     */
    public EEPROMState getHeadEEPROMState()
    {
        return headEEPROMState;
    }

    /**
     *
     * @return
     */
    public EEPROMState getReelEEPROMState()
    {
        return reelEEPROMState;
    }

    /**
     *
     * @return
     */
    public boolean isSDCardPresent()
    {
        return sdCardPresent;
    }

    /**
     *
     * @return
     */
    public float getHeadXPosition()
    {
        return headXPosition;
    }

    /**
     *
     * @return
     */
    public float getHeadYPosition()
    {
        return headYPosition;
    }

    /**
     *
     * @return
     */
    public float getHeadZPosition()
    {
        return headZPosition;
    }

    /**
     *
     * @return
     */
    public float getFilamentDiameter()
    {
        return filamentDiameter;
    }

    /**
     *
     * @return
     */
    public float getFilamentMultiplier()
    {
        return filamentMultiplier;
    }

    /**
     *
     * @return
     */
    public float getFeedRateMultiplier()
    {
        return feedRateMultiplier;
    }

    /**
     *
     * @return
     */
    public WhyAreWeWaitingState getWhyAreWeWaitingState()
    {
        return whyAreWeWaitingState;
    }

    /*
     * Errors...
     */
    /**
     *
     */
    public StatusResponse()
    {
        super(RxPacketTypeEnum.STATUS_RESPONSE, false, false);
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

            this.runningPrintJobID = new String(byteData, byteOffset, runningPrintJobIDBytes, charsetToUse);
            byteOffset += runningPrintJobIDBytes;

            this.printJobLineNumberString = new String(byteData, byteOffset, printJobLineNumberBytes, charsetToUse);
            byteOffset += printJobLineNumberBytes;

            this.printJobLineNumber = Integer.valueOf(printJobLineNumberString, 16);

            String pauseStatusString = new String(byteData, byteOffset, 1, charsetToUse);
            byteOffset += 1;
            this.pauseStatus = PauseStatus.modeFromValue(Integer.valueOf(pauseStatusString, 16));

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

            this.nozzleHeaterModeString = new String(byteData, byteOffset, 1, charsetToUse);
            byteOffset += 1;
            this.nozzleHeaterMode = HeaterMode.modeFromValue(Integer.valueOf(nozzleHeaterModeString, 16));

            this.nozzleTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzleTemperature = decimalFloatFormatter.parse(nozzleTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle temperature - " + nozzleTemperatureString);
            }

            this.nozzleTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzleTargetTemperature = decimalFloatFormatter.parse(nozzleTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle target temperature - " + nozzleTargetTemperatureString);
            }

            this.nozzleFirstLayerTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzleFirstLayerTargetTemperature = decimalFloatFormatter.parse(nozzleFirstLayerTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle first layer target temperature - " + nozzleFirstLayerTargetTemperatureString);
            }

            this.bedHeaterModeString = new String(byteData, byteOffset, 1, charsetToUse);
            byteOffset += 1;
            this.bedHeaterMode = HeaterMode.modeFromValue(Integer.valueOf(bedHeaterModeString, 16));

            this.bedTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.bedTemperature = decimalFloatFormatter.parse(bedTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse bed temperature - " + bedTemperatureString);
            }

            this.bedTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.bedTargetTemperature = decimalFloatFormatter.parse(bedTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse bed target temperature - " + bedTargetTemperatureString);
            }

            this.bedFirstLayerTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.bedFirstLayerTargetTemperature = decimalFloatFormatter.parse(bedFirstLayerTargetTemperatureString).intValue();
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
                this.ambientTemperature = decimalFloatFormatter.parse(ambientTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse ambient temperature - " + ambientTemperatureString);
            }

            this.ambientTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.ambientTargetTemperature = decimalFloatFormatter.parse(ambientTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse ambient target temperature - " + ambientTargetTemperatureString);
            }

            this.headFanOn = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            String whyAreWeWaitingStateString = new String(byteData, byteOffset, 1, charsetToUse);
            byteOffset += 1;
            this.whyAreWeWaitingState = WhyAreWeWaitingState.modeFromValue(Integer.valueOf(whyAreWeWaitingStateString, 16));

            String headEEPROMStateString = new String(byteData, byteOffset, 1, charsetToUse);
            byteOffset += 1;
            this.headEEPROMState = EEPROMState.modeFromValue(Integer.valueOf(headEEPROMStateString, 16));

            String reelEEPROMStateString = new String(byteData, byteOffset, 1, charsetToUse);
            byteOffset += 1;
            this.reelEEPROMState = EEPROMState.modeFromValue(Integer.valueOf(reelEEPROMStateString, 16));

            this.sdCardPresent = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            String headXPositionString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.headXPosition = decimalFloatFormatter.parse(headXPositionString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse head X position - " + headXPositionString);
            }

            String headYPositionString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.headYPosition = decimalFloatFormatter.parse(headYPositionString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse head Y position - " + headYPositionString);
            }

            String headZPositionString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.headZPosition = decimalFloatFormatter.parse(headZPositionString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse head Z position - " + headZPositionString);
            }

            String BPositionString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.BPosition = decimalFloatFormatter.parse(BPositionString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse B position - " + BPositionString);
            }

            String filamentDiameterString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.filamentDiameter = decimalFloatFormatter.parse(filamentDiameterString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse filament diameter - " + filamentDiameterString);
            }

            String filamentMultiplierString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.filamentMultiplier = decimalFloatFormatter.parse(filamentMultiplierString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse filament multiplier - " + filamentMultiplierString);
            }

            String feedRateMultiplierString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.feedRateMultiplier = decimalFloatFormatter.parse(feedRateMultiplierString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse feed rate multiplier - " + feedRateMultiplierString);
            }

            success = true;
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Failed to convert byte array to Status Response");
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
        outputString.append("Print job ID: " + getRunningPrintJobID());
        outputString.append("\n");
        outputString.append("Print line number: " + getPrintJobLineNumber());
        outputString.append("\n");
        outputString.append("Pause status: " + getPauseStatus());
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
        outputString.append("Extruder heater on: " + getNozzleHeaterMode());
        outputString.append("\n");
        outputString.append("Extruder temperature: " + getNozzleTemperature());
        outputString.append("\n");
        outputString.append("Extruder target temperature: " + getNozzleTargetTemperature());
        outputString.append("\n");
        outputString.append("Extruder first layer target temperature: " + getNozzleFirstLayerTargetTemperature());
        outputString.append("\n");
        outputString.append("Bed heater on: " + getBedHeaterMode());
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
        outputString.append("Head EEPROM present: " + getHeadEEPROMState());
        outputString.append("\n");
        outputString.append("Reel EEPROM present: " + getReelEEPROMState());
        outputString.append("\n");
        outputString.append("SD card present: " + isSDCardPresent());
        outputString.append("\n");
        outputString.append(">>>>>>>>>>\n");

        return outputString.toString();
    }
}
