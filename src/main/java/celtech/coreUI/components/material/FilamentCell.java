/*
 * Copyright 2015 CEL UK
 */
package celtech.coreUI.components.material;

import celtech.Lookup;
import celtech.configuration.Filament;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author tony
 */
public class FilamentCell extends ListCell<Object>
{

    private static int SWATCH_SQUARE_SIZE = 16;

    HBox cellContainer;
    Rectangle rectangle = new Rectangle();
    Label label;

    public FilamentCell()
    {
        cellContainer = new HBox();
        cellContainer.setAlignment(Pos.CENTER_LEFT);
        rectangle = new Rectangle(SWATCH_SQUARE_SIZE, SWATCH_SQUARE_SIZE);
        label = new Label();
        cellContainer.getChildren().addAll(rectangle, label);
    }

    @Override
    protected void updateItem(Object item, boolean empty)
    {
        super.updateItem(item, empty);
        if (item != null && !empty)
        {
            if (item instanceof Filament)
            {
                Filament filament = (Filament) item;
                setGraphic(cellContainer);
                rectangle.setFill(filament.getDisplayColour());

                label.setText(filament.getLongFriendlyName() + " "
                    + filament.getMaterial().getFriendlyName());
                label.getStyleClass().add("filamentSwatchPadding");
            } else {
                setGraphic(null);
                label.setText(Lookup.i18n("materialComponent.unknown"));
            }
        } else
        {
            setGraphic(null);
        }

    }

}
