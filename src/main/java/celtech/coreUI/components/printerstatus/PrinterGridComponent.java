/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.components.printerstatus;

import celtech.Lookup;
import celtech.printerControl.model.Printer;
import java.util.HashMap;
import java.util.Map;
import javafx.collections.ObservableList;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

/**
 * This component houses a square grid of PrinterComponents.
 *
 * @author tony
 */
public class PrinterGridComponent extends GridPane
{

    private final ObservableList<Printer> connectedPrinters = Lookup.getConnectedPrinters();
    private Printer selectedPrinter = null;
    private final Map<Printer, PrinterComponent> printerComponentsByPrinter = new HashMap<>();

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
     * Remove the given printer from the display. Update the selected printer to one of the
     * remaining printers.
     */
    public void removePrinter(Printer printer)
    {
        PrinterComponent printerComponent = printerComponentsByPrinter.get(printer);
        removePrinterComponentFromGrid(printerComponent);
    }

    public void clearAndAddAllPrintersToGrid()
    {
        removeAllPrintersFromGrid();
        int row = 0;
        int column = 0;
        int columnsPerRow = 2;
        if (connectedPrinters.size() > 4)
        {
            columnsPerRow = 3;
        }
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
     * Create the PrinterComponent for the given printer and set up any listeners on component
     * events.
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
     * This is called when the user clicks on the printer component for the given printer, and
     * handles click (select printer) and double-click (go to edit printer details).
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

    // To be passed back to owner
    private void selectPrinter(Printer printer)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void showEditPrinterDetails(Printer printer)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
