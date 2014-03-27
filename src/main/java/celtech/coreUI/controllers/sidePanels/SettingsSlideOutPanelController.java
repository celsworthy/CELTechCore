/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.coreUI.controllers.sidePanels;

import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.coreUI.controllers.popups.PopupCommandReceiver;
import celtech.coreUI.controllers.popups.PopupCommandTransmitter;
import celtech.coreUI.controllers.utilityPanels.MaterialDetailsController;
import celtech.coreUI.controllers.utilityPanels.ProfileDetailsController;
import celtech.printerControl.Printer;
import celtech.services.slicer.SlicerSettings;
import celtech.utils.FXUtils;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Toggle;
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
    private SettingsScreenState settingsScreenState = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;

    @FXML
    private MaterialDetailsController materialDetailsController;

    @FXML
    private ProfileDetailsController profileDetailsController;

    private StringConverter intConverter = FXUtils.getIntConverter();
    private StringConverter floatConverter = FXUtils.getFloatConverter(2);
    private StringConverter booleanConverter = null;

    private SlicerSettings draftSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName);
    private SlicerSettings normalSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.normalSettingsProfileName);
    private SlicerSettings fineSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.fineSettingsProfileName);
    private SlicerSettings customSettings = null;
    private SlicerSettings lastSettings = null;

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
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        applicationStatus = ApplicationStatus.getInstance();
        displayManager = DisplayManager.getInstance();

        settingsScreenState = SettingsScreenState.getInstance();
        
//        profileData.visibleProperty().bind(showProfileData);
    }

    public void updateFilamentData(Filament filament)
    {
        materialDetailsController.updateMaterialData(filament);
    }

    public void updateProfileData(SlicerSettings settings)
    {
        if (settings == null)
        {
            showProfileData.set(false);
        } else
        {
            showProfileData.set(true);
            profileDetailsController.updateProfileData(settings);
        }
    }

    @Override
    public void provideReceiver(PopupCommandReceiver receiver)
    {
        profileDetailsController.provideReceiver(receiver);
        materialDetailsController.provideReceiver(receiver);
    }

}
