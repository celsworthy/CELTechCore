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

/**
 *
 * @author Ian
 */
public class Spinner extends StackPane implements Initializable
{

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
            stage.initModality(Modality.APPLICATION_MODAL);
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

    private Stage stageToCentreOn = null;

    private void relocateWaitTimer()
    {
        System.out.println("Stage x " + stageToCentreOn.xProperty().get());
        System.out.println("Stage y " + stageToCentreOn.yProperty().get());
        System.out.println("Stage w " + stageToCentreOn.widthProperty().get());
        System.out.println("Stage h " + stageToCentreOn.heightProperty().get());
    }

    public void centreOnStage(Stage stageToCentreOn)
    {
        this.stageToCentreOn = stageToCentreOn;

        stageToCentreOn.xProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                relocateWaitTimer();
            });

        stageToCentreOn.yProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                relocateWaitTimer();
            });
        stageToCentreOn.widthProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                relocateWaitTimer();
            });
        stageToCentreOn.heightProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                relocateWaitTimer();
            });

        relocateWaitTimer();
    }

}
