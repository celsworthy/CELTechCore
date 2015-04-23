/*
 * Copyright 2014 CEL UK
 */
package celtech.services.purge;

import celtech.Lookup;
import celtech.appManager.SystemNotificationManager.PrinterErrorChoice;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.comms.events.ErrorConsumer;
import celtech.printerControl.model.CalibrationXAndYActions;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.utils.tasks.Cancellable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * The PurgePrinterErrorHandlerOrig listens for printer errors and if they occur then cause the user to
 get a Continue/Abort dialog.
 *
 * @author tony
 */
public class PurgePrinterErrorHandlerOrig
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationXAndYActions.class.getName());

    private final Printer printer;
    private final Cancellable cancellable;
    private boolean errorDialogIsOnDisplay = false;

    public PurgePrinterErrorHandlerOrig(Printer printer, Cancellable cancellable)
    {
        this.printer = printer;
        this.cancellable = cancellable;
    }

    ErrorConsumer errorConsumer = (FirmwareError error) ->
    {
        steno.debug("ERROR consumed in purge");
        notifyUserErrorHasOccurredAndAbortIfNotSlip(error);
    };

    public void registerForPrinterErrors()
    {
        List<FirmwareError> errors = new ArrayList<>();
        errors.add(FirmwareError.ALL_ERRORS);
        printer.registerErrorConsumer(errorConsumer, errors);
    }

    /**
     * Check if a printer error has occurred and if so notify the user via a dialog box (only giving
     * the Abort option). Return a boolean indicating if the process should abort.
     */
    private void notifyUserErrorHasOccurredAndAbortIfNotSlip(FirmwareError error)
    {
        if (! cancellable.cancelled().get())
        {
            if (error == FirmwareError.B_POSITION_LOST)
            {
                // Do nothing for the moment...
            } else if (error == FirmwareError.D_FILAMENT_SLIP
                || error == FirmwareError.E_FILAMENT_SLIP)
            {
                Optional<PrinterErrorChoice> response = Lookup.getSystemNotificationHandler().
                    showPrinterErrorDialog(
                        error.getLocalisedErrorTitle(),
                        error.getLocalisedErrorMessage(),
                        true,
                        true,
                        false,
                        false);

                boolean abort = false;

                if (response.isPresent())
                {
                    switch (response.get())
                    {
                        case ABORT:
                            abort = true;
                            break;
                    }
                } else
                {
                    abort = true;
                }

                if (abort)
                {
                    cancelPurge();
                }
            } else
            {
                // Must be something else
                // if not filament slip or B POSITION then cancel / abort printer activity immediately
                cancelPurge();
                Lookup.getSystemNotificationHandler().
                    showPrinterErrorDialog(
                        error.getLocalisedErrorTitle(),
                        Lookup.i18n("error.purge.cannotContinue"),
                        false,
                        false,
                        false,
                        true);
            }
        }
    }

    private void cancelPurge()
    {
        try
        {
            cancellable.cancelled().set(true);
            if (printer.canCancelProperty().get())
            {
                printer.cancel(null);
            }
        } catch (PrinterException ex)
        {
            steno.error("Cannot cancel print (in purge): " + ex);
        }
    }

    public void deregisterForPrinterErrors()
    {
        printer.deregisterErrorConsumer(errorConsumer);
    }
}
