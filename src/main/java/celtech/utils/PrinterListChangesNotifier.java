/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * PrinterListChangesNotifier listens to a list of printers and notifies registered listeners about
 * the following events: - Printer added - Printer removed - Head added to printer - Head removed
 * from printer - Reel added to printer (with reel index) - Reel removed from printer (with reel
 * index) - Printer Identity changed To be done: - Filament detected on extruder (with extruder
 * index) - Filament removed from extruder (with extruder index)
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
        for (PrinterListChangesListener listener : new ArrayList<>(listeners))
        {
            for (Entry<Integer, Reel> mappedReel : printer.reelsProperty().entrySet())
            {
                listener.whenReelRemoved(printer, mappedReel.getValue(), mappedReel.getKey());
            }

            if (printer.headProperty().get() != null)
            {
                listener.whenHeadRemoved(printer, printer.headProperty().get());
            }

            for (int extruderIndex = 0; extruderIndex < printer.extrudersProperty().size(); extruderIndex++)
            {
                if (printer.extrudersProperty().get(extruderIndex).isFittedProperty().get())
                {
                    listener.whenExtruderRemoved(printer, extruderIndex);
                }
            }

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
            public void whenHeadRemoved(Head head)
            {
                fireWhenHeadRemoved(printer, head);
            }

            @Override
            public void whenReelAdded(int reelIndex, Reel reel)
            {
                fireWhenReelAdded(printer, reelIndex);
            }

            @Override
            public void whenReelRemoved(int reelIndex, Reel reel)
            {
                fireWhenReelRemoved(printer, reel, reelIndex);
            }

            @Override
            public void whenReelChanged(Reel reel)
            {
                fireWhenReelChanged(printer, reel);
            }

            @Override
            public void whenExtruderAdded(int extruderIndex)
            {
                fireWhenExtruderAdded(printer, extruderIndex);
            }

            @Override
            public void whenExtruderRemoved(int extruderIndex)
            {
                fireWhenExtruderRemoved(printer, extruderIndex);
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
            if (printer.headProperty().get() != null)
            {
                listener.whenHeadAdded(printer);
            }

            printer.reelsProperty().entrySet().stream().
                forEach((mappedReel) ->
                    {
                        listener.whenReelAdded(printer, mappedReel.getKey());
                });

            for (int extruderIndex = 0; extruderIndex < printer.extrudersProperty().size(); extruderIndex++)
            {
                if (printer.extrudersProperty().get(extruderIndex).isFittedProperty().get())
                {
                    listener.whenExtruderAdded(printer, extruderIndex);
                }
            }
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

    private void fireWhenHeadRemoved(Printer printer, Head head)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenHeadRemoved(printer, head);
        }
    }

    private void fireWhenReelAdded(Printer printer, int reelIndex)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenReelAdded(printer, reelIndex);
        }
    }

    private void fireWhenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenReelRemoved(printer, reel, reelIndex);
        }
    }

    private void fireWhenReelChanged(Printer printer, Reel reel)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenReelChanged(printer, reel);
        }
    }

    private void fireWhenExtruderAdded(Printer printer, int extruderIndex)
    {
        for (PrinterListChangesListener listener : new ArrayList<>(listeners))
        {
            listener.whenExtruderAdded(printer, extruderIndex);
        }
    }

    private void fireWhenExtruderRemoved(Printer printer, int extruderIndex)
    {
        for (PrinterListChangesListener listener : listeners)
        {
            listener.whenExtruderRemoved(printer, extruderIndex);
        }
    }

}
