package celtech.coreUI.controllers;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterSettings
{

    private final ObjectProperty<Printer> selectedPrinter = new SimpleObjectProperty<>();
    private final ObjectProperty<Filament> selectedFilament0 = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Filament> selectedFilament1 = new SimpleObjectProperty<>(null);
    private final ObjectProperty<SlicerParametersFile> settings;
    private final ObjectProperty<PrintQualityEnumeration> printQuality = 
                                new SimpleObjectProperty<>(PrintQualityEnumeration.DRAFT);

    public PrinterSettings()
    {
        settings = new SimpleObjectProperty<>(
        SlicerParametersContainer.getSettingsByProfileName(
            ApplicationConfiguration.draftSettingsProfileName));
    }

    public void setSelectedPrinter(Printer value)
    {
        selectedPrinter.set(value);
    }

    /**
     *
     * @return
     */
    public Printer getSelectedPrinter()
    {
        return selectedPrinter.get();
    }

    /**
     *
     * @return
     */
    public ObjectProperty<Printer> selectedPrinterProperty()
    {
        return selectedPrinter;
    }

    /**
     *
     * @param value
     */
    public void setFilament0(Filament filament)
    {
            selectedFilament0.set(filament);
    }
    
    public void setFilament1(Filament filament)
    {
            selectedFilament1.set(filament);
    }    

    /**
     *
     * @return
     */
    public Filament getFilament0()
    {
        return selectedFilament0.get();
    }
    
    /**
     *
     * @return
     */
    public Filament getFilament1()
    {
        return selectedFilament1.get();
    }    

    /**
     *
     * @return
     */
    public ObjectProperty<Filament> filament0Property()
    {
        return selectedFilament0;
    }
    
    public ObjectProperty<Filament> filament1Property()
    {
        return selectedFilament1;
    }    

    /**
     *
     * @param value
     */
    public void setSettings(SlicerParametersFile value)
    {
        settings.set(value);
    }

    /**
     *
     * @return
     */
    public SlicerParametersFile getSettings()
    {
        return settings.get();
    }

    /**
     *
     * @return
     */
    public ObjectProperty<SlicerParametersFile> getSettingsProperty()
    {
        return settings;
    }

    /**
     *
     * @param value
     */
    public void setPrintQuality(PrintQualityEnumeration value)
    {
        printQuality.set(value);
    }

    /**
     *
     * @return
     */
    public PrintQualityEnumeration getPrintQuality()
    {
        return printQuality.get();
    }

    /**
     *
     * @return
     */
    public ObjectProperty<PrintQualityEnumeration> printQualityProperty()
    {
        return printQuality;
    }
}
