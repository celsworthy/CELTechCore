package celtech.printerControl.model;

import celtech.Lookup;
import celtech.configuration.EEPROMState;
import celtech.configuration.Filament;
import celtech.configuration.MaterialType;
import celtech.coreUI.DisplayManager;
import celtech.printerControl.comms.commands.rx.ReelEEPROMDataResponse;
import celtech.utils.SystemUtils;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

/**
 *
 * @author Ian
 */
public class Reel
{

    protected final ObjectProperty<EEPROMState> reelEEPROMStatusProperty = new SimpleObjectProperty<>(EEPROMState.NOT_PRESENT);

    protected final StringProperty friendlyFilamentNameProperty = new SimpleStringProperty("");
    protected final ObjectProperty<MaterialType> materialProperty = new SimpleObjectProperty();
    protected final StringProperty filamentIDProperty = new SimpleStringProperty();
    protected final FloatProperty diameterProperty = new SimpleFloatProperty(0);
    protected final FloatProperty filamentMultiplierProperty = new SimpleFloatProperty(0);
    protected final FloatProperty feedRateMultiplierProperty = new SimpleFloatProperty(0);
    protected final IntegerProperty ambientTemperatureProperty = new SimpleIntegerProperty(0);
    protected final IntegerProperty firstLayerBedTemperatureProperty = new SimpleIntegerProperty(0);
    protected final IntegerProperty bedTemperatureProperty = new SimpleIntegerProperty(0);
    protected final IntegerProperty firstLayerNozzleTemperatureProperty = new SimpleIntegerProperty(0);
    protected final IntegerProperty nozzleTemperatureProperty = new SimpleIntegerProperty(0);
    protected final ObjectProperty<Color> displayColourProperty = new SimpleObjectProperty<>();
    protected final FloatProperty remainingFilamentProperty = new SimpleFloatProperty(0);

    public ReadOnlyObjectProperty<EEPROMState> getReelEEPROMStatusProperty()
    {
        return reelEEPROMStatusProperty;
    }

    public ReadOnlyStringProperty getFriendlyFilamentNameProperty()
    {
        return friendlyFilamentNameProperty;
    }

    public ReadOnlyObjectProperty<MaterialType> getMaterialProperty()
    {
        return materialProperty;
    }

    public ReadOnlyStringProperty getFilamentIDProperty()
    {
        return filamentIDProperty;
    }

    public ReadOnlyFloatProperty getDiameterProperty()
    {
        return diameterProperty;
    }

    public ReadOnlyFloatProperty getFilamentMultiplierProperty()
    {
        return filamentMultiplierProperty;
    }

    public ReadOnlyFloatProperty getFeedRateMultiplierProperty()
    {
        return feedRateMultiplierProperty;
    }

    public ReadOnlyIntegerProperty getAmbientTemperatureProperty()
    {
        return ambientTemperatureProperty;
    }

    public ReadOnlyIntegerProperty getFirstLayerBedTemperatureProperty()
    {
        return firstLayerBedTemperatureProperty;
    }

    public ReadOnlyIntegerProperty getBedTemperatureProperty()
    {
        return bedTemperatureProperty;
    }

    public ReadOnlyIntegerProperty getFirstLayerNozzleTemperatureProperty()
    {
        return firstLayerNozzleTemperatureProperty;
    }

    public ReadOnlyIntegerProperty getNozzleTemperatureProperty()
    {
        return nozzleTemperatureProperty;
    }

    public ReadOnlyObjectProperty<Color> getDisplayColourProperty()
    {
        return displayColourProperty;
    }

    public ReadOnlyFloatProperty getRemainingFilamentProperty()
    {
        return remainingFilamentProperty;
    }

    public void updateFromEEPROMData(ReelEEPROMDataResponse eepromData)
    {
        ambientTemperatureProperty.set(eepromData.getAmbientTemperature());
        bedTemperatureProperty.set(eepromData.getBedTemperature());
        feedRateMultiplierProperty.set(eepromData.getFeedRateMultiplier());
        diameterProperty.set(eepromData.getFilamentDiameter());
        filamentMultiplierProperty.set(eepromData.getFilamentMultiplier());
        firstLayerBedTemperatureProperty.set(eepromData.getFirstLayerBedTemperature());
        firstLayerNozzleTemperatureProperty.set(eepromData.getFirstLayerNozzleTemperature());
        nozzleTemperatureProperty.set(eepromData.getNozzleTemperature());
        displayColourProperty.set(eepromData.getReelDisplayColour());
        filamentIDProperty.set(eepromData.getReelFilamentID());
        friendlyFilamentNameProperty.set(eepromData.getReelFriendlyName());
        materialProperty.set(eepromData.getReelMaterialType());
        remainingFilamentProperty.set(eepromData.getReelRemainingFilament());
    }

    public boolean isSameAs(Filament filament)
    {
        boolean same = false;

        if (filament.getAmbientTemperatureProperty().get() == ambientTemperatureProperty.intValue()
            && filament.getBedTemperatureProperty().get() == bedTemperatureProperty.intValue()
            && filament.getDiameterProperty().get() == diameterProperty.get()
            && filament.getDisplayColourProperty().get().equals(displayColourProperty.get())
            && SystemUtils.isDoubleSame(filament.getFeedRateMultiplierProperty().get(), feedRateMultiplierProperty.get())
            && filament.getFilamentIDProperty().get().equals(filamentIDProperty.get())
            && SystemUtils.isDoubleSame(filament.getFilamentMultiplierProperty().get(), filamentMultiplierProperty.get())
            && SystemUtils.isDoubleSame(filament.getFirstLayerBedTemperatureProperty().get(), firstLayerBedTemperatureProperty.get())
            && SystemUtils.isDoubleSame(filament.getFirstLayerNozzleTemperatureProperty().get(), firstLayerNozzleTemperatureProperty.get())
            && filament.getFriendlyFilamentNameProperty().get().equals(friendlyFilamentNameProperty.get())
            && filament.getMaterialProperty().get() == materialProperty.get()
            && filament.getNozzleTemperatureProperty().intValue() == nozzleTemperatureProperty.get()
            && SystemUtils.isDoubleSame(filament.getRemainingFilamentProperty().get(), remainingFilamentProperty.get()))
        {
            same = true;
        }

        return same;
    }

    protected void noReelLoaded()
    {
        ambientTemperatureProperty.set(0);
        bedTemperatureProperty.set(0);
        feedRateMultiplierProperty.set(0);
        diameterProperty.set(0);
        filamentMultiplierProperty.set(0);
        firstLayerBedTemperatureProperty.set(0);
        firstLayerNozzleTemperatureProperty.set(0);
        nozzleTemperatureProperty.set(0);
        displayColourProperty.set(Color.ALICEBLUE);
        filamentIDProperty.set("");
        friendlyFilamentNameProperty.set(Lookup.i18n("smartReelProgrammer.noReelLoaded"));
        materialProperty.set(MaterialType.ABS);
        remainingFilamentProperty.set(0);
    }
}
