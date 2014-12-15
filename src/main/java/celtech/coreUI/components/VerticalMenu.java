/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.coreUI.StandardColours;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 *
 * @author tony
 */
public class VerticalMenu extends VBox
{

    private static final int SQUARE_SIZE = 16;
    private static final int ROW_HEIGHT = 50;

    private Text selectedItem;
    private Rectangle selectedSquare;

    @FXML
    private GridPane verticalMenuGrid;

    @FXML
    private Text verticalMenuTitle;

    /**
     * The row number of the next item to be added
     */
    private int nextRowNum = 2;
    private boolean disableNonSelectedItems;

    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
    private Set<Text> allItems = new HashSet<>();

    private boolean firstItemInitialised = false;
    private Text firstItem;
    private Rectangle firstSquare;
    private Callable firstCallable;

    class Item
    {

        Text item;
        Rectangle square;

        public Item(Text item, Rectangle square)
        {
            this.item = firstItem;
            this.square = firstSquare;
        }
        
        ChangeListener<Boolean> enabledListener = new ChangeListener<Boolean>()
            {
                public void changed(ObservableValue<? extends Boolean> observable,
                    Boolean oldValue, Boolean newValue)
                {
                    whenEnabledChanged(newValue);
                }
            };

        private void whenEnabledChanged(Boolean newValue)
        {
            item.disableProperty().set(!newValue);
            square.disableProperty().set(!newValue);
        }

        private void setEnabledPredicate(BooleanProperty enabledPredicate)
        {
            enabledPredicate.addListener(enabledListener);
            whenEnabledChanged(enabledPredicate.get());
        }
    }

    public VerticalMenu()
    {
        super();
        URL fxml = getClass().getResource("/celtech/resources/fxml/components/verticalMenu.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.setClassLoader(getClass().getClassLoader());

        try
        {
            fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }

    }

    public void setTitle(String title)
    {
        verticalMenuTitle.setText(title);
    }

    public void addItem(String itemName, Callable<Object> callback, BooleanProperty enabledPredicate)
    {
        Item item = addItem(itemName, callback);
        if (enabledPredicate != null)
        {
            item.setEnabledPredicate(enabledPredicate);
        }
    }

    public Item addItem(String itemName, Callable<Object> callback)
    {
        Text text = new Text(itemName);
        allItems.add(text);
        text.getStyleClass().add("verticalMenuOption");
        Rectangle square = new Rectangle();
        square.getStyleClass().add("verticalMenuSquare");
        square.setHeight(SQUARE_SIZE);
        square.setWidth(SQUARE_SIZE);
        addRow(verticalMenuGrid, square, text);
        setUpEventHandlersForItem(square, text, callback);

        if (!firstItemInitialised)
        {
            firstItem = text;
            firstSquare = square;
            firstCallable = callback;
            firstItemInitialised = true;
        }

        return new Item(text, square);
    }

    public void selectFirstItem()
    {
        selectItem(firstItem, firstSquare, firstCallable);
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

                selectItem(itemName, square, callback);
            }
        });
    }

    private void selectItem(Text itemName, Rectangle square, Callable<Object> callback)
    {
        selectedItem = itemName;
        selectedSquare = square;
        displayAsSelected(selectedItem, selectedSquare);
        try
        {
            callback.call();
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
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
        selectedItem.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, false);
        square.setFill(Color.WHITE);
    }

    private void displayAsSelected(Text selectedItem, Rectangle square)
    {
        square.setVisible(true);
        selectedItem.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, true);
        square.setFill(StandardColours.ROBOX_BLUE);
    }

    /**
     * *
     * Disable all menu items except the currently selected item.
     */
    public void disableNonSelectedItems()
    {
        disableNonSelectedItems = true;
        for (Text item : allItems)
        {
            if (item != selectedItem)
            {
                item.setDisable(true);
            }
        }
    }

    /**
     * *
     * Enable all menu items.
     */
    public void enableNonSelectedItems()
    {
        disableNonSelectedItems = false;
        for (Text item : allItems)
        {
            if (item != selectedItem)
            {
                item.setDisable(false);
            }
        }
    }

    /**
     * If an item is selected then deselect it.
     */
    public void deselectSelectedItem()
    {
        if (selectedItem != null && selectedSquare != null)
        {
            deselect(selectedItem, selectedSquare);
            selectedItem = null;
            selectedSquare = null;
        }
    }

    public void reset()
    {
        enableNonSelectedItems();
        deselectSelectedItem();
    }
}
