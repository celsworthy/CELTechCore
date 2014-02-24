/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class AutoMakerRootPane extends AnchorPane
{

    private static final Stenographer steno = StenographerFactory.getStenographer(AutoMakerRootPane.class.getName());
    private final ApplicationStatus applicationStatus = ApplicationStatus.getInstance();

    private StackPane sidePanelContainer = null;
    private StackPane modeSelectionControl = null;
    private VBox layoutControls = null;
    private final HashMap<ApplicationMode, BorderPane> sidePanels = new HashMap<>();
    private static TabPane tabDisplay = null;
    private static Tab printerStatusTab = null;

    public AutoMakerRootPane()
    {

    }

    public void initialiseContainer()
    {
        for (ApplicationMode mode : ApplicationMode.values())
        {
            URL fxmlFileName = getClass().getResource(mode.getSidePanelFXMLName());
            steno.debug("About to load side panel fxml: " + fxmlFileName);
            try
            {
                BorderPane sidePanel = FXMLLoader.<BorderPane>load(fxmlFileName);
                sidePanels.put(mode, sidePanel);
            } catch (IOException ex)
            {
                steno.error("Couldn't load side panel for mode:" + mode + ". " + ex);
            }
        }

        // Create a place to hang the side panels from
        sidePanelContainer = new StackPane();
        getChildren().add(sidePanelContainer);

        try
        {
            modeSelectionControl = FXMLLoader.<StackPane>load(getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "ModeSelectionControl.fxml"));
            AnchorPane.setBottomAnchor(modeSelectionControl, 0.0);
            AnchorPane.setRightAnchor(modeSelectionControl, 0.0);

            getChildren().add(modeSelectionControl);
        } catch (IOException ex)
        {
            steno.error("Failed to load model selection control:" + ex);
        }

        try
        {
            layoutControls = FXMLLoader.<VBox>load(getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "LayoutControls.fxml"));
            AnchorPane.setBottomAnchor(layoutControls, 0.0);
        } catch (IOException ex)
        {
            steno.error("Failed to load layout controls:" + ex);
        }

        // Configure the main display tab pane - just the printer status page to start with
        tabDisplay = new TabPane();
        // The printer status tab will always be visible - the page is static
        try
        {
            Parent printerStatusPage = FXMLLoader.<Parent>load(getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "PrinterStatusPage.fxml"));
            printerStatusTab = new Tab();
            printerStatusTab.setText("Status");
            printerStatusTab.setClosable(false);
            printerStatusTab.setContent(printerStatusPage);
            tabDisplay.getTabs().add(printerStatusTab);
            AnchorPane.setLeftAnchor(tabDisplay, 0.0);
            AnchorPane.setTopAnchor(tabDisplay, 0.0);

            getChildren().add(tabDisplay);
        } catch (IOException ex)
        {
            steno.error("Failed to load printer status page:" + ex);
        }

        applicationStatus.modeProperty().addListener((ObservableValue<? extends ApplicationMode> ov, ApplicationMode oldMode, ApplicationMode newMode) ->
        {
            switchPagesForMode(oldMode, newMode);
        });
    }

    @Override
    protected void layoutChildren()
    {
        super.layoutChildren(); //To change body of generated methods, choose Tools | Templates.

        steno.info("Layout children");
    }

    private void switchPagesForMode(ApplicationMode oldMode, ApplicationMode newMode)
    {
        // Remove all of the existing side panel children
        for (Node sidePanelNode : sidePanelContainer.getChildren())
        {
            sidePanelContainer.getChildren().remove(sidePanelNode);
        }

        // Now add the relevant new one...
        sidePanelContainer.getChildren().add(sidePanels.get(newMode));
    }

}
