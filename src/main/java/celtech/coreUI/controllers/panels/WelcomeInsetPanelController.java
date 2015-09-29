package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class WelcomeInsetPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            WelcomeInsetPanelController.class.getName());

    @FXML
    private WebView textContainer;

    @FXML
    void backToStatusAction(ActionEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.STATUS);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        String readmeURL = "file:///" + ApplicationConfiguration.getApplicationInstallDirectory(WelcomeInsetPanelController.class) + "README/README_AutoMaker.html";

        textContainer.getEngine().getLoadWorker().stateProperty().addListener(
                (ObservableValue<? extends Worker.State> ov, Worker.State oldState, Worker.State newState) ->
                {
                    switch (newState)
                    {
                        case RUNNING:
                            steno.info("Running");
                            break;
                        case SUCCEEDED:
                            steno.info("Succeeded");
                            break;
                        case CANCELLED:
                            steno.info("Cancelled");
                            break;
                        case FAILED:
                            steno.info("Failed");
                            break;
                    }
                });
        textContainer.getEngine().load(readmeURL);
    }
}
