package celtech.coreUI.components.buttons;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 *
 * @author Ian
 */
public class GraphicToggleButtonWithLabel extends VBox
{

    private final GraphicToggleButton button = new GraphicToggleButton();
    private final Label label = new Label();

    public GraphicToggleButtonWithLabel()
    {
        this.setAlignment(Pos.CENTER);
        this.setPrefWidth(80);
        this.setPrefHeight(80);
        this.setMinWidth(USE_PREF_SIZE);
        this.setMaxWidth(USE_PREF_SIZE);
        this.setMinHeight(USE_PREF_SIZE);
        this.setMaxHeight(USE_PREF_SIZE);

        label.getStyleClass().add("graphic-button-label");

        getChildren().add(button);
        getChildren().add(label);
    }

    public String getFxmlFileName()
    {
        return button.getFxmlFileName();
    }

    public void setFxmlFileName(String fxmlFileName)
    {
        button.setFxmlFileName(fxmlFileName);
    }

    public StringProperty getFxmlFileNameProperty()
    {
        return button.getFxmlFileNameProperty();
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty()
    {
        return button.onActionProperty();
    }

    public final void setOnAction(EventHandler<ActionEvent> value)
    {
        button.onActionProperty().set(value);
    }

    public final EventHandler<ActionEvent> getOnAction()
    {
        return button.onActionProperty().get();
    }

    public String getLabelText()
    {
        return label.getText();
    }

    public void setLabelText(String text)
    {
        label.setText(text);
    }

    public StringProperty getLabelTextProperty()
    {
        return label.textProperty();
    }

    public void setSelected(boolean selected)
    {
        button.setSelected(selected);
    }
    
    public BooleanProperty selectedProperty()
    {
        return button.selectedProperty();
    }
}
