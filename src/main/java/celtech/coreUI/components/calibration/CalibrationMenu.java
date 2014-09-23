/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.calibration;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

/**
 *
 * @author tony
 */
public class CalibrationMenu extends VBox
{

    public CalibrationMenu()
    {
        super();
        URL fxml = getClass().getResource("/celtech/resources/fxml/calibration/calibrationMenu.fxml");
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