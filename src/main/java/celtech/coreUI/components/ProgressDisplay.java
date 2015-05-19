package celtech.coreUI.components;

import celtech.Lookup;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterMetaStatus;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.VBox;

/**
 *
 * @author Ian
 */
public class ProgressDisplay extends VBox
{

    private Printer printer = null;
    private PrinterMetaStatus printerMetaStatus = null;
    private final AppearingProgressBar generalPurposeProgressBar = new AppearingProgressBar();
    private final AppearingProgressBar transferringDataProgressBar = new AppearingProgressBar();

    private final ChangeListener<PrinterStatus> statusChangeListener = (ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue) ->
    {
        respondToStateChange();
    };

    private final ChangeListener<Boolean> ancillaryStatusListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        respondToStateChange();
    };

    public ProgressDisplay()
    {
        setFillWidth(true);
        getChildren().addAll(transferringDataProgressBar, generalPurposeProgressBar);
    }

    public void bindToPrinter(Printer printer)
    {
        this.printer = printer;
        printerMetaStatus = printer.getPrinterMetaStatus();
        printerMetaStatus.printerStatusProperty().addListener(statusChangeListener);
        printer.getPrintEngine().transferGCodeToPrinterService.runningProperty().addListener(ancillaryStatusListener);
        printer.getPrintEngine().slicerService.runningProperty().addListener(ancillaryStatusListener);
        printer.getPrintEngine().postProcessorService.runningProperty().addListener(ancillaryStatusListener);
        respondToStateChange();
    }

    public void unbindFromPrinter()
    {
        if (printerMetaStatus != null)
        {
            printerMetaStatus.printerStatusProperty().removeListener(statusChangeListener);
            this.printerMetaStatus = null;
        }
        respondToStateChange();

        if (printer != null)
        {
            printer.getPrintEngine().transferGCodeToPrinterService.runningProperty().removeListener(ancillaryStatusListener);
            printer.getPrintEngine().slicerService.runningProperty().removeListener(ancillaryStatusListener);
            printer.getPrintEngine().postProcessorService.runningProperty().removeListener(ancillaryStatusListener);
        }
        respondToStateChange();
        printer = null;
    }

    private void respondToStateChange()
    {
        boolean dontShowTransferringBar = true;

        if (printerMetaStatus != null)
        {
            generalPurposeProgressBar.configureForStatus(printerMetaStatus.printerStatusProperty().get(),
                    printerMetaStatus.currentStatusValueProperty(), printerMetaStatus.currentStatusValueTargetProperty(),
                    printer.getPrintEngine().progressETCProperty());

            if (printer != null)
            {
                if (printer.getPrintEngine().transferGCodeToPrinterService.isRunning()
                        && (printerMetaStatus.printerStatusProperty().get() == PrinterStatus.PRINTING
                        || printerMetaStatus.printerStatusProperty().get() == PrinterStatus.HEATING_BED
                        || printerMetaStatus.printerStatusProperty().get() == PrinterStatus.HEATING_NOZZLE))
                {
                    dontShowTransferringBar = false;
                    transferringDataProgressBar.manuallyConfigure(Lookup.i18n("printerStatus.sendingToPrinter"),
                            printer.getPrintEngine().transferGCodeToPrinterService.workDoneProperty(),
                            printer.getPrintEngine().transferGCodeToPrinterService.totalWorkProperty());
                } else if (printer.getPrintEngine().slicerService.isRunning())
                {
                    dontShowTransferringBar = false;
                    transferringDataProgressBar.manuallyConfigure(Lookup.i18n("printerStatus.slicing"),
                            printer.getPrintEngine().slicerService.workDoneProperty(),
                            printer.getPrintEngine().slicerService.totalWorkProperty());
                } else if (printer.getPrintEngine().postProcessorService.isRunning())
                {
                    dontShowTransferringBar = false;
                    transferringDataProgressBar.manuallyConfigure(Lookup.i18n("printerStatus.postProcessing"),
                            printer.getPrintEngine().postProcessorService.workDoneProperty(),
                            printer.getPrintEngine().postProcessorService.totalWorkProperty());
                }
            }

        } else
        {
            //OK need to do some tidying
            generalPurposeProgressBar.configureForStatus(null, null, null, null);
        }

        if (dontShowTransferringBar)
        {
            transferringDataProgressBar.manuallyConfigure(null, null, null);
        }
    }
}
