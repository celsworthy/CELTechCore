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
    
    private final ChangeListener<Head> headListener = (ObservableValue<? extends Head> ov, Head t, Head t1) ->
    {
        createNozzleHeaterBars(t1);
    };
    
    public ProgressDisplay()
    {
        setFillWidth(true);
        
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
            createNozzleHeaterBars(printer.headProperty().get());
        }
        
        getChildren().addAll(printPreparationDisplayBar, bedTemperatureDisplayBar, stateDisplayBar);
    }
    
    private void createNozzleHeaterBars(Head head)
    {
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
            printerInUse.headProperty().removeListener(headListener);
            
            stateDisplayBar.unbindAll();
            printPreparationDisplayBar.unbindAll();
            bedTemperatureDisplayBar.unbindAll();
            
            if (nozzle1TemperatureDisplayBar != null)
            {
                nozzle1TemperatureDisplayBar.unbindAll();
                nozzle1TemperatureDisplayBar = null;
            }
            
            if (nozzle2TemperatureDisplayBar != null)
            {
                nozzle2TemperatureDisplayBar.unbindAll();
                nozzle2TemperatureDisplayBar = null;
            }
            getChildren().clear();
        }
        printerInUse = null;
    }
}
