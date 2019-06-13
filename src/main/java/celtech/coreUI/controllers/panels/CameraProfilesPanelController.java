package celtech.coreUI.controllers.panels;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;

/**
 *
 * @author George Salter
 */
public class CameraProfilesPanelController implements Initializable, MenuInnerPanel
{

    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        
    }

    @Override
    public String getMenuTitle() 
    {
        return "extrasMenu.cameraProfile";
    }
    
    @Override
    public void panelSelected() 
    {
        
    }
    
    @Override
    public List<OperationButton> getOperationButtons() 
    {
        List<MenuInnerPanel.OperationButton> operationButtons = new ArrayList<>();
        return operationButtons;
    }
}
