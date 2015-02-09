/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.components.material;

import celtech.configuration.Filament;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author tony
 */
class FilamentCell extends ListCell<Filament>
{
    
    private static int SWATCH_SQUARE_SIZE = 16;
    
    HBox cellContainer;
    Rectangle rectangle = new Rectangle();
    Label label;

    public FilamentCell()
    {
        cellContainer = new HBox();
        rectangle = new Rectangle(SWATCH_SQUARE_SIZE, SWATCH_SQUARE_SIZE);
        label = new Label();
        cellContainer.getChildren().addAll(rectangle, label);
    }

    @Override
    protected void updateItem(Filament item, boolean empty)
    {
        super.updateItem(item, empty); 
        if (! empty) {
            setGraphic(cellContainer);
            rectangle.setFill(item.getDisplayColour());
            
            label.setText(item.getLongFriendlyName() + " " + item.getMaterial().getFriendlyName());
            label.getStyleClass().add("filamentSwatchPadding");
       } else {
            setGraphic(null);
        }
        
        
    }
    
    
    
}
