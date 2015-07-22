package celtech.coreUI.controllers.utilityPanels;

import celtech.coreUI.controllers.StatusInsetController;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProjectPanelController implements Initializable, StatusInsetController
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        ProjectPanelController.class.getName());

   
    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
       
    }

   
}
