package celtech.coreUI.components.buttons;

import celtech.coreUI.components.HideableTooltip;
import java.io.IOException;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

/**
 *
 * @author Ian
 */
public class GraphicButton extends Button
{

    private final StringProperty fxmlFileName = new SimpleStringProperty("");
    private final HideableTooltip tooltip = new HideableTooltip();

    public GraphicButton()
    {
        loadFXML();
        getStyleClass().add("graphic-button");
        setPickOnBounds(false);

        Tooltip.install(this, tooltip);
    }

    public String getTooltipText()
    {
        return tooltip.getText();
    }

    public void setTooltipText(String text)
    {
        tooltip.setText(text);
    }

    public StringProperty getTooltipTextProperty()
    {
        return tooltip.textProperty();
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
