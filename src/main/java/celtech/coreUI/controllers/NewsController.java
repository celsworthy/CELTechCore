package celtech.coreUI.controllers;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.NewsBot;
import celtech.appManager.NewsListener;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.buttons.GraphicButtonWithLabel;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
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
public class NewsController implements Initializable, NewsListener
{

    private static final Stenographer steno = StenographerFactory.getStenographer(NewsController.class.getName());

    private final WebView webView = new WebView();
    private WebEngine webEngine = null;

    private static final String allAutoMakerNewsFlashesURL = "http://www.cel-robox.com/category/AutoMakerNewsflash";

    private NewsBot newsBot = null;

    private ArrayList<NewsBot.NewsArticle> newsArticles = null;
    private int currentArticleIndex = -1;

    @FXML
    private StackPane webContentContainer;

    @FXML
    private GraphicButtonWithLabel forwardButton;

    private BooleanProperty forwardButtonUnavailable = new SimpleBooleanProperty(false);

    @FXML
    void cancelPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().modeProperty().set(ApplicationMode.LAYOUT);
    }

    @FXML
    void forwardPressed(ActionEvent event)
    {
        currentArticleIndex++;
        loadWebData();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        newsBot = NewsBot.getInstance();
        newsBot.registerListener(this);

        webEngine = webView.getEngine();
        VBox.setVgrow(webContentContainer, Priority.ALWAYS);
        webEngine.getLoadWorker().stateProperty().addListener(
                (ObservableValue<? extends Worker.State> ov, Worker.State oldState, Worker.State newState) ->
                {
                    switch (newState)
                    {
                        case RUNNING:
                            DisplayManager.getInstance().startSpinning(webContentContainer);
                            break;
                        case SUCCEEDED:
                            DisplayManager.getInstance().stopSpinning();
                            if (currentArticleIndex >= 0)
                            {
//                                newsBot.articleRead(newsArticles.get(currentArticleIndex));
                            }
                            NodeList nodeList = webEngine.documentProperty().get().getElementsByTagName("a");
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
                                        //handle opening URL outside JavaFX WebView
                                        System.out.println(href);
                                        evt.preventDefault();
                                    }
                                }, false);
                            }
                            break;
                        case CANCELLED:
                            DisplayManager.getInstance().stopSpinning();
                            break;
                        case FAILED:
                            DisplayManager.getInstance().stopSpinning();
                            break;
                    }
                });

        webContentContainer.getChildren().addAll(webView);
        webView.setVisible(false);

        ApplicationStatus.getInstance().modeProperty().addListener(new ChangeListener<ApplicationMode>()
        {
            @Override
            public void changed(ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue)
            {
//                if (newValue == ApplicationMode.NEWS)
//                {
//                    loadWebData();
//                }
            }
        });

        forwardButton.disableProperty().bind(forwardButtonUnavailable);
    }

    public void loadWebData()
    {
        if (newsArticles == null
                || newsArticles.isEmpty()
                || currentArticleIndex < 0)
        {
            webView.setVisible(false);
            forwardButtonUnavailable.set(true);
        } else
        {
            webEngine.load(newsArticles.get(currentArticleIndex).getLink());
            webView.setVisible(true);
            forwardButtonUnavailable.set((currentArticleIndex >= newsArticles.size() - 1));
        }
    }

    private boolean checkSiteIsReachable()
    {
        //This has been introduced since a lack of response from the far end seems to cause WebEngine to create lots and lots of threads
        // culminating in out of memory errors and preventing the printer from connecting.
        // Bit of a sticking plaster, but check to see if the site is reachable first...

        boolean available = false;

        try
        {
            URL obj = new URL(allAutoMakerNewsFlashesURL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.44 (KHTML, like Gecko) JavaFX/8.0 Safari/537.44");

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setConnectTimeout(500);
            int responseCode = con.getResponseCode();

            if (responseCode == 200
                    && con.getContentLength() > 0)
            {
                available = true;
            } else
            {
                steno.warning("My Mini Factory site unavailable");
            }
        } catch (IOException ex)
        {
            steno.error("Exception whilst attempting to contact My Mini Factory site");
        }

        return available;
    }

    @Override
    public void hereIsTheNews(List<NewsBot.NewsArticle> newsArticleList)
    {
        this.newsArticles = new ArrayList(newsArticleList);
        if (newsArticles.size() > 0)
        {
            currentArticleIndex = 0;
        } else
        {
            currentArticleIndex = -1;
        }
    }

}
