package celtech.printerControl.comms.commands.rx;

import celtech.configuration.MaterialType;
import static celtech.printerControl.comms.commands.ColourStringConverter.stringToColor;
import celtech.printerControl.comms.commands.EnumStringConverter;
import celtech.printerControl.comms.commands.StringToBase64Encoder;
import static celtech.printerControl.comms.commands.tx.WriteReel0EEPROM.FRIENDLY_NAME_LENGTH;
import static celtech.printerControl.comms.commands.tx.WriteReel0EEPROM.MATERIAL_TYPE_LENGTH;
import static celtech.printerControl.comms.commands.tx.WriteReel0EEPROM.REEL_EEPROM_PADDING_LENGTH;
import celtech.printerControl.model.Reel;
import celtech.utils.FixedDecimalFloatFormat;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import javafx.scene.paint.Color;

/**
 *
 * @author ianhudson
 */
public class ReelEEPROMDataResponse extends RoboxRxPacket
{

    private final String charsetToUse = "US-ASCII";

    private final int decimalFloatFormatBytes = 8;
    private final int materialTypeCodeBytes = 16;
    private final int uniqueIDBytes = 24;
    private final int colourBytes = 6;

    private String reelFilamentID;
    private int reelFirstLayerNozzleTemperature;
    private int reelNozzleTemperature;
    private int reelFirstLayerBedTemperature;
    private int reelBedTemperature;
    private int reelAmbientTemperature;
    private float reelFilamentDiameter;
    private float reelFilamentMultiplier;
    private float reelFeedRateMultiplier;
    private float reelRemainingFilament;
    private MaterialType reelMaterialType;
    private Color reelDisplayColour;
    private String reelFriendlyName;

    private int reelNumber = 0;

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
    public boolean populatePacket(byte[] byteData, float requiredFirmwareVersion)
    {
        boolean success = false;

        FixedDecimalFloatFormat decimalFloatFormatter = new FixedDecimalFloatFormat();

        try
        {
            int byteOffset = 1;

            reelFilamentID = (new String(byteData, byteOffset, materialTypeCodeBytes, charsetToUse)).trim();
            byteOffset += materialTypeCodeBytes;

            String displayColourString = new String(byteData, byteOffset, colourBytes, charsetToUse);
            reelDisplayColour = stringToColor(displayColourString);

            byteOffset += uniqueIDBytes;

            String firstLayerNozzleTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelFirstLayerNozzleTemperature = decimalFloatFormatter.parse(firstLayerNozzleTempString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse first layer nozzle temperature - " + firstLayerNozzleTempString);
            }

            String printNozzleTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelNozzleTemperature = decimalFloatFormatter.parse(printNozzleTempString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse nozzle temperature - " + printNozzleTempString);
            }

            String firstLayerBedTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelFirstLayerBedTemperature = decimalFloatFormatter.parse(firstLayerBedTempString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse first layer bed temperature - " + firstLayerBedTempString);
            }

            String bedTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelBedTemperature = decimalFloatFormatter.parse(bedTempString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse bed temperature - " + bedTempString);
            }

            String ambientTempString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelAmbientTemperature = decimalFloatFormatter.parse(ambientTempString.trim()).intValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse ambient temperature - " + ambientTempString);
            }

            String filamentDiameterString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelFilamentDiameter = decimalFloatFormatter.parse(filamentDiameterString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse filament diameter - " + filamentDiameterString);
            }

            String filamentMultiplierString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelFilamentMultiplier = decimalFloatFormatter.parse(filamentMultiplierString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse max extrusion rate - " + filamentMultiplierString);
            }

            String feedRateMultiplierString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelFeedRateMultiplier = decimalFloatFormatter.parse(feedRateMultiplierString.trim()).floatValue();
            } catch (ParseException ex)
            {
                steno.error("Couldn't parse extrusion multiplier - " + feedRateMultiplierString);
            }

            String encodedFriendlyName = new String(byteData, byteOffset, FRIENDLY_NAME_LENGTH, charsetToUse);
            reelFriendlyName = StringToBase64Encoder.decode(encodedFriendlyName);
            byteOffset += FRIENDLY_NAME_LENGTH;

            //Handle case where reelFriendlyName has not yet been set on EEPROM
            if (reelFriendlyName.length() == 0)
            {
                reelFriendlyName = reelFilamentID;
            }

