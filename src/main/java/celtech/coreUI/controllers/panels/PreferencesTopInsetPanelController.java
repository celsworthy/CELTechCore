/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.coreUI.components.VerticalMenu;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

/**
 *
 * @author tony
 */
public class PreferencesTopInsetPanelController implements Initializable
{

    @FXML
    void cancelPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
    }

    @FXML
    void backwardPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
    }

    @FXML
    private VBox preferencesListContainer;

    @FXML
    private VerticalMenu preferencesMenu;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        preferencesMenu.setTitle(Lookup.i18n("preferences.preferences"));
        preferencesMenu.addItem("Environment", this::showEnvironmentPreferences);
        preferencesMenu.addItem("Printing", this::showPrintingPreferences);
    }

    private Object showEnvironmentPreferences()
    {
        return null;
    }
    
    private Object showPrintingPreferences()
    {
        return null;
    }    

}
