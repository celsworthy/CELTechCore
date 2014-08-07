/*
 * Copyright 2014 CEL UK
 */

package celtech.coreUI.components.printerstatus;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;

/**
 *
 * @author tony
 */
public class PrinterSVGComponent extends Pane
{
    
    @FXML Pane groupPane;
    @FXML SVGPath p1;
    @FXML SVGPath p2;
    @FXML SVGPath p3;
    @FXML SVGPath p4;
    @FXML SVGPath p5;
    @FXML SVGPath p6;
    
    Set<SVGPath> paths = new HashSet<>();
    
    public PrinterSVGComponent()
    {
        URL fxml = getClass().getResource("/celtech/resources/fxml/printerstatus/printerSVG.fxml");
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
        
        paths.add(p1);
        paths.add(p2);
        paths.add(p3);
        paths.add(p4);
        paths.add(p5);
        paths.add(p6);
        
        for (SVGPath path : paths)
        {
            path.setStyle("-fx-fill: white;");
        }
        
    }
    
    public void setSize(double size) {
        setScaleX(size/260.0);
        setScaleY(size/260.0);
    }
    
}
