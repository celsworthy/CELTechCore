/*
 * Copyright 2014 CEL UK
 */
package celtech.services.purge;

import celtech.Lookup;
import celtech.coreUI.components.ChoiceLinkButton;
import celtech.coreUI.components.ChoiceLinkDialogBox;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.comms.events.ErrorConsumer;
import celtech.printerControl.model.CalibrationXAndYActions;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.utils.tasks.Cancellable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * The PurgePrinterErrorHandler listens for printer errors and if they occur then cause the user to
 * get a Continue/Abort dialog.
 *
 * @author tony
 */
public class PurgePrinterErrorHandler
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationXAndYActions.class.getName());

    private final Printer printer;
    private final Cancellable cancellable;

    public PurgePrinterErrorHandler(Printer printer, Cancellable cancellable)
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

        boolean allowContinue = false;
        if (error == FirmwareError.ERROR_D_FILAMENT_SLIP || error
            == FirmwareError.ERROR_E_FILAMENT_SLIP)
        {
            allowContinue = true;
        } else
        {
            // if not filament slip then cancel / abort printer activity immediately
            cancelPurge();
        }
        boolean abort = showPrinterErrorOccurred(error, allowContinue);
        if (abort && allowContinue)
        {
            cancelPurge();
        }
    }

    private void cancelPurge()
    {
        try
        {
            cancellable.cancelled = true;
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

    /**
     * Show a dialog to the user asking them to choose between available Continue or Abort actions.
     */
    private boolean showPrinterErrorOccurred(FirmwareError error, boolean allowContinue)
    {
        Callable<Boolean> askUserWhetherToAbort = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
                choiceLinkDialogBox.setTitle(Lookup.i18n(
                    "dialogs.purge.printerErrorTitle"));
//                choiceLinkDialogBox.setMessage(Lookup.i18n(
//                    "dialogs.purge.printerError"));
                choiceLinkDialogBox.setMessage(error.getLocalisedErrorTitle());
                if (allowContinue)
                {
                    choiceLinkDialogBox.addChoiceLink(
                        Lookup.i18n("error.handler.OK_CONTINUE.title"),
                        Lookup.i18n("dialogs.purge.continueAfterPrinterError"));
                }
                ChoiceLinkButton abort = choiceLinkDialogBox.addChoiceLink(
                    Lookup.i18n("error.handler.ABORT.title"),
                    Lookup.i18n("dialogs.purge.abort"));

                Optional<ChoiceLinkButton> shutdownResponse = choiceLinkDialogBox.
                    getUserInput();

                return shutdownResponse.get() == abort;
            }
        };

        FutureTask<Boolean> askWhetherToAbortTask = new FutureTask<>(askUserWhetherToAbort);
        Lookup.getTaskExecutor().runOnGUIThread(askWhetherToAbortTask);
        try
        {
            boolean doAbort = askWhetherToAbortTask.get();
            return doAbort;
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during purge");
        }
        return true;
    }

}
