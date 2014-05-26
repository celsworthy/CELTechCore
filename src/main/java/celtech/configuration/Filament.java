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

    /**
     *
     * @param friendlyFilamentName
     * @param material
     * @param reelID
     * @param diameter
     * @param filamentMultiplier
     * @param feedRateMultiplier
     * @param requiredAmbientTemperature
     * @param requiredFirstLayerBedTemperature
     * @param requiredBedTemperature
     * @param requiredFirstLayerNozzleTemperature
     * @param requiredNozzleTemperature
     * @param displayColour
     * @param mutable
     */
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

    /**
     *
     * @return
     */
    public String getFileName()
    {
        return friendlyFilamentName.get() + "_" + material.toString();
    }

    /**
     *
     * @return
     */
    public StringProperty getFriendlyFilamentNameProperty()
    {
        return friendlyFilamentName;
    }

    /**
     *
     * @return
     */
    public String getFriendlyFilamentName()
    {
        return friendlyFilamentName.get();
    }

    /**
     *
     * @return
     */
    public String getReelID()
    {
        return reelID.get();
    }

    /**
     *
     * @return
     */
    public StringProperty getReelIDProperty()
    {
        return reelID;
    }

    /**
     *
     * @return
     */
    public StringProperty getUniqueIDProperty()
    {
        return uniqueID;
    }

    /**
     *
     * @return
     */
    public String getUniqueID()
    {
        return uniqueID.get();
    }

    /**
     *
     * @return
     */
    public ObjectProperty<MaterialType> getMaterialProperty()
    {
        return material;
    }

    /**
     *
     * @return
     */
    public MaterialType getMaterial()
    {
        return material.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getDiameterProperty()
    {
        return diameter;
    }

    /**
     *
     * @return
     */
    public float getDiameter()
    {
        return diameter.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getFilamentMultiplierProperty()
    {
        return filamentMultiplier;
    }

    /**
     *
     * @return
     */
    public float getFilamentMultiplier()
    {
        return filamentMultiplier.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getFeedRateMultiplierProperty()
    {
        return feedRateMultiplier;
    }

    /**
     *
     * @return
     */
    public float getFeedRateMultiplier()
    {
        return feedRateMultiplier.get();
    }

    /**
     *
     * @return
     */
    public IntegerProperty getAmbientTemperatureProperty()
    {
        return requiredAmbientTemperature;
    }

    /**
     *
     * @return
     */
    public int getAmbientTemperature()
    {
        return requiredAmbientTemperature.get();
    }

    /**
     *
     * @return
     */
    public IntegerProperty getFirstLayerBedTemperatureProperty()
    {
        return requiredFirstLayerBedTemperature;
    }

    /**
     *
     * @return
     */
    public int getFirstLayerBedTemperature()
    {
        return requiredFirstLayerBedTemperature.get();
    }

    /**
     *
     * @return
     */
    public IntegerProperty getBedTemperatureProperty()
    {
        return requiredBedTemperature;
    }

    /**
     *
     * @return
     */
    public int getBedTemperature()
    {
        return requiredBedTemperature.get();
    }

    /**
     *
     * @return
     */
    public IntegerProperty getFirstLayerNozzleTemperatureProperty()
    {
        return requiredFirstLayerNozzleTemperature;
    }

    /**
     *
     * @return
     */
    public int getFirstLayerNozzleTemperature()
    {
        return requiredFirstLayerNozzleTemperature.get();
    }

    /**
     *
     * @return
     */
    public IntegerProperty getNozzleTemperatureProperty()
    {
        return requiredNozzleTemperature;
    }

    /**
     *
     * @return
     */
    public int getNozzleTemperature()
    {
        return requiredNozzleTemperature.get();
    }

    /**
     *
     * @return
     */
    public ObjectProperty<Color> getDisplayColourProperty()
    {
        return displayColour;
    }

    /**
     *
     * @return
     */
    public Color getDisplayColour()
    {
        return displayColour.get();
    }

    /**
     *
     * @return
     */
    public FloatProperty getRemainingFilamentProperty()
    {
        return remainingFilament;
    }

    /**
     *
     * @return
     */
    public float getRemainingFilament()
    {
        return remainingFilament.get();
    }

    /**
     *
     * @param friendlyColourName
     */
    public void setFriendlyFilamentName(String friendlyColourName)
    {
        this.friendlyFilamentName.set(friendlyColourName);
    }
    
    /**
     *
     * @param value
     */
    public void setReelID(String value)
    {
        this.reelID.set(value);
    }

    /**
     *
     * @param value
     */
    public void setUniqueID(String value)
    {
        this.uniqueID.set(value);
    }

    /**
     *
     * @param material
     */
    public void setMaterial(MaterialType material)
    {
        this.material.set(material);
    }

    /**
     *
     * @param diameter
     */
    public void setDiameter(float diameter)
    {
        this.diameter.set(diameter);
    }

    /**
     *
     * @param filamentMultiplier
     */
    public void setFilamentMultiplier(float filamentMultiplier)
    {
        this.filamentMultiplier.set(filamentMultiplier);
    }

    /**
     *
     * @param feedRateMultiplier
     */
    public void setFeedRateMultiplier(float feedRateMultiplier)
    {
        this.feedRateMultiplier.set(feedRateMultiplier);
    }

    /**
     *
     * @param requiredAmbientTemperature
     */
    public void setRequiredAmbientTemperature(int requiredAmbientTemperature)
    {
        this.requiredAmbientTemperature.set(requiredAmbientTemperature);
    }

    /**
     *
     * @param requiredFirstLayerBedTemperature
     */
    public void setRequiredFirstLayerBedTemperature(int requiredFirstLayerBedTemperature)
    {
        this.requiredFirstLayerBedTemperature.set(requiredFirstLayerBedTemperature);
    }

    /**
     *
     * @param requiredBedTemperature
     */
    public void setBedTemperature(int requiredBedTemperature)
    {
        this.requiredBedTemperature.set(requiredBedTemperature);
    }

    /**
     *
     * @param requiredFirstLayerNozzleTemperature
     */
    public void setRequiredFirstLayerNozzleTemperature(int requiredFirstLayerNozzleTemperature)
    {
        this.requiredFirstLayerNozzleTemperature.set(requiredFirstLayerNozzleTemperature);
    }

    /**
     *
     * @param requiredNozzleTemperature
     */
    public void setRequiredNozzleTemperature(int requiredNozzleTemperature)
    {
        this.requiredNozzleTemperature.set(requiredNozzleTemperature);
    }

    /**
     *
     * @param displayColour
     */
    public void setDisplayColour(Color displayColour)
    {
        this.displayColour.set(displayColour);
    }

    /**
     *
     * @param value
     */
    public void setRemainingFilament(float value)
    {
        this.remainingFilament.set(value);
    }

    /**
     *
     * @return
     */
    public boolean isMutable()
    {
        return mutable.get();
    }

    /**
     *
     * @param value
     */
    public void setMutable(boolean value)
    {
        this.mutable.set(value);
    }

    /**
     *
     * @return
     */
    public BooleanProperty getMutableProperty()
    {
        return mutable;
    }

    /**
     *
     * @return
     */
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

    /**
     *
     * @return
     */
    public static String generateUserReelID()
    {
        String fullID = SystemUtils.generate16DigitID();
        StringBuilder id = new StringBuilder();
        id.append('U');
        id.append(fullID.substring(1, fullID.length() - 1));
        return id.toString();
    }

    /**
     *
     * @return
     */
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
