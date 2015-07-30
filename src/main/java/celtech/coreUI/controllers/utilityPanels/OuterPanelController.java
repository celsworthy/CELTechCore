package celtech.coreUI.controllers.utilityPanels;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class OuterPanelController implements Initializable
{
    
    @FXML
    private VBox outerPanel;
    
    @FXML
    private Label title;    
    
    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
    }

    public void setInnerPanel(Node insetPanel)
    {
        outerPanel.getChildren().add(insetPanel);
    }
    
    public void setTitle(String title) {
        this.title.setText(title);
    }

  }
