package celtech.coreUI.controllers.panels;

import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.controllers.popups.PopupCommandReceiver;
import celtech.coreUI.controllers.popups.PopupCommandTransmitter;
import celtech.coreUI.controllers.utilityPanels.ProfileDetailsController;
import celtech.printerControl.model.Printer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Toggle;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SettingsSlideOutPanelController implements Initializable, PopupCommandTransmitter
{

    private Stenographer steno = StenographerFactory.getStenographer(SettingsSlideOutPanelController.class.getName());

    @FXML
    private TabPane detailedSettingsTabPane;

    @FXML
    private Tab materialTab;

    @FXML
    private Tab profileTab;

    @FXML
    private ProfileDetailsController profileDetailsController;

    @FXML
    private VBox profileData;

    @FXML
    private VBox materialData;

    private StringConverter booleanConverter = null;

    private SlicerParametersFile draftSettings = SlicerParametersContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName);
    private SlicerParametersFile normalSettings = SlicerParametersContainer.getSettingsByProfileName(ApplicationConfiguration.normalSettingsProfileName);
    private SlicerParametersFile fineSettings = SlicerParametersContainer.getSettingsByProfileName(ApplicationConfiguration.fineSettingsProfileName);
    private SlicerParametersFile customSettings = null;
    private SlicerParametersFile lastSettings = null;

    private ObservableList<String> nozzleOptions = FXCollections.observableArrayList(new String("0.3mm"), new String("0.8mm"));
    private ObservableList<String> fillPatternOptions = FXCollections.observableArrayList(new String("rectilinear"), new String("line"), new String("concentric"), new String("honeycomb"));
    private ObservableList<String> supportPatternOptions = FXCollections.observableArrayList(new String("rectilinear"), new String("rectilinear grid"), new String("honeycomb"));

    private ChangeListener<Toggle> nozzleSelectionListener = null;

    private ObservableList<Filament> availableFilaments = FXCollections.observableArrayList();

    private Printer currentPrinter = null;
    private Filament currentlyLoadedFilament = null;

    private int boundToNozzle = -1;

    private BooleanProperty showProfileData = new SimpleBooleanProperty(false);

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        showMaterialTab();
    }

    /**
     *
     * @param settings
     */
    public void updateProfileData(SlicerParametersFile settings)
    {
//        profileDetailsController.updateProfileData(settings);
    }

    /**
     *
     * @param receiver
     */
    @Override
    public void provideReceiver(PopupCommandReceiver receiver)
    {
//        profileDetailsController.provideReceiver(receiver);
    }

    /**
     *
     */
    public void showMaterialTab()
    {
        detailedSettingsTabPane.getSelectionModel().select(materialTab);
    }

    /**
     *
     */
    public void showProfileTab()
    {
        detailedSettingsTabPane.getSelectionModel().select(profileTab);
    }
}
