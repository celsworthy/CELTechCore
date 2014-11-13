package celtech.configuration;

import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.appManager.Notifier;
import celtech.coreUI.DisplayManager;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import celtech.utils.SystemUtils;
import java.io.Serializable;
import javafx.application.Platform;
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
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class Filament implements Serializable, Cloneable
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
        Filament.class.getName());

    private final BooleanProperty mutable = new SimpleBooleanProperty(false);
    private final StringProperty friendlyFilamentName = new SimpleStringProperty("");
    private final ObjectProperty<MaterialType> material = new SimpleObjectProperty();
    private final StringProperty filamentID = new SimpleStringProperty();

    private final FloatProperty diameter = new SimpleFloatProperty(0);
    private final FloatProperty filamentMultiplier = new SimpleFloatProperty(0);
    private final FloatProperty feedRateMultiplier = new SimpleFloatProperty(0);
    private final IntegerProperty requiredAmbientTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredFirstLayerBedTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredBedTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredFirstLayerNozzleTemperature = new SimpleIntegerProperty(0);
    private final IntegerProperty requiredNozzleTemperature = new SimpleIntegerProperty(0);
    private ObjectProperty<Color> displayColour = new SimpleObjectProperty<>();
    private final FloatProperty remainingFilament = new SimpleFloatProperty(
        ApplicationConfiguration.mmOfFilamentOnAReel);

    private static final float epsilon = 0.0001f;

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
        this.filamentID.set(reelID);
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

    public Filament(ReelEEPROMDataResponse response)
    {
        this.filamentID.set(response.getReelFilamentID());
        this.friendlyFilamentName.set(response.getReelFriendlyName());
        this.material.set(response.getReelMaterialType());
        this.displayColour.set(response.getReelDisplayColour());
        this.diameter.set(response.getFilamentDiameter());
        this.filamentMultiplier.set(response.getFilamentMultiplier());
        this.feedRateMultiplier.set(response.getFeedRateMultiplier());
        this.requiredAmbientTemperature.set(response.getAmbientTemperature());
        this.requiredFirstLayerBedTemperature.set(response.getFirstLayerBedTemperature());
        this.requiredBedTemperature.set(response.getBedTemperature());
        this.requiredFirstLayerNozzleTemperature.set(response.getFirstLayerNozzleTemperature());
        this.requiredNozzleTemperature.set(response.getNozzleTemperature());
    }

    public Filament(Reel reel)
    {
        this.filamentID.set(reel.filamentIDProperty().get());
        this.friendlyFilamentName.set(reel.friendlyFilamentNameProperty().get());
        this.material.set(reel.materialProperty().get());
        this.displayColour.set(reel.displayColourProperty().get());
        this.diameter.set(reel.diameterProperty().get());
        this.filamentMultiplier.set(reel.filamentMultiplierProperty().get());
        this.feedRateMultiplier.set(reel.feedRateMultiplierProperty().get());
        this.requiredAmbientTemperature.set(reel.ambientTemperatureProperty().get());
        this.requiredFirstLayerBedTemperature.set(reel.firstLayerBedTemperatureProperty().get());
        this.requiredBedTemperature.set(reel.bedTemperatureProperty().get());
        this.requiredFirstLayerNozzleTemperature.set(reel.firstLayerNozzleTemperatureProperty().get());
        this.requiredNozzleTemperature.set(reel.nozzleTemperatureProperty().get());
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
    public String getFilamentID()
    {
        return filamentID.get();
    }

    /**
     *
     * @return
     */
    public StringProperty getFilamentIDProperty()
    {
        return filamentID;
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
    public void setFilamentID(String value)
    {
        this.filamentID.set(value);
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
    public void setFilamentDiameter(float diameter)
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
    public void setAmbientTemperature(int requiredAmbientTemperature)
    {
        this.requiredAmbientTemperature.set(requiredAmbientTemperature);
    }

    /**
     *
     * @param requiredFirstLayerBedTemperature
     */
    public void setFirstLayerBedTemperature(int requiredFirstLayerBedTemperature)
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
    public void setFirstLayerNozzleTemperature(int requiredFirstLayerNozzleTemperature)
    {
        this.requiredFirstLayerNozzleTemperature.set(requiredFirstLayerNozzleTemperature);
    }

    /**
     *
     * @param requiredNozzleTemperature
     */
    public void setNozzleTemperature(int requiredNozzleTemperature)
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
     * Return the friendlyName and if it is a Robox filament then prepend it with Robox®.
     */
    public String getLongFriendlyName()
    {
        if (filamentID.get() != null)
        {
            if (filamentID.get().startsWith("RBX"))
            {
                return "Robox® " + friendlyFilamentName.get();
            }
        }

        return friendlyFilamentName.get();
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return getLongFriendlyName() + " " + material.get();
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
                                      this.getFilamentID(),
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

//    public static void repairFilamentIfNecessary(Printer printer)
//    {
//        try
//        {
//            ReelEEPROMDataResponse response = printer.readReelEEPROM();
//
//            if (ApplicationConfiguration.isAutoRepairReels())
//            {
//                if (response != null)
//                {
//                    String receivedTypeCode = response.getReelFilamentID();
//
//                    // Check that the filament has the right settings 
//                    if (receivedTypeCode != null)
//                    {
//                        boolean needToWriteFilamentData = false;
//
//                        Filament referenceFilament = FilamentContainer.getFilamentByID(
//                            receivedTypeCode);
//
//                        if (referenceFilament != null)
//                        {
//                            Filament filamentToWrite = new Filament(response);
//                            filamentToWrite.setRemainingFilament(response.getReelRemainingFilament());
//
//                            if (Math.abs(response.getAmbientTemperature()
//                                - referenceFilament.getAmbientTemperature()) > epsilon)
//                            {
//                                filamentToWrite.setAmbientTemperature(
//                                    referenceFilament.getAmbientTemperature());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (Math.abs(response.getBedTemperature()
//                                - referenceFilament.getBedTemperature()) > epsilon)
//                            {
//                                filamentToWrite.setBedTemperature(
//                                    referenceFilament.getBedTemperature());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (Math.abs(response.getFeedRateMultiplier()
//                                - referenceFilament.getFeedRateMultiplier()) > epsilon)
//                            {
//                                filamentToWrite.setFeedRateMultiplier(
//                                    referenceFilament.getFeedRateMultiplier());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (Math.abs(response.getFilamentDiameter()
//                                - referenceFilament.getDiameter()) > epsilon)
//                            {
//                                filamentToWrite.setFilamentDiameter(
//                                    referenceFilament.getDiameter());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (Math.abs(response.getFilamentMultiplier()
//                                - referenceFilament.getFilamentMultiplier()) > epsilon)
//                            {
//                                filamentToWrite.setFilamentMultiplier(
//                                    referenceFilament.getFilamentMultiplier());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (Math.abs(response.getFirstLayerBedTemperature()
//                                - referenceFilament.getFirstLayerBedTemperature()) > epsilon)
//                            {
//                                filamentToWrite.setFirstLayerBedTemperature(
//                                    referenceFilament.getFirstLayerBedTemperature());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (Math.abs(response.getFirstLayerNozzleTemperature()
//                                - referenceFilament.getFirstLayerNozzleTemperature()) > epsilon)
//                            {
//                                filamentToWrite.setFirstLayerNozzleTemperature(
//                                    referenceFilament.getFirstLayerNozzleTemperature());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (Math.abs(response.getNozzleTemperature()
//                                - referenceFilament.getNozzleTemperature()) > epsilon)
//                            {
//                                filamentToWrite.setNozzleTemperature(
//                                    referenceFilament.getNozzleTemperature());
//                                needToWriteFilamentData = true;
//                            }
//                            
//                            if (! response.getReelDisplayColour().equals(
//                                    referenceFilament.getDisplayColour()))
//                            {
//                                filamentToWrite.setDisplayColour(
//                                    referenceFilament.getDisplayColour());
//                                needToWriteFilamentData = true;
//                            }
//                            
//                            if (! response.getReelFriendlyName().equals(
//                                    referenceFilament.getFriendlyFilamentName()))
//                            {
//                                filamentToWrite.setFriendlyFilamentName(
//                                    referenceFilament.getFriendlyFilamentName());
//                                needToWriteFilamentData = true;
//                            }
//                            
//                            if (! response.getReelMaterialType().equals(
//                                    referenceFilament.getMaterial()))
//                            {
//                                filamentToWrite.setMaterial(
//                                    referenceFilament.getMaterial());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelDisplayColour().equals(
//                                referenceFilament.getDisplayColour()))
//                            {
//                                filamentToWrite.setDisplayColour(
//                                    referenceFilament.getDisplayColour());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelFriendlyName().equals(
//                                referenceFilament.getFriendlyFilamentName()))
//                            {
//                                filamentToWrite.setFriendlyFilamentName(
//                                    referenceFilament.getFriendlyFilamentName());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelMaterialType().equals(
//                                referenceFilament.getMaterial()))
//                            {
//                                filamentToWrite.setMaterial(
//                                    referenceFilament.getMaterial());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelDisplayColour().equals(
//                                referenceFilament.getDisplayColour()))
//                            {
//                                filamentToWrite.setDisplayColour(
//                                    referenceFilament.getDisplayColour());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelFriendlyName().equals(
//                                referenceFilament.getFriendlyFilamentName()))
//                            {
//                                filamentToWrite.setFriendlyFilamentName(
//                                    referenceFilament.getFriendlyFilamentName());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelMaterialType().equals(
//                                referenceFilament.getMaterial()))
//                            {
//                                filamentToWrite.setMaterial(
//                                    referenceFilament.getMaterial());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelDisplayColour().equals(
//                                referenceFilament.getDisplayColour()))
//                            {
//                                filamentToWrite.setDisplayColour(
//                                    referenceFilament.getDisplayColour());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelFriendlyName().equals(
//                                referenceFilament.getFriendlyFilamentName()))
//                            {
//                                filamentToWrite.setFriendlyFilamentName(
//                                    referenceFilament.getFriendlyFilamentName());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelMaterialType().equals(
//                                referenceFilament.getMaterial()))
//                            {
//                                filamentToWrite.setMaterial(
//                                    referenceFilament.getMaterial());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelDisplayColour().equals(
//                                referenceFilament.getDisplayColour()))
//                            {
//                                filamentToWrite.setDisplayColour(
//                                    referenceFilament.getDisplayColour());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelFriendlyName().equals(
//                                referenceFilament.getFriendlyFilamentName()))
//                            {
//                                filamentToWrite.setFriendlyFilamentName(
//                                    referenceFilament.getFriendlyFilamentName());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (!response.getReelMaterialType().equals(
//                                referenceFilament.getMaterial()))
//                            {
//                                filamentToWrite.setMaterial(
//                                    referenceFilament.getMaterial());
//                                needToWriteFilamentData = true;
//                            }
//
//                            if (needToWriteFilamentData)
//                            {
//                                printer.transmitWriteReelEEPROM(filamentToWrite);
//                                printer.readReelEEPROM();
//                                steno.info("Automatically updated filament data for "
//                                    + receivedTypeCode);
//
//                                Platform.runLater(new Runnable()
//                                {
//                                    @Override
//                                    public void run()
//                                    {
//                                        Notifier.showInformationNotification(
//                                            DisplayManager.getLanguageBundle().getString(
//                                                "notification.reelDataUpdatedTitle"),
//                                            DisplayManager.getLanguageBundle().getString(
//                                                "notification.noActionRequired"));
//                                    }
//                                });
//
//                            }
//                        } else
//                        {
//                            steno.error("Checking filament - got a filament I didn't recognise");
//                        }
//                    }
//                }
//            }
//        } catch (RoboxCommsException ex)
//        {
//            steno.error("Error from triggered read of Reel EEPROM");
//        }
//    }
}
