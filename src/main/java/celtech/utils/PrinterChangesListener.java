/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

/**
 *
 * @author tony
 */
interface PrinterChangesListener
{
    void whenHeadAdded();
    public void whenHeadRemoved();
    public void whenReelAdded(int reelIndex);
    public void whenReelRemoved(int reelIndex);
    public void whenPrinterIdentityChanged();    
    
}
