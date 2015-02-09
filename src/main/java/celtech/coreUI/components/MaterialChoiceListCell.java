package celtech.coreUI.components;

import celtech.Lookup;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.FilamentContainer;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author Ian
 */
public class MaterialChoiceListCell extends ListCell<Filament>
{

    private final static String LIST_CELL_STYLE_CLASS = "material-choice-list-cell-grid";
    private final GridPane grid = new GridPane();
    private final GridPane alternateGrid = new GridPane();
    private final Rectangle colourRectangle = new Rectangle(15, 15);
    private final Label name = new Label();
    private final Label remainingFilament = new Label();
    private static Image padlockImage = null;
    private final ImageView padlock = new ImageView();
    private final Label createNewFilamentLabel = new Label();

    /**
     *
     */
    public MaterialChoiceListCell()
    {
        if (padlockImage == null)
        {
            padlockImage = new Image(MaterialChoiceListCell.class.getResource(ApplicationConfiguration.imageResourcePath + "padlock.png").toExternalForm());
        }
        padlock.setImage(padlockImage);

        createNewFilamentLabel.setText(Lookup.i18n("sidePanel_settings.createNewMaterial"));
        createNewFilamentLabel.setAlignment(Pos.CENTER);
        
        alternateGrid.add(createNewFilamentLabel, 1, 1);
        alternateGrid.getStyleClass().add(LIST_CELL_STYLE_CLASS);

        grid.add(colourRectangle, 1, 1);
        grid.add(padlock, 2, 1);
        grid.add(name, 3, 1);
        grid.add(remainingFilament, 4, 1);
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
            name.textProperty().bind(Bindings.format("%s %s", filament.getFriendlyFilamentNameProperty(), filament.getMaterialProperty()));
            if (filament.isMutable() == false)
            {
                //Remaining filament is in mm - we want to display it in metres
                remainingFilament.textProperty().bind(filament.getRemainingFilamentProperty().divide(1000).asString("%.1fm"));
                remainingFilament.setVisible(true);
            }
            else
            {
                remainingFilament.textProperty().unbind();
                remainingFilament.setVisible(false);
            }
                 
            setGraphic(grid);
        }
    }
}
