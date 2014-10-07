/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.controllers.panels;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 *
 * @author tony
 */
public class SpinnerController implements Initializable
{

    @FXML
    private Node outerArcs;

    @FXML
    private Node innerArcs;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        new AnimationTimer()
        {
            @Override
            public void handle(long now)
            {
                if (outerArcs.isVisible())
                {
                    long milliseconds = (int) (now / 1e6);
                    double outerAngle = milliseconds * 120d / 1000d;
                    double index = (outerAngle % 360);
                    double opacity = Math.abs(index - 180) / 180d;
                    outerArcs.rotateProperty().set(outerAngle);
                    innerArcs.rotateProperty().set(-outerAngle);
                    outerArcs.opacityProperty().set(opacity);
                }
            }
        }.start();
    }

}
