/*
 * Copyright 2014 CEL UK
 */
package celtech.utils;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;

/**
 *
 * @author tony
 */
public class StringMetrics
{

    public static double getWidthOfString(String str, int fontSize)
    {
        final Text text = new Text(str);
        Scene scene = new Scene(new Group(text));
        scene.getStylesheets().add("/celtech/resources/css/JMetroDarkTheme.css");
        text.setStyle("-fx-font-size:" + fontSize + "pt;");
        text.applyCss();

        return text.getLayoutBounds().getWidth();
    }

}