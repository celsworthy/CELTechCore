/*
 * Copyright 2014 CEL UK
 */

package celtech.coreUI.components.material;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;

/**
 *
 * @author tony
 */
public class MaterialComponent extends AnchorPane
{

    public MaterialComponent()
    {
        super();
        URL fxml = getClass().getResource("/celtech/resources/fxml/components/material/material.fxml");
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
        
        redraw();
    }
    
    public void setMaterial(String materialName, String materialColour, int colour) {
        
    }


    private void redraw()
    {
       
    }

}
