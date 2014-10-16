/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;

/**
 *
 * @author tony
 */
public interface PrinterListChangesListener
{

    public void whenPrinterAdded(Printer printer);

    public void whenPrinterRemoved(Printer printer);

    public void whenHeadAdded(Printer printer);

    public void whenHeadRemoved(Printer printer, Head head);

    public void whenReelAdded(Printer printer, int reelIndex);

    public void whenReelRemoved(Printer printer, Reel reel);
    
}
