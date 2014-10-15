/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.printerControl.model.Printer;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * PrinterListChangesNotifier listens to a list of printers and notifies registered listeners about
 * the following events:
 *   - Printer added
 *   - Printer removed
 *   - Head added to printer
 *   - Head removed from printer
 *   - Reel added to printer (with reel index)
 *   - Reel removed from printer (with reel index)
 *   - Printer Identity changed
 * To be done:
 *   - Filament detected on extruder (with extruder index)
 *   - Filament removed from extruder (with extruder index)
 * @author tony
 */

public class PrinterListChangesNotifier
{
    private final List<PrinterListChangesListener> listeners = new ArrayList<>();
    
    public PrinterListChangesNotifier(ObservableList<Printer> printers) {

        printers.addListener((ListChangeListener.Change<? extends Printer> change) ->
        {
            while (change.next())
            {
                if (change.wasAdded())
                {
                    for (Printer printer : change.getAddedSubList())
                    {
                        for (PrinterListChangesListener listener : listeners)
                        {
                            listener.whenPrinterAdded(printer);
                        }
                    }
                } else if (change.wasRemoved())
                {
                    for (Printer printer : change.getRemoved())
                    {
                        for (PrinterListChangesListener listener : listeners)
                        {
                            listener.whenPrinterRemoved(printer);
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
    
    public void addListener(PrinterListChangesListener listener) {
        this.listeners.add(listener);
    }

}
