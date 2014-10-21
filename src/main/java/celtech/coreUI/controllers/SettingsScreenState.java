package celtech.coreUI.controllers;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParameters;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SettingsScreenState
{

    private static SettingsScreenState instance = null;
    private final ObjectProperty<Printer> selectedPrinter = new SimpleObjectProperty<>();
    private final ObjectProperty<Filament> selectedFilament = new SimpleObjectProperty<>(null);
    private final ObjectProperty<SlicerParameters> settings = new SimpleObjectProperty<>(SlicerParametersContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName));
    private final ObjectProperty<PrintQualityEnumeration> printQuality = new SimpleObjectProperty<>();

    private SettingsScreenState()
    {

    }

    /**
     *
     * @return
     */
    public static SettingsScreenState getInstance()
    {
        if (instance == null)
        {
            instance = new SettingsScreenState();
        }
        return instance;
    }

    /**
     *
     * @param value
     */
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
    public void setFilament(Filament value)
    {
        selectedFilament.set(value);
    }

    /**
     *
     * @return
     */
    public Filament getFilament()
    {
        return selectedFilament.get();
    }

    /**
     *
     * @return
     */
    public ObjectProperty<Filament> filamentProperty()
    {
        return selectedFilament;
    }

    /**
     *
     * @param value
     */
    public void setSettings(SlicerParameters value)
    {
        settings.set(value);
    }

    /**
     *
     * @return
     */
    public SlicerParameters getSettings()
    {
        return settings.get();
    }

    /**
     *
     * @return
     */
    public ObjectProperty<SlicerParameters> getSettingsProperty()
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
