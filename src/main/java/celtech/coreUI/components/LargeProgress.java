/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import celtech.printerControl.PrinterStatus;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.PrinterMetaStatus;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author tony
 */
public class LargeProgress extends BorderPane
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
    
    private DoubleProperty progressProperty = new SimpleDoubleProperty(0);
    private double progressPercent = 0;
    private Optional<PrinterMetaStatus> printerMetaStatus = Optional.empty();
    
    private ChangeListener<Number> progressChangeListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        setProgressPercent(newValue.doubleValue());
    };
    
    private ChangeListener<PrinterStatus> statusChangeListener = (ObservableValue<? extends PrinterStatus> observable, PrinterStatus oldValue, PrinterStatus newValue) ->
    {
        redraw();
    };
    
    public LargeProgress()
    {
        super();
        URL fxml = getClass().getResource(
            "/celtech/resources/fxml/components/largeProgress.fxml");
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
        
        largeProgressBarBack.boundsInLocalProperty().addListener(
            (ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) ->
            {
                redraw();
            });
        
        redraw();
        
    }
    
    public void setProgressPercent(double progress)
    {
        if (progress != this.progressPercent)
        {
            this.progressPercent = progress;
            redraw();
        }
    }
    
    public void setTargetValue(String targetValue)
    {
        largeTargetValue.setText(targetValue);
    }
    
    public void setProgressDescription(String progressDescription)
    {
        largeProgressDescription.setText(progressDescription);
    }
    
    public void setTargetLegend(String targetLegend)
    {
        largeTargetLegend.setText(targetLegend);
    }
    
    public void setCurrentValue(String currentValue)
    {
        largeProgressCurrentValue.setText(currentValue);
    }
    
    private void redraw()
    {
        if (printerMetaStatus.isPresent())
        {
            switch (printerMetaStatus.get().printerStatusProperty().get())
            {
                case PRINTING:
                case SLICING:
                case POST_PROCESSING:
                case EXECUTING_MACRO:
                case SENDING_TO_PRINTER:
                    progressBarElement.setVisible(true);
                    break;
                default:
                    progressBarElement.setVisible(false);
                    break;
            }
        }
        
        double normalisedProgress = progressPercent / 100;
        
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
        if (progressPercent < 8)
        {
            largeProgressCurrentValue.setTextFill(Color.WHITE);
        } else
        {
            largeProgressCurrentValue.setTextFill(Color.web("#0096e1"));
        }
    }
    
    public void bindToPrinter(PrinterMetaStatus printerMetaStatus)
    {
        unbindProgress();
        
        this.printerMetaStatus = Optional.of(printerMetaStatus);
        largeProgressDescription.textProperty().bind(printerMetaStatus.printerStatusProperty().
            asString());
        largeTargetValue.textProperty().bind(printerMetaStatus.currentStatusValueTargetProperty().
            asString("%.0f"));
        largeTargetValue.visibleProperty().bind(printerMetaStatus.targetValueValidProperty());
        largeTargetLegend.textProperty().bind(printerMetaStatus.legendProperty());
        largeProgressCurrentValue.textProperty().bind(
            printerMetaStatus.currentStatusValueProperty().asString("%.0f%%"));
        progressProperty.bind(printerMetaStatus.currentStatusValueProperty());
        progressProperty.addListener(progressChangeListener);
        printerMetaStatus.printerStatusProperty().addListener(statusChangeListener);
    }
    
    public void unbindProgress()
    {
        largeProgressDescription.textProperty().unbind();
        largeTargetValue.textProperty().unbind();
        largeTargetValue.visibleProperty().unbind();
        largeProgressCurrentValue.textProperty().unbind();
        progressProperty.removeListener(progressChangeListener);
        progressProperty.unbind();
        if (printerMetaStatus.isPresent())
        {
            printerMetaStatus.get().printerStatusProperty().removeListener(statusChangeListener);
            printerMetaStatus = Optional.empty();
        }
    }
}
