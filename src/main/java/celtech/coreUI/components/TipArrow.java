package celtech.coreUI.components;

import celtech.coreUI.DisplayManager;
import java.io.IOException;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

/**
 *
 * @author Ian
 */
public class TipArrow extends HBox
{

    @FXML
    Label label;

    @FXML
    SVGPath arrow;

    private Node attachedTo = null;

    public TipArrow()
    {
        getStylesheets().add("tip-arrow");

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
            "/celtech/resources/fxml/components/TipArrow.fxml"));
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

    public String getLabelText()
    {
        return label.getText();
    }

    public void setLabelText(String text)
    {
        label.setText(text);
    }

    public StringProperty labelTextProperty()
    {
        return label.textProperty();
    }

    public void attach(Node node)
    {
        DisplayManager.getInstance().getTipArrowContainer().getChildren().add(this);
        attachedTo = node;
        bindPosition();
        repositionText();
    }

    private void bindPosition()
    {
        DisplayManager.getInstance().nodesMayHaveMovedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
        {
            repositionText();
        });
    }

    private void repositionText()
    {
        if (attachedTo != null)
        {
            Bounds attachedBounds = attachedTo.getBoundsInLocal();
            Bounds sceneBounds = attachedTo.localToScene(attachedBounds);
            System.out.println("Scene bounds " + sceneBounds);
            setTranslateX(sceneBounds.getMinX() - getWidth());
            setTranslateY(sceneBounds.getMinY() - getHeight());
        }
    }
}
