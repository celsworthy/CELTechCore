package celtech.coreUI.components;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
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
public class ProfileChoiceListCell extends ListCell<SlicerParametersFile>
{   
    private final static String LIST_CELL_STYLE_CLASS = "profile-choice-list-cell-grid";
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
        
        createNewProfileLabel.setText(Lookup.i18n("sidePanel_settings.createNewProfile"));
        createNewProfileLabel.setAlignment(Pos.CENTER);
        
        grid.setHgap(10);
        grid.setVgap(4);
        grid.add(padlock, 1, 1);
        grid.add(name, 2, 1);
        
        grid.getStyleClass().add(LIST_CELL_STYLE_CLASS);
    }
    
    @Override
    protected void updateItem(SlicerParametersFile settings, boolean empty)
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
    
    private void addContent(SlicerParametersFile settings)
    {
        setText(null);
        if (settings == SlicerParametersContainer.createNewProfile)
        {
            setGraphic(createNewProfileLabel);
        } else
        {
            padlock.setVisible(SlicerParametersContainer.applicationProfileListContainsProfile(settings.getProfileName()));
            name.textProperty().set(settings.getProfileName());
            setGraphic(grid);
        }
    }
}
