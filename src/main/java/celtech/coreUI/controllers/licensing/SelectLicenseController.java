package celtech.coreUI.controllers.licensing;

import celtech.roboxbase.comms.LicenseCheckResult;
import celtech.roboxbase.licensing.LicenseManager;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author George Salter
 */
public class SelectLicenseController implements Initializable {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(SelectLicenseController.class.getName());
    
    @FXML
    private VBox mainVBox;
    
    @FXML
    private Label fileLabel;
    
    @FXML
    private Button browseButton;
    
    @FXML
    private Button acceptButton;
    
    private LicenseManager licenseManager;
    
    private LicenseCheckResult licenseCheckResult;
    
    private File licenseFile = null;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        licenseManager = new LicenseManager();
        licenseCheckResult = LicenseCheckResult.STILL_CHECKING;
    }
    
    public LicenseCheckResult getLicenseCheckResult() {
        return licenseCheckResult;
    }
    
    @FXML
    private void browse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Robox License");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("License Files", "*.lic"));
        File selectedLicenseFile = fileChooser.showOpenDialog(mainVBox.getScene().getWindow());
        
        if(selectedLicenseFile != null) {
            fileLabel.setText(selectedLicenseFile.getAbsolutePath());
            licenseFile = selectedLicenseFile;
        }
    }
    
    @FXML
    private void accept() {
        licenseCheckResult = licenseManager.checkEncryptedLicenseFileValid(licenseFile, true);

        if(licenseCheckResult == LicenseCheckResult.LICENSE_VALID) {
            closeDialog();
        }
    }
    
    private void closeDialog() {
        Stage dialogStage = (Stage)mainVBox.getScene().getWindow();
        dialogStage.close();
    }
}
