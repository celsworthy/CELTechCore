/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.calibration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

/**
 *
 * @author tony
 */
public class CalibrationMenu extends VBox
{
    
    private static int SQUARE_SIZE = 16;
    private static int ROW_HEIGHT = 50;
    
    @FXML
    private Rectangle square;
    
    @FXML
    private Text T1;
    
    @FXML
    private GridPane calibrationMenuGrid;
    
    private List<Callable> itemCallbacks = new ArrayList<>();
    /**
     * The row number of the next item to be added
     */
    private int nextRowNum = 3;

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
    
    public void addItem(String itemName, Callable callback) {
        itemCallbacks.add(callback);
        Text text = new Text(itemName);
        text.getStyleClass().add("calibrationMenuOption");
        Rectangle rectangle = new Rectangle();
        rectangle.getStyleClass().add("calibrationMenuSquare");
        rectangle.setHeight(SQUARE_SIZE);
        rectangle.setWidth(SQUARE_SIZE);
        addRow(calibrationMenuGrid, rectangle, text);
    }

    private void setUpEventHandlersForItem(Shape square, Text itemName)
    {
        square.setVisible(false);
        itemName.setOnMouseEntered((MouseEvent e) -> {
            square.setVisible(true);
            square.setFill(Color.WHITE);
        });
        itemName.setOnMouseExited((MouseEvent e) -> {
            square.setVisible(false);
        });
        itemName.setOnMouseClicked((MouseEvent e) -> {
            square.setVisible(true);
            itemName.setFill(Color.WHITE);
            square.setFill(Color.BLUE);
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
        setUpEventHandlersForItem(square, itemName);
        nextRowNum++;
    }

}