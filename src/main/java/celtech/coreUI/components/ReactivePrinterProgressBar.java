/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.Lookup;
import celtech.configuration.BusyStatus;
import celtech.configuration.WhyAreWeWaitingState;
import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Printer;
import celtech.utils.Time.TimeUtils;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.Transition;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
public class ReactivePrinterProgressBar extends BorderPane implements Initializable
{

    @FXML
    private Rectangle progressBarInner;

    @FXML
    private HBox progressBarBack;

    @FXML
    private Label targetValue;

    @FXML
    private Label title;

    @FXML
    private Label progressCurrentValue;

    @FXML
    private Label targetTitle;

    @FXML
    private Label alternateTitle;

    @FXML
    private StackPane progressBar;

    private DoubleProperty progress = new SimpleDoubleProperty(0);

    private Printer associatedPrinter = null;

    private Animation hideSidebar = null;
    private Animation showSidebar = null;
    private boolean slidIn = false;
    private boolean sliding = false;
    private double panelHeight = 0;
    private int delayTime = 250;

    private ChangeListener<PrinterStatus> printerStatusListener = (ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue) ->
    {
        respondToPrinterStatus(newValue);
    };

    public ReactivePrinterProgressBar()
    {
        super();
        URL fxml = getClass().getResource(
            "/celtech/resources/fxml/components/reactivePrinterProgressBar.fxml");
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

        Lookup.getCurrentlySelectedPrinterProperty().addListener(
            (ObservableValue<? extends Printer> observable, Printer lastSelectedPrinter, Printer newlySelectedPrinter) ->
            {
                if (newlySelectedPrinter != null)
                {
                    startNotifierAppearing();
                    associatedPrinter = newlySelectedPrinter;
                    respondToPrinterStatus(associatedPrinter.printerStatusProperty().get());
                    associatedPrinter.printerStatusProperty().addListener(printerStatusListener);
                } else
                {
                    startNotifierDisappearing();
                    associatedPrinter.printerStatusProperty().removeListener(printerStatusListener);
                    progress.unbind();
                }
            });

        progressBarBack.boundsInLocalProperty().addListener(
            (ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) ->
            {
                redraw();
            });

        progress.addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                redraw();
            });

        hideSidebar = new Transition()
        {
            {
                setCycleDuration(Duration.millis(delayTime));
            }

            @Override
            public void interpolate(double frac)
            {
                slideMenuPanel(1.0 - frac);
            }
        };

        hideSidebar.onFinishedProperty().set(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                slidIn = true;
            }
        });

        // create an animation to show a sidebar.
        showSidebar = new Transition()
        {
            {
                setCycleDuration(Duration.millis(delayTime));
            }

            @Override
            public void interpolate(double frac)
            {
                slideMenuPanel(frac);
            }
        };

        showSidebar.onFinishedProperty().set(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                slidIn = false;
            }
        });

        redraw();

        disappear();
    }

    private void unbindElements()
    {
        targetValue.textProperty().unbind();
        progressCurrentValue.textProperty().unbind();
        progress.unbind();
    }

    private void respondToPrinterStatus(PrinterStatus status)
    {
        switch (status)
        {
            case IDLE:
                setVisible(false);
                break;
            case OPENING_DOOR:
            case CANCELLING:
            case LOADING_FILAMENT:
            case EJECTING_FILAMENT:
                setVisible(true);
                title.setVisible(false);
                progressBar.setVisible(false);
                alternateTitle.setVisible(true);
                alternateTitle.setText(status.getI18nString().toUpperCase().concat("..."));
                targetTitle.setVisible(false);
                targetValue.setVisible(false);
                break;
            case PRINTING:
                unbindElements();

                switch (associatedPrinter.getPrinterAncillarySystems().whyAreWeWaitingProperty().
                    get())
                {
                    case BED_HEATING:
                        setupBarForBedHeating(status);
                        break;
                    case COOLING:
                        break;
                    case NOZZLE_HEATING:
                        break;
                    case NOT_WAITING:
                        setupBarForNormalPrinting(status);
                        break;
                }
                setVisible(true);
                redraw();
                break;
            default:
                break;
        }
    }

    private void setupBarForBedHeating(PrinterStatus status)
    {
        alternateTitle.setVisible(true);
        progressBar.setVisible(false);
        alternateTitle.setText(Lookup.i18n(status.getI18nString().toUpperCase().concat("...")));
        targetTitle.setVisible(true);
        targetTitle.setText(Lookup.i18n("progress.targetTemperature"));
        targetValue.setVisible(true);
        targetValue.setText(associatedPrinter.headProperty()
            .get()
            .getNozzleHeaters()
            .get(0)
            .nozzleTargetTemperatureProperty()
            .asString().concat(Lookup.i18n("misc.degreesC")).get());
        progress.set(associatedPrinter.getPrintEngine().progressProperty().get());
        progress.bind(associatedPrinter.getPrintEngine().progressProperty());
        progressCurrentValue.textProperty().bind(associatedPrinter.getPrintEngine().
            progressProperty().asString("%.0f%%"));
    }

    private void setupBarForNormalPrinting(PrinterStatus status)
    {
        alternateTitle.setVisible(false);
        title.setText(status.getI18nString().toUpperCase().concat("..."));
        targetTitle.setText(Lookup.i18n("progress.ETCLabel"));
        targetValue.textProperty().bind(new StringBinding()
        {
            {
                super.bind(
                    associatedPrinter.getPrintEngine().
                    progressETCProperty());
            }

            @Override
            protected String computeValue()
            {
                int secondsRemaining = associatedPrinter.getPrintEngine().
                    progressETCProperty().get();
                secondsRemaining += 30;
                if (secondsRemaining > 60)
                {
                    String hoursMinutes = TimeUtils.convertToHoursMinutes(secondsRemaining);
                    return hoursMinutes;
                } else
                {
                    return Lookup.i18n("dialogs.lessThanOneMinute");
                }
            }
        });
        progress.set(associatedPrinter.getPrintEngine().progressProperty().get());
        progress.bind(associatedPrinter.getPrintEngine().progressProperty());
        progressCurrentValue.textProperty().bind(associatedPrinter.getPrintEngine().
            progressProperty().multiply(100).asString("%.0f%%"));
    }

