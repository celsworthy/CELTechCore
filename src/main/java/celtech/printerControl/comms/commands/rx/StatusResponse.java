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
    /*
     v 689
     status: <0xe1> iiiiiiiiiiiiiiii llllllll p b x y z e d b g h i j a m n k mmmmmmmm nnnnnnnn ccccccccl rrrrrrrr uuuuuuuu dddddddd o pppppppp qqqqqqqq aaaaaaaa r ssssssss tttttttt u c v w x p s xxxxxxxx yyyyyyyy zzzzzzzz bbbbbbbb eeeeeeee gggggggg hhhhhhhh jjjjjjjj ffffffff
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
     m = extruder E ('0'->not present, '1'->present)
     n = extruder D ('0'->not present, '1'->present)
     k = nozzle 0 heater mode ('0'->off, '1'->normal, '2'->first layer)

     mmmmmmmm = nozzle 0 temperature (decimal float format)
     nnnnnnnn = nozzle 0 target (decimal float format)
     cccccccc = nozzle 0 first layer target (decimal float format)
     l = nozzle 1 heater mode ('0'-> off, '1'-<normal, '2'->first layer)
     rrrrrrrr = nozzle 1 temperature (decimal float format)
     uuuuuuuu = nozzle 1 target (decimal float format)
     dddddddd = nozzle 1 first layer target (decimal float format)
     o = bed heater mode ('0'->off, '1'->normal, '2'->first layer)
     pppppppp = bed temperature (decimal float format)
     qqqqqqqq = bed target (decimal float format)
     aaaaaaaa = bed first layer target (decimal float format)
     r = ambient controller on
     ssssssss = ambient temperature (decimal float format)
     tttttttt = ambient target (decimal float format)
     u = head fan on
     c = why are we waiting ('0'->not waiting, '1'->waiting for bed to cool, '2'->waiting for bed to reach target, '3'->waiting for nozzle to reach target
     v = head EEPROM state ('0'->none, '1'->not valid, '2'->valid)
     w = reel0 EEPROM state ('0'->none, '1'->not valid, '2'->valid)
     x = reel1 EEPROM state ('0'->none, '1'->not valid, '2'->valid)
     p = dual-reel adaptor ('0'->not present, '1'->present)
     s = SD card present
     xxxxxxxx = X position (decimal float format)
     yyyyyyyy = Y position (decimal float format)
     zzzzzzzz = Z position (decimal float format)
     bbbbbbbb = B position (decimal float format)
     eeeeeeee = E filament diameter (decimal float format)
     gggggggg = E filament multiplier (decimal float format)
     hhhhhhhh = D filament diameter (decimal float format)
     jjjjjjjj = D filament multiplier (decimal float format)
     ffffffff = Feed rate multiplier (decimal float format)
     total length = 211
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
    private boolean extruderEPresent = false;
    private boolean extruderDPresent = false;
    private HeaterMode nozzle0HeaterMode = HeaterMode.OFF;
    private String nozzle0HeaterModeString = null;
    private String nozzle0TemperatureString = null;
    private int nozzle0Temperature = 0;
    private String nozzle0TargetTemperatureString = null;
    private int nozzle0TargetTemperature = 0;
    private String nozzle0FirstLayerTargetTemperatureString = null;
    private int nozzle0FirstLayerTargetTemperature = 0;
    private HeaterMode nozzle1HeaterMode = HeaterMode.OFF;
    private String nozzle1HeaterModeString = null;
    private String nozzle1TemperatureString = null;
    private int nozzle1Temperature = 0;
    private String nozzle1TargetTemperatureString = null;
    private int nozzle1TargetTemperature = 0;
    private String nozzle1FirstLayerTargetTemperatureString = null;
    private int nozzle1FirstLayerTargetTemperature = 0;
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
    private EEPROMState reel0EEPROMState = EEPROMState.NOT_PRESENT;
    private EEPROMState reel1EEPROMState = EEPROMState.NOT_PRESENT;
    private boolean dualReelAdaptorPresent = false;
    private boolean sdCardPresent = false;
    private final int decimalFloatFormatBytes = 8;
    private float headXPosition = 0;
    private float headYPosition = 0;
    private float headZPosition = 0;
    private float BPosition = 0;
    private float EFilamentDiameter = 0;
    private float EFilamentMultiplier = 0;
    private float DFilamentDiameter = 0;
    private float DFilamentMultiplier = 0;
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
    public boolean isExtruderEPresent()
    {
        return extruderEPresent;
    }

    /**
     *
     * @return
     */
    public boolean isExtruderDPresent()
    {
        return extruderDPresent;
    }

    /**
     *
     * @return
     */
    public HeaterMode getNozzle0HeaterMode()
    {
        return nozzle0HeaterMode;
    }

    /**
     *
     * @return
     */
    public int getNozzle0Temperature()
    {
        return nozzle0Temperature;
    }

    /**
     *
     * @return
     */
    public int getNozzle0TargetTemperature()
    {
        return nozzle0TargetTemperature;
    }

    /**
     *
     * @return
     */
    public int getNozzle0FirstLayerTargetTemperature()
    {
        return nozzle0FirstLayerTargetTemperature;
    }

    /**
     *
     * @return
     */
    public HeaterMode getNozzle1HeaterMode()
    {
        return nozzle1HeaterMode;
    }

    /**
     *
     * @return
     */
    public int getNozzle1Temperature()
    {
        return nozzle1Temperature;
    }

    /**
     *
     * @return
     */
    public int getNozzle1TargetTemperature()
    {
        return nozzle1TargetTemperature;
    }

    /**
     *
     * @return
     */
    public int getNozzle1FirstLayerTargetTemperature()
    {
        return nozzle1FirstLayerTargetTemperature;
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
    public EEPROMState getReelEEPROMState(int reelNumber)
    {
        EEPROMState returnValue = EEPROMState.NOT_PRESENT;

        switch (reelNumber)
        {
            case 0:
                returnValue = reel0EEPROMState;
                break;
            case 1:
                returnValue = reel1EEPROMState;
                break;
        }
        return returnValue;
    }

    /**
     *
     * @return
     */
    public boolean isDualReelAdaptorPresent()
    {
        return dualReelAdaptorPresent;
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
    public float getBPosition()
    {
        return BPosition;
    }

    /**
     *
     * @return
     */
    public float getFilamentDiameter()
    {
        return EFilamentDiameter;
    }

    /**
     *
     * @return
     */
    public float getFilamentMultiplier()
    {
        return EFilamentMultiplier;
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

    public void setRunningPrintJobID(String runningPrintJobID)
    {
        this.runningPrintJobID = runningPrintJobID;
    }

    public void setPrintJobLineNumberString(String printJobLineNumberString)
    {
        this.printJobLineNumberString = printJobLineNumberString;
    }

    public void setPrintJobLineNumber(int printJobLineNumber)
    {
        this.printJobLineNumber = printJobLineNumber;
    }

    public void setxSwitchStatus(boolean xSwitchStatus)
    {
        this.xSwitchStatus = xSwitchStatus;
    }

    public void setySwitchStatus(boolean ySwitchStatus)
    {
        this.ySwitchStatus = ySwitchStatus;
    }

    public void setzSwitchStatus(boolean zSwitchStatus)
    {
        this.zSwitchStatus = zSwitchStatus;
    }

    public void setPauseStatus(PauseStatus pauseStatus)
    {
        this.pauseStatus = pauseStatus;
    }

    public void setBusyStatus(boolean busyStatus)
    {
        this.busyStatus = busyStatus;
    }

    public void setFilament1SwitchStatus(boolean filament1SwitchStatus)
    {
        this.filament1SwitchStatus = filament1SwitchStatus;
    }

    public void setFilament2SwitchStatus(boolean filament2SwitchStatus)
    {
        this.filament2SwitchStatus = filament2SwitchStatus;
    }

    public void setNozzleSwitchStatus(boolean nozzleSwitchStatus)
    {
        this.nozzleSwitchStatus = nozzleSwitchStatus;
    }

    public void setLidSwitchStatus(boolean lidSwitchStatus)
    {
        this.lidSwitchStatus = lidSwitchStatus;
    }

    public void setReelButtonStatus(boolean reelButtonStatus)
    {
        this.reelButtonStatus = reelButtonStatus;
    }

    public void setEIndexStatus(boolean EIndexStatus)
    {
        this.EIndexStatus = EIndexStatus;
    }

    public void setDIndexStatus(boolean DIndexStatus)
    {
        this.DIndexStatus = DIndexStatus;
    }

    public void setTopZSwitchStatus(boolean topZSwitchStatus)
    {
        this.topZSwitchStatus = topZSwitchStatus;
    }

    public void setExtruderEPresent(boolean value)
    {
        this.extruderEPresent = value;
    }

    public void setExtruderDPresent(boolean value)
    {
        this.extruderDPresent = value;
    }

    public void setNozzle0HeaterMode(HeaterMode nozzleHeaterMode)
    {
        this.nozzle0HeaterMode = nozzleHeaterMode;
    }

    public void setNozzle0HeaterModeString(String nozzleHeaterModeString)
    {
        this.nozzle0HeaterModeString = nozzleHeaterModeString;
    }

    public void setNozzle0TemperatureString(String nozzleTemperatureString)
    {
        this.nozzle0TemperatureString = nozzleTemperatureString;
    }

    public void setNozzle0Temperature(int nozzleTemperature)
    {
        this.nozzle0Temperature = nozzleTemperature;
    }

    public void setNozzle0TargetTemperatureString(String nozzleTargetTemperatureString)
    {
        this.nozzle0TargetTemperatureString = nozzleTargetTemperatureString;
    }

    public void setNozzle0TargetTemperature(int nozzleTargetTemperature)
    {
        this.nozzle0TargetTemperature = nozzleTargetTemperature;
    }

    public void setNozzle0FirstLayerTargetTemperatureString(String nozzleFirstLayerTargetTemperatureString)
    {
        this.nozzle0FirstLayerTargetTemperatureString = nozzleFirstLayerTargetTemperatureString;
    }

    public void setNozzle0FirstLayerTargetTemperature(int nozzleFirstLayerTargetTemperature)
    {
        this.nozzle0FirstLayerTargetTemperature = nozzleFirstLayerTargetTemperature;
    }

    public void setBedHeaterMode(HeaterMode bedHeaterMode)
    {
        this.bedHeaterMode = bedHeaterMode;
    }

    public void setBedHeaterModeString(String bedHeaterModeString)
    {
        this.bedHeaterModeString = bedHeaterModeString;
    }

    public void setBedTemperatureString(String bedTemperatureString)
    {
        this.bedTemperatureString = bedTemperatureString;
    }

    public void setBedTemperature(int bedTemperature)
    {
        this.bedTemperature = bedTemperature;
    }

    public void setBedTargetTemperatureString(String bedTargetTemperatureString)
    {
        this.bedTargetTemperatureString = bedTargetTemperatureString;
    }

    public void setBedTargetTemperature(int bedTargetTemperature)
    {
        this.bedTargetTemperature = bedTargetTemperature;
    }

    public void setBedFirstLayerTargetTemperatureString(String bedFirstLayerTargetTemperatureString)
    {
        this.bedFirstLayerTargetTemperatureString = bedFirstLayerTargetTemperatureString;
    }

    public void setBedFirstLayerTargetTemperature(int bedFirstLayerTargetTemperature)
    {
        this.bedFirstLayerTargetTemperature = bedFirstLayerTargetTemperature;
    }

    public void setAmbientFanOn(boolean ambientFanOn)
    {
        this.ambientFanOn = ambientFanOn;
    }

    public void setAmbientTemperature(int ambientTemperature)
    {
        this.ambientTemperature = ambientTemperature;
    }

    public void setAmbientTemperatureString(String ambientTemperatureString)
    {
        this.ambientTemperatureString = ambientTemperatureString;
    }

    public void setAmbientTargetTemperatureString(String ambientTargetTemperatureString)
    {
        this.ambientTargetTemperatureString = ambientTargetTemperatureString;
    }

    public void setAmbientTargetTemperature(int ambientTargetTemperature)
    {
        this.ambientTargetTemperature = ambientTargetTemperature;
    }

    public void setHeadFanOn(boolean headFanOn)
    {
        this.headFanOn = headFanOn;
    }

    public void setHeadEEPROMState(EEPROMState headEEPROMState)
    {
        this.headEEPROMState = headEEPROMState;
    }

    public void setReel0EEPROMState(EEPROMState reelEEPROMState)
    {
        this.reel0EEPROMState = reelEEPROMState;
    }

    public void setReel1EEPROMState(EEPROMState reelEEPROMState)
    {
        this.reel1EEPROMState = reelEEPROMState;
    }

    public void setDualReelAdaptorPresent(boolean value)
    {
        this.dualReelAdaptorPresent = value;
    }

    public void setSdCardPresent(boolean sdCardPresent)
    {
        this.sdCardPresent = sdCardPresent;
    }

    public void setHeadXPosition(float headXPosition)
    {
        this.headXPosition = headXPosition;
    }

    public void setHeadYPosition(float headYPosition)
    {
        this.headYPosition = headYPosition;
    }

    public void setHeadZPosition(float headZPosition)
    {
        this.headZPosition = headZPosition;
    }

    public void setBPosition(float BPosition)
    {
        this.BPosition = BPosition;
    }

    public void setFilamentDiameter(float filamentDiameter)
    {
        this.EFilamentDiameter = filamentDiameter;
    }

    public void setFilamentMultiplier(float filamentMultiplier)
    {
        this.EFilamentMultiplier = filamentMultiplier;
    }

    public void setFeedRateMultiplier(float feedRateMultiplier)
    {
        this.feedRateMultiplier = feedRateMultiplier;
    }

    public void setWhyAreWeWaitingState(WhyAreWeWaitingState whyAreWeWaitingState)
    {
        this.whyAreWeWaitingState = whyAreWeWaitingState;
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

        //iiiiiiiiiiiiiiii llllllll p b x y z e d b g h i j a 
        //k mmmmmmmm nnnnnnnn cccccccc l rrrrrrrr uuuuuuuu dddddddd
        //o pppppppp qqqqqqqq aaaaaaaa r ssssssss tttttttt
        //u c v w x s xxxxxxxx yyyyyyyy zzzzzzzz bbbbbbbb eeeeeeee gggggggg hhhhhhhh jjjjjjjj ffffffff
        //        
        //iiiiiiiiiiiiiiii = id of running job
        //llllllll = line # of running job in hex
        //p = pause ('0'->normal, '1'->pause pending, '2'->paused, '3'->resume pending)
        //b = busy
        //x = X switch state
        //y = Y switch state
        //z = Z switch state
        //e = E switch state
        //d = D switch state
        //b = nozzle switch state
        //g = lid switch state
        //h = eject switch state
        //i = E index wheel state
        //j = D index wheel state
        //a = Z top switch state
        //k = nozzle 0 heater mode ('0'->off, '1'->normal, '2'->first layer)
        //mmmmmmmm = nozzle 0 temperature (decimal float format)
        //nnnnnnnn = nozzle 0 target (decimal float format)
        //cccccccc = nozzle 0 first layer target (decimal float format)
        //l = nozzle 1 heater mode ('0'-> off, '1'-<normal, '2'->first layer)
        //rrrrrrrr = nozzle 1 temperature (decimal float format)
        //uuuuuuuu = nozzle 1 target (decimal float format)
        //dddddddd = nozzle 1 first layer target (decimal float format)
        //o = bed heater mode ('0'->off, '1'->normal, '2'->first layer)
        //pppppppp = bed temperature (decimal float format)
        //
        //qqqqqqqq = bed target (decimal float format)
        //aaaaaaaa = bed first layer target (decimal float format)
        //r = ambient controller on
        //ssssssss = ambient temperature (decimal float format)
        //tttttttt = ambient target (decimal float format)
        //u = head fan on
        //c = why are we waiting ('0'->not waiting, '1'->waiting for bed to cool, '2'->waiting for bed to reach target, '3'->waiting for nozzle to reach target
        //v = head EEPROM state ('0'->none, '1'->not valid, '2'->valid)
        //w = reel0 EEPROM state ('0'->none, '1'->not valid, '2'->valid)
        //x = reel1 EEPROM state ('0'->none, '1'->not valid, '2'->valid)
        //s = SD card present
        //xxxxxxxx = X position (decimal float format)
        //yyyyyyyy = Y position (decimal float format)
        //zzzzzzzz = Z position (decimal float format)
        //bbbbbbbb = B position (decimal float format)
        //eeeeeeee = E filament diameter (decimal float format)
        //gggggggg = E filament multiplier (decimal float format)
        //hhhhhhhh = D filament diameter (decimal float format)
        //jjjjjjjj = D filament multiplier (decimal float format)
        //ffffffff = Feed rate multiplier (decimal float format)
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

            this.extruderEPresent = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            this.extruderDPresent = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

            // Nozzle 0
            this.nozzle0HeaterModeString = new String(byteData, byteOffset, 1, charsetToUse);
            byteOffset += 1;
            this.nozzle0HeaterMode = HeaterMode.modeFromValue(Integer.valueOf(nozzle0HeaterModeString, 16));

            this.nozzle0TemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzle0Temperature = decimalFloatFormatter.parse(nozzle0TemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle temperature - " + nozzle0TemperatureString);
            }

            this.nozzle0TargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzle0TargetTemperature = decimalFloatFormatter.parse(nozzle0TargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle target temperature - " + nozzle0TargetTemperatureString);
            }

            this.nozzle0FirstLayerTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzle0FirstLayerTargetTemperature = decimalFloatFormatter.parse(nozzle0FirstLayerTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle first layer target temperature - " + nozzle0FirstLayerTargetTemperatureString);
            }

            // Nozzle 1
            this.nozzle1HeaterModeString = new String(byteData, byteOffset, 1, charsetToUse);
            byteOffset += 1;
            this.nozzle1HeaterMode = HeaterMode.modeFromValue(Integer.valueOf(nozzle1HeaterModeString, 16));

            this.nozzle1TemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzle1Temperature = decimalFloatFormatter.parse(nozzle1TemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle temperature - " + nozzle1TemperatureString);
            }

            this.nozzle1TargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzle1TargetTemperature = decimalFloatFormatter.parse(nozzle1TargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle target temperature - " + nozzle1TargetTemperatureString);
            }

            this.nozzle1FirstLayerTargetTemperatureString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                this.nozzle1FirstLayerTargetTemperature = decimalFloatFormatter.parse(nozzle1FirstLayerTargetTemperatureString).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle first layer target temperature - " + nozzle1FirstLayerTargetTemperatureString);
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

            String reel0EEPROMStateString = new String(byteData, byteOffset, 1, charsetToUse);
            byteOffset += 1;
            this.reel0EEPROMState = EEPROMState.modeFromValue(Integer.valueOf(reel0EEPROMStateString, 16));

            String reel1EEPROMStateString = new String(byteData, byteOffset, 1, charsetToUse);
            byteOffset += 1;
            this.reel1EEPROMState = EEPROMState.modeFromValue(Integer.valueOf(reel1EEPROMStateString, 16));

            this.dualReelAdaptorPresent = (byteData[byteOffset] & 1) > 0 ? true : false;
            byteOffset += 1;

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

            // E Filament
            String filamentDiameterString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.EFilamentDiameter = decimalFloatFormatter.parse(filamentDiameterString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse filament diameter - " + filamentDiameterString);
            }

            String filamentMultiplierString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.EFilamentMultiplier = decimalFloatFormatter.parse(filamentMultiplierString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse filament multiplier - " + filamentMultiplierString);
            }

            // D Filament
            String DfilamentDiameterString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.DFilamentDiameter = decimalFloatFormatter.parse(DfilamentDiameterString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse filament diameter - " + DfilamentDiameterString);
            }

            String DfilamentMultiplierString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;
            try
            {
                this.DFilamentMultiplier = decimalFloatFormatter.parse(DfilamentMultiplierString).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse filament multiplier - " + DfilamentMultiplierString);
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
        outputString.append("Extruder E present: " + isExtruderEPresent());
        outputString.append("\n");
        outputString.append("Extruder D present: " + isExtruderDPresent());
        outputString.append("\n");
        outputString.append("Extruder heater on: " + getNozzle0HeaterMode());
        outputString.append("\n");
        outputString.append("Extruder temperature: " + getNozzle0Temperature());
        outputString.append("\n");
        outputString.append("Extruder target temperature: " + getNozzle0TargetTemperature());
        outputString.append("\n");
        outputString.append("Extruder first layer target temperature: " + getNozzle0FirstLayerTargetTemperature());
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
        outputString.append("Reel 0 EEPROM present: " + getReelEEPROMState(0));
        outputString.append("\n");
        outputString.append("Reel 1 EEPROM present: " + getReelEEPROMState(1));
        outputString.append("\n");
        outputString.append("Dual reel adaptor present: " + isDualReelAdaptorPresent());
        outputString.append("\n");
        outputString.append("SD card present: " + isSDCardPresent());
        outputString.append("\n");
        outputString.append(">>>>>>>>>>\n");

        return outputString.toString();
    }
}
