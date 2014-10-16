/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.printerControl.model.Printer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * PrinterListChangesNotifier listens to a list of printers and notifies registered listeners about
 * the following events: 
 * - Printer added 
 * - Printer removed 
 * - Head added to printer 
 * - Head removed from printer 
 * - Reel added to printer (with reel index) 
 * - Reel removed from printer (with reel index) 
 * - Printer Identity changed 
 * To be done: 
 * - Filament detected on extruder (with extruder index) 
 * - Filament removed from extruder (with extruder index)
 *
 * @author tony
 */
public class PrinterListChangesNotifier
{

    private final List<PrinterListChangesListener> listeners = new ArrayList<>();
    private final Map<Printer, PrinterChangesListener> printerListeners = new HashMap<>();
    private final Map<Printer, PrinterChangesNotifier> printerNotifiers = new HashMap<>();

    public PrinterListChangesNotifier(ObservableList<Printer> printers)
    {

        printers.addListener((ListChangeListener.Change<? extends Printer> change) ->
        {
            while (change.next())
            {
                if (change.wasAdded())
                {
                    for (Printer printer : change.getAddedSubList())
                    {
                        
                        fireWhenPrinterAdded(printer);
                        setupPrinterChangesNotifier(printer);
                    }
                } else if (change.wasRemoved())
                {
                    for (Printer printer : change.getRemoved())
                    {
                        fireWhenPrinterRemoved(printer);
                        removePrinterChangesNotifier(printer);
                    }
                } else if (change.wasReplaced())
                {
                } else if (change.wasUpdated())
                {
                }
            }
        });
    }

    private void fireWhenPrinterRemoved(Printer printer)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenPrinterRemoved(printer);
        }
    }

    private void setupPrinterChangesNotifier(Printer printer)
    {
        PrinterChangesNotifier printerChangesNotifier = new PrinterChangesNotifier(printer);
        PrinterChangesListener printerChangesListener = new PrinterChangesListener()
        {
            
            @Override
            public void whenHeadAdded()
            {
                fireWhenHeadAdded(printer);
            }
            
            @Override
            public void whenHeadRemoved()
            {
                fireWhenHeadRemoved(printer);
            }
            
            @Override
            public void whenReelAdded(int reelIndex)
            {
                fireWhenReelAdded(printer, reelIndex);
            }
            
            @Override
            public void whenReelRemoved(int reelIndex)
            {
                fireWhenReelRemoved(printer, reelIndex);
            }
            
            @Override
            public void whenPrinterIdentityChanged()
            {
            }
        };
        printerListeners.put(printer, printerChangesListener);
        printerNotifiers.put(printer, printerChangesNotifier);
        printerChangesNotifier.addListener(printerChangesListener);
    }
    
    private void removePrinterChangesNotifier(Printer printer)
    {
        printerNotifiers.get(printer).removeListener(printerListeners.get(printer));
    }    

    private void fireWhenPrinterAdded(Printer printer)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenPrinterAdded(printer);
        }
    }

    public void addListener(PrinterListChangesListener listener)
    {
        this.listeners.add(listener);
    }

    private void fireWhenHeadAdded(Printer printer)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenHeadAdded(printer);
        }
    }
    
    private void fireWhenHeadRemoved(Printer printer)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenHeadRemoved(printer);
        }
    }    
    
    private void fireWhenReelAdded(Printer printer, int reelIndex)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenReelAdded(printer, reelIndex);
        }
    }
    
    private void fireWhenReelRemoved(Printer printer, int reelIndex)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenReelRemoved(printer, reelIndex);
        }
    }       

}
