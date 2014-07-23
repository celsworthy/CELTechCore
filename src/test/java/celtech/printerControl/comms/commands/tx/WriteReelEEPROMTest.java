/*
 * Copyright 2014 CEL UK
 */

package celtech.printerControl.comms.commands.tx;

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
        System.out.println("populateEEPROM");
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
        String materialName = "PLZ";
        int displayColourHashCode = 0x123456;
        WriteReelEEPROM instance = new WriteReelEEPROM();
        instance.populateEEPROM(filamentID, reelFirstLayerNozzleTemperature, reelNozzleTemperature,
                                reelFirstLayerBedTemperature, reelBedTemperature,
                                reelAmbientTemperature, reelFilamentDiameter, reelFilamentMultiplier,
                                reelFeedRateMultiplier, reelRemainingFilament, friendlyName,
                                materialName, displayColourHashCode);
        String bufferString = instance.messagePayload;
        assertEquals(192, bufferString.length());
        assertEquals("ABCDEF                                        11      22      33      44      55      66      77      88TkFNRTE=                                                                              99", bufferString);
    }
    
    @Test
    public void testPopulateEEPROMArabicName()
    {
        System.out.println("populateEEPROM");
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
        String friendlyName = "سلام";
        String materialName = "ABX";
        int displayColourHashCode = 0x123456;
        WriteReelEEPROM instance = new WriteReelEEPROM();
        instance.populateEEPROM(filamentID, reelFirstLayerNozzleTemperature, reelNozzleTemperature,
                                reelFirstLayerBedTemperature, reelBedTemperature,
                                reelAmbientTemperature, reelFilamentDiameter, reelFilamentMultiplier,
                                reelFeedRateMultiplier, reelRemainingFilament, friendlyName,
                                materialName, displayColourHashCode);
        String bufferString = instance.messagePayload;
        assertEquals(192, bufferString.length());
        assertEquals("ABCDEF                                        11      22      33      44      55      66      77      882LPZhNin2YU=                                                                          99", bufferString);
    }    

  

    
}
