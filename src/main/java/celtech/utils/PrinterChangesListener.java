/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import celtech.printerControl.model.Head;
import celtech.printerControl.model.Reel;

/**
 *
 * @author tony
 */
interface PrinterChangesListener
{
    void whenHeadAdded();
    public void whenHeadRemoved(Head head);
    public void whenReelAdded(int reelIndex);
    public void whenReelRemoved(Reel reel);
}
