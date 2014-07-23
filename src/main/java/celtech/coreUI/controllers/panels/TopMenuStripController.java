/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class TopMenuStripController
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        TopMenuStripController.class.getName());
    private ApplicationStatus applicationStatus = null;

    @FXML
    private VBox container;

    @FXML
    void preferencesPressed(ActionEvent event)
    {
        applicationStatus.setMode(ApplicationMode.PREFERENCES_TOP_LEVEL);
    }

    @FXML
    void helpPressed(ActionEvent event)
    {
        applicationStatus.setMode(ApplicationMode.ABOUT);
    }

    /*
     * JavaFX initialisation method
     */
    @FXML
    void initialize()
    {
        applicationStatus = ApplicationStatus.getInstance();

        container.visibleProperty().bind(applicationStatus.modeProperty().isEqualTo(ApplicationMode.STATUS)
            .or(applicationStatus.modeProperty().isEqualTo(ApplicationMode.LAYOUT)
                .or(applicationStatus.modeProperty().isEqualTo(ApplicationMode.SETTINGS))));
    }
}
