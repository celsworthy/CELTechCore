package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.components.VerticalMenu;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

public class ExtrasMenuPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        ExtrasMenuPanelController.class.getName());

    private ResourceBundle resources;
    private FilamentLibraryPanelController libraryController;
    private List<ExtrasMenuInnerPanel> innerPanels = new ArrayList<>();
    
    @FXML
    private VerticalMenu libraryMenu;

    @FXML
    private VBox insetNodeContainer;

    @FXML
    private GraphicButtonWithLabel saveButton;

    @FXML
    private GraphicButtonWithLabel newButton;

    @FXML
    private GraphicButtonWithLabel deleteButton;

    @FXML
    private GraphicButtonWithLabel copyButton;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {

        this.resources = resources;

        setupInnerPanels();

        buildExtras();
    }

    /**
     * Define the inner panels to be offered in the main menu. For the future this is
     * configuration information that could be e.g. stored in XML or in a plugin.
     */
    private void setupInnerPanels()
    {
        
        ExtrasMenuInnerPanel libraryInnerPanel = new ExtrasMenuInnerPanel()
        {

            @Override
            public String getMenuTitle()
            {
                return "extrasMenu.filament";
            }

            @Override
            public URL getFXMLURL()
            {
                String fxmlFileName = ApplicationConfiguration.fxmlPanelResourcePath
                    + "filamentLibraryPanel.fxml";
                return getClass().getResource(fxmlFileName);
            }

            @Override
            public List<ExtrasMenuInnerPanel.OperationButton> getOperationButtons()
            {
                List<ExtrasMenuInnerPanel.OperationButton> operationButtons = new ArrayList<>();
                return operationButtons;
            }
        };
        
        innerPanels.add(libraryInnerPanel);
            
    }
    
    /**
     * Open the given inner panel.
     */
    private void doOpenInnerPanel(ExtrasMenuInnerPanel innerPanel) {
        FXMLLoader loader = new FXMLLoader(innerPanel.getFXMLURL(), resources);
        try
        {
            Node node = loader.load();
            insetNodeContainer.getChildren().clear();
            insetNodeContainer.getChildren().add(node);
            associateInnerController(loader.getController());
        } catch (IOException ex)
        {
            steno.error("Cannot load fxml: " + innerPanel.getFXMLURL().toString() + ex);
        }
    }

    /**
     * For each InnerPanel, create a menu item that will open it.
     */
    private void buildExtras()
    {
        libraryMenu.setTitle(Lookup.i18n("extrasMenu.title"));
        
        for (ExtrasMenuInnerPanel innerPanel : innerPanels)
        {
            libraryMenu.addItem(Lookup.i18n(innerPanel.getMenuTitle()), () ->
            {
                doOpenInnerPanel(innerPanel);
                return null;
            }, null);
        }
    }

    private void associateInnerController(FilamentLibraryPanelController libraryController)
    {
        saveButton.disableProperty().bind(libraryController.getCanSave().not());
        deleteButton.disableProperty().bind(libraryController.getCanDelete().not());
    }

    @FXML
    private void okPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
    }

    @FXML
    private void newPressed(ActionEvent event)
    {
        libraryController.whenNewPressed();
    }

    @FXML
    private void savePressed(ActionEvent event)
    {
        libraryController.whenSavePressed();
    }

    @FXML
    private void copyPressed(ActionEvent event)
    {
        libraryController.whenCopyPressed();
    }

    @FXML
    private void deletePressed(ActionEvent event)
    {
        libraryController.whenDeletePressed();
    }

}
