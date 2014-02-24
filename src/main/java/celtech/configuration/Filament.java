/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.configuration;

import javafx.scene.paint.Color;

/**
 *
 * @author ianhudson
 */
public class Filament
{
    private String reelTypeCode;
    private String friendlyColourName;
    private String material;
    private float diameter;
    private float maxExtrusionRate;
    private float extrusionMultiplier;
    private int requiredAmbientTemperature;
    private int requiredFirstLayerBedTemperature;
    private int requiredBedTemperature;
    private int requiredFirstLayerNozzleTemperature;
    private int requiredNozzleTemperature;
    private Color displayColour;
    
    public Filament(String reelTypeCode, String friendlyColourName, String material, float diameter, float maxExtrusionRate, float extrusionMultiplier, int requiredAmbientTemperature, int requiredFirstLayerBedTemperature, int requiredBedTemperature, int requiredFirstLayerNozzleTemperature, int requiredNozzleTemperature, Color displayColour)
    {
        this.reelTypeCode = reelTypeCode;
        this.friendlyColourName = friendlyColourName;
        this.material = material;
        this.diameter = diameter;
        this.maxExtrusionRate = maxExtrusionRate;
        this.extrusionMultiplier = extrusionMultiplier;
        this.requiredAmbientTemperature = requiredAmbientTemperature;
        this.requiredFirstLayerBedTemperature = requiredFirstLayerBedTemperature;
        this.requiredBedTemperature = requiredBedTemperature;
        this.requiredFirstLayerNozzleTemperature = requiredFirstLayerNozzleTemperature;
        this.requiredNozzleTemperature = requiredNozzleTemperature;
        this.displayColour = displayColour;
    }

    public String getReelTypeCode()
    {
        return reelTypeCode;
    }

    public String getFriendlyColourName()
    {
        return friendlyColourName;
    }

    public String getMaterial()
    {
        return material;
    }

    public float getDiameter()
    {
        return diameter;
    }

    public float getMaxExtrusionRate()
    {
        return maxExtrusionRate;
    }

    public float getExtrusionMultiplier()
    {
        return extrusionMultiplier;
    }

    public int getAmbientTemperature()
    {
        return requiredAmbientTemperature;
    }

    public int getFirstLayerBedTemperature()
    {
        return requiredFirstLayerBedTemperature;
    }

    public int getBedTemperature()
    {
        return requiredBedTemperature;
    }

    public int getFirstLayerNozzleTemperature()
    {
        return requiredFirstLayerNozzleTemperature;
    }

    public int getNozzleTemperature()
    {
        return requiredNozzleTemperature;
    }

    public Color getDisplayColour()
    {
        return displayColour;
    }
    
    @Override
    public String toString()
    {
        return friendlyColourName + " " + material;
    }
}
