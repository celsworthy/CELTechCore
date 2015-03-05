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
            public List<OperationButton> getOperationButtons()
            {
                List<OperationButton> operationButtons = new ArrayList<>();
                OperationButton saveButton = new OperationButton()
                {

                    @Override
                    public String getTextId()
                    {
                        return "genericFirstLetterCapitalised.Save";
                    }

                    @Override
                    public String getFXMLLocation()
                    {
                        return "saveButton";
                    }

                    @Override
                    public String getTooltipTextId()
                    {
                        return "genericFirstLetterCapitalised.Save";
                    }
                };
                operationButtons.add(saveButton);
                return operationButtons;
            }
        };

        innerPanels.add(libraryInnerPanel);
    }

    /**
     * Open the given inner panel.
     */
    private void doOpenInnerPanel(ExtrasMenuInnerPanel innerPanel)
    {
        FXMLLoader loader = new FXMLLoader(innerPanel.getFXMLURL(), resources);
        try
        {
            Node node = loader.load();
            insetNodeContainer.getChildren().clear();
            insetNodeContainer.getChildren().add(node);
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
                                Lookup.setExtrasInnerPanel(innerPanel);
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
