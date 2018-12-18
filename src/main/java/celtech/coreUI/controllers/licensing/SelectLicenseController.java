package celtech.coreUI.controllers.licensing;

import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.licensing.License;
import celtech.roboxbase.licensing.LicenseManager;
import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
    private VBox rootVBox;
    
    @FXML
    private Label fileLabel;
    
    @FXML
    private Button browseButton;
    
    @FXML
    private Button acceptButton;
    
    @FXML
    private TextArea licenseInfo;
    
    private boolean licenseValid;
    
    private License license;
    
    private File licenseFile = null;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Optional<License> potentialLicense = LicenseManager.getInstance().readCachedLicenseFile();
        if(potentialLicense.isPresent()) {
            license = potentialLicense.get();
            licenseInfo.setText(license.toString());
        }
    }
    
    public boolean isLicenseValid() {
        return licenseValid;
    }
    
    @FXML
    private void browse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(BaseLookup.i18n("dialogs.selectLicense"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("License Files", "*.lic"));
        File selectedLicenseFile = fileChooser.showOpenDialog(rootVBox.getScene().getWindow());
        
        if(selectedLicenseFile != null) {
            fileLabel.setText(selectedLicenseFile.getAbsolutePath());
            Optional<License> potentialLicense = LicenseManager.getInstance().readEncryptedLicenseFile(selectedLicenseFile);
            if(potentialLicense.isPresent()) {
                licenseInfo.setText(potentialLicense.get().toString());
            }
            licenseFile = selectedLicenseFile;
        }
    }
    
    @FXML
    private void accept() {
        licenseValid = LicenseManager.getInstance().checkEncryptedLicenseFileValid(licenseFile, true, true);

        if(licenseValid) {
            closeDialog();
        }
    }
    
    @FXML
    private void cancel() {
        closeDialog();
    }
    
    private void closeDialog() {
        Stage dialogStage = (Stage)rootVBox.getScene().getWindow();
        dialogStage.close();
    }
}