            String intMaterialTypeString = new String(byteData, byteOffset, MATERIAL_TYPE_LENGTH, charsetToUse);
            int intMaterialType = EnumStringConverter.stringToInt(intMaterialTypeString);
            try
            {
                reelMaterialType = MaterialType.values()[intMaterialType];
            } catch (ArrayIndexOutOfBoundsException ex)
            {
                steno.error("Couldn't parse material type from reel");
                reelMaterialType = MaterialType.values()[0];
            }
            byteOffset += MATERIAL_TYPE_LENGTH;

            byteOffset += REEL_EEPROM_PADDING_LENGTH;

            String remainingLengthString = new String(byteData, byteOffset, decimalFloatFormatBytes, charsetToUse);
            byteOffset += decimalFloatFormatBytes;

            try
            {
                reelRemainingFilament = decimalFloatFormatter.parse(remainingLengthString.trim()).intValue();
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
    public String getReelFilamentID()
    {
        return reelFilamentID;
    }

    public String getReelFriendlyName()
    {
        return reelFriendlyName;
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

    /**
     * @return the reelMaterialType
     */
    public MaterialType getReelMaterialType()
    {
        return reelMaterialType;
    }

    public Color getReelDisplayColour()
    {
        return reelDisplayColour;
    }

    public void updateContents(Reel attachedReel)
    {
        reelFilamentID = attachedReel.filamentIDProperty().get();
        reelFirstLayerNozzleTemperature = attachedReel.firstLayerNozzleTemperatureProperty().get();
        reelNozzleTemperature = attachedReel.nozzleTemperatureProperty().get();
        reelFirstLayerBedTemperature = attachedReel.firstLayerBedTemperatureProperty().get();
        reelBedTemperature = attachedReel.bedTemperatureProperty().get();
        reelAmbientTemperature = attachedReel.ambientTemperatureProperty().get();
        reelFilamentDiameter = attachedReel.diameterProperty().get();
        reelFilamentMultiplier = attachedReel.filamentMultiplierProperty().get();
        reelFeedRateMultiplier = attachedReel.feedRateMultiplierProperty().get();
        reelRemainingFilament = attachedReel.remainingFilamentProperty().get();
        reelMaterialType = attachedReel.materialProperty().get();
        reelDisplayColour = attachedReel.displayColourProperty().get();
        reelFriendlyName = attachedReel.friendlyFilamentNameProperty().get();
    }

    public void setReelFilamentID(String reelFilamentID)
    {
        this.reelFilamentID = reelFilamentID;
    }

    public void setReelFirstLayerNozzleTemperature(int reelFirstLayerNozzleTemperature)
    {
        this.reelFirstLayerNozzleTemperature = reelFirstLayerNozzleTemperature;
    }

    public void setReelNozzleTemperature(int reelNozzleTemperature)
    {
        this.reelNozzleTemperature = reelNozzleTemperature;
    }

    public void setReelFirstLayerBedTemperature(int reelFirstLayerBedTemperature)
    {
        this.reelFirstLayerBedTemperature = reelFirstLayerBedTemperature;
    }

    public void setReelBedTemperature(int reelBedTemperature)
    {
        this.reelBedTemperature = reelBedTemperature;
    }

    public void setReelAmbientTemperature(int reelAmbientTemperature)
    {
        this.reelAmbientTemperature = reelAmbientTemperature;
    }

    public void setReelFilamentDiameter(float reelFilamentDiameter)
    {
        this.reelFilamentDiameter = reelFilamentDiameter;
    }

    public void setReelFilamentMultiplier(float reelFilamentMultiplier)
    {
        this.reelFilamentMultiplier = reelFilamentMultiplier;
    }

    public void setReelFeedRateMultiplier(float reelFeedRateMultiplier)
    {
        this.reelFeedRateMultiplier = reelFeedRateMultiplier;
    }

    public void setReelRemainingFilament(float reelRemainingFilament)
    {
        this.reelRemainingFilament = reelRemainingFilament;
    }

    public void setReelMaterialType(MaterialType reelMaterialType)
    {
        this.reelMaterialType = reelMaterialType;
    }

    public void setReelDisplayColour(Color reelDisplayColour)
    {
        this.reelDisplayColour = reelDisplayColour;
    }

    public void setReelFriendlyName(String reelFriendlyName)
    {
        this.reelFriendlyName = reelFriendlyName;
    }

    public void setReelNumber(int reelNumber)
    {
        this.reelNumber = reelNumber;
    }

    public int getReelNumber()
    {
        return reelNumber;
    }

    @Override
    public int packetLength(float requiredFirmwareVersion)
    {
        return 193;
    }
}
