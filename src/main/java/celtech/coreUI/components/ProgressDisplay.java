package celtech.coreUI.components;

import celtech.Lookup;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import javafx.scene.layout.VBox;

/**
 *
 * @author Ian
 */
public class ProgressDisplay extends VBox implements PrinterListChangesListener
{

    private Printer printerInUse = null;
    private PrintStatusBar stateDisplayBar;
    private BedHeaterStatusBar bedTemperatureDisplayBar;
    private NozzleHeaterStatusBar nozzle1TemperatureDisplayBar;
    private NozzleHeaterStatusBar nozzle2TemperatureDisplayBar;
    private PrintPreparationStatusBar printPreparationDisplayBar;

    public ProgressDisplay()
    {
        setFillWidth(true);

        Lookup.getPrinterListChangesNotifier().addListener(this);
    }

    public void bindToPrinter(Printer printer)
    {
        if (this.printerInUse != null)
        {
            unbindFromPrinter();
        }
        this.printerInUse = printer;
        stateDisplayBar = new PrintStatusBar(printer);
        printPreparationDisplayBar = new PrintPreparationStatusBar(printer);
        bedTemperatureDisplayBar = new BedHeaterStatusBar(printer.getPrinterAncillarySystems());

        getChildren().addAll(printPreparationDisplayBar, bedTemperatureDisplayBar, stateDisplayBar);
    }

    public void unbindFromPrinter()
    {
        if (printerInUse != null)
        {
            stateDisplayBar.unbindAll();
            printPreparationDisplayBar.unbindAll();
            bedTemperatureDisplayBar.unbindAll();
            getChildren().clear();
        }
        printerInUse = null;
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
        if (printer == printerInUse)
        {
            if (printer.headProperty().get().getNozzleHeaters().size() > 0)
            {
                nozzle1TemperatureDisplayBar = new NozzleHeaterStatusBar(printer.headProperty().get().getNozzleHeaters().get(0));
                getChildren().add(0, nozzle1TemperatureDisplayBar);
            }

            if (printer.headProperty().get().getNozzleHeaters().size() == 2)
            {
                nozzle2TemperatureDisplayBar = new NozzleHeaterStatusBar(printer.headProperty().get().getNozzleHeaters().get(1));
                getChildren().add(0, nozzle2TemperatureDisplayBar);
            }
        }
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
        if (nozzle1TemperatureDisplayBar != null)
        {
            getChildren().remove(nozzle1TemperatureDisplayBar);
            nozzle1TemperatureDisplayBar = null;
        }

        if (nozzle2TemperatureDisplayBar != null)
        {
            getChildren().remove(nozzle2TemperatureDisplayBar);
            nozzle2TemperatureDisplayBar = null;
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
}
