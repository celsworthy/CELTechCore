/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.PrintProfileContainer;
import celtech.printerControl.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SlicerSettings;
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
    private final ObjectProperty<SlicerSettings> settings = new SimpleObjectProperty<>(PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName));
    private final ObjectProperty<PrintQualityEnumeration> printQuality = new SimpleObjectProperty<>();

    private SettingsScreenState()
    {

    }

    public static SettingsScreenState getInstance()
    {
        if (instance == null)
        {
            instance = new SettingsScreenState();
        }
        return instance;
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

    public void setFilament(Filament value)
    {
        selectedFilament.set(value);
    }

    public Filament getFilament()
    {
        return selectedFilament.get();
    }

    public ObjectProperty<Filament> filamentProperty()
    {
        return selectedFilament;
    }

    public void setSettings(SlicerSettings value)
    {
        settings.set(value);
    }

    public SlicerSettings getSettings()
    {
        return settings.get();
    }

    public ObjectProperty<SlicerSettings> getSettingsProperty()
    {
        return settings;
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
}
