package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.components.VerticalMenu;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import java.io.IOException;
import java.net.URL;
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

/**
 *
 * @author Ian
 */
public class LibraryPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        LibraryPanelController.class.getName());

    private Mode mode;
    private ResourceBundle resources;
    private FilamentLibraryPanelController libraryController;

    enum Mode
    {

        FILAMENT, PRINT_PROFILE, WRITE_TO_REEL
    }

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

        setupLibraryMenu();
    }

    private void setupLibraryMenu()
    {
        libraryMenu.setTitle(Lookup.i18n("library.title"));
        Callable doOpenFilamentLibrary = () ->
        {
            setMode(Mode.FILAMENT);
            return null;
        };
        libraryMenu.addItem(Lookup.i18n("library.filament"),
                            doOpenFilamentLibrary, null);

        Callable doOpenPrintProfileLibrary = () ->
        {
            setMode(Mode.PRINT_PROFILE);
            return null;
        };
        libraryMenu.addItem(Lookup.i18n("library.printProfile"),
                            doOpenPrintProfileLibrary, null);
        
        Callable doOpenWriteToReel = () ->
        {
            setMode(Mode.WRITE_TO_REEL);
            return null;
        };
        libraryMenu.addItem(Lookup.i18n("library.writeToReel"),
                            doOpenWriteToReel, null);        

        libraryMenu.selectFirstItem();
    }

    private void associateInnerController(FilamentLibraryPanelController libraryController)
    {
        saveButton.disableProperty().bind(libraryController.getCanSave().not());
        deleteButton.disableProperty().bind(libraryController.getCanDelete().not());
    }

    void setMode(Mode mode)
    {
        this.mode = mode;
        String fxmlFileName = ApplicationConfiguration.fxmlPanelResourcePath
            + "filamentLibraryPanel.fxml";
        URL fxmlURL = getClass().getResource(fxmlFileName);
        FXMLLoader loader = new FXMLLoader(fxmlURL, resources);
        libraryController = new FilamentLibraryPanelController();
        loader.setController(libraryController);
        try
        {
            Node node = loader.load();
            insetNodeContainer.getChildren().clear();
            insetNodeContainer.getChildren().add(node);
            associateInnerController(libraryController);
        } catch (IOException ex)
        {
            steno.error("Cannot load fxml: " + "filamentLibraryPanel.fxml " + ex);
        }
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
