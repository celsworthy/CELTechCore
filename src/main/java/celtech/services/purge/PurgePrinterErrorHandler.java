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
import java.util.logging.Level;
import java.util.logging.Logger;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * The PurgePrinterErrorHandler listens for printer errors and if they occur then the next call to
 * {@link #checkIfPrinterErrorHasOccurred()} will cause the user to get an Continue/Abort dialog.
 *
 * @author tony
 */
public class PurgePrinterErrorHandler
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationXAndYActions.class.getName());

    private boolean errorOccurred = true;
    private final Printer printer;
    private final Cancellable cancellable;
    private ErrorConsumer errorConsumer;

    public PurgePrinterErrorHandler(Printer printer, Cancellable cancellable)
    {
        this.printer = printer;
        this.cancellable = cancellable;
    }

    public void registerForPrinterErrors()
    {
        errorOccurred = false;
        List<FirmwareError> errors = new ArrayList<>();
        errors.add(FirmwareError.ALL_ERRORS);
        errorConsumer = (FirmwareError error) ->
        {
            cancellable.cancelled = true;
            errorOccurred = true;
        };
        printer.registerErrorConsumer(errorConsumer, errors);
    }

    /**
     * Check if a printer error has occurred and if so notify the user via a dialog box (only giving
     * the Abort option) and then raise an exception so as to cause the calling action to fail.
     */
    public void checkIfPrinterErrorHasOccurred()
    {
        if (errorOccurred)
        {
            showPrinterErrorOccurred();
        }
    }

    public void deregisterForPrinterErrors()
    {
        errorOccurred = false;
        printer.deregisterErrorConsumer(errorConsumer);
    }

    /**
     * Show a dialog to the user asking them to choose between available Continue or Abort 
     * actions. Call the chosen handler. If a given handler is null then that option will not be
     * offered to the user. At least one handler must be non-null.
     */
    private void showPrinterErrorOccurred() 
    {
        Callable<Boolean> askUserWhetherToAbort = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                ChoiceLinkDialogBox choiceLinkDialogBox = new ChoiceLinkDialogBox();
                choiceLinkDialogBox.setTitle(Lookup.i18n(
                    "dialogs.purge.printerErrorTitle"));
                choiceLinkDialogBox.setMessage(Lookup.i18n(
                    "dialogs.purge.printerError"));
                choiceLinkDialogBox.addChoiceLink(
                    Lookup.i18n("error.handler.OK_CONTINUE.title"),
                    Lookup.i18n("dialogs.purge.continueAfterPrinterError"));
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
            if (doAbort) {
                cancellable.cancelled = true;
                try
                {
                    printer.cancel(null);
                } catch (PrinterException ex)
                {
                   steno.error("Cannot cancel purge");
                }
            } else {
                errorOccurred = false;
            }
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error during purge");
        }
    }

}
