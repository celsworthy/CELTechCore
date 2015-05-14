package celtech.coreUI.components;

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
        respondToMetaStateChange(newValue);
    };

    private final ChangeListener<Boolean> sendingDataChangeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        respondToSendingDataChange(newValue);
    };

    public ProgressDisplay()
    {
        generalPurposeProgressBar.slideIn();
        transferringDataProgressBar.slideIn();
        getChildren().addAll(generalPurposeProgressBar);
    }

    public void bindToPrinter(Printer printer)
    {
//        unbindProgress();

        this.printer = printer;
        printerMetaStatus = printer.getPrinterMetaStatus();
        printerMetaStatus.printerStatusProperty().addListener(statusChangeListener);
        printer.getPrintEngine().sendingDataToPrinterProperty().addListener(sendingDataChangeListener);
        respondToMetaStateChange(printerMetaStatus.printerStatusProperty().get());

//        largeProgressDescription.textProperty().bind(printerMetaStatus.printerStatusProperty().
//                asString());
//        largeTargetValue.setText("");
//        largeTargetLegend.setText("");
//        largeProgressCurrentValue.setText("");
//        progressProperty.bind(printerMetaStatus.currentStatusValueProperty());
//        progressProperty.addListener(progressChangeListener);
    }

    public void unbindFromPrinter()
    {
        if (printerMetaStatus != null)
        {
            printerMetaStatus.printerStatusProperty().removeListener(statusChangeListener);
            this.printerMetaStatus = null;
        }
        respondToMetaStateChange(null);

        if (printer != null)
        {
            printer.getPrintEngine().sendingDataToPrinterProperty().removeListener(sendingDataChangeListener);
        }
        respondToSendingDataChange(false);
        printer = null;
    }

    private void respondToMetaStateChange(PrinterStatus printerStatus)
    {
        if (printerStatus != null)
        {
            switch (printerStatus)
            {
                case IDLE:
                    generalPurposeProgressBar.startSlidingIn();
                    break;
                default:
                    generalPurposeProgressBar.startSlidingOut();
                    System.out.println("Progress Display status --- " + printerStatus.name());
                    break;
            }
        } else
        {
            //OK need to do some tidying
        }
    }

    private void respondToSendingDataChange(boolean sendingData)
    {
        if (printer == null
                || !sendingData)
        {
            transferringDataProgressBar.slideIn();
        }
    }
    
    public void forceAppear()
    {
        generalPurposeProgressBar.startSlidingOut();
    }
    public void forceDisappear()
    {
        generalPurposeProgressBar.startSlidingIn();
    }
}
