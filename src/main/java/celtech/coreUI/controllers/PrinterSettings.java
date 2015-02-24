package celtech.coreUI.controllers;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
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

    public PrinterSettings()
    {
        settingsName.set(ApplicationConfiguration.draftSettingsProfileName);
    }

    public SlicerParametersFile getSettings()
    {
        return null;
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
    }

    public void setFilament1(Filament filament)
    {
        selectedFilament1.set(filament);
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
    }
    
    public String getSettingsName()
    {
        return settingsName.get();
    }    
}
