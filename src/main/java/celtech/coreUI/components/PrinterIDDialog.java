/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.components;

import celtech.coreUI.DisplayManager;
import celtech.configuration.ApplicationConfiguration;
import celtech.coreUI.controllers.PrinterIDDialogController;
import celtech.printerControl.Printer;
import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class PrinterIDDialog
{

    private Stenographer steno = StenographerFactory.getStenographer(PrinterIDDialog.class.getName());
    private Stage dialogStage = null;
    private PrinterIDDialogController dialogController = null;

    public PrinterIDDialog()
    {
        dialogStage = new Stage(StageStyle.TRANSPARENT);
        URL dialogFXMLURL = PrinterIDDialog.class.getResource(ApplicationConfiguration.fxmlResourcePath + "PrinterIDDialog.fxml");
        FXMLLoader dialogLoader = new FXMLLoader(dialogFXMLURL, DisplayManager.getLanguageBundle());
        try
        {
            Parent dialogBoxScreen = (Parent) dialogLoader.load();
            dialogController = (PrinterIDDialogController) dialogLoader.getController();

            Scene dialogScene = new Scene(dialogBoxScreen, Color.TRANSPARENT);
            dialogScene.getStylesheets().add(ApplicationConfiguration.mainCSSFile);
            dialogStage.setScene(dialogScene);
            dialogStage.initOwner(DisplayManager.getMainStage());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogController.configure(dialogStage);
        } catch (IOException ex)
        {
            steno.error("Couldn't load printer ID dialog box FXML");
        }
    }

    public void show()
    {
        dialogStage.showAndWait();
    }

    public void close()
    {
        dialogStage.hide();
    }

    public boolean isShowing()
    {
        return dialogStage.isShowing();
    }

    public String getChosenPrinterID()
    {
        return dialogController.getChosenPrinterID();
    }

    public String getChosenPrinterName()
    {
        return dialogController.getChosenPrinterName();
    }

    public Color getColour()
    {
        return dialogController.getChosenColour();
    }

    public void setPrinterToUse(Printer printerToUse)
    {
        dialogController.setPrinterToUse(printerToUse);
    }

    public void setChosenColour(Color colour)
    {
        dialogController.setChosenColour(colour);
    }

    public void setChosenPrinterID(String printerID)
    {
        dialogController.setChosenPrinterID(printerID);
    }

    public void setChosenPrinterName(String printerName)
    {
        dialogController.setChosenPrinterName(printerName);
    }
}
