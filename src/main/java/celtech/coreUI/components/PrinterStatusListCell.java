/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.printerControl.Printer;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author Ian
 */
public class PrinterStatusListCell extends ListCell<Printer>
{
private final static String PRINTER_STATUS_LIST_CELL_STYLE_CLASS = "printer-status-list-cell";
    private final GridPane grid = new GridPane();
    private final Rectangle printerColour = new Rectangle();
    private final Label name = new Label();
    private final Label status = new Label();

    public PrinterStatusListCell()
    {
        grid.setHgap(10);
        grid.setVgap(4);
        printerColour.setWidth(15);
        printerColour.setHeight(15);
        grid.add(printerColour, 1, 1);
        grid.add(name, 2, 1);
        grid.add(status, 3, 1);
        
        grid.getStyleClass().add(PRINTER_STATUS_LIST_CELL_STYLE_CLASS);
    }

    @Override
    protected void updateItem(Printer printer, boolean empty)
    {
        super.updateItem(printer, empty);
        if (empty)
        {
            clearContent();
        } else
        {
            addContent(printer);
        }
    }

    private void clearContent()
    {
        setText(null);
        setGraphic(null);
    }

    private void addContent(Printer printer)
    {
        setText(null);
        printerColour.fillProperty().bind(printer.printerColourProperty());
//        printerColour.setFill(printer.getPrinterColour());
        name.textProperty().bind(printer.printerFriendlyNameProperty());
//        name.setText(printer.getPrinterFriendlyName());
        status.textProperty().bind(printer.printerStatusProperty().asString());
//        status.setText(printer.getPrinterStatus().getDescription());
        setGraphic(grid);
    }
}
