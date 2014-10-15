/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components;

import java.io.IOException;
import java.net.URL;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
    private Label largeTargetLegend;

    private double progress = 0;

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

    public void setProgress(double progress)
    {
        if (progress != this.progress)
        {
            this.progress = progress;
            redraw();
        }
    }

    public void setTargetValue(String targetValue)
    {
        largeTargetValue.setText(targetValue);
    }
    
    public void setProgressDescription(String progressDescription) {
        largeProgressDescription.setText(progressDescription);
    }
    
    public void setTargetLegend(String targetLegend) {
        largeTargetLegend.setText(targetLegend);
    }

    public void setCurrentValue(String currentValue)
    {
        largeProgressCurrentValue.setText(currentValue);
    }

    private void redraw()
    {
        double progressBackWidth = largeProgressBarBack.boundsInParentProperty().get().getWidth();
        double barWidth = progressBackWidth * progress;
        largeProgressBarInner.setWidth(barWidth);

        // place currentValue in correct place on progress bar (just to the left of RHS of the bar)
        double barEndXPosition = largeProgressBarInner.getLayoutX()
            + largeProgressBarInner.boundsInParentProperty().get().getWidth();
        double barStartXPosition = largeProgressBarInner.getLayoutX();
        double currentValueWidth = largeProgressCurrentValue.boundsInParentProperty().get().getWidth();
        int OFFSET_FROM_PROGRESS_BAR_RHS = 10;  // px
        double requiredCurrentValueXPosition = barEndXPosition - currentValueWidth
            - OFFSET_FROM_PROGRESS_BAR_RHS;
        
        double leftmostValuePositionAllowed = barStartXPosition + 2;
        if (requiredCurrentValueXPosition < leftmostValuePositionAllowed) {
            requiredCurrentValueXPosition = leftmostValuePositionAllowed;
        }

        double currentX = largeProgressCurrentValue.getLayoutX();
        double requiredTranslate = requiredCurrentValueXPosition - currentX;
        largeProgressCurrentValue.setTranslateX(requiredTranslate);
    }

}
