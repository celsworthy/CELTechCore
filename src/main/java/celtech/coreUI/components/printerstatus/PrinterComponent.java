/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.printerstatus;

import java.io.IOException;
import java.net.URL;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 *
 * @author tony
 */
public class PrinterComponent extends Pane
{

    @FXML
    private Label name;
    
    @FXML
    private WhiteProgressBarComponent progressBar;   
    
    @FXML
    private PrinterSVGComponent printerSVG;

    public PrinterComponent()
    {
        URL fxml = getClass().getResource("/resources/printer.fxml");
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
        
        initialise();
    }

    public void setName(String value)
    {
        nameTextProperty().set(value);
    }

    public StringProperty nameTextProperty()
    {
        return name.textProperty();
    }

    /**
     * Initialise the component
     */
    private void initialise()
    {
        
        progressBar.setStyle("-fx-progress-color: red;");
        setWidth(50);
        name.setTextFill(Color.WHITE);
    }
    
    public void setProgress(double progress) {
        progressBar.setProgress(progress);
    }
    
    public void setColour(int color) {
        String colourHexString = String.format("#%06X", color);
        String style = "-fx-background-color: " + colourHexString + ";";
        System.out.println("style is " + style);
        setStyle(style);
    }
    
    public void setColour(Color color) {
        double colorRGB = color.getRed() * 0x10000 + color.getGreen() * 0x100 + color.getBlue();
        String colourHexString = String.format("#%06X", colorRGB);
        String style = "-fx-background-color: " + colourHexString + ";";
        System.out.println("style is " + style);
        setStyle(style);
    }    
    
    
    public void setSize(int size) {
//        setPrefWidth(size);
        size = (int) (size * 0.9d);
        setMinWidth(size);
        setMaxWidth(size);
//        setPrefHeight(size);
        setMinHeight(size );
        setMaxHeight(size);
        printerSVG.setSize(size * 0.9);
        progressBar.setLayoutX(size * (1 - 0.846) * 0.5);
        progressBar.setLayoutY(size * 0.7);
//        progressBar.setPrefWidth(size * 0.9);
//        progressBar.setPrefHeight(size * 0.02);
        progressBar.setControlWidth(size * 0.846);
        progressBar.setControlHeight(size * 20.0 / 260.0);
        
        name.setStyle("-fx-font-size: 20;");
        name.setLayoutX(size * 0.02);
        name.setLayoutY(size * 0.85);
    }
}
