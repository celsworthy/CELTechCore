/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.Lookup;
import celtech.printerControl.model.Printer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;

/**
 *
 * @author tony
 */
public class PrintPreparationStatusBar extends AppearingProgressBar implements Initializable
{

    private Printer printer = null;

    private final ChangeListener<Boolean> serviceStatusListener = (ObservableValue<? extends Boolean> ov, Boolean lastState, Boolean newState) ->
    {
        reassessStatus();
    };

    public PrintPreparationStatusBar(Printer printer)
    {
        super();
        this.printer = printer;

        printer.getPrintEngine().slicerService.runningProperty().addListener(serviceStatusListener);
        printer.getPrintEngine().postProcessorService.runningProperty().addListener(serviceStatusListener);
        printer.getPrintEngine().transferGCodeToPrinterService.runningProperty().addListener(serviceStatusListener);

        reassessStatus();
    }

    private void reassessStatus()
    {
        boolean barShouldBeDisplayed = false;

        unbindVariables();

        if (printer.getPrintEngine().slicerService.runningProperty().get())
        {
            barShouldBeDisplayed = true;
            largeProgressDescription.setText(Lookup.i18n("printerStatus.slicing"));

            largeProgressCurrentValue.textProperty().bind(printer.getPrintEngine().slicerService.progressProperty().multiply(100).asString("%.0f%%"));
            largeProgressCurrentValue.setVisible(true);

            progressBar.progressProperty().bind(printer.getPrintEngine().slicerService.progressProperty());
            progressBar.setVisible(true);
        } else if (printer.getPrintEngine().postProcessorService.runningProperty().get())
        {
            barShouldBeDisplayed = true;
            largeProgressDescription.setText(Lookup.i18n("printerStatus.postProcessing"));

            largeProgressCurrentValue.textProperty().bind(printer.getPrintEngine().postProcessorService.progressProperty().multiply(100).asString("%.0f%%"));
            largeProgressCurrentValue.setVisible(true);

            progressBar.progressProperty().bind(printer.getPrintEngine().postProcessorService.progressProperty());
            progressBar.setVisible(true);
        } else if (printer.getPrintEngine().transferGCodeToPrinterService.runningProperty().get()
                && printer.getPrintEngine().macroBeingRun.get() == null)
        {
            barShouldBeDisplayed = true;
            largeProgressDescription.setText(Lookup.i18n("printerStatus.sendingToPrinter"));

            largeProgressCurrentValue.textProperty().bind(printer.getPrintEngine().transferGCodeToPrinterService.progressProperty().multiply(100).asString("%.0f%%"));
            largeProgressCurrentValue.setVisible(true);

            progressBar.progressProperty().bind(printer.getPrintEngine().transferGCodeToPrinterService.progressProperty());
            progressBar.setVisible(true);
        }

        if (barShouldBeDisplayed)
        {
            startSlidingInToView();
        } else
        {
            startSlidingOutOfView();
        }
    }

    public void unbindAll()
    {
        if (printer != null)
        {
            printer.getPrintEngine().slicerService.runningProperty().removeListener(serviceStatusListener);
            printer.getPrintEngine().postProcessorService.runningProperty().removeListener(serviceStatusListener);
            printer.getPrintEngine().transferGCodeToPrinterService.runningProperty().removeListener(serviceStatusListener);
            unbindVariables();
            slideOutOfView();
            printer = null;
        }
    }
}
