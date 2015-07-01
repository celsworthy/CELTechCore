/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 *
 * @author tony
 */
public abstract class AppearingProgressBar extends BorderPane implements Initializable
{

    @FXML
    protected Label largeTargetValue;

    @FXML
    protected Label largeProgressDescription;

    @FXML
    protected Label largeProgressCurrentValue;

    @FXML
    protected Label largeTargetLegend;

    @FXML
    protected ProgressBar progressBar;

    private static final Duration transitionLengthMillis = Duration.millis(250);

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
    private boolean slidCompletelyInToView = false;
    private boolean slidCompletelyOutOfView = false;
    private SlidingComponentDirection directionToSlide = SlidingComponentDirection.UP_FROM_BOTTOM;
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
            slidCompletelyInToView = true;
        });

        hideSidebar.setOnFinished((ActionEvent t) ->
        {
            slidCompletelyOutOfView = true;
        });

        slideOutOfView();
    }

    /**
     *
     */
    public void slideInToView()
    {
        slideMenuPanel(1.0);
        slidCompletelyInToView = true;
        slidCompletelyOutOfView = false;
    }

    /**
     *
     */
    public void slideOutOfView()
    {
        slideMenuPanel(0.0);
        slidCompletelyInToView = false;
        slidCompletelyOutOfView = true;
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

        if (directionToSlide == SlidingComponentDirection.IN_FROM_LEFT
                || directionToSlide == SlidingComponentDirection.IN_FROM_RIGHT)
        {
            double targetPanelWidth = panelWidth * amountToShow;
            double widthToHide = panelWidth - targetPanelWidth;
            double translateByX = 0;

            if (directionToSlide == SlidingComponentDirection.IN_FROM_LEFT)
            {
                translateByX = -panelWidth + targetPanelWidth;
                clippingRectangle.setX(-translateByX);
            } else
            {
                translateByX = panelWidth - targetPanelWidth;
            }

            clippingRectangle.setHeight(panelHeight);
            clippingRectangle.setWidth(targetPanelWidth);

            setClip(clippingRectangle);
            setTranslateX(translateByX);
        } else if (directionToSlide == SlidingComponentDirection.DOWN_FROM_TOP
                || directionToSlide == SlidingComponentDirection.UP_FROM_BOTTOM)
        {
            System.out.println("Called with " + amountToShow);
            double targetPanelHeight = panelHeight * amountToShow;
            double heightToHide = panelHeight - targetPanelHeight;
            double translateByY = 0;

            if (directionToSlide == SlidingComponentDirection.DOWN_FROM_TOP)
            {
                translateByY = -panelHeight + targetPanelHeight;
                clippingRectangle.setY(panelLayoutMinY + heightToHide);

            } else
            {
                translateByY = panelHeight - targetPanelHeight;
            }

            clippingRectangle.setHeight(targetPanelHeight);
            clippingRectangle.setWidth(panelWidth);

            setClip(clippingRectangle);
            setTranslateY(translateByY);
            setMaxHeight(targetPanelHeight);
            setMinHeight(0);
        }
    }

    /**
     *
     * @return
     */
    public boolean isSliding()
    {
        return showSidebar.statusProperty().get() != Animation.Status.STOPPED
                || hideSidebar.statusProperty().get() != Animation.Status.STOPPED;
    }

    /**
     *
     */
    public void startSlidingOutOfView()
    {
        if (!slidCompletelyOutOfView
                && hideSidebar.getStatus() == Animation.Status.STOPPED)
        {
            slidCompletelyInToView = false;
            slidCompletelyOutOfView = false;
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
        }
    }

    /**
     *
     */
    public void startSlidingInToView()
    {
        if (!slidCompletelyInToView
                && showSidebar.getStatus() == Animation.Status.STOPPED)
        {
            slidCompletelyInToView = false;
            slidCompletelyOutOfView = false;
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
        }
    }

    /**
     *
     * @return
     */
    public boolean isSlidIn()
    {
        return slidCompletelyInToView;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        panelHeight = getPrefHeight();
        widthProperty().addListener((ObservableValue<? extends Number> ov, Number t, Number newWidth) ->
        {
            panelWidth = newWidth.doubleValue();
            slideMenuPanel(lastAmountShown);
        });

        largeProgressCurrentValue.translateXProperty().bind(new DoubleBinding()
        {
            {
                super.bind(progressBar.widthProperty(), progressBar.progressProperty());
            }

            @Override
            protected double computeValue()
            {
                double barWidth = progressBar.widthProperty().get();
                double textWidth = largeProgressCurrentValue.getLayoutBounds().getWidth();
                double minTranslation = -(barWidth / 2 - textWidth);
                double maxTranslation = barWidth / 2 + textWidth / 2;

                double translation = (progressBar.progressProperty().get() * barWidth)
                        - barWidth / 2;

                double offsetForTextWidth = textWidth / 2 + 10;

                translation -= offsetForTextWidth;

                if (translation > maxTranslation)
                {
                    translation = maxTranslation;
                } else if (translation < minTranslation)
                {
                    translation = minTranslation;
                }
                return translation;
            }
        });
    }

    protected void unbindVariables()
    {
        largeProgressCurrentValue.textProperty().unbind();
        largeProgressCurrentValue.setVisible(false);
        largeTargetValue.textProperty().unbind();
        largeTargetValue.setVisible(false);
        largeTargetLegend.setVisible(false);
        progressBar.progressProperty().unbind();
        progressBar.setVisible(false);
    }
}
