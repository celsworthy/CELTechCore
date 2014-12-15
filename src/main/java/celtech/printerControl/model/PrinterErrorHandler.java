/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.Lookup;
import celtech.appManager.SystemNotificationManager;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.comms.events.ErrorConsumer;
import celtech.utils.tasks.Cancellable;
import celtech.utils.tasks.TaskExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The PrinterErrorHandler listens for printer errors and if they occur then the next call to 
 * {@link #checkIfPrinterErrorHasOccurred()} will cause the user to get an Abort dialog, which is
 * followed by raising an exception.
 * @author tony
 */
class PrinterErrorHandler
{
    
    private boolean errorOccurred = true;
    private final Printer printer;
    private Cancellable cancellable;

    TaskExecutor.NoArgsVoidFunc continueHandler = null;
    TaskExecutor.NoArgsVoidFunc retryHandler = null;
    TaskExecutor.NoArgsVoidFunc abortHandler = () ->
    {
        cancellable.cancelled = true;
        throw new CalibrationException("An error occurred with the printer");
    };    
    
    
    public PrinterErrorHandler(Printer printer, Cancellable cancellable)
    {
        this.printer = printer;
        this.cancellable = cancellable;
    }
    
    public void registerForPrinterErrors()
    {
        errorOccurred = false;
        List<FirmwareError> errors = new ArrayList<>();
        errors.add(FirmwareError.ALL_ERRORS);
        printer.registerErrorConsumer(errorConsumer, errors);
    }
    
    /**
     * Check if a printer error has occurred and if so notify the user via a dialog box (only
     * giving the Abort option) and then raise an exception so as to cause the calling
     * action to fail.
     */
    public void checkIfPrinterErrorHasOccurred() throws CalibrationException
    {
        if (errorOccurred)
        {
            showPrinterErrorOccurred(continueHandler, abortHandler, retryHandler);
        }
    }

    ErrorConsumer errorConsumer = (FirmwareError error) ->
    {
        cancellable.cancelled = true;
        errorOccurred = true;
    };

    public void deregisterForPrinterErrors()
    {
        errorOccurred = false;
        printer.deregisterErrorConsumer(errorConsumer);
    }    
    
/**
     * Show a dialog to the user asking them to choose between available Continue, Abort or Retry
     * actions. Call the chosen handler. If a given handler is null then that option will
     * not be offered to the user. At least one handler must be non-null.
     */
    private void showPrinterErrorOccurred(TaskExecutor.NoArgsVoidFunc continueHandler,
        TaskExecutor.NoArgsVoidFunc abortHandler, TaskExecutor.NoArgsVoidFunc retryHandler) throws CalibrationException
    {
        try
        {
            Optional<SystemNotificationManager.PrinterErrorChoice> choice = Lookup.getSystemNotificationHandler().showPrinterErrorDialog(
                Lookup.i18n("calibrationPanel.title"), Lookup.i18n("calibrationPanel.errorInPrinter"),
                continueHandler != null, abortHandler != null, retryHandler != null);
            if (!choice.isPresent())
            {
                cancellable.cancelled = true;
                abortHandler.run();
                return;
            }
            switch (choice.get())
            {
                case CONTINUE:
                    continueHandler.run();
                    break;
                case ABORT:
                    cancellable.cancelled = true;
                    abortHandler.run();
                    break;
                case RETRY:
                    retryHandler.run();
                    break;
            }
        } catch (Exception ex)
        {
            throw new CalibrationException(ex.getMessage());
        }
    }
    
}
