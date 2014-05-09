/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.configuration.PrinterColourMap;
import celtech.configuration.WhyAreWeWaitingState;
import celtech.printerControl.Printer;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
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
    private final static PrinterColourMap colourMap = PrinterColourMap.getInstance();
    private ChangeListener<Color> printerColourChangeListener = null;
    
    public PrinterStatusListCell()
    {
        printerColourChangeListener = new ChangeListener<Color>()
        {
            @Override
            public void changed(ObservableValue<? extends Color> observable, Color oldValue, Color newValue)
            {
                printerColour.setFill(colourMap.printerToDisplayColour(newValue));
            }
        };
        
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
        printer.printerColourProperty().addListener(printerColourChangeListener);
        printerColour.setFill(colourMap.printerToDisplayColour(printer.printerColourProperty().get()));
        name.textProperty().bind(printer.printerFriendlyNameProperty());
        status.textProperty().bind(Bindings.when(printer.whyAreWeWaitingProperty().isEqualTo(WhyAreWeWaitingState.NOT_WAITING)).then(printer.printerStatusProperty().asString()).otherwise(Bindings.format("%s - %s", printer.printerStatusProperty(), printer.getWhyAreWeWaitingStringProperty())));
        setGraphic(grid);
    }
}
