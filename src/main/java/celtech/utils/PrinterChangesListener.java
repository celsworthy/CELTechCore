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
    public void whenReelAdded(int reelIndex, Reel reel);
    public void whenReelRemoved(int reelIndex, Reel reel);
    public void whenReelChanged(Reel reel);
    public void whenExtruderAdded(int extruderIndex);
    public void whenExtruderRemoved(int extruderIndex);    
}