//    public void setProgress(double progress)
//    {
//        if (progress != this.progress)
//        {
//            this.progress = progress;
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
    private void redraw()
    {
        double progressBackWidth = progressBarBack.boundsInParentProperty().get().getWidth();
        double barWidth = progressBackWidth * progress.get();
        progressBarInner.setWidth(barWidth);

        // place currentValue in correct place on progress bar (just to the left of RHS of the bar)
        double barEndXPosition = progressBarInner.getLayoutX()
            + progressBarInner.boundsInParentProperty().get().getWidth();
        double barStartXPosition = progressBarInner.getLayoutX();
        double currentValueWidth = progressCurrentValue.boundsInParentProperty().get().
            getWidth();
        int OFFSET_FROM_PROGRESS_BAR_RHS = 10;  // px
        double requiredCurrentValueXPosition = barEndXPosition - currentValueWidth
            - OFFSET_FROM_PROGRESS_BAR_RHS;

        double leftmostValuePositionAllowed = barStartXPosition + 2;
        if (requiredCurrentValueXPosition < leftmostValuePositionAllowed)
        {
            requiredCurrentValueXPosition = leftmostValuePositionAllowed;
        }

        double currentX = progressCurrentValue.getLayoutX();
        double requiredTranslate = requiredCurrentValueXPosition - currentX;
        progressCurrentValue.setTranslateX(requiredTranslate);
    }

