/*
 * Copyright 2015 CEL UK
 */
package celtech.printerControl.model;

import celtech.printerControl.PrinterStatus;
import celtech.printerControl.comms.commands.exceptions.RoboxCommsException;
import celtech.utils.tasks.Cancellable;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class PurgeActions
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        PurgeActions.class.getName());

    private final Printer printer;
    private final Cancellable cancellable = new Cancellable();

    PurgeActions(Printer printer)
    {
        this.printer = printer;
    }
    
    public void doFinishedAction() throws RoboxCommsException
    {
//        printerErrorHandler.deregisterForPrinterErrors();
//        saveSettings();
//        turnHeaterAndLEDSOff();
//        printer.inhibitHeadIntegrityChecks(false);
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }    

    public void doFailedAction() throws RoboxCommsException
    {
//        printerErrorHandler.deregisterForPrinterErrors();
//        restoreHeadState();
//        turnHeaterAndLEDSOff();
//        printer.inhibitHeadIntegrityChecks(false);
        try
        {
            if (printer.canCancelProperty().get())
            {
                printer.cancel(null);
            }
        } catch (PrinterException ex)
        {
            steno.error("Failed to cancel print - " + ex.getMessage());
        }
        printer.setPrinterStatus(PrinterStatus.IDLE);
    }

    public void cancel() throws RoboxCommsException
    {
        cancellable.cancelled.set(true);
        try
        {
            // wait for any current actions to respect cancelled flag
            Thread.sleep(500);
        } catch (InterruptedException ex)
        {
            steno.warning("interrupted during wait of cancel");
        }
        doFailedAction();
    }

}
