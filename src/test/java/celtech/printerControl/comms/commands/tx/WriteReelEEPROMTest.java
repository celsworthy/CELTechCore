/*
 * Copyright 2014 CEL UK
 */

package celtech.printerControl.comms.commands.tx;

import celtech.configuration.MaterialType;
import javafx.scene.paint.Color;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tony
 */
public class WriteReelEEPROMTest
{

    @Test
    public void testPopulateEEPROMEnglishName()
    {
        String filamentID = "ABCDEF";
        float reelFirstLayerNozzleTemperature = 11;
        float reelNozzleTemperature = 22;
        float reelFirstLayerBedTemperature = 33;
        float reelBedTemperature = 44;
        float reelAmbientTemperature = 55;
        float reelFilamentDiameter = 66;
        float reelFilamentMultiplier = 77;
        float reelFeedRateMultiplier = 88;
        float reelRemainingFilament = 99;
        String friendlyName = "NAME1";
        MaterialType materialType = MaterialType.ABS;
        Color displayColour = Color.RED;
        WriteReel0EEPROM instance = new WriteReel0EEPROM();
        instance.populateEEPROM(filamentID, reelFirstLayerNozzleTemperature, reelNozzleTemperature,
                                reelFirstLayerBedTemperature, reelBedTemperature,
                                reelAmbientTemperature, reelFilamentDiameter, reelFilamentMultiplier,
                                reelFeedRateMultiplier, reelRemainingFilament, friendlyName,
                                materialType, displayColour);
        String bufferString = instance.messagePayload;
        assertEquals(192, bufferString.length());
        System.out.println(bufferString);
        assertEquals("ABCDEF          FF0000                        11      22      33      44      55      66      77      88TkFNRTE=                      A                                                       99", bufferString);
    }
    
    @Test
    public void testPopulateEEPROMArabicName()
    {
        String filamentID = "ABCABC";
        float reelFirstLayerNozzleTemperature = 11;
        float reelNozzleTemperature = 22;
        float reelFirstLayerBedTemperature = 33;
        float reelBedTemperature = 44;
        float reelAmbientTemperature = 55;
        float reelFilamentDiameter = 66;
        float reelFilamentMultiplier = 77;
        float reelFeedRateMultiplier = 88;
        float reelRemainingFilament = 99;
        String friendlyName = "سلام";
        MaterialType materialType = MaterialType.Nylon;
        Color displayColour = Color.BLUE;
        WriteReel0EEPROM instance = new WriteReel0EEPROM();
        instance.populateEEPROM(filamentID, reelFirstLayerNozzleTemperature, reelNozzleTemperature,
                                reelFirstLayerBedTemperature, reelBedTemperature,
                                reelAmbientTemperature, reelFilamentDiameter, reelFilamentMultiplier,
                                reelFeedRateMultiplier, reelRemainingFilament, friendlyName,
                                materialType, displayColour);
        String bufferString = instance.messagePayload;
        assertEquals(192, bufferString.length());
        System.out.println(bufferString);
        assertEquals("ABCABC          0000FF                        11      22      33      44      55      66      77      882LPZhNin2YU=                  C                                                       99", bufferString);
    }    

  

    
}
