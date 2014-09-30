/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.calibration;

import celtech.coreUI.StandardColours;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author tony
 */
public class CalibrationMenu extends VBox
{

    private Stenographer steno = StenographerFactory.getStenographer(CalibrationMenu.class.getName());

    private static int SQUARE_SIZE = 16;
    private static int ROW_HEIGHT = 50;

    private static String SELECTED_STYLE_CLASS = "calibrationSelectedMenuOption";

    private Text selectedItem;
    private Rectangle selectedSquare;

    @FXML
    private GridPane calibrationMenuGrid;

    /**
     * The row number of the next item to be added
     */
    private int nextRowNum = 2;
    private boolean disableNonSelectedItems;

    public CalibrationMenu()
    {
        super();
        URL fxml = getClass().getResource("/celtech/resources/fxml/calibration/calibrationMenu.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try
        {
            fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }

    }

    public void addItem(String itemName, Callable<Object> callback)
    {
        Text text = new Text(itemName);
        text.getStyleClass().add("calibrationMenuOption");
        Rectangle square = new Rectangle();
        square.getStyleClass().add("calibrationMenuSquare");
        square.setHeight(SQUARE_SIZE);
        square.setWidth(SQUARE_SIZE);
        addRow(calibrationMenuGrid, square, text);
        setUpEventHandlersForItem(square, text, callback);
    }

    private void setUpEventHandlersForItem(Rectangle square, Text itemName,
        Callable<Object> callback)
    {
        square.setVisible(false);
        itemName.setOnMouseEntered((MouseEvent e) ->
        {
            if (itemName != selectedItem && !disableNonSelectedItems)
            {
                square.setVisible(true);
                square.setFill(Color.WHITE);
            }
        });
        itemName.setOnMouseExited((MouseEvent e) ->
        {
            if (itemName != selectedItem && !disableNonSelectedItems)
            {
                square.setVisible(false);
                if (itemName.getStyleClass().contains(SELECTED_STYLE_CLASS))
                {
                    itemName.getStyleClass().remove(SELECTED_STYLE_CLASS);
                }
            }

        });
        itemName.setOnMouseClicked((MouseEvent e) ->
        {
            if (itemName != selectedItem && !disableNonSelectedItems)
            {
                if (selectedItem != null)
                {
                    deselect(selectedItem, selectedSquare);
                }

                selectedItem = itemName;
                selectedSquare = square;
                select(selectedItem, selectedSquare);
                try
                {
                    callback.call();
                } catch (Exception ex)
                {
                    steno.error("Error calling menu callback: " + ex);
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Add the given controls to a new row in the grid pane.
     */
    private void addRow(GridPane menuGrid, Rectangle square, Text itemName)
    {
        menuGrid.add(square, 0, nextRowNum);
        menuGrid.add(itemName, 1, nextRowNum);
        menuGrid.getRowConstraints().add(nextRowNum, new RowConstraints(ROW_HEIGHT, ROW_HEIGHT,
                                                                        ROW_HEIGHT));
        nextRowNum++;
    }

    private void deselect(Text selectedItem, Rectangle square)
    {
        square.setVisible(false);
        selectedItem.getStyleClass().remove(SELECTED_STYLE_CLASS);
        square.setFill(Color.WHITE);
    }

    private void select(Text selectedItem, Rectangle square)
    {
        square.setVisible(true);
        selectedItem.getStyleClass().add(SELECTED_STYLE_CLASS);
        square.setFill(StandardColours.ROBOX_BLUE);

    }

    /**
     * *
     * Disable all menu items except the currently selected item.
     */
    public void disableNonSelectedItems()
    {
        disableNonSelectedItems = true;
    }

    /**
     * *
     * Enable all menu items.
     */
    public void enableNonSelectedItems()
    {
        disableNonSelectedItems = false;
    }

    /**
     * If an item is selected then deselect it.
     */
    public void deselectSelectedItem()
    {
        if (selectedItem != null && selectedSquare != null)
        {
            deselect(selectedItem, selectedSquare);
        }
    }
}
