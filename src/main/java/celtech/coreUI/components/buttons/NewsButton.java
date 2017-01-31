package celtech.coreUI.components.buttons;

import celtech.appManager.NewsBot;
import celtech.appManager.NewsListener;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.MachineType;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.apache.commons.math3.util.FastMath;

/**
 * There should only be one instance of this news button...
 *
 * @author Ian
 */
public class NewsButton extends GraphicButton implements NewsListener
{

    private static final String fxmlFileName = "newsButton";
    private NewsBot newsBot = null;
    private final Tooltip tooltip = new Tooltip();
    private static final String allAutoMakerNewsFlashesURL = "http://www.cel-robox.com/category/AutoMakerNewsflash";

    @FXML
    private Text newsCounter;
    @FXML
    private StackPane newsCounterContainer;

    public NewsButton()
    {
        super(fxmlFileName);

        newsCounterContainer.setVisible(false);

        setOnAction((event) ->
        {
            if (Desktop.isDesktopSupported()
                    && BaseConfiguration.getMachineType()
                    != MachineType.LINUX_X86
                    && BaseConfiguration.getMachineType()
                    != MachineType.LINUX_X64)
            {
                try
                {
                    newsBot.allNewsHasBeenRead();
                    URI linkToVisit = new URI(allAutoMakerNewsFlashesURL);
                    Desktop.getDesktop().browse(linkToVisit);
                } catch (IOException | URISyntaxException ex)
                {
                    System.err.println("Error when attempting to browse to "
                            + allAutoMakerNewsFlashesURL);
                }
            } else
            {
                System.err.println(
                        "Couldn't get Desktop - not able to support hyperlinks");
            }
//            ApplicationStatus.getInstance().setMode(ApplicationMode.NEWS);
        });

        newsBot = NewsBot.getInstance();
        newsBot.registerListener(this);
    }

    @Override
    public void hereIsTheNews(List<NewsBot.NewsArticle> newsArticles)
    {
        if (newsArticles.isEmpty())
        {
            newsCounterContainer.setVisible(false);
            Tooltip.uninstall(this, tooltip);
        } else
        {
            newsCounterContainer.setVisible(true);
            int numberOfItemsToUse = FastMath.min(newsArticles.size(), 99);
            newsCounter.setText(String.valueOf(numberOfItemsToUse));

            StringBuilder tooltipBuilder = new StringBuilder();
            tooltipBuilder.append("--- AutoMaker News ---");
            newsArticles.forEach((article) ->
            {
                if (tooltipBuilder.length() > 0)
                {
                    tooltipBuilder.append("\n");
                }
                tooltipBuilder.append(article.getTitle());
            });

            tooltip.setText(tooltipBuilder.toString());
            Tooltip.install(this, tooltip);
        }
    }
}
