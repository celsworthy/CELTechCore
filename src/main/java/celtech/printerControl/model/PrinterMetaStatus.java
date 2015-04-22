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

/**
 *
 * @author Ian
 */
public class PrinterMetaStatus implements PrinterListChangesListener
{

    // Precedence:
    // Heating
    // Printing
    private final Printer printer;
    private final ObjectProperty<PrinterStatus> printerStatus = new SimpleObjectProperty<>(null);
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
    private final BooleanProperty targetValueVisibleProperty = new SimpleBooleanProperty(false);

    public PrinterMetaStatus(Printer printer)
    {
        this.printer = printer;

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
                    tempStatus = PrinterStatus.HEATING;
                }
            }
        }

        switch (tempStatus)
        {
            case HEATING:
                bindProgressForHeating();
                break;
            case PRINTING:
                bindProgressForPrinting();
                break;
            case EXECUTING_MACRO:
                bindProgressForMacro();
//                statusStringProperty.set(printer.getPrintEngine().)
            default:
                unbindProgress();
                statusStringProperty.set(tempStatus.getI18nString());
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
        targetValueVisibleProperty.set(true);
    }

    private void bindProgressForPrinting()
    {
        unbindProgress();
        currentStatusValueTarget.set(100);
        currentStatusValue.bind(printer.getPrintEngine().progressProperty());
        legendProperty.set("");
        targetValueVisibleProperty.set(false);
    }

    private void bindProgressForMacro()
    {
        unbindProgress();
        currentStatusValueTarget.set(100);
        currentStatusValue.bind(printer.getPrintEngine().progressProperty());
        legendProperty.set("");
        targetValueVisibleProperty.set(false);
    }

    private void unbindProgress()
    {
        currentStatusValue.unbind();
        currentStatusValueTarget.unbind();
        targetValueVisibleProperty.set(false);
    }

    public ReadOnlyStringProperty legendProperty()
    {
        return legendProperty;
    }

    public ReadOnlyBooleanProperty targetValueVisibleProperty()
    {
        return targetValueVisibleProperty;
    }
}
