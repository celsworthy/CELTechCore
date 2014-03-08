/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */






package celtech.configuration;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

/**
 *
 * @author ianhudson
 */
public class Filament
{

    private boolean mutable = false;
    private final StringProperty fileName = new SimpleStringProperty("");
    private final StringProperty reelTypeCode = new SimpleStringProperty("");
    private final StringProperty friendlyFilamentName = new SimpleStringProperty("");
    private final StringProperty material = new SimpleStringProperty("");
    private final StringProperty uniqueID = new SimpleStringProperty("");

    private final FloatProperty diameter = new SimpleFloatProperty(0);
    private final FloatProperty maxExtrusionRate = new SimpleFloatProperty(0);
    private final FloatProperty extrusionMultiplier = new SimpleFloatProperty(0);
    private final IntegerProperty requiredAmbientTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredFirstLayerBedTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredBedTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredFirstLayerNozzleTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredNozzleTemperature = new SimpleIntegerProperty(0);
    private ObjectProperty<Color> displayColour = new SimpleObjectProperty<>();
    private final FloatProperty remainingFilament = new SimpleFloatProperty(0);

    public Filament(String fileName,
            String reelTypeCode,
            String friendlyFilamentName,
            String material,
            float diameter,
            float maxExtrusionRate,
            float extrusionMultiplier,
            int requiredAmbientTemperature,
            int requiredFirstLayerBedTemperature,
            int requiredBedTemperature,
            int requiredFirstLayerNozzleTemperature,
            int requiredNozzleTemperature,
            Color displayColour,
            boolean mutable)
    {
        this.fileName.set(fileName);
        this.reelTypeCode.set(reelTypeCode);
        this.friendlyFilamentName.set(friendlyFilamentName);
        this.material.set(material);
        this.diameter.set(diameter);
        this.maxExtrusionRate.set(maxExtrusionRate);
        this.extrusionMultiplier.set(extrusionMultiplier);
        this.requiredAmbientTemperature.set(requiredAmbientTemperature);
        this.requiredFirstLayerBedTemperature.set(requiredFirstLayerBedTemperature);
        this.requiredBedTemperature.set(requiredBedTemperature);
        this.requiredFirstLayerNozzleTemperature.set(requiredFirstLayerNozzleTemperature);
        this.requiredNozzleTemperature.set(requiredNozzleTemperature);
        this.displayColour.set(displayColour);
        this.mutable = mutable;
    }

    public StringProperty getFileNameProperty()
    {
        return fileName;
    }

    public String getFileName()
    {
        return fileName.get();
    }

    public StringProperty getReelTypeCodeProperty()
    {
        return reelTypeCode;
    }

    public String getReelTypeCode()
    {
        return reelTypeCode.get();
    }

    public StringProperty getFriendlyFilamentNameProperty()
    {
        return friendlyFilamentName;
    }

    public String getFriendlyFilamentName()
    {
        return friendlyFilamentName.get();
    }

    public StringProperty getUniqueIDProperty()
    {
        return uniqueID;
    }

    public String getUniqueID()
    {
        return friendlyFilamentName.get();
    }

    public StringProperty getMaterialProperty()
    {
        return material;
    }

    public String getMaterial()
    {
        return material.get();
    }

    public FloatProperty getDiameterProperty()
    {
        return diameter;
    }

    public float getDiameter()
    {
        return diameter.get();
    }

    public FloatProperty getMaxExtrusionRateProperty()
    {
        return maxExtrusionRate;
    }

    public float getMaxExtrusionRate()
    {
        return maxExtrusionRate.get();
    }

    public FloatProperty getExtrusionMultiplierProperty()
    {
        return extrusionMultiplier;
    }

    public float getExtrusionMultiplier()
    {
        return extrusionMultiplier.get();
    }

    public IntegerProperty getAmbientTemperatureProperty()
    {
        return requiredAmbientTemperature;
    }

    public int getAmbientTemperature()
    {
        return requiredAmbientTemperature.get();
    }

    public IntegerProperty getFirstLayerBedTemperatureProperty()
    {
        return requiredFirstLayerBedTemperature;
    }

    public int getFirstLayerBedTemperature()
    {
        return requiredFirstLayerBedTemperature.get();
    }

    public IntegerProperty getBedTemperatureProperty()
    {
        return requiredBedTemperature;
    }

    public int getBedTemperature()
    {
        return requiredBedTemperature.get();
    }

    public IntegerProperty getFirstLayerNozzleTemperatureProperty()
    {
        return requiredFirstLayerNozzleTemperature;
    }

    public int getFirstLayerNozzleTemperature()
    {
        return requiredFirstLayerNozzleTemperature.get();
    }

    public IntegerProperty getNozzleTemperatureProperty()
    {
        return requiredNozzleTemperature;
    }

    public int getNozzleTemperature()
    {
        return requiredNozzleTemperature.get();
    }

    public ObjectProperty<Color> getDisplayColourProperty()
    {
        return displayColour;
    }

    public Color getDisplayColour()
    {
        return displayColour.get();
    }

    public FloatProperty getRemainingFilamentProperty()
    {
        return remainingFilament;
    }

    public float getRemainingFilament()
    {
        return remainingFilament.get();
    }

    public void setReelTypeCode(String reelTypeCode)
    {
        this.reelTypeCode.set(reelTypeCode);
    }

    public void setFriendlyColourName(String friendlyColourName)
    {
        this.friendlyFilamentName.set(friendlyColourName);
    }

    public void setUniqueID(String value)
    {
        this.uniqueID.set(value);
    }

    public void setMaterial(String material)
    {
        this.material.set(material);
    }

    public void setDiameter(float diameter)
    {
        this.diameter.set(diameter);
    }

    public void setMaxExtrusionRate(float maxExtrusionRate)
    {
        this.maxExtrusionRate.set(maxExtrusionRate);
    }

    public void setExtrusionMultiplier(float extrusionMultiplier)
    {
        this.extrusionMultiplier.set(extrusionMultiplier);
    }

    public void setRequiredAmbientTemperature(int requiredAmbientTemperature)
    {
        this.requiredAmbientTemperature.set(requiredAmbientTemperature);
    }

    public void setRequiredFirstLayerBedTemperature(int requiredFirstLayerBedTemperature)
    {
        this.requiredFirstLayerBedTemperature.set(requiredFirstLayerBedTemperature);
    }

    public void setBedTemperature(int requiredBedTemperature)
    {
        this.requiredBedTemperature.set(requiredBedTemperature);
    }

    public void setRequiredFirstLayerNozzleTemperature(int requiredFirstLayerNozzleTemperature)
    {
        this.requiredFirstLayerNozzleTemperature.set(requiredFirstLayerNozzleTemperature);
    }

    public void setRequiredNozzleTemperature(int requiredNozzleTemperature)
    {
        this.requiredNozzleTemperature.set(requiredNozzleTemperature);
    }

    public void setDisplayColour(Color displayColour)
    {
        this.displayColour.set(displayColour);
    }

    public void setRemainingFilament(float value)
    {
        this.remainingFilament.set(value);
    }

    public boolean isMutable()
    {
        return mutable;
    }

    @Override
    public String toString()
    {
        return friendlyFilamentName.get() + " " + material.get();
    }
}
