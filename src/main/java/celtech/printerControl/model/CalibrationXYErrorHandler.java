/*
 * Copyright 2014 CEL UK
 */
package celtech.printerControl.model;

import celtech.Lookup;
import celtech.printerControl.comms.commands.rx.FirmwareError;
import celtech.printerControl.comms.events.ErrorConsumer;
import celtech.utils.tasks.Cancellable;
import java.util.ArrayList;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * The CalibrationXYErrorHandler listens for printer errors and if they occur then cause the user to
 * get a Continue/Abort dialog.
 *
 * @author tony
 */
public class CalibrationXYErrorHandler
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationOpeningErrorHandler.class.getName());
    private boolean errorOccurred = true;
    private FirmwareError lastError = null;
    private final Printer printer;
    private Cancellable cancellable;

    public CalibrationXYErrorHandler(Printer printer, Cancellable cancellable)
    {
        this.printer = printer;
        this.cancellable = cancellable;
    }

    public void registerForPrinterErrors()
    {
        errorOccurred = false;
        lastError = null;
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
            showPrinterErrorOccurred(lastError);
        }

        return errorOccurred;
    }

    ErrorConsumer errorConsumer = (FirmwareError error) ->
    {
        boolean errorPopupRequired = false;

        steno.info(error.name() + " occurred during nozzle XY calibration");
        // B Position Lost reflects that the printer has detected a non-fatal problem - ignore it
        //TODO modify for multiple extruders
        if (error == FirmwareError.B_POSITION_LOST)
        {
            steno.info("Discarded error " + error.name() + " during calibration");
        } else if (error == FirmwareError.D_FILAMENT_SLIP
            || error == FirmwareError.E_FILAMENT_SLIP)
        {
            boolean reachedLimit = runReducePrintSpeed(error);

            if (reachedLimit)
            {
                errorPopupRequired = true;
            }
        } else
        {
            errorPopupRequired = true;
        }

        if (errorPopupRequired)
        {
            errorOccurred = true;
            lastError = error;
            cancellable.cancelled().set(true);
        }
    };

    private boolean runReducePrintSpeed(FirmwareError error)
    {
        return printer.doFilamentSlipWhilePrinting(error);
    }

    public void deregisterForPrinterErrors()
    {
        errorOccurred = false;
        lastError = null;
        printer.deregisterErrorConsumer(errorConsumer);
    }

    /**
     * Show a dialog to the user asking them to choose between available Continue, Abort or Retry
     * actions. Call the chosen handler. If a given handler is null then that option will not be
     * offered to the user. At least one handler must be non-null.
     */
    private void showPrinterErrorOccurred(FirmwareError error) throws CalibrationException
    {
        if (printer.canCancelProperty().get())
        {
            try
            {
                printer.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Failed to cancel XY calibration print");
            }
        }

        Lookup.getSystemNotificationHandler().
            showPrinterErrorDialog(
                error.getLocalisedErrorTitle(),
                Lookup.i18n("calibrationPanel.errorInPrinter"),
                false,
                false,
                false,
                true);

        throw new CalibrationException("An error occurred with the printer");
    }
}
