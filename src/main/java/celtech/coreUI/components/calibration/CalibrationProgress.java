/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.calibration;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.BorderPane;

/**
 *
 * @author tony
 */
public class CalibrationProgress extends BorderPane
{

    public CalibrationProgress()
    {
        super();
        URL fxml = getClass().getResource("/celtech/resources/fxml/calibration/calibrationProgress.fxml");
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
        
    }

}