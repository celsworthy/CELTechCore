package celtech.coreUI.components;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
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
        setText(settings.getProfileName());
    }
}
