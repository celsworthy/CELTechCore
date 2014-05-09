/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import celtech.coreUI.components.ColourChooserButton;
import celtech.printerControl.Printer;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author ianhudson
 */
public class PrinterIDDialogController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(PrinterIDDialogController.class.getName());

    private boolean okPressed = false;

    @FXML
    private Label dialogMessage;

    @FXML
    private Label dialogTitle;

    @FXML
    private Button okButton;

    @FXML
    private TextField roboxNameField;

    @FXML
    private ToggleGroup colourButtonGroup;

    @FXML
    void okButtonPressed(MouseEvent event)
    {
        okPressed = true;
        myStage.close();
    }

    @FXML
    void cancelButtonPressed(MouseEvent event)
    {
        okPressed = false;
        myStage.close();
    }

    private int buttonValue = -1;
    private Stage myStage = null;

    private ArrayList<Button> buttons = new ArrayList<>();

    private EventHandler<KeyEvent> textInputHandler = null;

    private Printer printerToUse = null;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {

        textInputHandler = new EventHandler<KeyEvent>()
        {
            @Override
            public void handle(KeyEvent event)
            {
                TextField field = (TextField) event.getSource();
                if (field.getText().length() > 100 && event.getCode() != KeyCode.BACK_SPACE)
                {
                    event.consume();
                }
            }
        };

        roboxNameField.addEventFilter(KeyEvent.KEY_TYPED, textInputHandler);

        okButton.disableProperty().bind(Bindings.length(roboxNameField.textProperty()).isEqualTo(0));
    }

    public void configure(Stage dialogStage)
    {
        myStage = dialogStage;
    }

    public Color getChosenDisplayColour()
    {
        return ((ColourChooserButton) colourButtonGroup.getSelectedToggle()).getDisplayColour();
    }

    public String getChosenPrinterName()
    {
        return roboxNameField.getText();
    }

    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    public void setChosenColour(Color colour)
    {
        for (Toggle toggle : colourButtonGroup.getToggles())
        {
            ColourChooserButton button = (ColourChooserButton) toggle;

            if (button.getDisplayColour().equals(colour))
            {
                colourButtonGroup.selectToggle(button);
            }
        }
    }

    public void setChosenPrinterName(String printerName)
    {
        roboxNameField.setText(printerName);
    }

    public boolean okPressed()
    {
        return okPressed;
    }
}
