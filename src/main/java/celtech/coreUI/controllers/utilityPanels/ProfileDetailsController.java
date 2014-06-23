package celtech.coreUI.controllers.utilityPanels;

import celtech.CoreTest;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.coreUI.controllers.popups.PopupCommandReceiver;
import celtech.coreUI.controllers.popups.PopupCommandTransmitter;
import celtech.services.slicer.RoboxProfile;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.controlsfx.control.textfield.CustomTextField;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProfileDetailsController implements Initializable, PopupCommandTransmitter
{
    
    private Stenographer steno = StenographerFactory.getStenographer(ProfileDetailsController.class.getName());
    private SettingsScreenState settingsScreenState = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;
    
    @FXML
    private VBox container;
    
    @FXML
    private RestrictedNumberField fillDensity;
    
    @FXML
    private RestrictedNumberField slowFanIfLayerTimeBelow;
    
    @FXML
    private ToggleButton nozzle2Button;
    
    @FXML
    private Button saveAsButton;
    
    @FXML
    private ToggleGroup nozzleChoiceGroup;
    
    @FXML
    private GridPane supportGrid;
    
    @FXML
    private RestrictedNumberField enableFanIfLayerTimeBelow;
    
    @FXML
    private CheckBox enableAutoCooling;
    
    @FXML
    private RestrictedNumberField solidInfillSpeed;
    
    @FXML
    private Label nozzleEjectionVolumeLabel;
    
    @FXML
    private RestrictedNumberField nozzlePartialOpen;
    
    @FXML
    private HBox nozzleEjectionVolumeHBox;
    
    @FXML
    private RestrictedNumberField supportOverhangThreshold;
    
    @FXML
    private ComboBox<String> fillNozzleChoice;
    
    @FXML
    private RestrictedNumberField nozzleEjectionVolume;
    
    @FXML
    private Label nozzleWipeVolumeLabel;
    
    @FXML
    private RestrictedNumberField solidLayersTop;
    
    @FXML
    private RestrictedNumberField solidLayersBottom;
    
    @FXML
    private CheckBox supportMaterialEnabled;
    
    @FXML
    private ComboBox<String> supportNozzleChoice;
    
    @FXML
    private RestrictedNumberField numberOfPerimeters;
    
    @FXML
    private RestrictedNumberField topSolidInfillSpeed;
    
    @FXML
    private Button deleteProfileButton;
    
    @FXML
    private RestrictedNumberField perimeterSpeed;
    
    @FXML
    private GridPane coolingGrid;
    
    @FXML
    private GridPane speedGrid;
    
    @FXML
    private Label supportInterfaceNozzleChoiceLabel;
    
    @FXML
    private RestrictedNumberField gapFillSpeed;
    
    @FXML
    private CustomTextField profileNameField;
    
    @FXML
    private RestrictedNumberField retractLength;
    
    @FXML
    private HBox retractLengthHBox;
    
    @FXML
    private Label retractLengthLabel;
    
    @FXML
    private ComboBox<String> supportInterfaceNozzleChoice;
    
    @FXML
    private ComboBox<String> fillPatternChoice;
    
    @FXML
    private Label perimeterNozzleChoiceLabel;
    
    @FXML
    private RestrictedNumberField supportMaterialSpeed;
    
    @FXML
    private RestrictedNumberField brimWidth;
    
    @FXML
    private RestrictedNumberField infillSpeed;
    
    @FXML
    private Button cancelButton;
    
    @FXML
    private RestrictedNumberField nozzleWipeVolume;
    
    @FXML
    private RestrictedNumberField minPrintSpeed;
    
    @FXML
    private ToggleButton nozzle1Button;
    
    @FXML
    private RestrictedNumberField minFanSpeed;
    
    @FXML
    private RestrictedNumberField infillEveryN;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private RestrictedNumberField forcedSupportLayers;
    
    @FXML
    private Label fillNozzleChoiceLabel;
    
    @FXML
    private Label supportNozzleChoiceLabel;
    
    @FXML
    private HBox notEditingOptions;
    
    @FXML
    private TabPane customTabPane;
    
    @FXML
    private ComboBox<String> perimeterNozzleChoice;
    
    @FXML
    private RestrictedNumberField supportPatternSpacing;
    
    @FXML
    private RestrictedNumberField smallPerimeterSpeed;
    
    @FXML
    private RestrictedNumberField maxFanSpeed;
    
    @FXML
    private HBox editingOptions;
    
    @FXML
    private Label nozzlePartialOpenLabel;
    
    @FXML
    private RestrictedNumberField disableFanForFirstNLayers;
    
    @FXML
    private HBox nozzleWipeVolumeHBox;
    
    @FXML
    private RestrictedNumberField retractSpeed;
    
    @FXML
    private HBox retractSpeedHBox;
    
    @FXML
    private Label retractSpeedLabel;
    
    @FXML
    private HBox nozzlePartialOpenHBox;
    
    @FXML
    private ComboBox<String> supportPattern;
    
    @FXML
    private RestrictedNumberField bridgesFanSpeed;
    
    @FXML
    private HBox immutableOptions;
    
    @FXML
    private RestrictedNumberField bridgesSpeed;
    
    @FXML
    private RestrictedNumberField layerHeight;
    
    @FXML
    private GridPane extrusionGrid;
    
    @FXML
    private RestrictedNumberField externalPerimeterSpeed;
    
    @FXML
    private RestrictedNumberField supportPatternAngle;
    
    @FXML
    void saveData(ActionEvent event)
    {
        final RoboxProfile profileToSave = getProfileData();
        PrintProfileContainer.saveProfile(profileToSave);
        isDirty.set(false);
    }
    
    @FXML
    void cancelEdit(ActionEvent event)
    {
        if (lastSettings != null)
        {
            updateProfileData(lastSettings);
        }
    }
    
    @FXML
    void deleteProfile(ActionEvent event)
    {
        final RoboxProfile profileToSave = getProfileData();
        PrintProfileContainer.deleteProfile(profileToSave.getProfileName());
    }
    
    @FXML
    void launchSaveAsDialogue(ActionEvent event)
    {
        if (commandReceiver != null)
        {
            commandReceiver.triggerSaveAs(this);
        }
    }
    
    private BooleanProperty profileNameInvalid = new SimpleBooleanProperty(false);
    
    private final Image redcrossImage = new Image(CoreTest.class.getResource(ApplicationConfiguration.imageResourcePath + "redcross.png").toExternalForm());
    private final ImageView redcrossHolder = new ImageView(redcrossImage);
    
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private final BooleanProperty isMutable = new SimpleBooleanProperty(false);
    private final BooleanProperty showButtons = new SimpleBooleanProperty(true);
    
    private final ObservableList<String> nozzleOptions = FXCollections.observableArrayList(new String("0.3mm"), new String("0.8mm"));
    private final ObservableList<String> fillPatternOptions = FXCollections.observableArrayList(new String("rectilinear"), new String("line"), new String("concentric"), new String("honeycomb"));
    private final ObservableList<String> supportPatternOptions = FXCollections.observableArrayList(new String("rectilinear"), new String("rectilinear grid"), new String("honeycomb"));
    
    private ChangeListener<Toggle> nozzleSelectionListener = null;
    
    private RoboxProfile lastSettings = null;
    
    private int boundToNozzle = -1;
    
    private final ChangeListener<String> dirtyStringListener = (ObservableValue<? extends String> ov, String t, String t1) ->
    {
        isDirty.set(true);
    };
    
    private PopupCommandReceiver commandReceiver = null;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        applicationStatus = ApplicationStatus.getInstance();
        displayManager = DisplayManager.getInstance();
        settingsScreenState = SettingsScreenState.getInstance();
        
        profileNameField.setRight(redcrossHolder);
        profileNameField.getRight().visibleProperty().bind(profileNameInvalid.and(isDirty));

//        profileNameField.addEventHandler(KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {
//
//            @Override
//            public void handle(KeyEvent event)
//            {
//                    validateProfileName();
//            }
//        });
        editingOptions.visibleProperty().bind(isDirty.and(showButtons).and(isMutable));
        notEditingOptions.visibleProperty().bind(isDirty.not().and(showButtons).and(isMutable));
        immutableOptions.visibleProperty().bind(isDirty.not().and(showButtons).and(isMutable.not()));
        
        profileNameField.disableProperty().bind(isMutable.not());
        coolingGrid.disableProperty().bind(isMutable.not());
        extrusionGrid.disableProperty().bind(isMutable.not());
        perimeterNozzleChoice.disableProperty().bind(isMutable.not());
        perimeterNozzleChoiceLabel.disableProperty().bind(isMutable.not());
        fillNozzleChoice.disableProperty().bind(isMutable.not());
        fillNozzleChoiceLabel.disableProperty().bind(isMutable.not());
        supportNozzleChoice.disableProperty().bind(isMutable.not());
        supportNozzleChoiceLabel.disableProperty().bind(isMutable.not());
        supportInterfaceNozzleChoice.disableProperty().bind(isMutable.not());
        supportInterfaceNozzleChoiceLabel.disableProperty().bind(isMutable.not());
        nozzleEjectionVolumeLabel.disableProperty().bind(isMutable.not());
        nozzleEjectionVolumeHBox.disableProperty().bind(isMutable.not());
        nozzleWipeVolumeLabel.disableProperty().bind(isMutable.not());
        nozzleWipeVolumeHBox.disableProperty().bind(isMutable.not());
        nozzlePartialOpenLabel.disableProperty().bind(isMutable.not());
        nozzlePartialOpenHBox.disableProperty().bind(isMutable.not());
        retractLength.disableProperty().bind(isMutable.not());
        retractLengthLabel.disableProperty().bind(isMutable.not());
        retractLengthHBox.disableProperty().bind(isMutable.not());
        retractSpeed.disableProperty().bind(isMutable.not());
        retractSpeedHBox.disableProperty().bind(isMutable.not());
        retractSpeedLabel.disableProperty().bind(isMutable.not());
        supportGrid.disableProperty().bind(isMutable.not());
        speedGrid.disableProperty().bind(isMutable.not());

        // Nozzle 1 relates to the first set of data - nozzle 2 the second...
        nozzle1Button.setUserData(0);
        nozzle2Button.setUserData(1);
        
        nozzleSelectionListener = new ChangeListener<Toggle>()
        {
            @Override
            public void changed(ObservableValue<? extends Toggle> ov, Toggle t, Toggle t1)
            {
                if (t1 != null)
                {
                    int newNozzle = (int) t1.getUserData();
                    
                    if (newNozzle != boundToNozzle)
                    {
                        unbindNozzleParameters();
                        bindNozzleParameters(newNozzle);
                    }
                }
            }
        };
        
        nozzle1Button.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>()
                             {
                                 @Override
                                 public void handle(MouseEvent mouseEvent)
                                 {
                                     if (nozzle1Button.equals(nozzleChoiceGroup.getSelectedToggle()))
                                     {
                                         mouseEvent.consume();
                                     }
                                 }
        });
        
        nozzle2Button.addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>()
                             {
                                 @Override
                                 public void handle(MouseEvent mouseEvent)
                                 {
                                     if (nozzle2Button.equals(nozzleChoiceGroup.getSelectedToggle()))
                                     {
                                         mouseEvent.consume();
                                     }
                                 }
        });
        
        nozzleChoiceGroup.selectedToggleProperty().addListener(nozzleSelectionListener);
        
        perimeterNozzleChoice.setItems(nozzleOptions);
        fillNozzleChoice.setItems(nozzleOptions);
        supportNozzleChoice.setItems(nozzleOptions);
        supportInterfaceNozzleChoice.setItems(nozzleOptions);
        
        fillPatternChoice.setItems(fillPatternOptions);
        
        supportPattern.setItems(supportPatternOptions);

        //Dirty listeners...
        profileNameField.textProperty().addListener(dirtyStringListener);
        perimeterNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        fillNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        supportNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        supportInterfaceNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        fillDensity.textProperty().addListener(dirtyStringListener);
        slowFanIfLayerTimeBelow.textProperty().addListener(dirtyStringListener);
        enableFanIfLayerTimeBelow.textProperty().addListener(dirtyStringListener);
        enableAutoCooling.textProperty().addListener(dirtyStringListener);
        solidInfillSpeed.textProperty().addListener(dirtyStringListener);
        supportMaterialEnabled.textProperty().addListener(dirtyStringListener);
        supportOverhangThreshold.textProperty().addListener(dirtyStringListener);
        solidLayersTop.textProperty().addListener(dirtyStringListener);
        solidLayersBottom.textProperty().addListener(dirtyStringListener);
        numberOfPerimeters.textProperty().addListener(dirtyStringListener);
        topSolidInfillSpeed.textProperty().addListener(dirtyStringListener);
        perimeterSpeed.textProperty().addListener(dirtyStringListener);
        gapFillSpeed.textProperty().addListener(dirtyStringListener);
        retractLength.textProperty().addListener(dirtyStringListener);
        fillPatternChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        supportMaterialSpeed.textProperty().addListener(dirtyStringListener);
        brimWidth.textProperty().addListener(dirtyStringListener);
        infillSpeed.textProperty().addListener(dirtyStringListener);
        minPrintSpeed.textProperty().addListener(dirtyStringListener);
        minFanSpeed.textProperty().addListener(dirtyStringListener);
        infillEveryN.textProperty().addListener(dirtyStringListener);
        forcedSupportLayers.textProperty().addListener(dirtyStringListener);
        supportPatternSpacing.textProperty().addListener(dirtyStringListener);
        smallPerimeterSpeed.textProperty().addListener(dirtyStringListener);
        maxFanSpeed.textProperty().addListener(dirtyStringListener);
        disableFanForFirstNLayers.textProperty().addListener(dirtyStringListener);
        retractSpeed.textProperty().addListener(dirtyStringListener);
        bridgesFanSpeed.textProperty().addListener(dirtyStringListener);
        bridgesSpeed.textProperty().addListener(dirtyStringListener);
        layerHeight.textProperty().addListener(dirtyStringListener);
        externalPerimeterSpeed.textProperty().addListener(dirtyStringListener);
        supportPatternAngle.textProperty().addListener(dirtyStringListener);
        
        isDirty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
        {
            if (newValue == true)
            {
                DisplayManager displayManager = DisplayManager.getInstance();
                if (displayManager != null)
                {
                    Project currentProject = displayManager.getCurrentlyVisibleProject();
                    
                    if (currentProject != null)
                    {
                        currentProject.projectModified();
                    }
                }
            }
        });
    }
    
    private void bindNozzleParameters(int nozzleNumber)
    {
        boundToNozzle = nozzleNumber;
        
        Bindings.bindBidirectional(nozzleEjectionVolume.floatValueProperty(), lastSettings.getNozzle_ejection_volume().get(nozzleNumber));
        Bindings.bindBidirectional(nozzleWipeVolume.floatValueProperty(), lastSettings.getNozzle_wipe_volume().get(nozzleNumber));
        Bindings.bindBidirectional(nozzlePartialOpen.floatValueProperty(), lastSettings.getNozzle_partial_b_minimum().get(nozzleNumber));
        Bindings.bindBidirectional(retractLength.floatValueProperty(), lastSettings.retract_lengthProperty().get(nozzleNumber));
        Bindings.bindBidirectional(retractSpeed.intValueProperty(), lastSettings.retract_speedProperty().get(nozzleNumber));
        
        nozzleEjectionVolume.textProperty().addListener(dirtyStringListener);
        nozzleWipeVolume.textProperty().addListener(dirtyStringListener);
        nozzlePartialOpen.textProperty().addListener(dirtyStringListener);
        retractLength.textProperty().addListener(dirtyStringListener);
        retractSpeed.textProperty().addListener(dirtyStringListener);
    }
    
    private void unbindNozzleParameters()
    {
        if (boundToNozzle != -1)
        {
            nozzleEjectionVolume.textProperty().removeListener(dirtyStringListener);
            nozzleWipeVolume.textProperty().removeListener(dirtyStringListener);
            nozzlePartialOpen.textProperty().removeListener(dirtyStringListener);
            retractLength.textProperty().removeListener(dirtyStringListener);
            retractSpeed.textProperty().removeListener(dirtyStringListener);
            
            Bindings.unbindBidirectional(nozzleEjectionVolume.floatValueProperty(), lastSettings.getNozzle_ejection_volume().get(boundToNozzle));
            Bindings.unbindBidirectional(nozzleWipeVolume.floatValueProperty(), lastSettings.getNozzle_wipe_volume().get(boundToNozzle));
            Bindings.unbindBidirectional(nozzlePartialOpen.floatValueProperty(), lastSettings.getNozzle_partial_b_minimum().get(boundToNozzle));
            Bindings.unbindBidirectional(retractLength.floatValueProperty(), lastSettings.retract_lengthProperty().get(boundToNozzle));
            Bindings.unbindBidirectional(retractSpeed.intValueProperty(), lastSettings.retract_speedProperty().get(boundToNozzle));
        }
        
        boundToNozzle = -1;
    }
    
    private void bindToNewSettings(RoboxProfile newSettings)
    {
        if (lastSettings != null)
        {
            //Nozzle independent custom settings
            Bindings.unbindBidirectional(layerHeight.floatValueProperty(), lastSettings.getLayer_height());
            
            lastSettings.perimeter_nozzleProperty().unbind();
            lastSettings.getPerimeter_extrusion_width().unbind();
            
            lastSettings.infill_nozzleProperty().unbind();
            lastSettings.getInfill_extrusion_width().unbind();
            lastSettings.getSolid_infill_extrusion_width().unbind();
            
            lastSettings.getExtrusion_width().unbind();
            lastSettings.getTop_infill_extrusion_width().unbind();
            lastSettings.getSupport_material_extrusion_width().unbind();
            
            lastSettings.support_material_nozzleProperty().unbind();
            
            Bindings.unbindBidirectional(lastSettings.support_materialProperty(), supportMaterialEnabled.selectedProperty());
            
            Bindings.unbindBidirectional(fillDensity.floatValueProperty(), lastSettings.fill_densityProperty());
            
            Bindings.unbindBidirectional(fillPatternChoice.valueProperty(), lastSettings.fill_patternProperty());
            Bindings.unbindBidirectional(infillEveryN.intValueProperty(), lastSettings.infill_every_layersProperty());
            
            Bindings.unbindBidirectional(supportOverhangThreshold.intValueProperty(), lastSettings.support_material_thresholdProperty());
            Bindings.unbindBidirectional(forcedSupportLayers.intValueProperty(), lastSettings.support_material_enforce_layersProperty());
            
            Bindings.unbindBidirectional(supportPattern.valueProperty(), lastSettings.support_material_patternProperty());
            
            Bindings.unbindBidirectional(supportPatternSpacing.floatValueProperty(), lastSettings.support_material_spacingProperty());
            Bindings.unbindBidirectional(supportPatternAngle.intValueProperty(), lastSettings.support_material_angleProperty());
            
            Bindings.unbindBidirectional(numberOfPerimeters.intValueProperty(), lastSettings.perimetersProperty());
            
            Bindings.unbindBidirectional(perimeterSpeed.intValueProperty(), lastSettings.perimeter_speedProperty());
            
            Bindings.unbindBidirectional(smallPerimeterSpeed.intValueProperty(), lastSettings.small_perimeter_speedProperty());
            Bindings.unbindBidirectional(externalPerimeterSpeed.intValueProperty(), lastSettings.external_perimeter_speedProperty());
            Bindings.unbindBidirectional(infillSpeed.intValueProperty(), lastSettings.infill_speedProperty());
            Bindings.unbindBidirectional(solidInfillSpeed.intValueProperty(), lastSettings.solid_infill_speedProperty());
            Bindings.unbindBidirectional(topSolidInfillSpeed.intValueProperty(), lastSettings.top_solid_infill_speedProperty());
            Bindings.unbindBidirectional(supportMaterialSpeed.intValueProperty(), lastSettings.support_material_speedProperty());
            Bindings.unbindBidirectional(bridgesSpeed.intValueProperty(), lastSettings.bridge_speedProperty());
            Bindings.unbindBidirectional(gapFillSpeed.intValueProperty(), lastSettings.gap_fill_speedProperty());
            
            Bindings.unbindBidirectional(solidLayersTop.intValueProperty(), lastSettings.top_solid_layersProperty());
            Bindings.unbindBidirectional(solidLayersBottom.intValueProperty(), lastSettings.bottom_solid_layersProperty());
            
            Bindings.unbindBidirectional(brimWidth.intValueProperty(), lastSettings.getBrim_width());
            
            Bindings.unbindBidirectional(enableAutoCooling.textProperty(), lastSettings.getCooling());
            Bindings.unbindBidirectional(minFanSpeed.intValueProperty(), lastSettings.getMin_fan_speed());
            Bindings.unbindBidirectional(maxFanSpeed.intValueProperty(), lastSettings.getMax_fan_speed());
            Bindings.unbindBidirectional(bridgesFanSpeed.intValueProperty(), lastSettings.getBridge_fan_speed());
            Bindings.unbindBidirectional(disableFanForFirstNLayers.intValueProperty(), lastSettings.getDisable_fan_first_layers());
            Bindings.unbindBidirectional(enableFanIfLayerTimeBelow.intValueProperty(), lastSettings.getFan_below_layer_time());
            Bindings.unbindBidirectional(slowFanIfLayerTimeBelow.intValueProperty(), lastSettings.getSlowdown_below_layer_time());
            Bindings.unbindBidirectional(minPrintSpeed.intValueProperty(), lastSettings.getMin_print_speed());
            
            unbindNozzleParameters();
        }
        
        profileNameField.setText(newSettings.getProfileName());
        
        Bindings.bindBidirectional(layerHeight.floatValueProperty(), newSettings.getLayer_height());
        
        perimeterNozzleChoice.getSelectionModel().select(newSettings.perimeter_nozzleProperty().get() - 1);
        newSettings.perimeter_nozzleProperty().bind(perimeterNozzleChoice.getSelectionModel().selectedIndexProperty().add(1));
//        newSettings.getPerimeter_extrusion_width().bind(newSettings.getExtrusion_width());

        fillNozzleChoice.getSelectionModel().select(newSettings.infill_nozzleProperty().get() - 1);
        newSettings.infill_nozzleProperty().bind(fillNozzleChoice.getSelectionModel().selectedIndexProperty().add(1));
//        newSettings.getInfill_extrusion_width().bind(...);
//        newSettings.getSolid_infill_extrusion_width().bind(...);

//        newSettings.getExtrusion_width().bind(
//        ...);
//            newSettings.getTop_infill_extrusion_width().bind(
//        ...);
//            newSettings.getSupport_material_extrusion_width().bind(
//        ...);
        supportNozzleChoice.getSelectionModel().select(newSettings.support_material_nozzleProperty().get() - 1);
        newSettings.support_material_nozzleProperty().bind(supportNozzleChoice.getSelectionModel().selectedIndexProperty().add(1));
        
        supportInterfaceNozzleChoice.getSelectionModel().select(newSettings.support_material_interface_nozzleProperty().get() - 1);
        newSettings.support_material_interface_nozzleProperty().bind(supportInterfaceNozzleChoice.getSelectionModel().selectedIndexProperty().add(1));
        
        Bindings.bindBidirectional(newSettings.support_materialProperty(), supportMaterialEnabled.selectedProperty());
        
        Bindings.bindBidirectional(fillDensity.floatValueProperty(), newSettings.fill_densityProperty());
        
        Bindings.bindBidirectional(fillPatternChoice.valueProperty(), newSettings.fill_patternProperty());
        Bindings.bindBidirectional(infillEveryN.intValueProperty(), newSettings.infill_every_layersProperty());
        
        Bindings.bindBidirectional(supportOverhangThreshold.intValueProperty(), newSettings.support_material_thresholdProperty());
        Bindings.bindBidirectional(forcedSupportLayers.intValueProperty(), newSettings.support_material_enforce_layersProperty());
        
        Bindings.bindBidirectional(supportPattern.valueProperty(), newSettings.support_material_patternProperty());
        
        Bindings.bindBidirectional(supportPatternSpacing.floatValueProperty(), newSettings.support_material_spacingProperty());
        Bindings.bindBidirectional(supportPatternAngle.intValueProperty(), newSettings.support_material_angleProperty());
        
        Bindings.bindBidirectional(numberOfPerimeters.intValueProperty(), newSettings.perimetersProperty());
        
        Bindings.bindBidirectional(perimeterSpeed.intValueProperty(), newSettings.perimeter_speedProperty());
        
        Bindings.bindBidirectional(smallPerimeterSpeed.intValueProperty(), newSettings.small_perimeter_speedProperty());
        Bindings.bindBidirectional(externalPerimeterSpeed.intValueProperty(), newSettings.external_perimeter_speedProperty());
        Bindings.bindBidirectional(infillSpeed.intValueProperty(), newSettings.infill_speedProperty());
        Bindings.bindBidirectional(solidInfillSpeed.intValueProperty(), newSettings.solid_infill_speedProperty());
        Bindings.bindBidirectional(topSolidInfillSpeed.intValueProperty(), newSettings.top_solid_infill_speedProperty());
        Bindings.bindBidirectional(supportMaterialSpeed.intValueProperty(), newSettings.support_material_speedProperty());
        Bindings.bindBidirectional(bridgesSpeed.intValueProperty(), newSettings.bridge_speedProperty());
        Bindings.bindBidirectional(gapFillSpeed.intValueProperty(), newSettings.gap_fill_speedProperty());
        
        Bindings.bindBidirectional(solidLayersTop.intValueProperty(), newSettings.top_solid_layersProperty());
        Bindings.bindBidirectional(solidLayersBottom.intValueProperty(), newSettings.bottom_solid_layersProperty());
        
        Bindings.bindBidirectional(brimWidth.intValueProperty(), newSettings.getBrim_width());
        
        Bindings.bindBidirectional(enableAutoCooling.selectedProperty(), newSettings.getCooling());
        Bindings.bindBidirectional(minFanSpeed.intValueProperty(), newSettings.getMin_fan_speed());
        Bindings.bindBidirectional(maxFanSpeed.intValueProperty(), newSettings.getMax_fan_speed());
        Bindings.bindBidirectional(bridgesFanSpeed.intValueProperty(), newSettings.getBridge_fan_speed());
        Bindings.bindBidirectional(disableFanForFirstNLayers.intValueProperty(), newSettings.getDisable_fan_first_layers());
        Bindings.bindBidirectional(enableFanIfLayerTimeBelow.intValueProperty(), newSettings.getFan_below_layer_time());
        Bindings.bindBidirectional(slowFanIfLayerTimeBelow.intValueProperty(), newSettings.getSlowdown_below_layer_time());
        Bindings.bindBidirectional(minPrintSpeed.intValueProperty(), newSettings.getMin_print_speed());

        //Switch to nozzle 1 data by default
        nozzle1Button.setSelected(true);
        
        lastSettings = newSettings;
        
        bindNozzleParameters(0);
    }

    /**
     *
     * @param settings
     */
    public void updateProfileData(RoboxProfile settings)
    {
        if (settings == null)
        {
            container.setVisible(false);
        } else
        {
            container.setVisible(true);
            bindToNewSettings(settings);
            isMutable.set(settings.isMutable());
            isDirty.set(false);
        }
    }

    /**
     *
     * @return
     */
    public RoboxProfile getProfileData()
    {
        return lastSettings;
    }

    /**
     *
     * @param show
     */
    public void showButtons(boolean show)
    {
        showButtons.set(show);
    }

    /**
     *
     * @param receiver
     */
    @Override
    public void provideReceiver(PopupCommandReceiver receiver)
    {
        commandReceiver = receiver;
    }
    
    private void validateProfileName()
    {
        boolean invalid = false;
        String profileNameText = profileNameField.getText();
        
        if (profileNameText.equals(""))
        {
            invalid = true;
        } else
        {
            ObservableList<RoboxProfile> existingProfileList = PrintProfileContainer.getUserProfileList();
            for (RoboxProfile settings : existingProfileList)
            {
                if (settings.getProfileName().equals(profileNameText))
                {
                    invalid = true;
                    break;
                }
            }
        }
        profileNameInvalid.set(invalid);
    }

    /**
     *
     * @return
     */
    public ReadOnlyBooleanProperty getProfileNameInvalidProperty()
    {
        return profileNameInvalid;
    }

    /**
     *
     * @return
     */
    public String getProfileName()
    {
        return profileNameField.getText();
    }
}
