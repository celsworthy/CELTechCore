/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.Notifications;

import celtech.Lookup;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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

    private final ChangeListener<Number> serviceProgressListener = (ObservableValue<? extends Number> ov, Number lastState, Number newState) ->
    {
        reassessStatus();
    };

    private final BooleanProperty cancelAllowed = new SimpleBooleanProperty(false);

    private final EventHandler<ActionEvent> cancelEventHandler = new EventHandler<ActionEvent>()
    {
        @Override
        public void handle(ActionEvent t)
        {
            try
            {
                printer.cancel(null, Lookup.getUserPreferences().isSafetyFeaturesOn());
            } catch (PrinterException ex)
            {
                System.out.println("Couldn't resume print");
            }
        }
    };

    public PrintPreparationStatusBar(Printer printer)
    {
        super();
        this.printer = printer;

//        printer.getPrintEngine().slicerService.runningProperty().addListener(serviceStatusListener);
//        printer.getPrintEngine().slicerService.progressProperty().addListener(serviceProgressListener);
//        printer.getPrintEngine().postProcessorService.runningProperty().addListener(serviceStatusListener);
//        printer.getPrintEngine().postProcessorService.progressProperty().addListener(serviceProgressListener);
        printer.getPrintEngine().transferGCodeToPrinterService.runningProperty().addListener(serviceStatusListener);
        printer.getPrintEngine().transferGCodeToPrinterService.progressProperty().addListener(serviceProgressListener);

        cancelButton.visibleProperty().bind(printer.canCancelProperty().and(cancelAllowed));
        cancelButton.setOnAction(cancelEventHandler);

        getStyleClass().add("secondaryStatusBar");

        reassessStatus();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        super.initialize(location, resources);
        targetLegendRequired(false);
        targetValueRequired(false);
        currentValueRequired(false);
        progressRequired(true);
        layerDataRequired(false);
    }

    private void reassessStatus()
    {
        boolean showBar = false;

//        if (printer.getPrintEngine().slicerService.runningProperty().get())
//        {
//            largeProgressDescription.setText(Lookup.i18n("printerStatus.slicing"));
//            progressBar.setProgress(printer.getPrintEngine().slicerService.getProgress());
//            cancelAllowed.set(true);
//            showBar = true;
//        } else if (printer.getPrintEngine().postProcessorService.runningProperty().get())
//        {
//            largeProgressDescription.setText(Lookup.i18n("printerStatus.postProcessing"));
//            progressBar.setProgress(printer.getPrintEngine().postProcessorService.getProgress());
//            cancelAllowed.set(true);
//            showBar = true;
//        } else 
        if (printer.getPrintEngine().transferGCodeToPrinterService.runningProperty().get())
        {
            largeProgressDescription.setText(Lookup.i18n("printerStatus.sendingToPrinter"));
            progressBar.setProgress(printer.getPrintEngine().transferGCodeToPrinterService.getProgress());
            //Cancel is provided from the print bar in this mode
            cancelAllowed.set(false);
            showBar = true;
        }

        if (showBar)
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
//            printer.getPrintEngine().slicerService.runningProperty().removeListener(serviceStatusListener);
//            printer.getPrintEngine().slicerService.progressProperty().removeListener(serviceProgressListener);
//            printer.getPrintEngine().postProcessorService.runningProperty().removeListener(serviceStatusListener);
//            printer.getPrintEngine().postProcessorService.progressProperty().removeListener(serviceProgressListener);
            printer.getPrintEngine().transferGCodeToPrinterService.runningProperty().removeListener(serviceStatusListener);
            printer.getPrintEngine().transferGCodeToPrinterService.progressProperty().removeListener(serviceProgressListener);
            printer = null;
        }
    }
}
