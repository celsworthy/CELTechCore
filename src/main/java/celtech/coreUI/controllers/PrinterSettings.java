package celtech.coreUI.controllers;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * PrinterSettings represents the choices made by the user for a project on the Settings panel. It
 * is serialised with the project.
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterSettings
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PrinterSettings.class.getName());

    private final ObjectProperty<Printer> selectedPrinter = new SimpleObjectProperty<>();
    private final ObjectProperty<Filament> selectedFilament0 = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Filament> selectedFilament1 = new SimpleObjectProperty<>(null);
    private final StringProperty customSettingsName = new SimpleStringProperty();
    private final ObjectProperty<PrintQualityEnumeration> printQuality
        = new SimpleObjectProperty<>(PrintQualityEnumeration.DRAFT);
    private final BooleanProperty dataChanged = new SimpleBooleanProperty(false);

    private int brimOverride = 0;
    private float fillDensityOverride = 0;
    private boolean printSupportOverride = false;
    private boolean raftOverride = false;

    public PrinterSettings()
    {
        customSettingsName.set("");
        SlicerParametersFile draftParametersFile = SlicerParametersContainer.getSettingsByProfileName(
            ApplicationConfiguration.draftSettingsProfileName);
        brimOverride = draftParametersFile.getBrimWidth_mm();
        fillDensityOverride = draftParametersFile.getFillDensity_normalised();
        printSupportOverride = draftParametersFile.getGenerateSupportMaterial();
    }

    private void toggleDataChanged()
    {
        dataChanged.set(dataChanged.not().get());
    }

    public ReadOnlyBooleanProperty getDataChanged()
    {
        return dataChanged;
    }

    public void setSelectedPrinter(Printer value)
    {
        selectedPrinter.set(value);
    }

    public Printer getSelectedPrinter()
    {
        return selectedPrinter.get();
    }

    public ObjectProperty<Printer> selectedPrinterProperty()
    {
        return selectedPrinter;
    }

    public void setFilament0(Filament filament)
    {
        if (selectedFilament0.get() != filament)
        {
            selectedFilament0.set(filament);
            toggleDataChanged();
        }
    }

    public void setFilament1(Filament filament)
    {
        if (selectedFilament1.get() != filament)
        {
            selectedFilament1.set(filament);
            toggleDataChanged();
        }
    }

    public Filament getFilament0()
    {
        return selectedFilament0.get();
    }

    public Filament getFilament1()
    {
        return selectedFilament1.get();
    }

    public ObjectProperty<Filament> getFilament0Property()
    {
        return selectedFilament0;
    }

    public ObjectProperty<Filament> getFilament1Property()
    {
        return selectedFilament1;
    }

    public void setPrintQuality(PrintQualityEnumeration value)
    {
        if (printQuality.get() != value)
        {
            printQuality.set(value);
            toggleDataChanged();
        }
    }

    public PrintQualityEnumeration getPrintQuality()
    {
        return printQuality.get();
    }

    public ObjectProperty<PrintQualityEnumeration> printQualityProperty()
    {
        return printQuality;
    }

    public void setSettingsName(String settingsName)
    {
        if (!this.customSettingsName.get().equals(settingsName))
        {
            steno.debug("change custom printer settings to " + settingsName);
            this.customSettingsName.set(settingsName);
            toggleDataChanged();
        }
    }

    public String getSettingsName()
    {
        return customSettingsName.get();
    }

    public StringProperty getSettingsNameProperty()
    {
        return customSettingsName;
    }

    public SlicerParametersFile getSettings()
    {
        switch (printQuality.get())
        {
            case DRAFT:
                return applyOverrides(SlicerParametersContainer.getSettingsByProfileName(
                    ApplicationConfiguration.draftSettingsProfileName));
            case NORMAL:
                return applyOverrides(SlicerParametersContainer.getSettingsByProfileName(
                    ApplicationConfiguration.normalSettingsProfileName));
            case FINE:
                return applyOverrides(SlicerParametersContainer.getSettingsByProfileName(
                    ApplicationConfiguration.fineSettingsProfileName));
            case CUSTOM:
                return applyOverrides(SlicerParametersContainer.getSettingsByProfileName(
                    customSettingsName.get()));

        }
        throw new RuntimeException("Unknown print quality");
    }

    /**
     * Standard profiles must have the overrides applied.
     */
    public SlicerParametersFile applyOverrides(SlicerParametersFile settingsByProfileName)
    {
        SlicerParametersFile profileCopy = settingsByProfileName.clone();
        profileCopy.setBrimWidth_mm(brimOverride);
        profileCopy.setFillDensity_normalised(fillDensityOverride);
        profileCopy.setGenerateSupportMaterial(printSupportOverride);
        profileCopy.setPrintRaft(raftOverride);
        return profileCopy;
    }

    public int getBrimOverride()
    {
        return brimOverride;
    }

    public void setBrimOverride(int brimOverride)
    {
        if (this.brimOverride != brimOverride)
        {
            this.brimOverride = brimOverride;
            toggleDataChanged();
        }
    }

    public float getFillDensityOverride()
    {
        return fillDensityOverride;
    }

    public void setFillDensityOverride(float fillDensityOverride)
    {
        if (this.fillDensityOverride != fillDensityOverride)
        {
            this.fillDensityOverride = fillDensityOverride;
            toggleDataChanged();
        }
    }

    public boolean getPrintSupportOverride()
    {
        return printSupportOverride;
    }

    public void setPrintSupportOverride(boolean printSupportOverride)
    {
        if (this.printSupportOverride != printSupportOverride)
        {
            this.printSupportOverride = printSupportOverride;
            toggleDataChanged();
        }
    }
    
    public boolean getRaftOverride()
    {
        return raftOverride;
    }    

    public void setRaftOverride(boolean raftOverride)
    {
        if (this.raftOverride != raftOverride)
        {
            this.raftOverride = raftOverride;
            toggleDataChanged();
        }
    }
}
