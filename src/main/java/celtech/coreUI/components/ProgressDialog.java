/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.CoreTest;
import celtech.coreUI.DisplayManager;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.controllers.ProgressDialogController;
import celtech.services.ControllableService;
import java.io.IOException;
import java.net.URL;
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
 * @author ianhudson
 */
public class ProgressDialog
{

    private final Stenographer steno = StenographerFactory.getStenographer(ProgressDialog.class.getName());
    private Stage dialogStage = null;
    private ProgressDialogController dialogController = null;

    public ProgressDialog(ControllableService service)
    {
        dialogStage = new Stage(StageStyle.TRANSPARENT);
        URL dialogFXMLURL = ProgressDialog.class.getResource(ApplicationConfiguration.fxmlResourcePath + "ProgressDialog.fxml");
        FXMLLoader dialogLoader = new FXMLLoader(dialogFXMLURL);
        try
        {
            Parent dialogBoxScreen = (Parent) dialogLoader.load();
            dialogController = (ProgressDialogController) dialogLoader.getController();

            Scene dialogScene = new Scene(dialogBoxScreen, Color.TRANSPARENT);
            dialogScene.getStylesheets().add(ApplicationConfiguration.mainCSSFile);
            dialogStage.setScene(dialogScene);
            dialogStage.initOwner(DisplayManager.getMainStage());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogController.configure(service, dialogStage);
        } catch (IOException ex)
        {
            steno.error("Couldn't load dialog box FXML");
        }
    }
}
