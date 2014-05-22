package celtech.coreUI.components;

import celtech.configuration.ApplicationConfiguration;
import java.net.URL;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 *
 * @author Ian
 */
public class SlideoutAndProjectHolder extends HBox
{

    private final VBox slideOutHolder = new VBox();
    private Button slideButton = null;
    private final VBox projectTabPaneHolder = new VBox();

    private ObjectProperty<HBox> panelToSlide = new SimpleObjectProperty<>();

    private BooleanProperty slidIn = new SimpleBooleanProperty(false);
    private boolean sliding = false;
    private final int slideMs = 250;
    private final Animation hideSidebar = new Transition()
    {
        {
            setCycleDuration(Duration.millis(slideMs));
        }

        @Override
        public void interpolate(double frac)
        {
            slideMenuPanel(1.0 - frac);
        }
    };
    private final Animation showSidebar = new Transition()
    {
        {
            setCycleDuration(Duration.millis(slideMs));
        }

        @Override
        public void interpolate(double frac)
        {
            slideMenuPanel(frac);
        }
    };

    public SlideoutAndProjectHolder()
    {
        hideSidebar.onFinishedProperty().set(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                slidIn.set(true);
            }
        });

        // create an animation to show a sidebar.
        showSidebar.onFinishedProperty().set(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                slidIn.set(false);
            }
        });

        try
        {
            URL fxmlFileName = getClass().getResource(ApplicationConfiguration.fxmlResourcePath + "slideHandleButton.fxml");
            FXMLLoader buttonLoader = new FXMLLoader(fxmlFileName);
            slideButton = (Button) buttonLoader.load();
        } catch (Exception ex)
        {
            System.out.println("Exception: " + ex.getMessage());
        }

        getStyleClass().add("slideout-and-project-holder");

        slideButton.getStyleClass().add("slideout-control-button");
        slideButton.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                toggleSlide();
            }
        });
        slideButton.disableProperty().bind(panelToSlide.isNull());

        getChildren().addAll(slideOutHolder, slideButton, projectTabPaneHolder);
    }

    public void toggleSlide()
    {
        if (slidIn.get())
        {
            startSlidingOut();
        } else
        {
            startSlidingIn();
        }
    }

    public void slideIn()
    {
        slideMenuPanel(0.0);
        slidIn.set(true);
    }

    public void slideMenuPanel(double amountToShow)
    {
        double adjustedWidth = (panelToSlide.get().getMaxWidth() * amountToShow);
        panelToSlide.get().setMinWidth(adjustedWidth);
        panelToSlide.get().setPrefWidth(adjustedWidth);
    }

    public boolean startSlidingOut()
    {
        if (hideSidebar.statusProperty().get() == Animation.Status.STOPPED)
        {
            showSidebar.play();
            return true;
        } else
        {
            return false;
        }
    }

    public boolean startSlidingIn()
    {
        if (showSidebar.statusProperty().get() == Animation.Status.STOPPED)
        {
            hideSidebar.play();
            return true;
        } else
        {
            return false;
        }
    }

    public boolean isSlidIn()
    {
        return slidIn.get();
    }

    public boolean isSliding()
    {
        return showSidebar.statusProperty().get() != Animation.Status.STOPPED || hideSidebar.statusProperty().get() != Animation.Status.STOPPED;
    }

    public void switchInSlideout(HBox slideout)
    {
        if (slideOutHolder.getChildren().isEmpty() == false)
        {
            slideOutHolder.getChildren().remove(0);
        }

        panelToSlide.set(slideout);

        if (slideout != null)
        {
            slideOutHolder.getChildren().add(slideout);
            slideIn();
        }
    }

    public VBox getProjectTabPaneHolder()
    {
        return projectTabPaneHolder;
    }

    public void populateProjectDisplay(Node node)
    {
        projectTabPaneHolder.getChildren().add(node);
    }
}
