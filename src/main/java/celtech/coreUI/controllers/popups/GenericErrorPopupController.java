/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.popups;

import celtech.printerControl.comms.commands.rx.AckResponse;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 *
 * @author Ian
 */
public class GenericErrorPopupController implements Initializable
{

    @FXML
    private VBox container;

    @FXML
    private TextArea errorDisplay;

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
    }

    public void populateErrorList(AckResponse errors)
    {
        errorDisplay.setText(errors.getErrorsAsString());
    }
}
