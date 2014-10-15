package celtech.coreUI.controllers;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.PrintProfileContainer;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
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
    private final ObjectProperty<RoboxProfile> settings = new SimpleObjectProperty<>(PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName));
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
    public void setSettings(RoboxProfile value)
    {
        settings.set(value);
    }

    /**
     *
     * @return
     */
    public RoboxProfile getSettings()
    {
        return settings.get();
    }

    /**
     *
     * @return
     */
    public ObjectProperty<RoboxProfile> getSettingsProperty()
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
