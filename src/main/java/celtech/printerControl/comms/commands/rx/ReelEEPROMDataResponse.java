package celtech.printerControl.comms.commands.rx;

import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 *
 * @author ianhudson
 */
public class ReelEEPROMDataResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";
    private NumberFormat numberFormatter = NumberFormat.getNumberInstance();

    private final int decimalFloatFormatBytes = 8;
    private final int materialTypeCodeBytes = 16;
    private final int uniqueIDBytes = 24;

    private String reelTypeCode;
    private String reelUniqueID;
    private int reelFirstLayerNozzleTemperature;
    private int reelNozzleTemperature;
    private int reelFirstLayerBedTemperature;
    private int reelBedTemperature;
    private int reelAmbientTemperature;
    private float reelFilamentDiameter;
    private float reelFilamentMultiplier;
    private float reelFeedRateMultiplier;
    private float reelRemainingFilament;

    /**
     *
     */
    public ReelEEPROMDataResponse()
    {
        super(RxPacketTypeEnum.REEL_EEPROM_DATA, false, false);
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

        try
        {
            int byteOffset = 1;

            reelTypeCode = (new String(byteData, byteOffset, materialTypeCodeBytes, charsetToUse)).trim();
            byteOffset += materialTypeCodeBytes;

            reelUniqueID = (new String(byteData, byteOffset, uniqueIDBytes, charsetToUse)).trim();
            byteOffset += uniqueIDBytes;

            String firstLayerNozzleTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelFirstLayerNozzleTemperature = numberFormatter.parse(firstLayerNozzleTempString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse first layer nozzle temperature - " + firstLayerNozzleTempString);
            }

            String printNozzleTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelNozzleTemperature = numberFormatter.parse(printNozzleTempString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle temperature - " + printNozzleTempString);
            }

            String firstLayerBedTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelFirstLayerBedTemperature = numberFormatter.parse(firstLayerBedTempString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse first layer bed temperature - " + firstLayerBedTempString);
            }

            String bedTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelBedTemperature = numberFormatter.parse(bedTempString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse bed temperature - " + bedTempString);
            }

            String ambientTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelAmbientTemperature = numberFormatter.parse(ambientTempString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse ambient temperature - " + ambientTempString);
            }

            String filamentDiameterString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelFilamentDiameter = numberFormatter.parse(filamentDiameterString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse filament diameter - " + filamentDiameterString);
            }

            String filamentMultiplierString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelFilamentMultiplier = numberFormatter.parse(filamentMultiplierString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse max extrusion rate - " + filamentMultiplierString);
            }

            String feedRateMultiplierString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelFeedRateMultiplier = numberFormatter.parse(feedRateMultiplierString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse extrusion multiplier - " + feedRateMultiplierString);
            }

            byteOffset += 80;

            String remainingLengthString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelRemainingFilament = numberFormatter.parse(remainingLengthString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse remaining length - " + remainingLengthString);
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
    public String getReelTypeCode()
    {
        return reelTypeCode;
    }

    /**
     *
     * @return
     */
    public String getReelUniqueID()
    {
        return reelUniqueID;
    }

    /**
     *
     * @return
     */
    public int getFirstLayerNozzleTemperature()
    {
        return reelFirstLayerNozzleTemperature;
    }

    /**
     *
     * @return
     */
    public int getNozzleTemperature()
    {
        return reelNozzleTemperature;
    }

    /**
     *
     * @return
     */
    public int getFirstLayerBedTemperature()
    {
        return reelFirstLayerBedTemperature;
    }

    /**
     *
     * @return
     */
    public int getBedTemperature()
    {
        return reelBedTemperature;
    }

    /**
     *
     * @return
     */
    public int getAmbientTemperature()
    {
        return reelAmbientTemperature;
    }

    /**
     *
     * @return
     */
    public float getFilamentDiameter()
    {
        return reelFilamentDiameter;
    }

    /**
     *
     * @return
     */
    public float getFilamentMultiplier()
    {
        return reelFilamentMultiplier;
    }

    /**
     *
     * @return
     */
    public float getFeedRateMultiplier()
    {
        return reelFeedRateMultiplier;
    }

    /**
     *
     * @return
     */
    public float getReelRemainingFilament()
    {
        return reelRemainingFilament;
    }

}
