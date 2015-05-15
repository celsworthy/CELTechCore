/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 *
 * @author tony
 */
public class AppearingProgressBar extends BorderPane implements Initializable
{

    @FXML
    private Rectangle largeProgressBarInner;

    @FXML
    private HBox largeProgressBarBack;

    @FXML
    private Label largeTargetValue;

    @FXML
    private Label largeProgressDescription;

    @FXML
    private Label largeProgressCurrentValue;

    @FXML
    private StackPane progressBarElement;

    @FXML
    private Label largeTargetLegend;

    private final ReadOnlyDoubleProperty currentValue = new SimpleDoubleProperty(0);
    private final ReadOnlyDoubleProperty currentTarget = new SimpleDoubleProperty(0);

    private final ChangeListener<Number> progressChangeListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
//        setProgressPercent(newValue.doubleValue());
    };

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

        hideSidebar = new Transition()
        {

            {
                setCycleDuration(Duration.millis(250));
            }

            @Override
            public void interpolate(double frac)
            {
                slideMenuPanel(1.0 - frac);
            }
        };

        // create an animation to show a sidebar.
        showSidebar = new Transition()
        {

            {
                setCycleDuration(Duration.millis(250));
            }

            @Override
            public void interpolate(double frac)
            {
                slideMenuPanel(frac);
            }
        };

        largeProgressBarBack.boundsInLocalProperty().addListener(
                (ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) ->
                {
//                    redraw();
                });

//        hideSidebar = new Transition()
//        {
//            {
//                setCycleDuration(Duration.millis(delayTime));
//            }
//
//            @Override
//            public void interpolate(double frac)
//            {
//                slideMenuPanel(1.0 - frac);
//            }
//        };
//
//        hideSidebar.onFinishedProperty().set(new EventHandler<ActionEvent>()
//        {
//            @Override
//            public void handle(ActionEvent event)
//            {
//                slidIn = true;
//            }
//        });
//
//        // create an animation to show a sidebar.
//        showSidebar = new Transition()
//        {
//            {
//                setCycleDuration(Duration.millis(delayTime));
//            }
//
//            @Override
//            public void interpolate(double frac)
//            {
//                slideMenuPanel(frac);
//            }
//        };
//
//        showSidebar.onFinishedProperty().set(new EventHandler<ActionEvent>()
//        {
//            @Override
//            public void handle(ActionEvent event)
//            {
//                slidIn = false;
//            }
//        });
//        redraw();
    }

