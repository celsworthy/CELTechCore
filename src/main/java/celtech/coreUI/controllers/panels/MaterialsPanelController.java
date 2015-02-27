package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationStatus;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

/**
 *
 * @author Ian
 */
public class MaterialsPanelController implements Initializable
{
    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
    }
    
    @FXML
    private void okPressed(ActionEvent event)
    {
        ApplicationStatus.getInstance().returnToLastMode();
    }    
}
