package celtech.coreUI.components;

import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.ModalDialogController;
import celtech.coreUI.controllers.MyMiniFactoryLoaderController;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class Spinner extends StackPane implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(Spinner.class.getName());
    @FXML
    private SVGPath outerArcs;

    @FXML
    private SVGPath innerArcs;

    private Stage stage = null;
    private AnimationTimer timer = null;

    public Spinner()
    {
        stage = new Stage(StageStyle.TRANSPARENT);

        stage.setResizable(false);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
            "/celtech/resources/fxml/components/spinner.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        fxmlLoader.setClassLoader(this.getClass().getClassLoader());

        try
        {
            StackPane fxmlParent = fxmlLoader.load();
            scaleXProperty().set(0.5);
            scaleYProperty().set(0.5);

            Scene dialogScene = new Scene(fxmlParent, Color.TRANSPARENT);
            dialogScene.getStylesheets().add(ApplicationConfiguration.mainCSSFile);
            stage.setScene(dialogScene);
            stage.initOwner(DisplayManager.getMainStage());
            stage.initModality(Modality.NONE);
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }

        this.getStyleClass().add("spinner");
    }

    public void startSpinning()
    {
        timer.start();
        stage.show();
    }

    public void stopSpinning()
    {
        stage.hide();
        timer.stop();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        timer = new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
                if (outerArcs.isVisible())
                {
                    long milliseconds = (int) (now / 1e6);
                    double outerAngle = milliseconds * 120d / 1000d;
                    double index = (outerAngle % 360);
                    double opacity = Math.abs(index - 180) / 180d;
                    outerArcs.rotateProperty().set(outerAngle);
                    innerArcs.rotateProperty().set(-outerAngle);
                    outerArcs.opacityProperty().set(opacity);
                }
            }
        };
    }

    public void recentre(Stage stageToCentreOn)
    {
//        steno.info("Stage x " + stageToCentreOn.xProperty().get());
//        steno.info("Stage y " + stageToCentreOn.yProperty().get());
//        steno.info("Stage w " + stageToCentreOn.widthProperty().get());
//        steno.info("Stage h " + stageToCentreOn.heightProperty().get());
        
        stage.setX(stageToCentreOn.getX() + stageToCentreOn.getWidth() / 2 - stage.getWidth() / 2);
        stage.setY(stageToCentreOn.getY() + stageToCentreOn.getHeight() / 2 - stage.getHeight() / 2);
    }
    
    public void recentre(Node nodeToCentreOn)
    {
    }

}
