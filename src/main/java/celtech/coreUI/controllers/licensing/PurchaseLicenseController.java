package celtech.coreUI.controllers.licensing;

import celtech.Lookup;
import celtech.coreUI.components.HyperlinkedLabel;
import celtech.roboxbase.BaseLookup;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author George Salter
 */
public class PurchaseLicenseController implements Initializable {

    @FXML
    VBox rootVBox;
    
    @FXML
    HyperlinkedLabel licensePurchaseInfo;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        licensePurchaseInfo.replaceText(Lookup.i18n("dialogs.licensePurchaseInfo"));
    }    
    
    @FXML
    protected void openSelectLicense() {
        BaseLookup.getSystemNotificationHandler().showSelectLicenseDialog();
        closeDialog();
        
    }
    
    @FXML
    protected void close() {
        closeDialog();
    }
    
    private void closeDialog() {
        Stage dialogStage = (Stage)rootVBox.getScene().getWindow();
        dialogStage.close();
    }
}
