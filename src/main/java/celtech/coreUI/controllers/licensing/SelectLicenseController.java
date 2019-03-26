package celtech.coreUI.controllers.licensing;

import celtech.Lookup;
import celtech.coreUI.components.HyperlinkedLabel;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.licence.Licence;
import celtech.roboxbase.licence.LicenceUtilities;
import celtech.roboxbase.licensing.LicenceManager;
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
    
    @FXML
    HyperlinkedLabel selectLicenceInfo;
    
    private boolean licenseValid;
    
    private Licence licence;
    
    private File licenseFile;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Optional<Licence> potentialLicense = LicenceManager.getInstance().readCachedLicenseFile();
        if(potentialLicense.isPresent()) {
            licence = potentialLicense.get();
            licenseInfo.setText(licence.toString());
        }
        selectLicenceInfo.replaceText(Lookup.i18n("dialogs.selectLicenseInfo"));
        licenseFile = LicenceManager.getInstance().tryAndGetCachedLicenseFile();
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
            Optional<Licence> potentialLicense = LicenceUtilities.readEncryptedLicenceFile(selectedLicenseFile);
            if(potentialLicense.isPresent()) {
                licenseInfo.setText(potentialLicense.get().toString());
            }
            licenseFile = selectedLicenseFile;
        }
    }
    
    @FXML
    private void accept() 
    {
        if(licenseFile.exists()) 
        {
            licenseValid = LicenceManager.getInstance().checkEncryptedLicenseFileValid(licenseFile, true, true);
        }
        
        if(licenseValid) 
        {
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
