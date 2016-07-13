package celtech.coreUI.components.Notifications;

import celtech.Lookup;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
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
    private MaterialHeatingStatusBar material1TemperatureDisplayBar;
    private MaterialHeatingStatusBar material2TemperatureDisplayBar;
    private PrintPreparationStatusBar printPreparationDisplayBar;

    private final ChangeListener<Boolean> headDataChangedListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        createMaterialHeatBars(printerInUse.headProperty().get());
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

        createMaterialHeatBars(newHead);
    };

    public ProgressDisplay()
    {
        setFillWidth(true);
        setPickOnBounds(false);

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
            createMaterialHeatBars(printer.headProperty().get());
        }

        getChildren().addAll(printPreparationDisplayBar, bedTemperatureDisplayBar, stateDisplayBar);
    }

    private void destroyMaterialHeatBars()
    {
        if (material1TemperatureDisplayBar != null)
        {
            material1TemperatureDisplayBar.unbindAll();
            getChildren().remove(material1TemperatureDisplayBar);
            material1TemperatureDisplayBar = null;
        }

        if (material2TemperatureDisplayBar != null)
        {
            material2TemperatureDisplayBar.unbindAll();
            getChildren().remove(material2TemperatureDisplayBar);
            material2TemperatureDisplayBar = null;
        }
    }

    private void createMaterialHeatBars(Head head)
    {
        destroyMaterialHeatBars();
        if (head != null
                && head.getNozzleHeaters().size() > 0)
        {
            int materialNumber = 1;
            if (head.headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD)
            {
                materialNumber = 2;
        }

            material1TemperatureDisplayBar = new MaterialHeatingStatusBar(head.getNozzleHeaters().get(0), materialNumber, head.getNozzleHeaters().size() == 1);
            getChildren().add(0, material1TemperatureDisplayBar);
        }

        if (head != null
                && head.getNozzleHeaters().size() == 2)
        {
            //Must be DM - material 1
            material2TemperatureDisplayBar = new MaterialHeatingStatusBar(head.getNozzleHeaters().get(1), 1, false);
            getChildren().add(0, material2TemperatureDisplayBar);
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

            destroyMaterialHeatBars();
            stateDisplayBar.unbindAll();
            printPreparationDisplayBar.unbindAll();
            bedTemperatureDisplayBar.unbindAll();

            getChildren().clear();
        }
        printerInUse = null;
    }
}
