/*
 * Copyright 2014 CEL UK
 */
package celtech.coreUI.components.printerstatus;

import celtech.printerControl.Printer;
import static celtech.printerControl.comms.commands.ColourStringConverter.colourToString;
import java.io.IOException;
import java.net.URL;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 *
 * @author tony
 */
public class PrinterComponent extends Pane
{

    private boolean selected;
    private Size currentSize;

    public enum Size
    {

        SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE;
    }

    @FXML
    private Label name;

    @FXML
    private Pane innerPane;

    @FXML
    private WhiteProgressBarComponent progressBar;

    @FXML
    private PrinterSVGComponent printerSVG;
    private final Printer printer;
    private ChangeListener<String> nameListener;
    private ChangeListener<Color> colorListener;

    public PrinterComponent(Printer printer)
    {
        this.printer = printer;
        URL fxml = getClass().getResource("/celtech/resources/fxml/printerstatus/printer.fxml");
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

        setStyle("-fx-background-color: white;");

        name.setTextFill(Color.WHITE);
        name.setText(printer.getPrinterFriendlyName());
        setColour(printer.getPrinterColour());

        nameListener = (ObservableValue<? extends String> observable, String oldValue, String newValue) ->
        {
            setName(newValue);
        };

        colorListener = (ObservableValue<? extends Color> observable, Color oldValue, Color newValue) ->
        {
            setColour(newValue);
        };

        printer.printerFriendlyNameProperty().addListener(nameListener);
        printer.printerColourProperty().addListener(colorListener);

        setSize(Size.SIZE_LARGE);
    }

    public void setProgress(double progress)
    {
        progressBar.setProgress(progress);
    }

    public void setColour(int color)
    {
        String colourHexString = String.format("#%06X", color);
        String style = "-fx-background-color: " + colourHexString + ";";
        innerPane.setStyle(style);
    }

    public void setColour(Color color)
    {
        String colourHexString = "#" + colourToString(color);
        String style = "-fx-background-color: " + colourHexString + ";";
        innerPane.setStyle(style);
    }

    public void setSelected(boolean select)
    {
        if (selected != select)
        {
            selected = select;
            redraw();
        }
    }

    public void setSize(Size size)
    {
        if (size != currentSize)
        {
            currentSize = size;
            redraw();
        }
    }

    private void redraw()
    {
        int sizePixels;
        int fontSize;
        int progressBarWidth;
        int progressBarHeight;
        int nameLayoutY;
        int borderWidth;
        if (selected)
        {
            borderWidth = 3;
        } else
        {
            borderWidth = 0;
        }

        switch (currentSize)
        {
            case SIZE_SMALL:
                sizePixels = 80;
                fontSize = 9;
                progressBarWidth = 65;
                progressBarHeight = 6;
                nameLayoutY = 70;
                break;
            case SIZE_MEDIUM:
                sizePixels = 120;
                fontSize = 14;
                progressBarWidth = 100;
                progressBarHeight = 9;
                nameLayoutY = 105;
                break;
            default:
                sizePixels = 260;
                fontSize = 30;
                progressBarWidth = 220;
                progressBarHeight = 20;
                nameLayoutY = 225;
                break;
        }
        setMinWidth(sizePixels);
        setMaxWidth(sizePixels);
        setMinHeight(sizePixels);
        setMaxHeight(sizePixels);
        innerPane.setMinWidth(sizePixels - borderWidth * 2);
        innerPane.setMaxWidth(sizePixels - borderWidth * 2);
        innerPane.setMinHeight(sizePixels - borderWidth * 2);
        innerPane.setMaxHeight(sizePixels - borderWidth * 2);
        innerPane.setTranslateX(borderWidth);
        innerPane.setTranslateY(borderWidth);
        printerSVG.setSize(sizePixels * 0.9);
        progressBar.setLayoutX((sizePixels - progressBarWidth) / 2.0);
        progressBar.setLayoutY(sizePixels * 0.7);
        progressBar.setControlWidth(progressBarWidth);
        progressBar.setControlHeight(progressBarHeight);

        name.setStyle("-fx-font-size: " + fontSize + "pt;");
        Font font = name.getFont();
        Font font2 = new Font(font.getName(), fontSize);
        name.setFont(font2);
        name.setLayoutX((sizePixels - progressBarWidth) / 2.0);
        name.setLayoutY(nameLayoutY);
    }
}
