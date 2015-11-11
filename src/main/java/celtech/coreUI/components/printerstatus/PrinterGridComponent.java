/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.components.printerstatus;

import celtech.Lookup;
import celtech.configuration.PrinterColourMap;
import celtech.coreUI.components.PrinterIDDialog;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterException;
import celtech.printerControl.model.PrinterIdentity;
import celtech.printerControl.model.Reel;
import celtech.utils.PrinterListChangesListener;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * This component houses a square grid of PrinterComponents and is used a
 * printer selector.
 *
 * @author tony
 */
public class PrinterGridComponent extends GridPane implements PrinterListChangesListener
{

    private ObservableList<Printer> connectedPrinters;
    private final Map<Printer, PrinterComponent> printerComponentsByPrinter = new HashMap<>();
    private ObjectProperty<Printer> selectedPrinter = new SimpleObjectProperty<>();
    private PrinterIDDialog printerIDDialog = null;

    public PrinterGridComponent()
    {
        try
        {
            connectedPrinters = Lookup.getConnectedPrinters();
            Lookup.getPrinterListChangesNotifier().addListener(this);
        } catch (NoClassDefFoundError error)
        {
            // this should only happen in SceneBuilder
            connectedPrinters = new SimpleListProperty<>();
        }
        printerIDDialog = new PrinterIDDialog();
        clearAndAddAllPrintersToGrid();
    }

    public ReadOnlyObjectProperty<Printer> getSelectedPrinter()
    {
        return selectedPrinter;
    }

    /**
     * Add the given printer component to the given grid coordinates.
     */
    private void addPrinterComponentToGrid(PrinterComponent printerComponent, int row,
            int column)
    {
        PrinterComponent.Size size;
        if (connectedPrinters.size() > 6)
        {
            size = PrinterComponent.Size.SIZE_SMALL;
        } else if (connectedPrinters.size() > 1)
        {
            size = PrinterComponent.Size.SIZE_MEDIUM;
        } else
        {
            size = PrinterComponent.Size.SIZE_LARGE;
        }
        printerComponent.setSize(size);
        add(printerComponent, column, row);
    }

    private void removeAllPrintersFromGrid()
    {
        for (Printer printer : connectedPrinters)
        {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
            removePrinterComponentFromGrid(printerComponent);
        }
    }

    /**
     * Remove the given printer from the grid.
     *
     * @param printerComponent
     */
    private void removePrinterComponentFromGrid(PrinterComponent printerComponent)
    {
        getChildren().remove(printerComponent);
    }

    /**
     * Remove the given printer from the display. Update the selected printer to
     * one of the remaining printers.
     */
    public void removePrinter(Printer printer)
    {
        PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
        removePrinterComponentFromGrid(printerComponent);
    }

    public final void clearAndAddAllPrintersToGrid()
    {
        removeAllPrintersFromGrid();
        int row = 0;
        int column = 0;
        int columnsPerRow = 2;
        if (connectedPrinters.size() > 4)
        {
            columnsPerRow = 3;
        }

        if (connectedPrinters.size() > 0)
        {
            for (Printer printer : connectedPrinters)
            {
                PrinterComponent printerComponent = createPrinterComponentForPrinter(printer);
                addPrinterComponentToGrid(printerComponent, row, column);
                column += 1;
                if (column == columnsPerRow)
                {
                    column = 0;
                    row += 1;
                }
            }
        } else
        {
            PrinterComponent printerComponent = createPrinterComponentForPrinter(null);
            addPrinterComponentToGrid(printerComponent, row, column);
        }
        // UGH shouldnt need this here but can't get PrinterComponent / Grid to negotiate size
        if (connectedPrinters.size() > 1 && connectedPrinters.size() <= 2)
        {
            setPrefSize(260, 120);
        } else if (connectedPrinters.size() > 2 && connectedPrinters.size() <= 4)
        {
            setPrefSize(260, 260);
        } else if (connectedPrinters.size() > 4 && connectedPrinters.size() < 7)
        {
            setPrefSize(260, 180);
        } else
        {
            setPrefSize(260, 260);
        }
    }

    /**
     * Create the PrinterComponent for the given printer and set up any
     * listeners on component events.
     */
    private PrinterComponent createPrinterComponentForPrinter(Printer printer)
    {
        PrinterComponent printerComponent = new PrinterComponent(printer);
        printerComponent.setOnMouseClicked((MouseEvent event) ->
        {
            handlePrinterClicked(event, printer);
        });
        printerComponentsByPrinter.put(printer, printerComponent);
        return printerComponent;
    }

    /**
     * This is called when the user clicks on the printer component for the
     * given printer, and handles click (select printer) and double-click (go to
     * edit printer details).
     *
     * @param event
     */
    private void handlePrinterClicked(MouseEvent event, Printer printer)
    {
        if (event.getClickCount() == 1)
        {
            selectPrinter(printer);
        }
        if (event.getClickCount() > 1)
        {
            showEditPrinterDetails(printer);
        }
    }

    private void selectPrinter(Printer printer)
    {
        if (selectedPrinter.get() != null)
        {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(selectedPrinter.get());
            printerComponent.setSelected(false);
        }
        if (printer != null)
        {
            PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
            printerComponent.setSelected(true);
        }
        selectedPrinter.set(printer);
    }

    /**
     * Show the printerIDDialog for the given printer.
     */
    private void showEditPrinterDetails(Printer printer)
    {
        Stenographer steno = StenographerFactory.getStenographer(
                PrinterGridComponent.class.getName());
        PrinterColourMap colourMap = PrinterColourMap.getInstance();
        if (printer != null)
        {
            printerIDDialog.setPrinterToUse(printer);
            PrinterIdentity printerIdentity = printer.getPrinterIdentity();
            printerIDDialog.setChosenDisplayColour(colourMap.printerToDisplayColour(
                    printerIdentity.printerColourProperty().get()));
            printerIDDialog.
                    setChosenPrinterName(printerIdentity.printerFriendlyNameProperty().get());

            boolean okPressed = printerIDDialog.show();

            if (okPressed)
            {
                try
                {
                    printer.updatePrinterName(printerIDDialog.getChosenPrinterName());
                    printer.updatePrinterDisplayColour(colourMap.displayToPrinterColour(
                            printerIDDialog.getChosenDisplayColour()));
                } catch (PrinterException ex)
                {
                    steno.error("Error writing printer ID");
                }
            }
        }
    }

    /**
     * Select any one of the active printers. If there are no printers left then
     * select 'null'
     */
    private void selectOnePrinter()
    {
        if (connectedPrinters.size() > 0)
        {
            selectPrinter(connectedPrinters.get(0));
        } else
        {
            selectPrinter(null);
            Lookup.setSelectedPrinter(null);
        }
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
        clearAndAddAllPrintersToGrid();
        selectPrinter(printer);
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
        removePrinter(printer);
        clearAndAddAllPrintersToGrid();
        selectOnePrinter();
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
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
