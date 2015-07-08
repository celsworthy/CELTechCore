/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.Lookup;
import celtech.coreUI.components.buttons.GraphicButton;
import celtech.utils.Math.MathUtils;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 *
 * @author tony
 */
public abstract class AppearingProgressBar extends StackPane implements Initializable
{

    @FXML
    protected Label largeTargetValue;

    @FXML
    protected Label largeProgressDescription;

    @FXML
    protected Label largeTargetLegend;

    @FXML
    protected ProgressBar progressBar;

    @FXML
    protected GraphicButton pauseButton;

    @FXML
    protected GraphicButton resumeButton;

    @FXML
    protected GraphicButton cancelButton;

    private static final Duration transitionLengthMillis = Duration.millis(125);

    private Animation hideSidebar = new Transition()
    {
        {
            setCycleDuration(transitionLengthMillis);
        }

        @Override
        public void interpolate(double frac)
        {
            slideMenuPanel(1.0 - frac);
        }
    };
    private Animation showSidebar = new Transition()
    {

        {
            setCycleDuration(transitionLengthMillis);
        }

        @Override
        public void interpolate(double frac)
        {
            slideMenuPanel(frac);
        }
    };

    private final double minimumToShow = 0.0;
    private final double maximumToShow = 1.0;
    private boolean slidingIntoView = false;
    private boolean slidingOutOfView = false;
    private boolean slidIntoView = false;
    private boolean slidOutOfView = false;
    private double panelWidth = 0;
    private double panelHeight = 0;
    private double panelLayoutMinX = 0;
    private double panelLayoutMinY = 0;
    private final Rectangle clippingRectangle = new Rectangle();
    private double lastAmountShown = 0;

    public AppearingProgressBar()
    {
        super();

        URL fxml = getClass().getResource(
                "/celtech/resources/fxml/components/appearingProgressBar.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.setClassLoader(getClass().getClassLoader());

        try
        {
            fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }

        showSidebar.setOnFinished((ActionEvent t) ->
        {
            slidingIntoView = false;
            slidIntoView = true;
        });

        hideSidebar.setOnFinished((ActionEvent t) ->
        {
            slidingOutOfView = false;
            slidOutOfView = true;
            setVisible(false);
        });
    }

    /**
     *
     * @param amountToShow
     */
    private void slideMenuPanel(double amountToShow)
    {
        lastAmountShown = amountToShow;

        if (amountToShow < minimumToShow)
        {
            amountToShow = minimumToShow;
        } else if (amountToShow > maximumToShow)
        {
            amountToShow = maximumToShow;
        }

        double targetPanelHeight = panelHeight * amountToShow;

        clippingRectangle.setHeight(panelHeight - targetPanelHeight);
//        clippingRectangle.setTranslateY(tar);
        clippingRectangle.setWidth(panelWidth);

        setPrefHeight(targetPanelHeight);
    }

    /**
     *
     */
    public void startSlidingOutOfView()
    {
        if (!isSlidOutOrSlidingOut())
        {
            slidingOutOfView = true;
            slidIntoView = false;
            slidOutOfView = false;
            showSidebar.stop();
            Duration time = showSidebar.getCurrentTime();
            Duration startFromTime;
            if (time.lessThanOrEqualTo(Duration.ZERO))
            {
                startFromTime = Duration.ZERO;
            } else
            {
                startFromTime = transitionLengthMillis.subtract(time);
            }
            hideSidebar.jumpTo(startFromTime);
            hideSidebar.play();
        } else if (slidOutOfView)
        {
            if (MathUtils.compareDouble(getPrefHeight(), 0.0, 0.01) == MathUtils.MORE_THAN)
            {
                slideMenuPanel(0);
            }
        }

    }

    /**
     *
     */
    public void startSlidingInToView()
    {
        if (!isSlidInOrSlidingIn())
        {
            setVisible(true);
            slidingIntoView = true;
            slidIntoView = false;
            slidOutOfView = false;
            hideSidebar.stop();
            Duration time = hideSidebar.getCurrentTime();
            Duration startFromTime;
            if (time.lessThanOrEqualTo(Duration.ZERO))
            {
                startFromTime = Duration.ZERO;
            } else
            {
                startFromTime = transitionLengthMillis.subtract(time);
                showSidebar.jumpTo(startFromTime);
            }
            showSidebar.play();
        } else if (slidIntoView)
        {
            if (MathUtils.compareDouble(getPrefHeight(), 1.0, 0.01) == MathUtils.LESS_THAN)
            {
                slideMenuPanel(1.0);
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        panelHeight = getPrefHeight();
//        widthProperty().addListener((ObservableValue<? extends Number> ov, Number t, Number newWidth) ->
//        {
//            panelWidth = newWidth.doubleValue();
//            slideMenuPanel(lastAmountShown);
//        });

        setMinHeight(0);

        slideMenuPanel(0);
        slidIntoView = false;
        slidOutOfView = true;
        slidingIntoView = false;
        slidingOutOfView = false;

        pauseButton.setVisible(false);
        resumeButton.setVisible(false);
        cancelButton.setVisible(false);    
        
        clippingRectangle.setHeight(0);
        
        setVisible(false);
//        setClip(clippingRectangle);
    }

    public boolean isSlidInOrSlidingIn()
    {
        return slidIntoView || slidingIntoView;
    }

    public boolean isSlidOutOrSlidingOut()
    {
        return slidOutOfView || slidingOutOfView;
    }

    public void targetRequired(boolean required)
    {
        largeTargetLegend.setVisible(required);
        largeTargetValue.setVisible(required);
    }

    public void progressRequired(boolean required)
    {
        progressBar.setVisible(required);
    }
}
