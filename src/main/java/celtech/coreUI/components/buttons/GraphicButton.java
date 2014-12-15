package celtech.coreUI.components.buttons;

import java.io.IOException;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;

/**
 *
 * @author Ian
 */
public class GraphicButton extends Button
{
    private final StringProperty fxmlFileName = new SimpleStringProperty("");

    public GraphicButton()
    {
        loadFXML();
        getStyleClass().add("graphic-button");
        setPickOnBounds(false);
    }

    public String getFxmlFileName()
    {
        return fxmlFileName.get();
    }

    public void setFxmlFileName(String fxmlFileName)
    {
        this.fxmlFileName.set(fxmlFileName);

        loadFXML();
    }

    public StringProperty getFxmlFileNameProperty()
    {
        return fxmlFileName;
    }

    private void loadFXML() throws RuntimeException
    {
        if (fxmlFileName.get().equalsIgnoreCase("") == false)
        {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "/celtech/resources/fxml/buttons/" + fxmlFileName.get() + ".fxml"));
            fxmlLoader.setRoot(this);
            fxmlLoader.setController(this);

            fxmlLoader.setClassLoader(this.getClass().getClassLoader());

            try
            {
                fxmlLoader.load();
            } catch (IOException exception)
            {
                exception.printStackTrace();
                throw new RuntimeException(exception);
            }
        }
    }
}
