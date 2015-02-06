/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.components.material;

import celtech.configuration.Filament;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

/**
 *
 * @author tony
 */
class FilamentCell extends ListCell<Filament>
{
    
    HBox cellContainer;
    Label label;

    public FilamentCell()
    {
        cellContainer = new HBox();
        
        label = new Label();
        cellContainer.getChildren().addAll(label);
    }

    @Override
    protected void updateItem(Filament item, boolean empty)
    {
        super.updateItem(item, empty); 
        if (! empty) {
            setGraphic(cellContainer);
            label.setText(item.getFriendlyFilamentName());
       } else {
            setGraphic(null);
        }
        
        
    }
    
    
    
}
