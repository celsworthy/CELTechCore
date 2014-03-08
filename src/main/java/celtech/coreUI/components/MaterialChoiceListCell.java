/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.CoreTest;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.coreUI.DisplayManager;
import celtech.printerControl.Printer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

/**
 *
 * @author Ian
 */
public class MaterialChoiceListCell extends ListCell<Filament>
{
    
    private final static String LIST_CELL_STYLE_CLASS = "material-choice-list-cell";
    private final GridPane grid = new GridPane();
    private final Rectangle colourRectangle = new Rectangle(10, 10);
    private final Label name = new Label();
    private static Image padlockImage = null;
    private ImageView padlock = new ImageView();
    private final Label createNewFilamentLabel = new Label();
    
    public MaterialChoiceListCell()
    {
        if (padlockImage == null)
        {
            padlockImage = new Image(MaterialChoiceListCell.class.getResource(ApplicationConfiguration.imageResourcePath + "padlock.png").toExternalForm());
        }
        padlock.setImage(padlockImage);
        
        createNewFilamentLabel.setText(DisplayManager.getLanguageBundle().getString("sidePanel_settings.createNewMaterial"));
        createNewFilamentLabel.setAlignment(Pos.CENTER);
        
        grid.setHgap(10);
        grid.setVgap(4);
        grid.add(colourRectangle, 1, 1);
        grid.add(padlock, 2, 1);
        grid.add(name, 3, 1);
        
        grid.getStyleClass().add(LIST_CELL_STYLE_CLASS);
    }
    
    @Override
    protected void updateItem(Filament filament, boolean empty)
    {
        super.updateItem(filament, empty);
        if (empty)
        {
            clearContent();
        } else
        {
            addContent(filament);
        }
    }
    
    private void clearContent()
    {
        setText(null);
        setGraphic(null);
    }
    
    private void addContent(Filament filament)
    {
        setText(null);
        if (filament == FilamentContainer.createNewFilament)
        {
            setGraphic(createNewFilamentLabel);
        } else
        {
            colourRectangle.setFill(filament.getDisplayColour());
            padlock.setVisible(!filament.isMutable());
            name.textProperty().bind(filament.getFriendlyFilamentNameProperty());
            setGraphic(grid);
        }
    }
}
