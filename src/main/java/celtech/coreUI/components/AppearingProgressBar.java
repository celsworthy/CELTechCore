/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.Lookup;
import celtech.printerControl.PrinterStatus;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
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

    private ReadOnlyDoubleProperty currentValueProperty = null;
    private ReadOnlyDoubleProperty currentTargetProperty = null;
    private ReadOnlyIntegerProperty currentEtcProperty = null;
    private DisplayMode displayMode = DisplayMode.PERCENT;
    private ChangeListener<Number> progressChangeListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
    {
        redraw();
    };

    enum DisplayMode
    {

        PERCENT, TEMPERATURE;
    }

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

        slideIn();
    }

    public void manuallyConfigure(String progressDescription,
            ReadOnlyDoubleProperty currentValueProperty,
            ReadOnlyDoubleProperty currentTargetProperty)
    {
        if (progressDescription == null)
        {
            startSlidingIn();
            unbind();
            currentTargetProperty = null;
            currentValueProperty = null;
        } else
        {
            unbind();
            largeProgressDescription.setText(progressDescription);
            this.currentTargetProperty = currentTargetProperty;
            this.currentTargetProperty.addListener(progressChangeListener);
            this.currentValueProperty = currentValueProperty;
            this.currentValueProperty.addListener(progressChangeListener);
            displayMode = DisplayMode.PERCENT;
            progressBarElement.setVisible(true);
            largeTargetLegend.setVisible(false);
            largeTargetValue.setVisible(false);
            startSlidingOut();
        }

        redraw();
    }

    public void configureForStatus(PrinterStatus printerStatus,
            ReadOnlyDoubleProperty currentValueProperty,
            ReadOnlyDoubleProperty currentTargetProperty,
            ReadOnlyIntegerProperty etcProperty)
    {
        if (printerStatus == null)
        {
            startSlidingIn();
            unbind();
            currentTargetProperty = null;
            currentValueProperty = null;
        } else
        {
            unbind();

            if (printerStatus != PrinterStatus.IDLE)
            {
                largeProgressDescription.setText(printerStatus.getI18nString());
            }
            this.currentTargetProperty = currentTargetProperty;
            this.currentTargetProperty.addListener(progressChangeListener);
            this.currentValueProperty = currentValueProperty;
            this.currentValueProperty.addListener(progressChangeListener);

            switch (printerStatus)
            {
                case HEATING_BED:
                case HEATING_NOZZLE:
                    progressBarElement.setVisible(true);
                    largeTargetLegend.setVisible(true);
                    largeTargetValue.setVisible(true);
                    largeTargetValue.textProperty().bind(currentTargetProperty.asString("%.0f")
                            .concat(Lookup.i18n("misc.degreesC")));
                    largeTargetLegend.setText(Lookup.i18n("progressBar.targetTemperature"));
                    displayMode = DisplayMode.TEMPERATURE;
                    startSlidingOut();
                    break;
                case PRINTING:
                    progressBarElement.setVisible(true);
                    displayMode = DisplayMode.PERCENT;
                    if (etcProperty != null)
                    {
                        this.currentEtcProperty = etcProperty;
                        largeTargetLegend.setVisible(true);
                        largeTargetLegend.setText(Lookup.i18n("dialogs.progressETCLabel"));
                        largeTargetValue.textProperty().bind(new StringBinding()
                        {
                            {
                                super.bind(currentEtcProperty);
                            }

                            @Override
                            protected String computeValue()
                            {
                                int secondsRemaining = currentEtcProperty.intValue();
                                secondsRemaining += 30;
                                if (secondsRemaining > 60)
                                {
                                    String hoursMinutes = convertToHoursMinutes(
                                            secondsRemaining);
                                    return hoursMinutes;
                                } else
                                {
                                    return Lookup.i18n("dialogs.lessThanOneMinute");
                                }
                            }
                        });
                        largeTargetValue.setVisible(true);
                    } else
                    {
                        largeTargetLegend.setVisible(false);
                        largeTargetValue.setVisible(false);
                        largeTargetLegend.setText("");
                    }
                    startSlidingOut();
                    break;
                case SLICING:
                case POST_PROCESSING:
                    progressBarElement.setVisible(true);
                    largeTargetLegend.setVisible(false);
                    largeTargetValue.setVisible(false);
                    largeTargetValue.textProperty().bind(currentTargetProperty.asString("%.0f%%"));
                    largeTargetLegend.setText("");
                    displayMode = DisplayMode.PERCENT;
                    startSlidingOut();
                    break;
                case IDLE:
                    startSlidingIn();
                    progressBarElement.setVisible(false);
                    largeTargetLegend.setVisible(false);
                    largeTargetValue.setVisible(false);
                    break;
                default:
                    displayMode = DisplayMode.PERCENT;
                    progressBarElement.setVisible(false);
                    largeTargetLegend.setVisible(false);
                    largeTargetValue.setVisible(false);
                    startSlidingOut();
                    break;
            }
        }

        redraw();
    }

    private String convertToHoursMinutes(int seconds)
    {
        int minutes = (int) (seconds / 60);
        int hours = minutes / 60;
        minutes = minutes - (60 * hours);
        return String.format("%02d:%02d", hours, minutes);
    }

    private void redraw()
    {
        double normalisedProgress = 0;

        if (currentValueProperty != null
                && currentTargetProperty != null)
        {
            normalisedProgress = Math.max(Math.min(currentValueProperty.get() / currentTargetProperty.get(), 1.0), 0);
            switch (displayMode)
            {
                case PERCENT:
                    largeProgressCurrentValue.setText(String.format("%.0f", normalisedProgress * 100.0).concat("%"));
                    break;
                case TEMPERATURE:
                    largeProgressCurrentValue.setText(String.format("%.0f", currentValueProperty.get()).concat(Lookup.i18n("misc.degreesC")));
                    break;
            }
            largeProgressCurrentValue.setVisible(true);
        } else
        {
            largeProgressCurrentValue.setText("");
            largeProgressCurrentValue.setVisible(false);
        }
        double progressBackWidth = largeProgressBarBack.getWidth();
        double barWidth = progressBackWidth * normalisedProgress;

        largeProgressBarInner.setWidth(barWidth);

        // place currentValue in correct place on progress bar (just to the left of RHS of the bar)
        double barEndXPosition = largeProgressBarInner.getLayoutX()
                + largeProgressBarInner.boundsInParentProperty().get().getWidth();
        double barStartXPosition = largeProgressBarInner.getLayoutX();
        double currentValueWidth = largeProgressCurrentValue.boundsInParentProperty().get().
                getWidth();
        int OFFSET_FROM_PROGRESS_BAR_RHS = 10;  // px
        double requiredCurrentValueXPosition = barEndXPosition - currentValueWidth
                - OFFSET_FROM_PROGRESS_BAR_RHS;

        double leftmostValuePositionAllowed = barStartXPosition + 2;
        if (requiredCurrentValueXPosition < leftmostValuePositionAllowed)
        {
            requiredCurrentValueXPosition = leftmostValuePositionAllowed;
        }

        double currentX = largeProgressCurrentValue.getLayoutX();
        double requiredTranslate = requiredCurrentValueXPosition - currentX;

        largeProgressCurrentValue.setTranslateX(requiredTranslate);
    }

    private void unbind()
    {
        largeTargetValue.textProperty().unbind();

        if (currentValueProperty != null)
        {
            currentValueProperty.removeListener(progressChangeListener);
        }

        if (currentTargetProperty != null)
        {
            currentTargetProperty.removeListener(progressChangeListener);
        }
    }

    private Animation hideSidebar = null;
    private Animation showSidebar = null;
    private boolean hidden = false;
    private final double minimumToShow = 0.0;
    private final double maximumToShow = 1.0;
    private boolean slidIn = false;
    private SlidingComponentDirection directionToSlide = SlidingComponentDirection.UP_FROM_BOTTOM;
    private double panelWidth = 0;
    private double panelHeight = 0;
    private double panelLayoutMinX = 0;
    private double panelLayoutMinY = 0;
    private final Rectangle clippingRectangle = new Rectangle();
    private double lastAmountShown = 0;

    /**
     *
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
    private void slideIn()
    {
        slideMenuPanel(0.0);
        hidden = true;
    }

    /**
     *
     */
    private void slideOut()
    {
        slideMenuPanel(1.0);
        hidden = false;
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
            setMaxHeight(targetPanelHeight);
            setMinHeight(0);
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
    private boolean startSlidingOut()
    {
        if (hideSidebar.statusProperty().get() == Animation.Status.STOPPED
                && slidIn)
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
    private boolean startSlidingIn()
    {
        if (showSidebar.statusProperty().get() == Animation.Status.STOPPED
                && !slidIn)
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
        panelHeight = getPrefHeight();
        widthProperty().addListener((ObservableValue<? extends Number> ov, Number t, Number newWidth) ->
        {
            panelWidth = newWidth.doubleValue();
            slideMenuPanel(lastAmountShown);
            redraw();
        });
    }
}
