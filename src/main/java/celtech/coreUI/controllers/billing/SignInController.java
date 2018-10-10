/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.billing;

import celtech.roboxbase.billing.killbill.KillbillAccountService;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.killbill.billing.client.KillBillClientException;

/**
 * FXML Controller class
 *
 * @author George Salter
 */
public class SignInController implements Initializable {

    private KillbillAccountService killbillAccountService;
    
    @FXML
    TextField usernameField;
    
    @FXML
    TextField emailField;
    
    @FXML
    Button createAccountButton;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        killbillAccountService = new KillbillAccountService();
    }    
    
    @FXML
    private void createAccount() throws KillBillClientException {
        killbillAccountService.createAccount(usernameField.getText(), emailField.getText());
    }
}
