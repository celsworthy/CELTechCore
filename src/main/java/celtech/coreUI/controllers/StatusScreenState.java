/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.printerControl.Printer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class StatusScreenState
{

    private static StatusScreenState instance = null;
    private static ObjectProperty<Printer> currentlySelectedPrinter = new SimpleObjectProperty<>();
    private static ObjectProperty<StatusScreenMode> currentMode = new SimpleObjectProperty<>();

    private StatusScreenState()
    {
    }

    public static StatusScreenState getInstance()
    {
        if (instance == null)
        {
            instance = new StatusScreenState();
        }
        return instance;
    }

    public Printer getCurrentlySelectedPrinter()
    {
        return currentlySelectedPrinter.get();
    }

    public void setCurrentlySelectedPrinter(Printer currentlySelectedPrinter)
    {
        StatusScreenState.currentlySelectedPrinter.set(currentlySelectedPrinter);
    }

    public ObjectProperty<Printer> currentlySelectedPrinterProperty()
    {
        return currentlySelectedPrinter;
    }

    public StatusScreenMode getMode()
    {
        return currentMode.get();
    }

    public void setMode(StatusScreenMode value)
    {
        currentMode.set(value);
    }

    public ObjectProperty<StatusScreenMode> modeProperty()
    {
        return currentMode;
    }
}
