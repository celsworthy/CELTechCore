/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.printerControl;

import celtech.utils.tasks.TaskResponder;

/**
 *
 * @author Ian
 */
public abstract class NewPrinter
{
    protected NewPrinterStatus newPrinterStatus = NewPrinterStatus.IDLE;
    
    public abstract boolean canRemoveHead();
    public abstract void removeHead(TaskResponder responder) throws PrinterException;
    
    public abstract boolean canPrint();
}