//    public void setProgressPercent(double progress)
//    {
//        if (progress != currentValue.get())
//        {
//            currentValue.set(progress);
//            redraw();
//        }
//    }
//
//    public void setTargetValue(String targetValue)
//    {
//        largeTargetValue.setText(targetValue);
//    }
//
//    public void setProgressDescription(String progressDescription)
//    {
//        largeProgressDescription.setText(progressDescription);
//    }
//
//    public void setTargetLegend(String targetLegend)
//    {
//        largeTargetLegend.setText(targetLegend);
//    }
//
//    public void setCurrentValue(String currentValue)
//    {
//        largeProgressCurrentValue.setText(currentValue);
//    }
//    public void configureForStatus(PrinterMetaStatus metaStatus)
//    {
//        switch (metaStatus)
//        {
//            case HEATING_BED:
//            case HEATING_NOZZLE:
//                progressBarElement.setVisible(true);
//                largeTargetLegend.setVisible(true);
//                largeTargetValue.setVisible(true);
//                largeTargetValue.textProperty().unbind();
//                largeTargetValue.textProperty().bind(
//                        metaStatus.get()
//                        .currentStatusValueTargetProperty()
//                        .asString("%.0f").concat(Lookup.i18n("misc.degreesC")));
//                largeTargetLegend.setText(Lookup.i18n("progressBar.targetTemperature"));
//                largeProgressCurrentValue.textProperty().bind(
//                        metaStatus.get()
//                        .currentStatusValueProperty().asString("%.0f").concat(Lookup.i18n(
//                                        "misc.degreesC")));
//                this.setVisible(true);
//                break;
//            case PRINTING:
//            case SLICING:
//            case POST_PROCESSING:
//                progressBarElement.setVisible(true);
//                largeTargetLegend.setVisible(false);
//                largeTargetValue.setVisible(false);
//                largeTargetValue.textProperty().unbind();
//                largeTargetValue.textProperty().bind(
//                        metaStatus.get()
//                        .currentStatusValueTargetProperty()
//                        .asString("%.0f%%"));
//                largeTargetLegend.setText("");
//                largeProgressCurrentValue.textProperty().bind(
//                        metaStatus.get()
//                        .currentStatusValueProperty().asString("%.0f%%"));
//                this.setVisible(true);
//                break;
//            case IDLE:
//                progressBarElement.setVisible(false);
//                largeTargetLegend.setVisible(false);
//                largeTargetValue.setVisible(false);
//                this.setVisible(false);
//                break;
//            default:
//                progressBarElement.setVisible(false);
//                largeTargetLegend.setVisible(false);
//                largeTargetValue.setVisible(false);
//                this.setVisible(true);
//                break;
//        }
//
//    }
//    private void redraw()
//    {
//        double normalisedProgress = 0;
//
//        double progressBackWidth = largeProgressBarBack.getWidth();
//        double barWidth = progressBackWidth * normalisedProgress;
//        largeProgressBarInner.setWidth(barWidth);
//
//        // place currentValue in correct place on progress bar (just to the left of RHS of the bar)
//        double barEndXPosition = largeProgressBarInner.getLayoutX()
//                + largeProgressBarInner.boundsInParentProperty().get().getWidth();
//        double barStartXPosition = largeProgressBarInner.getLayoutX();
//        double currentValueWidth = largeProgressCurrentValue.boundsInParentProperty().get().
//                getWidth();
//        int OFFSET_FROM_PROGRESS_BAR_RHS = 10;  // px
//        double requiredCurrentValueXPosition = barEndXPosition - currentValueWidth
//                - OFFSET_FROM_PROGRESS_BAR_RHS;
//
//        double leftmostValuePositionAllowed = barStartXPosition + 2;
//        if (requiredCurrentValueXPosition < leftmostValuePositionAllowed)
//        {
//            requiredCurrentValueXPosition = leftmostValuePositionAllowed;
//        }
//
//        double currentX = largeProgressCurrentValue.getLayoutX();
//        double requiredTranslate = requiredCurrentValueXPosition - currentX;
//        largeProgressCurrentValue.setTranslateX(requiredTranslate);
//        if (currentValue < 5)
//        {
//            largeProgressCurrentValue.setTextFill(Color.DARKBLUE);
//        } else
//        {
//            largeProgressCurrentValue.setTextFill(Color.web("#000000"));
//        }
//    }
//
//    public void bindToData(PrinterStatus printerStatus, 
//            ReadOnlyDoubleProperty currentValue,
//            ReadOnlyDoubleProperty currentTarget)
//    {
//        largeProgressDescription.textProperty().bind(printerStatus.asString());
//        largeTargetValue.setText("");
//        largeTargetLegend.setText("");
//        largeProgressCurrentValue.setText("");
//        this.
//        c.addListener(progressChangeListener);
//        printerMetaStatus.printerStatusProperty().addListener(statusChangeListener);
//    }
//
//    public void unbindProgress()
//    {
//        largeProgressDescription.textProperty().unbind();
//        largeTargetValue.textProperty().unbind();
//        largeProgressCurrentValue.textProperty().unbind();
//        progressProperty.removeListener(progressChangeListener);
//        progressProperty.unbind();
//        if (printerMetaStatus.isPresent())
//        {
//            printerMetaStatus.get().printerStatusProperty().removeListener(statusChangeListener);
//            printerMetaStatus = Optional.empty();
//        }
//    }
    private Animation hideSidebar = null;
    private Animation showSidebar = null;
    private boolean hidden = false;
    private final double minimumToShow = 0.1;
    private final double maximumToShow = 1.0;
    private boolean slidIn = false;
    private SlidingComponentDirection directionToSlide = SlidingComponentDirection.UP_FROM_BOTTOM;
    private double panelWidth = 0;
    private double panelHeight = 0;
    private double panelLayoutMinX = 0;
    private double panelLayoutMinY = 0;
    private final Rectangle clippingRectangle = new Rectangle();

    /**
     *
     * @param paneToSlide
     * @param directionToSlide
     */
    public void configurePanel(SlidingComponentDirection directionToSlide)
    {
        this.directionToSlide = directionToSlide;
    }

    /**
     *
     */
    public void toggleSlide()
    {
        if (slidIn)
        {
            startSlidingOut();
        } else
        {
            startSlidingIn();
        }
    }

    /**
     *
     */
    public void slideIn()
    {
        slideMenuPanel(0.0);
        hidden = true;
    }

    /**
     *
     */
    public void slideOut()
    {
        slideMenuPanel(1.0);
        hidden = false;
    }

    /**
     *
     * @param amountToShow
     */
    public void slideMenuPanel(double amountToShow)
    {
        if (amountToShow < minimumToShow)
        {
            amountToShow = minimumToShow;
        } else if (amountToShow > maximumToShow)
        {
            amountToShow = maximumToShow;
        }

        if (amountToShow == minimumToShow)
        {
            slidIn = true;
        } else
        {
            slidIn = false;
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
        }
    }

    /**
     *
     * @return
     */
    public boolean isHidden()
    {
        return hidden;
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
     * @return
     */
    public boolean startSlidingOut()
    {
        if (hideSidebar.statusProperty().get() == Animation.Status.STOPPED)
        {
//            steno.info("Pulling out");
            showSidebar.play();
            return true;
        } else
        {
            return false;
        }
    }

    /**
     *
     * @return
     */
    public boolean startSlidingIn()
    {
        if (showSidebar.statusProperty().get() == Animation.Status.STOPPED)
        {
//            steno.info("Hiding");
            hideSidebar.play();
            return true;
        } else
        {
            return false;
        }
    }

    /**
     *
     * @return
     */
    public boolean isSlidIn()
    {
        return slidIn;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        this.panelWidth = getWidth();
        this.panelHeight = getPrefHeight();
        this.panelLayoutMinX = 0;
        this.panelLayoutMinY = 0;
    }
}