//        progressLayerLabel.setText(i18nBundle.getString("dialogs.progressLayerLabel"));
//        progressETCLabel.setText(i18nBundle.getString("dialogs.progressETCLabel"));
//        
//                                progressBar.progressProperty().bind(selectedPrinter.getPrintEngine().progressProperty());
//                        progressPercent.textProperty().bind(
//                            Bindings.multiply(selectedPrinter.getPrintEngine().progressProperty(), 100).asString("%.0f%%"));
//                        BooleanBinding progressVisible
//                        = selectedPrinter.printerStatusProperty().isNotEqualTo(
//                            PrinterStatus.PRINTING)
//                        .or(Bindings.and(
//                                selectedPrinter.getPrintEngine().
//                                linesInPrintingFileProperty().greaterThan(
//                                    0),
//                                selectedPrinter.getPrintEngine().
//                                progressProperty().greaterThanOrEqualTo(
//                                    0)));
//                        BooleanBinding progressETCVisible
//                        = Bindings.and(
//                            selectedPrinter.getPrintEngine().etcAvailableProperty(),
//                            Bindings.or(
//                                selectedPrinter.printerStatusProperty().isEqualTo(
//                                    PrinterStatus.PRINTING),
//                                selectedPrinter.printerStatusProperty().isEqualTo(
//                                    PrinterStatus.SENDING_TO_PRINTER)));
//                        progressPercent.visibleProperty().bind(progressVisible);
//                        progressETC.visibleProperty().bind(progressETCVisible);
//                        progressETC.textProperty().bind(new StringBinding()
//                            {
//                                {
//                                    super.bind(
//                                        selectedPrinter.getPrintEngine().
//                                        progressETCProperty());
//                                }
//
//                                @Override
//                                protected String computeValue()
//                                {
//                                    int secondsRemaining = selectedPrinter.getPrintEngine().
//                                    progressETCProperty().get();
//                                    secondsRemaining += 30;
//                                    if (secondsRemaining > 60)
//                                    {
//                                        String hoursMinutes = convertToHoursMinutes(
//                                            secondsRemaining);
//                                        return hoursMinutes;
//                                    } else
//                                    {
//                                        return i18nBundle.getString("dialogs.lessThanOneMinute");
//                                    }
//                                }
//                        });
//                        progressLayers.textProperty().bind(new StringBinding()
//                            {
//                                {
//                                    super.bind(
//                                        selectedPrinter.getPrintEngine().progressCurrentLayerProperty(),
//                                        selectedPrinter.getPrintEngine().progressNumLayersProperty());
//                                }
//
//                                @Override
//                                protected String computeValue()
//                                {
//                                    int currentLayer = selectedPrinter.getPrintEngine().progressCurrentLayerProperty().get();
//                                    int totalLayers = selectedPrinter.getPrintEngine().progressNumLayersProperty().get();
//                                    return (currentLayer + 1) + "/" + totalLayers;
//                                }
//                        });
//                        progressLayers.visibleProperty().bind(progressETCVisible);
//                        progressLayerLabel.visibleProperty().bind(progressETCVisible);
//                        progressETCLabel.visibleProperty().bind(progressETCVisible);
//                        secondProgressBar.visibleProperty().bind(
//                            selectedPrinter.getPrintEngine().
//                            sendingDataToPrinterProperty());
//                        secondProgressBar.progressProperty().bind(
//                            selectedPrinter.getPrintEngine().
//                            secondaryProgressProperty());
//                        secondProgressPercent.visibleProperty().bind(
//                            selectedPrinter.getPrintEngine().
//                            sendingDataToPrinterProperty());
//                        secondProgressPercent.textProperty().bind(
//                            Bindings.format(
//                                "%s %.0f%%", transferringDataString,
//                                Bindings.multiply(
//                                    selectedPrinter.getPrintEngine().
//                                    secondaryProgressProperty(),
//                                    100)));
//                        progressTitle.textProperty().bind(Bindings.when(selectedPrinter.printerStatusProperty()
//                                .isEqualTo(PrinterStatus.EXECUTING_MACRO))
//                            .then(selectedPrinter.macroTypeProperty().asString())
//                            .otherwise(selectedPrinter.printerStatusProperty().asString()));
//                        progressMessage.textProperty().bind(
//                            selectedPrinter.getPrintEngine().messageProperty());
    public void slideMenuPanel(double amountToShow)
    {
        double adjustedHeight = (panelHeight * amountToShow);
        this.setMinHeight(adjustedHeight);
        this.setPrefHeight(adjustedHeight);
    }

    private boolean startNotifierAppearing()
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

    private boolean startNotifierDisappearing()
    {
        if (showSidebar.statusProperty().get() == Animation.Status.STOPPED)
        {
            panelHeight = getHeight();
            setMaxHeight(panelHeight);
            hideSidebar.play();
            return true;
        } else
        {
            return false;
        }
    }

    private void disappear()
    {
        slideMenuPanel(0.0);
        slidIn = true;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        panelHeight = getPrefHeight();
    }
}
