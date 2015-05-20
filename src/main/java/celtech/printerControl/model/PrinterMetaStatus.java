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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
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

    private final ChangeListener<Number> numberValueListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        recalculateStatus();
    };

    private final ChangeListener<HeaterMode> heaterModeListener = (ObservableValue<? extends HeaterMode> observable, HeaterMode oldValue, HeaterMode newValue) ->
    {
        recalculateStatus();
    };

    private final ChangeListener<PrinterStatus> printerStatusListener = (ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue) ->
    {
        recalculateStatus();
    };

    private final ChangeListener<Boolean> booleanTriggerListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        recalculateStatus();
    };

    private final DoubleProperty currentStatusValue = new SimpleDoubleProperty(0);
    private final DoubleProperty currentStatusValueTarget = new SimpleDoubleProperty(0);
    private final BooleanProperty targetValueValidProperty = new SimpleBooleanProperty(false);

    private final int temperatureBoxForHeatingMode = 4;

    public PrinterMetaStatus(Printer printer)
    {
        this.printer = printer;

        Lookup.getPrinterListChangesNotifier().addListener(this);
        printer.printerStatusProperty().addListener(printerStatusListener);

        printer.getPrintEngine().slicerService.runningProperty().addListener(booleanTriggerListener);
        printer.getPrintEngine().postProcessorService.runningProperty().addListener(
                booleanTriggerListener);
        printer.getPrintEngine().transferGCodeToPrinterService.runningProperty().addListener(
                booleanTriggerListener);
        printer.getPrintEngine().printInProgressProperty().addListener(booleanTriggerListener);
        printer.getPrinterAncillarySystems().bedHeaterMode.addListener(heaterModeListener);
    }

    private void recalculateStatus()
    {
        PrinterStatus tempStatus = null;

        if (attachedHead != null && printerStatus.get() != PrinterStatus.EJECTING_FILAMENT)
        {
            for (NozzleHeater heater : attachedHead.getNozzleHeaters())
            {
                switch (heater.heaterMode.get())
                {
                    case FIRST_LAYER:
                        if (Math.abs(heater.nozzleFirstLayerTargetTemperature.get()
                                - heater.nozzleTemperature.get())
                                > temperatureBoxForHeatingMode)
                        {
                            tempStatus = PrinterStatus.HEATING_NOZZLE;
                        }
                        break;
                    case NORMAL:
                        if (Math.abs(heater.nozzleTargetTemperature.get()
                                - heater.nozzleTemperature.get())
                                > temperatureBoxForHeatingMode)
                        {
                            tempStatus = PrinterStatus.HEATING_NOZZLE;
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        if (tempStatus == null)
        {
            switch (printer.getPrinterAncillarySystems().bedHeaterMode.get())
            {
                case FIRST_LAYER:
                    if (Math.abs(printer.getPrinterAncillarySystems().bedTemperature.get()
                            - printer.getPrinterAncillarySystems().bedFirstLayerTargetTemperature.get())
                            > temperatureBoxForHeatingMode)
                    {
                        tempStatus = PrinterStatus.HEATING_BED;
                    }
                    break;
                case NORMAL:
                    if (Math.abs(printer.getPrinterAncillarySystems().bedTemperature.get()
                            - printer.getPrinterAncillarySystems().bedTargetTemperature.get())
                            > temperatureBoxForHeatingMode)
                    {
                        tempStatus = PrinterStatus.HEATING_BED;
                    }
                    break;
                default:
                    break;
            }
        }

        if (tempStatus == null)
        {
////            if (printer.printerStatusProperty().get() == PrinterStatus.IDLE
////                    && printer.getPrintEngine().slicerService.isRunning())
////            {
////                tempStatus = PrinterStatus.SLICING;
////            } else if (printer.printerStatusProperty().get() == PrinterStatus.IDLE
////                    && printer.getPrintEngine().postProcessorService.isRunning())
////            {
////                tempStatus = PrinterStatus.POST_PROCESSING;
            if (printer.printerStatusProperty().get() == PrinterStatus.IDLE
                    && printer.getPrintEngine().printInProgressProperty().get())
            {
                tempStatus = PrinterStatus.PRINTING;
            }
        }

        if (tempStatus == null)
        {
            tempStatus = printer.printerStatusProperty().get();
        }

        switch (tempStatus)
        {
            case PRINTING:
            case SLICING:
            case POST_PROCESSING:
                bindProgressToPrimaryPrintEnginePercent();
                break;
            case HEATING_BED:
                bindProgressForBedHeating();
                break;
            case HEATING_NOZZLE:
                bindProgressForNozzleHeating();
                break;
//            case SENDING_TO_PRINTER:
//                bindProgressToSecondaryPrintEnginePercent();
//                break;
            default:
                bindProgressDefaultCase();
                break;
        }

        if (tempStatus != printerStatus.get())
        {
            printerStatus.set(tempStatus);
            steno.debug("Meta status is now " + tempStatus.getI18nString());
        }
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
        printer.getPrinterAncillarySystems().bedTemperature.addListener(numberValueListener);
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        printer.getPrinterAncillarySystems().bedTemperature.removeListener(numberValueListener);
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
                heater.nozzleTemperature.addListener(numberValueListener);
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
                heater.nozzleTemperature.removeListener(numberValueListener);
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

    private void bindProgressForBedHeating()
    {
        unbindProgress();
        switch (printer.getPrinterAncillarySystems().bedHeaterMode.get())
        {
            case NORMAL:
                currentStatusValueTarget.bind(
                        printer.getPrinterAncillarySystems().bedTargetTemperature);
                break;
            case FIRST_LAYER:
                currentStatusValueTarget.bind(
                        printer.getPrinterAncillarySystems().bedFirstLayerTargetTemperature);
                break;
        }
        currentStatusValue.bind(printer.getPrinterAncillarySystems().bedTemperature);
        targetValueValidProperty.set(true);
    }

    private void bindProgressForNozzleHeating()
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
        targetValueValidProperty.set(true);
    }

    private void bindProgressToPrimaryPrintEnginePercent()
    {
        unbindProgress();
        currentStatusValueTarget.set(100);
        currentStatusValue.bind(printer.getPrintEngine().progressProperty().multiply(100));
        targetValueValidProperty.set(false);
    }

    private void bindProgressToSecondaryPrintEnginePercent()
    {
        unbindProgress();
        currentStatusValueTarget.set(100);
        currentStatusValue.bind(printer.getPrintEngine().secondaryProgressProperty().multiply(100));
        targetValueValidProperty.set(false);
    }

    private void bindProgressDefaultCase()
    {
        unbindProgress();
//        currentStatusValue.bind(printer.getPrintEngine().progressProperty());
        targetValueValidProperty.set(false);
    }

    private void unbindProgress()
    {
        currentStatusValue.unbind();
        currentStatusValueTarget.unbind();
        targetValueValidProperty.set(false);
    }

    public ReadOnlyBooleanProperty targetValueValidProperty()
    {
        return targetValueValidProperty;
    }
}
