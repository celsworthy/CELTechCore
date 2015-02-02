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
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * The CalibrationOpeningErrorHandler listens for printer errors and if they occur then the next
 * call to {@link #checkIfPrinterErrorHasOccurred()} will cause the user to get an Abort dialog,
 * which is followed by raising an exception.
 *
 * @author tony
 */
class CalibrationOpeningErrorHandler
{

    private final Stenographer steno = StenographerFactory.getStenographer(CalibrationOpeningErrorHandler.class.getName());
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

    public CalibrationOpeningErrorHandler(Printer printer, Cancellable cancellable)
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
     * Check if a printer error has occurred and if so notify the user via a dialog box (only giving
     * the Abort option) and then raise an exception so as to cause the calling action to fail.
     */
    public boolean checkIfPrinterErrorHasOccurred() throws CalibrationException
    {
        if (errorOccurred)
        {
            showPrinterErrorOccurred(continueHandler, abortHandler, retryHandler);
        }
        
        return errorOccurred;
    }

    ErrorConsumer errorConsumer = (FirmwareError error) ->
    {
        steno.info(error.name() + " occurred during nozzle opening calibration");
        // Filament slips can occur during pressurisation - we need to ignore them
        if (error == FirmwareError.E_FILAMENT_SLIP)
        {
            steno.info("Discarded filament slip error during calibration");
        } else
        {
            cancellable.cancelled = true;
            errorOccurred = true;
        }
    };

    public void deregisterForPrinterErrors()
    {
        errorOccurred = false;
        printer.deregisterErrorConsumer(errorConsumer);
    }

    /**
     * Show a dialog to the user asking them to choose between available Continue, Abort or Retry
     * actions. Call the chosen handler. If a given handler is null then that option will not be
     * offered to the user. At least one handler must be non-null.
     */
    private void showPrinterErrorOccurred(TaskExecutor.NoArgsVoidFunc continueHandler,
        TaskExecutor.NoArgsVoidFunc abortHandler, TaskExecutor.NoArgsVoidFunc retryHandler) throws CalibrationException
    {
        try
        {
            Optional<SystemNotificationManager.PrinterErrorChoice> choice = Lookup.
                getSystemNotificationHandler().showPrinterErrorDialog(
                    Lookup.i18n("calibrationPanel.title"), Lookup.i18n(
                        "calibrationPanel.errorInPrinter"),
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
            ex.printStackTrace();
            throw new CalibrationException(ex.getMessage());
        }
    }

}
