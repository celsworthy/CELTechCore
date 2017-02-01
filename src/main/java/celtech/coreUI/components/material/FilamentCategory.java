package celtech.coreUI.components.material;

import celtech.roboxbase.MaterialType;
import celtech.roboxbase.configuration.Filament;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 *
 * @author Ian
 */
public class FilamentCategory extends VBox
{

    @FXML
    private Text categoryTitle;

    @FXML
    private VBox swatchContainer;

    private Comparator<Entry<MaterialType, List<Filament>>> byMaterialName
            = (Entry<MaterialType, List<Filament>> o1, Entry<MaterialType, List<Filament>> o2) -> o1.getKey().getFriendlyName().compareTo(o2.getKey().getFriendlyName());

    private final FilamentSelectionListener materialSelectionListener;
    private Map<MaterialType, List<Filament>> filamentMap = null;

    public FilamentCategory(FilamentSelectionListener materialSelectionListener)
    {
        this.materialSelectionListener = materialSelectionListener;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "/celtech/resources/fxml/components/material/filamentCategory.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        fxmlLoader.setClassLoader(this.getClass().getClassLoader());

        try
        {
            fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }

//        this.getStyleClass().add("error-dialog-choice-button");
    }

    public void setCategoryData(String category, Map<MaterialType, List<Filament>> filamentMap)
    {
        categoryTitle.setText(category);

        swatchContainer.getChildren().clear();

        this.filamentMap = filamentMap;
        
        filamentMap.entrySet().stream().sorted(byMaterialName).forEach((materialEntry) ->
        {
            MaterialType material = materialEntry.getKey();
            List<Filament> filaments = materialEntry.getValue();

            Text materialTitle = new Text(material.getFriendlyName());
            swatchContainer.getChildren().add(materialTitle);

            FlowPane flowPane = new FlowPane();
            swatchContainer.getChildren().add(flowPane);

            for (Filament filament : filaments)
            {
                FilamentSwatch swatch = new FilamentSwatch(materialSelectionListener, filament);
                flowPane.getChildren().add(swatch);
            }
        });
    }
    public Map<MaterialType, List<Filament>> getFilamentMap()
    {
        return filamentMap;
    }

}
