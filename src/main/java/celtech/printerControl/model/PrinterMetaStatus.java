package celtech.printerControl.model;

import celtech.Lookup;
import celtech.configuration.HeaterMode;
import celtech.printerControl.PrinterStatus;
import celtech.utils.PrinterListChangesListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PrinterMetaStatus implements PrinterListChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(PrinterMetaStatus.class.
        getName());
    // Precedence:
    // Heating
    // Printing
    private final Printer printer;
    private final ObjectProperty<PrinterStatus> printerStatus = new SimpleObjectProperty<>(
        PrinterStatus.IDLE);
    private Head attachedHead = null;
    private final ChangeListener<HeaterMode> heaterModeListener = (ObservableValue<? extends HeaterMode> observable, HeaterMode oldValue, HeaterMode newValue) ->
    {
        recalculateStatus();
    };

    private final ChangeListener<PrinterStatus> printerStatusListener = (ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue) ->
    {
        recalculateStatus();
    };

    private final StringProperty statusStringProperty = new SimpleStringProperty("");
    private final StringProperty legendProperty = new SimpleStringProperty("");
    private final DoubleProperty currentStatusValue = new SimpleDoubleProperty(0);
    private final DoubleProperty currentStatusValueTarget = new SimpleDoubleProperty(0);
    private final BooleanProperty targetValueValidProperty = new SimpleBooleanProperty(false);

    public PrinterMetaStatus(Printer printer)
    {
        this.printer = printer;

        Lookup.getPrinterListChangesNotifier().addListener(this);
        printer.printerStatusProperty().addListener(printerStatusListener);
    }

    private void recalculateStatus()
    {
        PrinterStatus tempStatus = PrinterStatus.IDLE;

        tempStatus = printer.printerStatusProperty().get();

        if (attachedHead != null)
        {
            for (NozzleHeater heater : attachedHead.getNozzleHeaters())
            {
                if (heater.heaterMode.get() != HeaterMode.OFF)
                {
                    if (tempStatus == PrinterStatus.SENDING_TO_PRINTER)
                    {
                        tempStatus = PrinterStatus.HEATING;
                    }
                }
            }
        }

        switch (tempStatus)
        {
            case PRINTING:
            case SLICING:
            case POST_PROCESSING:
            case EXECUTING_MACRO:
                bindProgressToPrimaryPrintEnginePercent();
                break;
            case HEATING:
                bindProgressForHeating();
                break;
            case SENDING_TO_PRINTER:
                bindProgressToSecondaryPrintEnginePercent();
                break;
            default:
                bindProgressDefaultCase();
                break;
        }

        if (tempStatus != printerStatus.get())
        {
            printerStatus.set(tempStatus);
        }
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
        if (printer == this.printer)
        {
            attachedHead = printer.headProperty().get();
            attachedHead.nozzleHeaters.forEach(heater ->
            {
                heater.heaterMode.addListener(heaterModeListener);
            });
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        if (printer == this.printer)
        {
            attachedHead.nozzleHeaters.forEach(heater ->
            {
                heater.heaterMode.removeListener(heaterModeListener);
            });
            attachedHead = null;
        }
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelIndex)
    {
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
    }

    @Override
    public void whenExtruderAdded(Printer printer, int extruderIndex)
    {
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {
    }

    public ReadOnlyDoubleProperty currentStatusValueProperty()
    {
        return currentStatusValue;
    }

    public ReadOnlyDoubleProperty currentStatusValueTargetProperty()
    {
        return currentStatusValueTarget;
    }

    public ReadOnlyObjectProperty<PrinterStatus> printerStatusProperty()
    {
        return printerStatus;
    }

    private void bindProgressForHeating()
    {
        unbindProgress();
        NozzleHeater heater = attachedHead.getNozzleHeaters().get(0);

        switch (heater.heaterMode.get())
        {
            case NORMAL:
                currentStatusValueTarget.bind(heater.nozzleTargetTemperature);
                break;
            case FIRST_LAYER:
                currentStatusValueTarget.bind(heater.nozzleFirstLayerTargetTemperature);
                break;
        }
        currentStatusValue.bind(heater.nozzleTemperature);
        legendProperty.set(Lookup.i18n("misc.degreesC"));
        targetValueValidProperty.set(true);
    }

    private void bindProgressToPrimaryPrintEnginePercent()
    {
        unbindProgress();
        currentStatusValueTarget.set(100);
        currentStatusValue.bind(printer.getPrintEngine().progressProperty().multiply(100));
        legendProperty.set("");
        targetValueValidProperty.set(false);
    }

    private void bindProgressToSecondaryPrintEnginePercent()
    {
        unbindProgress();
        currentStatusValueTarget.set(100);
        currentStatusValue.bind(printer.getPrintEngine().secondaryProgressProperty().multiply(100));
        legendProperty.set("");
        targetValueValidProperty.set(false);
    }

    private void bindProgressDefaultCase()
    {
        unbindProgress();
        currentStatusValue.bind(printer.getPrintEngine().progressProperty());
        legendProperty.set("");
        targetValueValidProperty.set(false);
    }

    private void unbindProgress()
    {
        currentStatusValue.unbind();
        currentStatusValueTarget.unbind();
        targetValueValidProperty.set(false);
    }

    public ReadOnlyStringProperty legendProperty()
    {
        return legendProperty;
    }

    public ReadOnlyBooleanProperty targetValueValidProperty()
    {
        return targetValueValidProperty;
    }
}
