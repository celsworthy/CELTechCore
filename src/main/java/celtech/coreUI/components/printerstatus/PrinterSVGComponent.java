/*
 * Copyright 2014 CEL UK
 */

package celtech.coreUI.components.printerstatus;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;

/**
 *
 * @author tony
 */
public class PrinterSVGComponent extends Pane
{
    
    @FXML Pane printerIcon;
    
    @FXML Pane readyIcon;
    @FXML Pane printingIcon;
    @FXML Pane pausedIcon;
    @FXML Pane notificationIcon;
    @FXML Pane errorIcon;

    private void hideAllIcons()
    {
        readyIcon.setVisible(false);
        printingIcon.setVisible(false);
        pausedIcon.setVisible(false);
        notificationIcon.setVisible(false);
        errorIcon.setVisible(false);
    }
    
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
    }
    
    public void setStatus(PrinterComponent.Status status) {
        hideAllIcons();

        switch (status) {
            case READY: 
                readyIcon.setVisible(true);
                break;
            case PAUSED: 
                pausedIcon.setVisible(true);
                break;
            case NOTIFICATION: 
                notificationIcon.setVisible(true);
                break;
            case PRINTING: 
                printingIcon.setVisible(true);
                break;
            case ERROR: 
                errorIcon.setVisible(true);
                break;                
        }
    }
    
    public void setSize(double size) {
        Scale scale = new Scale(size/260.0, size/260.0, 0, 0);
        getTransforms().clear();
        getTransforms().add(scale);
    }
    
}
