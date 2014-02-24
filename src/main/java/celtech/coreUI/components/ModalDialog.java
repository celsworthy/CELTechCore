/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.coreUI.DisplayManager;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.controllers.ModalDialogController;
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
public class ModalDialog
{

    private Stenographer steno = StenographerFactory.getStenographer(ModalDialog.class.getName());
    private Stage dialogStage = null;
    private ModalDialogController dialogController = null;

    public ModalDialog()
    {
        dialogStage = new Stage(StageStyle.TRANSPARENT);
        URL dialogFXMLURL = ModalDialog.class.getResource(ApplicationConfiguration.fxmlResourcePath + "ModalDialog.fxml");
        FXMLLoader dialogLoader = new FXMLLoader(dialogFXMLURL);
        try
        {
            Parent dialogBoxScreen = (Parent) dialogLoader.load();
            dialogController = (ModalDialogController) dialogLoader.getController();

            Scene dialogScene = new Scene(dialogBoxScreen, Color.TRANSPARENT);
            dialogScene.getStylesheets().add(ApplicationConfiguration.mainCSSFile);
            dialogStage.setScene(dialogScene);
            dialogStage.initOwner(DisplayManager.getMainStage());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogController.configure(dialogStage);
        } catch (IOException ex)
        {
            steno.error("Couldn't load dialog box FXML");
        }
    }

    public void setTitle(String title)
    {
        dialogController.setDialogTitle(title);
    }

    public void setMessage(String message)
    {
        dialogController.setDialogMessage(message);
    }

    public int addButton(String text)
    {
        return dialogController.addButton(text);
    }

    public int show()
    {
        dialogStage.showAndWait();

        return dialogController.getButtonValue();
    }

    public void close()
    {
        dialogStage.hide();
    }

    public boolean isShowing()
    {
        return dialogStage.isShowing();
    }
}
