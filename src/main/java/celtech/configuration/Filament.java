package celtech.configuration;

import celtech.utils.SystemUtils;
import java.io.Serializable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
public class Filament implements Serializable, Cloneable
{

    private final BooleanProperty mutable = new SimpleBooleanProperty(false);
    private final StringProperty friendlyFilamentName = new SimpleStringProperty("");
    private final ObjectProperty<MaterialType> material = new SimpleObjectProperty();
    private final StringProperty reelID = new SimpleStringProperty();
    private final StringProperty uniqueID = new SimpleStringProperty("");

    private final FloatProperty diameter = new SimpleFloatProperty(0);
    private final FloatProperty filamentMultiplier = new SimpleFloatProperty(0);
    private final FloatProperty feedRateMultiplier = new SimpleFloatProperty(0);
    private final IntegerProperty requiredAmbientTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredFirstLayerBedTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredBedTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredFirstLayerNozzleTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredNozzleTemperature = new SimpleIntegerProperty(0);
    private ObjectProperty<Color> displayColour = new SimpleObjectProperty<>();
    private final FloatProperty remainingFilament = new SimpleFloatProperty(ApplicationConfiguration.mmOfFilamentOnAReel);

    public Filament(
            String friendlyFilamentName,
            MaterialType material,
            String reelID,
            float diameter,
            float filamentMultiplier,
            float feedRateMultiplier,
            int requiredAmbientTemperature,
            int requiredFirstLayerBedTemperature,
            int requiredBedTemperature,
            int requiredFirstLayerNozzleTemperature,
            int requiredNozzleTemperature,
            Color displayColour,
            boolean mutable)
    {
        this.friendlyFilamentName.set(friendlyFilamentName);
        this.material.set(material);
        this.reelID.set(reelID);
        this.diameter.set(diameter);
        this.filamentMultiplier.set(filamentMultiplier);
        this.feedRateMultiplier.set(feedRateMultiplier);
        this.requiredAmbientTemperature.set(requiredAmbientTemperature);
        this.requiredFirstLayerBedTemperature.set(requiredFirstLayerBedTemperature);
        this.requiredBedTemperature.set(requiredBedTemperature);
        this.requiredFirstLayerNozzleTemperature.set(requiredFirstLayerNozzleTemperature);
        this.requiredNozzleTemperature.set(requiredNozzleTemperature);
        this.displayColour.set(displayColour);
        this.mutable.set(mutable);
    }

    public String getFileName()
    {
        return friendlyFilamentName.get() + "_" + material.toString();
    }

    public StringProperty getFriendlyFilamentNameProperty()
    {
        return friendlyFilamentName;
    }

    public String getFriendlyFilamentName()
    {
        return friendlyFilamentName.get();
    }

    public String getReelID()
    {
        return reelID.get();
    }

    public StringProperty getReelIDProperty()
    {
        return reelID;
    }

    public StringProperty getUniqueIDProperty()
    {
        return uniqueID;
    }

    public String getUniqueID()
    {
        return uniqueID.get();
    }

    public ObjectProperty<MaterialType> getMaterialProperty()
    {
        return material;
    }

    public MaterialType getMaterial()
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

    public FloatProperty getFilamentMultiplierProperty()
    {
        return filamentMultiplier;
    }

    public float getFilamentMultiplier()
    {
        return filamentMultiplier.get();
    }

    public FloatProperty getFeedRateMultiplierProperty()
    {
        return feedRateMultiplier;
    }

    public float getFeedRateMultiplier()
    {
        return feedRateMultiplier.get();
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

    public void setFriendlyFilamentName(String friendlyColourName)
    {
        this.friendlyFilamentName.set(friendlyColourName);
    }
    
    public void setReelID(String value)
    {
        this.reelID.set(value);
    }

    public void setUniqueID(String value)
    {
        this.uniqueID.set(value);
    }

    public void setMaterial(MaterialType material)
    {
        this.material.set(material);
    }

    public void setDiameter(float diameter)
    {
        this.diameter.set(diameter);
    }

    public void setFilamentMultiplier(float filamentMultiplier)
    {
        this.filamentMultiplier.set(filamentMultiplier);
    }

    public void setFeedRateMultiplier(float feedRateMultiplier)
    {
        this.feedRateMultiplier.set(feedRateMultiplier);
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
        return mutable.get();
    }

    public void setMutable(boolean value)
    {
        this.mutable.set(value);
    }

    public BooleanProperty getMutableProperty()
    {
        return mutable;
    }

    @Override
    public String toString()
    {
        String prefix = null;
        String displayName = friendlyFilamentName.get() + " " + material.get();

        if (reelID.get() != null)
        {
            if (reelID.get().startsWith("RBX"))
            {
                prefix = "Robox® ";
            }
        }

        if (prefix == null)
        {
            return displayName;
        } else
        {
            return prefix + displayName;
        }
    }

    public static String generateUserReelID()
    {
        String fullID = SystemUtils.generate16DigitID();
        StringBuilder id = new StringBuilder();
        id.append('U');
        id.append(fullID.substring(1, fullID.length() - 1));
        return id.toString();
    }

    @Override
    public Filament clone()
    {
        Filament clone = new Filament(this.getFriendlyFilamentName(),
                this.getMaterial(),
                this.getReelID(),
                this.getDiameter(),
                this.getFilamentMultiplier(),
                this.getFeedRateMultiplier(),
                this.getAmbientTemperature(),
                this.getFirstLayerBedTemperature(),
                this.getBedTemperature(),
                this.getFirstLayerNozzleTemperature(),
                this.getNozzleTemperature(),
                this.getDisplayColour(),
                this.isMutable()
        );

        return clone;
    }
}
