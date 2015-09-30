package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

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

        textContainer.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>()
        {

            @Override
            public void changed(ObservableValue<? extends Worker.State> ov, Worker.State t, Worker.State newState)
            {
                if (newState == Worker.State.SUCCEEDED)
                {
                    NodeList nodeList = textContainer.getEngine().getDocument().getElementsByTagName("a");
                    for (int i = 0; i < nodeList.getLength(); i++)
                    {
                        Node node = nodeList.item(i);
                        EventTarget eventTarget = (EventTarget) node;
                        eventTarget.addEventListener("click", new EventListener()
                        {
                            @Override
                            public void handleEvent(Event evt)
                            {
                                EventTarget target = evt.getCurrentTarget();
                                HTMLAnchorElement anchorElement = (HTMLAnchorElement) target;
                                String href = anchorElement.getHref();
                                evt.preventDefault();

                                try
                                {
                                    URI outboundURI = new URI(href);
                                    if (Desktop.isDesktopSupported()
                                            && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                                    {
                                        Desktop.getDesktop().browse(outboundURI);
                                    }
                                } catch (URISyntaxException | IOException ex)
                                {
                                    steno.error("Unable to generate URI from " + href);
                                }
                            }
                        }, false);
                    }
                }
            }
        });
        textContainer.getEngine().load(readmeURL);

    }
}
