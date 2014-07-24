/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components.buttons;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ToggleButton;

/**
 *
 * @author Ian
 */
public class SnapToGroundButton extends ToggleButton
{
    public SnapToGroundButton()
    {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
            "/celtech/resources/fxml/buttons/snapToGroundButton.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        fxmlLoader.setClassLoader(this.getClass().getClassLoader());

        try
        {
            fxmlLoader.load();
        } catch (IOException exception)
        {
            throw new RuntimeException(exception);
        }
    }
}
