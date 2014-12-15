/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;

/**
 * PrinterChangesNotifier listens to a list of printers and notifies registered listeners about the following events:
 * <p>
 * - Head added to printer<p>
 * - Head removed from printer<p>
 * - Reel added to printer (with reel index)
 * <p>
 * - Reel removed from printer (with reel)
 * <p>
 * - Reel changed<p>
 * - Printer Identity changed<p>
 * To be done:
 * <p>
 * - Filament detected on extruder (with extruder index)
 * <p>
 * - Filament removed from extruder (with extruder index)
 * <p>
 *
 * XXX Always passes reel index = 0 at the moment.
 *
 * @author tony
 */
public class PrinterChangesNotifier
{

    List<PrinterChangesListener> listeners = new ArrayList<>();
    private final Map<Reel, ReelChangesListener> reelListeners = new HashMap<>();
    private final Map<Reel, ReelChangesNotifier> reelNotifiers = new HashMap<>();

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
                } else
                {
                    for (PrinterChangesListener printerChangesListener : listeners)
                    {
                        printerChangesListener.whenHeadRemoved(oldValue);
                    }
                }
            });

        printer.reelsProperty().addListener(new MapChangeListener<Integer, Reel>()
        {
            @Override
            public void onChanged(
                MapChangeListener.Change<? extends Integer, ? extends Reel> change)
            {
                if (change.wasAdded())
                {
                    for (PrinterChangesListener listener : listeners)
                    {
                        listener.whenReelAdded(change.getKey(), change.getValueAdded());
                        setupReelChangesNotifier(change.getValueAdded());
                    }
                } else
                {
                    for (PrinterChangesListener listener : listeners)
                    {
                        listener.whenReelRemoved(change.getKey(), change.getValueRemoved());
                        setupReelChangesNotifier(change.getValueRemoved());
                    }
                }
            }
        });
    }

    public void addListener(PrinterChangesListener listener)
    {
        this.listeners.add(listener);
    }

    void removeListener(PrinterChangesListener listener)
    {
        this.listeners.remove(listener);
    }

    private void setupReelChangesNotifier(Reel reel)
    {
        ReelChangesNotifier reelChangesNotifier = new ReelChangesNotifier(reel);
        ReelChangesListener reelChangesListener = new ReelChangesListener()
        {
            @Override
            public void whenReelChanged()
            {
                fireWhenReelChanged(reel);
            }

        };
        reelListeners.put(reel, reelChangesListener);
        reelNotifiers.put(reel, reelChangesNotifier);
        reelChangesNotifier.addListener(reelChangesListener);
    }

    private void removeReelChangesNotifier(Reel reel)
    {
        reelNotifiers.get(reel).removeListener(reelListeners.get(reel));
    }

    private void fireWhenReelChanged(Reel reel)
    {
        listeners.stream().
            forEach((listener) ->
                {
                    listener.whenReelChanged(reel);
            });
    }
}
