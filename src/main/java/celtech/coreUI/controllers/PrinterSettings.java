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
 * PrinterSettings represents the choices made by the user for a project on the
 * Settings panel. It is serialised with the project.
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
    private BooleanProperty dataChanged = new SimpleBooleanProperty(false);
    
    private int brimOverride = 0;
    private float fillDensityOverride = 0;
    private boolean printSupportOverride = false;

    public PrinterSettings()
    {
        settingsName.set(ApplicationConfiguration.draftSettingsProfileName);
    }

    private void toggleDataChanged() {
        dataChanged.set(dataChanged.not().get());
    }
    
    public ReadOnlyBooleanProperty getDataChanged() {
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
        selectedFilament0.set(filament);
        toggleDataChanged();
    }

    public void setFilament1(Filament filament)
    {
        selectedFilament1.set(filament);
        toggleDataChanged();
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
        printQuality.set(value);
        toggleDataChanged();
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
        this.settingsName.set(settingsName);
        toggleDataChanged();
    }
    
    public String getSettingsName()
    {
        return settingsName.get();
    }    
    
    public SlicerParametersFile getSettings() {
        switch (printQuality.get()) {
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
    private SlicerParametersFile applyOverrides(SlicerParametersFile settingsByProfileName)
    {
        SlicerParametersFile profileCopy =  settingsByProfileName.clone();
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
        this.brimOverride = brimOverride;
        toggleDataChanged();
    }

    public float getFillDensityOverride()
    {
        return fillDensityOverride;
    }

    public void setFillDensityOverride(float fillDensityOverride)
    {
        this.fillDensityOverride = fillDensityOverride;
        toggleDataChanged();
    }

    public boolean getPrintSupportOverride()
    {
        return printSupportOverride;
    }

    public void setPrintSupportOverride(boolean printSupportOverride)
    {
        this.printSupportOverride = printSupportOverride;
        toggleDataChanged();
    }    
}
