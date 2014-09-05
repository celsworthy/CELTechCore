/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.material;

import static celtech.printerControl.comms.commands.ColourStringConverter.colourToString;
import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

/**
 *
 * @author tony
 */
public class MaterialComponent extends AnchorPane
{

    @FXML
    private Text reelNumberMaterial;

    @FXML
    private SVGPath reel1SVG;

    @FXML
    private Text materialColour;

    @FXML
    private Text materialRemaining;

    @FXML
    private HBox materialColourContainer;

    @FXML
    private HBox materialRemainingContainer;

    public MaterialComponent()
    {
        super();
        URL fxml = getClass().getResource(
            "/celtech/resources/fxml/components/material/material.fxml");
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

    public void setMaterial(int reelNumber, String materialName, String materialColourString,
        Color colour)
    {
        String numberMaterial = String.valueOf(reelNumber) + ":" + materialName;
        reelNumberMaterial.setText(numberMaterial);

        String colourString = colourToString(colour);
        materialColourContainer.setStyle("-fx-background-color:" + colourString + ";");
        reel1SVG.setStyle("-fx-fill:" + colourString + ";");

        materialColour.setText(materialColourString);
        if (colour.getBrightness() < 0.5)
        {
            materialColour.setStyle("-fx-fill:white;");
        } else {
            materialColour.setStyle("-fx-fill:black;");
        }
    }
}
