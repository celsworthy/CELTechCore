package celtech.coreUI.components.Notifications;

import celtech.coreUI.components.Notifications.BedHeaterStatusBar;
import celtech.coreUI.components.Notifications.PrintPreparationStatusBar;
import celtech.coreUI.components.Notifications.PrintStatusBar;
import celtech.coreUI.components.Notifications.NozzleHeaterStatusBar;
import celtech.Lookup;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.VBox;

/**
 *
 * @author Ian
 */
public class ProgressDisplay extends VBox
{

    private Printer printerInUse = null;
    private PrintStatusBar stateDisplayBar;
    private BedHeaterStatusBar bedTemperatureDisplayBar;
    private NozzleHeaterStatusBar nozzle1TemperatureDisplayBar;
    private NozzleHeaterStatusBar nozzle2TemperatureDisplayBar;
    private PrintPreparationStatusBar printPreparationDisplayBar;

    private final ChangeListener<Boolean> headDataChangedListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        createNozzleHeaterBars(printerInUse.headProperty().get());
    };

    private final ChangeListener<Head> headListener = (ObservableValue<? extends Head> ov, Head oldHead, Head newHead) ->
    {
        if (oldHead != null)
        {
            oldHead.dataChangedProperty().removeListener(headDataChangedListener);
        }

        if (newHead != null)
        {
            newHead.dataChangedProperty().removeListener(headDataChangedListener);
            newHead.dataChangedProperty().addListener(headDataChangedListener);
        }

        createNozzleHeaterBars(newHead);
    };

    public ProgressDisplay()
    {
        setFillWidth(true);
        setPickOnBounds(false);
//        setMouseTransparent(true);

        Lookup.getSelectedPrinterProperty().addListener((ObservableValue<? extends Printer> ov, Printer oldSelection, Printer newSelection) ->
        {
            unbindFromPrinter();
            if (newSelection != null)
            {
                bindToPrinter(newSelection);
            }
        });
    }

    private void bindToPrinter(Printer printer)
    {
        if (this.printerInUse != null)
        {
            unbindFromPrinter();
        }
        this.printerInUse = printer;
        stateDisplayBar = new PrintStatusBar(printer);
        printPreparationDisplayBar = new PrintPreparationStatusBar(printer);
        bedTemperatureDisplayBar = new BedHeaterStatusBar(printer.getPrinterAncillarySystems());

        printer.headProperty().addListener(headListener);
        if (printer.headProperty().get() != null)
        {
            printerInUse.headProperty().get().dataChangedProperty().addListener(headDataChangedListener);
            createNozzleHeaterBars(printer.headProperty().get());
        }

        getChildren().addAll(printPreparationDisplayBar, bedTemperatureDisplayBar, stateDisplayBar);
    }

    private void destroyNozzleHeaterBars()
    {
        if (nozzle1TemperatureDisplayBar != null)
        {
            nozzle1TemperatureDisplayBar.unbindAll();
            getChildren().remove(nozzle1TemperatureDisplayBar);
            nozzle1TemperatureDisplayBar = null;
        }

        if (nozzle2TemperatureDisplayBar != null)
        {
            nozzle2TemperatureDisplayBar.unbindAll();
            getChildren().remove(nozzle2TemperatureDisplayBar);
            nozzle2TemperatureDisplayBar = null;
        }
    }

    private void createNozzleHeaterBars(Head head)
    {
        destroyNozzleHeaterBars();
        if (head != null
                && head.getNozzleHeaters().size() > 0)
        {
            nozzle1TemperatureDisplayBar = new NozzleHeaterStatusBar(head.getNozzleHeaters().get(0), 0, head.getNozzleHeaters().size() == 1);
            getChildren().add(0, nozzle1TemperatureDisplayBar);
        }

        if (head != null
                && head.getNozzleHeaters().size() == 2)
        {
            nozzle2TemperatureDisplayBar = new NozzleHeaterStatusBar(head.getNozzleHeaters().get(1), 1, false);
            getChildren().add(0, nozzle2TemperatureDisplayBar);
        }
    }

    private void unbindFromPrinter()
    {
        if (printerInUse != null)
        {
            if (printerInUse.headProperty().get() != null)
            {
                printerInUse.headProperty().get().dataChangedProperty().removeListener(headDataChangedListener);
                printerInUse.headProperty().removeListener(headListener);
            }

            destroyNozzleHeaterBars();
            stateDisplayBar.unbindAll();
            printPreparationDisplayBar.unbindAll();
            bedTemperatureDisplayBar.unbindAll();

            getChildren().clear();
        }
        printerInUse = null;
    }
}
