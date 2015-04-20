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

/**
 * PrinterSettings represents the choices made by the user for a project on the Settings panel. It
 * is serialised with the project.
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterSettings
{

    private final ObjectProperty<Printer> selectedPrinter = new SimpleObjectProperty<>();
    private final ObjectProperty<Filament> selectedFilament0 = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Filament> selectedFilament1 = new SimpleObjectProperty<>(null);
    private final StringProperty settingsName = new SimpleStringProperty();
    private final ObjectProperty<PrintQualityEnumeration> printQuality
        = new SimpleObjectProperty<>(PrintQualityEnumeration.DRAFT);
    private final BooleanProperty dataChanged = new SimpleBooleanProperty(false);

    private int brimOverride = 0;
    private float fillDensityOverride = 0;
    private boolean printSupportOverride = false;

    public PrinterSettings()
    {
        settingsName.set(ApplicationConfiguration.draftSettingsProfileName);
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
            System.out.println("F0 changed");
            toggleDataChanged();
        }
    }

    public void setFilament1(Filament filament)
    {
        if (selectedFilament1.get() != filament)
        {
            selectedFilament1.set(filament);
            System.out.println("F1 changed");
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
            System.out.println("quality changed");
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
        if (! this.settingsName.get().equals(settingsName))
        {
            this.settingsName.set(settingsName);
            System.out.println("settings name changed");
            toggleDataChanged();
        }
    }

    public String getSettingsName()
    {
        return settingsName.get();
    }

    public StringProperty getSettingsNameProperty()
    {
        return settingsName;
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
                return SlicerParametersContainer.getSettingsByProfileName(settingsName.get());

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
            System.out.println("brim changed");
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
            System.out.println("fill density changed");
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
            System.out.println("support changed");
            toggleDataChanged();
        }
    }
}
