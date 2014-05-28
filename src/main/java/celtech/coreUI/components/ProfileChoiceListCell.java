/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.DisplayManager;
import celtech.services.slicer.RoboxProfile;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

/**
 *
 * @author Ian
 */
public class ProfileChoiceListCell extends ListCell<RoboxProfile>
{   
    private final static String LIST_CELL_STYLE_CLASS = "profile-choice-list-cell";
    private final GridPane grid = new GridPane();
    private final Label name = new Label();
    private static Image padlockImage = null;
    private ImageView padlock = new ImageView();
    private final Label createNewProfileLabel = new Label();
    
    /**
     *
     */
    public ProfileChoiceListCell()
    {
        if (padlockImage == null)
        {
            padlockImage = new Image(ProfileChoiceListCell.class.getResource(ApplicationConfiguration.imageResourcePath + "padlock.png").toExternalForm());
        }
        padlock.setImage(padlockImage);
        
        createNewProfileLabel.setText(DisplayManager.getLanguageBundle().getString("sidePanel_settings.createNewProfile"));
        createNewProfileLabel.setAlignment(Pos.CENTER);
        
        grid.setHgap(10);
        grid.setVgap(4);
        grid.add(padlock, 1, 1);
        grid.add(name, 2, 1);
        
        grid.getStyleClass().add(LIST_CELL_STYLE_CLASS);
    }
    
    @Override
    protected void updateItem(RoboxProfile settings, boolean empty)
    {
        super.updateItem(settings, empty);
        if (empty)
        {
            clearContent();
        } else
        {
            addContent(settings);
        }
    }
    
    private void clearContent()
    {
        setText(null);
        setGraphic(null);
    }
    
    private void addContent(RoboxProfile settings)
    {
        setText(null);
        if (settings == PrintProfileContainer.createNewProfile)
        {
            setGraphic(createNewProfileLabel);
        } else
        {
            padlock.setVisible(!settings.isMutable());
            name.textProperty().bind(settings.getProfileNameProperty());
            setGraphic(grid);
        }
    }
}
