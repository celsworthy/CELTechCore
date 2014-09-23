/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.calibration;

import java.io.IOException;
import java.net.URL;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
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

    private double progress;

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

        progress = 0.9;
        redraw();

        calibrationProgressBarBack.boundsInLocalProperty().addListener(
            (ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) ->
            {
                redraw();
            });

    }

    public void setProgress(double progress)
    {
        if (progress != this.progress)
        {
            this.progress = progress;
            redraw();
        }
    }

    private void redraw()
    {
        double barWidth = calibrationProgressBarBack.boundsInLocalProperty().get().getWidth()
            * progress;
        calibrationProgressBarInner.setWidth(barWidth);
    }

}
