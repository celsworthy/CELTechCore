package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.components.VerticalMenu;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

public class ExtrasMenuPanelController implements Initializable
{

    private class InnerPanel
    {

        Node node;
        ExtrasMenuInnerPanel controller;

        InnerPanel(Node node, ExtrasMenuInnerPanel controller)
        {
            this.node = node;
            this.controller = controller;
        }
    }

    List<InnerPanel> innerPanels = new ArrayList<>();

    private final Stenographer steno = StenographerFactory.getStenographer(
        ExtrasMenuPanelController.class.getName());

    private ResourceBundle resources;

    @FXML
    private VerticalMenu libraryMenu;

    @FXML
    private VBox insetNodeContainer;

    @FXML
    private HBox buttonBoxContainer;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {

        this.resources = resources;

        ButtonBox buttonBox = new ButtonBox(Lookup.getExtrasInnerPanel());
        buttonBoxContainer.getChildren().add(buttonBox);

        setupInnerPanels();

        buildExtras();
    }

    /**
     * Define the inner panels to be offered in the main menu. For the future this is configuration
     * information that could be e.g. stored in XML or in a plugin.
     */
    private void setupInnerPanels()
    {
        loadInnerPanel(
            ApplicationConfiguration.fxmlPanelResourcePath + "filamentLibraryPanel.fxml");
    }

    /**
     * Load the given inner panel.
     */
    private void loadInnerPanel(String fxmlLocation)
    {
        URL fxmlURL = getClass().getResource(fxmlLocation);
        FXMLLoader loader = new FXMLLoader(fxmlURL, resources);
        try
        {
            Node node = loader.load();
            InnerPanel innerPanel = new InnerPanel(node,
                                                   (ExtrasMenuInnerPanel) loader.getController());
            innerPanels.add(innerPanel);
        } catch (IOException ex)
        {
            steno.error("Unable to load panel: " + fxmlLocation + " " + ex);
        }
    }

    /**
     * Open the given inner panel.
     */
    private void doOpenInnerPanel(InnerPanel innerPanel)
    {
        insetNodeContainer.getChildren().clear();
        insetNodeContainer.getChildren().add(innerPanel.node);
    }

    /**
     * For each InnerPanel, create a menu item that will open it.
     */
    private void buildExtras()
    {
        libraryMenu.setTitle(Lookup.i18n("extrasMenu.title"));

        for (InnerPanel innerPanel : innerPanels)
        {
            libraryMenu.addItem(Lookup.i18n(innerPanel.controller.getMenuTitle()), () ->
                            {
                                doOpenInnerPanel(innerPanel);
                                Lookup.setExtrasInnerPanel(innerPanel.controller);
                                return null;
            }, null);
        }
    }

    @FXML
    private void okPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
    }
}
