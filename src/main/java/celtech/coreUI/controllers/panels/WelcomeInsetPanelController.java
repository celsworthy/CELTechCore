package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class WelcomeInsetPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        CalibrationInsetPanelController.class.getName());

    @FXML
    private TextFlow titleTextFlow;

    @FXML
    private Label intro;

    @FXML
    private TextFlow textFlow;

    @FXML
    void backToStatusAction(ActionEvent event)
    {
        ApplicationStatus.getInstance().setMode(ApplicationMode.STATUS);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        Text appNameBoldPart = new Text();
        appNameBoldPart.setText(Lookup.i18n("aboutPanel.applicationNamePart1"));
        appNameBoldPart.getStyleClass().add("welcome-title-bold");

        Text appNameLightPart = new Text();
        appNameLightPart.setText(Lookup.i18n("aboutPanel.applicationNamePart2")
        + "\n");
        appNameLightPart.getStyleClass().add("welcome-title-light");

        Text version = new Text();
        version.setText(Lookup.i18n("aboutPanel.version")
            + " "
            + ApplicationConfiguration.getApplicationVersion());
        version.getStyleClass().add("welcome-version");

        titleTextFlow.getChildren().addAll(appNameBoldPart, appNameLightPart, version);

        intro.setText(Lookup.i18n("versionWelcomeBoilerplateIntro"));

        boolean subTitlesToProcess = true;

        String versionWelcomeSubtitleBase = "versionWelcomeSubtitle";
        String versionWelcomeBodyBase = "versionWelcomeBody";

        int lineCounter = 1;

        while (subTitlesToProcess)
        {
            try
            {
                Text welcomeSubtitle = new Text();
                String subTitleString = Lookup.i18n(versionWelcomeSubtitleBase + lineCounter)
                    + "\n";
                welcomeSubtitle.setText(subTitleString);
                welcomeSubtitle.getStyleClass().add("welcome-subtitle");

                Text welcomeBody = new Text();
                welcomeBody.setText(Lookup.i18n(versionWelcomeBodyBase + lineCounter) + "\n");
                welcomeBody.getStyleClass().add("welcome-body");
                textFlow.getChildren().addAll(welcomeSubtitle, welcomeBody);

                lineCounter++;
            } catch (Exception e)
            {
                subTitlesToProcess = false;
            }
        }
    }
}
