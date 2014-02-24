/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author ianhudson
 */
public class ModalDialogController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(ModalDialogController.class.getName());
    @FXML
    private Label dialogTitle;
    @FXML
    private Label dialogMessage;
    @FXML
    private HBox buttonHolder;
    private EventHandler<ActionEvent> buttonHandler = null;
    private int buttonValue = -1;
    private Stage myStage = null;
    /*
     * 
     */
    private ArrayList<Button> buttons = new ArrayList<>();

    public ModalDialogController()
    {
        buttonHandler = new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent t)
            {
                buttonValue = buttons.indexOf(t.getSource());
                myStage.close();
            }
        };

    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        dialogTitle.setText("");
    }

    public void setDialogTitle(String title)
    {
        dialogTitle.setText(title);
    }

    public void setDialogMessage(String message)
    {
        dialogMessage.setText(message);
    }

    public int addButton(String text)
    {
        Button newButton = new Button(text);
        newButton.setOnAction(buttonHandler);
        buttonHolder.getChildren().add(newButton);
        buttons.add(newButton);
        return buttons.indexOf(newButton);
    }

    public int getButtonValue()
    {
        return buttonValue;
    }

    public void configure(Stage dialogStage)
    {
        myStage = dialogStage;
    }
}
