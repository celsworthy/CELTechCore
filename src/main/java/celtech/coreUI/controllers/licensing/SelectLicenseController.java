package celtech.coreUI.controllers.licensing;

import celtech.Lookup;
import celtech.coreUI.components.HyperlinkedLabel;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.SlicerType;
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

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
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

    private File licenseFile;

    boolean modified = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        licenseInfo.setText("permanent_pro_license");
        selectLicenceInfo.replaceText(Lookup.i18n("dialogs.selectLicenseInfo"));
        modified = false;
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

        if (selectedLicenseFile != null) {
            licenseInfo.setText("permanent_pro_license"); //todo remove this section at all
        }
    }

    @FXML
    private void accept() {
        if (modified &&
                Lookup.getUserPreferences().getSlicerTypeProperty().get() != SlicerType.Cura4) {
            Lookup.getUserPreferences().setSlicerType(SlicerType.Cura4);
        }

        closeDialog();
    }

    @FXML
    private void cancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage dialogStage = (Stage) rootVBox.getScene().getWindow();
        dialogStage.close();
    }
}
