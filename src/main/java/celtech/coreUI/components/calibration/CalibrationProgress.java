/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.calibration;

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
public class CalibrationProgress extends BorderPane
{

    @FXML
    private Rectangle calibrationProgressBarInner;

    @FXML
    private HBox calibrationProgressBarBack;
    
    @FXML
    private Label calibrationTargetValue;
    
    @FXML
    private Label calibrationTargetLegend;

    private double progress = 0;

    public CalibrationProgress()
    {
        super();
        URL fxml = getClass().getResource(
            "/celtech/resources/fxml/calibration/calibrationProgress.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try
        {
            fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }

        calibrationProgressBarBack.boundsInLocalProperty().addListener(
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
    
    public void setTargetValue(String targetValue) {
        calibrationTargetValue.setText(targetValue);
    }

    private void redraw()
    {
        double barWidth = calibrationProgressBarBack.boundsInLocalProperty().get().getWidth()
            * progress;
        calibrationProgressBarInner.setWidth(barWidth);
    }

}
