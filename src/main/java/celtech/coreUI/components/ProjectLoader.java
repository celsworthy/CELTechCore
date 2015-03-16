/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.Lookup;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.ProjectLoaderController;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProjectLoader
{

    private Stenographer steno = StenographerFactory.getStenographer(ModalDialog.class.getName());
    private Stage dialogStage = null;
    private ProjectLoaderController dialogController = null;

    /**
     *
     */
    public ProjectLoader()
    {
        dialogStage = new Stage(StageStyle.TRANSPARENT);
        URL dialogFXMLURL = ModalDialog.class.getResource(ApplicationConfiguration.fxmlResourcePath + "ProjectLoader.fxml");
        ResourceBundle i18nBundle = Lookup.getLanguageBundle();
        FXMLLoader dialogLoader = new FXMLLoader(dialogFXMLURL, i18nBundle);
        
        try
        {
            Parent dialogBoxScreen = (Parent) dialogLoader.load();
            dialogController = (ProjectLoaderController) dialogLoader.getController();

            Scene dialogScene = new Scene(dialogBoxScreen, Color.TRANSPARENT);
            dialogScene.getStylesheets().add(ApplicationConfiguration.getMainCSSFile());
            dialogStage.setScene(dialogScene);
            dialogStage.initOwner(DisplayManager.getMainStage());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogController.configure(dialogStage);
        } catch (IOException ex)
        {
            steno.error("Couldn't load project loader FXML");
        }
    }

    /**
     *
     * @return
     */
    public int show()
    {
        dialogController.repopulateProjects();
        dialogStage.showAndWait();

        return dialogController.getButtonValue();
    }

    /**
     *
     * @return
     */
    public Project getSelectedProject()
    {
        return dialogController.getSelectedProject();
    }
}
