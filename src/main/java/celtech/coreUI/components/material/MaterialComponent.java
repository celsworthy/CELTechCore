/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.material;

import celtech.configuration.MaterialType;
import celtech.coreUI.DisplayManager;
import static celtech.printerControl.comms.commands.ColourStringConverter.colourToString;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
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

    public void setMaterial(int reelNumber, MaterialType materialType, String materialColourString,
        Color colour, double remainingFilament, double filamentDiameter)
    {
        String numberMaterial = String.valueOf(reelNumber) + ":" + materialType.getFriendlyName();

        double remainingLengthMeters = remainingFilament / 1000d;
        double densityKGM2 = materialType.getDensity() * 1000d;
        double crossSectionM2 = Math.PI * filamentDiameter * filamentDiameter / 4d * 1e-6;
        double remainingWeightG = remainingLengthMeters * crossSectionM2 * densityKGM2 * 1000d;
        String remaining = ((int) remainingLengthMeters) + "m / " + ((int) remainingWeightG)
            + "g remaining";

        showDetails(numberMaterial, remaining, materialColourString, colour);
    }

    private void showDetails(String numberMaterial, String materialRemainingString,
        String materialColourString, Color colour)
    {
        reelNumberMaterial.setText(numberMaterial);
        materialRemaining.setText(materialRemainingString);
        String colourString = colourToString(colour);
        reelNumberMaterial.setStyle("-fx-fill:" + colourString + ";");
        materialColourContainer.setStyle("-fx-background-color:" + colourString + ";");
        reel1SVG.setStyle("-fx-fill:" + colourString + ";");

        materialColour.setText(materialColourString);
        if (colour.getBrightness() < 0.5)
        {
            materialColour.setStyle("-fx-fill:white;");
        } else
        {
            materialColour.setStyle("-fx-fill:black;");
        }
    }

    /**
     * Indicate that the reel is not formatted
     */
    public void showReelNotFormatted()
    {
        ResourceBundle languageBundle = DisplayManager.getLanguageBundle();
        String reelNotFormattedString = languageBundle.getString(
            "smartReelProgrammer.reelNotFormatted");
        showDetails("1:ERROR", "Not available", reelNotFormattedString, Color.BLACK);

    }

    public void showFilamentNotLoaded()
    {
        ResourceBundle languageBundle = DisplayManager.getLanguageBundle();
        String filamentNotLoadedString = languageBundle.getString("smartReelProgrammer.noReelLoaded");
        showDetails("1:", "Please create a profile", filamentNotLoadedString, Color.BLACK);
    }

}
