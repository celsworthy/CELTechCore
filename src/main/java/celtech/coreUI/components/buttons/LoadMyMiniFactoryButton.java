package celtech.coreUI.components.buttons;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;

/**
 *
 * @author Ian
 */
public class LoadMyMiniFactoryButton extends Button
{
    public LoadMyMiniFactoryButton()
    {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
            "/celtech/resources/fxml/buttons/loadMyMiniFactoryButton.fxml"));
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
