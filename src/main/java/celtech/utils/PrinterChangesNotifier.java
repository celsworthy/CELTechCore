/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;

/**
 * PrinterChangesNotifier listens to a list of printers and notifies registered listeners about the
 * following events: 
 * - Head added to printer 
 * - Head removed from printer 
 * - Reel added to printer (with reel index) 
 * - Reel removed from printer (with reel index) 
 * - Printer Identity changed
 * To be done: 
 * - Filament detected on extruder (with extruder index) 
 * - Filament removed from extruder
 * (with extruder index)
 * 
 * XXX Always passes reel index = 0 at the moment.
 *
 * @author tony
 */
public class PrinterChangesNotifier
{

    List<PrinterChangesListener> listeners = new ArrayList<>();

    public PrinterChangesNotifier(Printer printer)
    {
        printer.headProperty().addListener(
            (ObservableValue<? extends Head> observable, Head oldValue, Head newValue) ->
            {
                if (newValue != null)
                {
                    for (PrinterChangesListener printerChangesListener : listeners)
                    {
                        printerChangesListener.whenHeadAdded();
                    }
                } else {
                    for (PrinterChangesListener printerChangesListener : listeners)
                    {
                        printerChangesListener.whenHeadRemoved();
                    }
                }
            });
        
        printer.reelsProperty().addListener((ListChangeListener.Change<? extends Reel> change) ->
        {
            while (change.next())
            {
                if (change.wasAdded())
                {
                    for (Reel reel : change.getAddedSubList())
                    {
                        for (PrinterChangesListener listener : listeners)
                        {
                            listener.whenReelAdded(0);
                        }
                    }
                } else if (change.wasRemoved())
                {
                    for (Reel reel : change.getRemoved())
                    {
                        for (PrinterChangesListener listener : listeners)
                        {
                            listener.whenReelRemoved(0);
                        }
                    }
                } else if (change.wasReplaced())
                {
                } else if (change.wasUpdated())
                {
                }
            }
        });
    }
    
    public void addListener(PrinterChangesListener listener) {
        this.listeners.add(listener);
    }

    void removeListener(PrinterChangesListener listener)
    {
        this.listeners.remove(listener);
    }
}
