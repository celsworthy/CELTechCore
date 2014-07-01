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
    private Label nozzlePreejectionVolumeLabel;

    @FXML
    private Label nozzleEjectionVolumeLabel;

    @FXML
    private RestrictedNumberField nozzlePartialOpen;

    @FXML
    private HBox nozzlePreejectionVolumeHBox;

    @FXML
    private HBox nozzleEjectionVolumeHBox;

    @FXML
    private RestrictedNumberField supportOverhangThreshold;

    @FXML
    private ComboBox<String> fillNozzleChoice;

    @FXML
    private RestrictedNumberField nozzlePreejectionVolume;

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
        updateSettingsFromGUI(workingProfile);
        saveNozzleParametersToWorkingProfile();
        if (commandReceiver != null)
        {
            commandReceiver.triggerSave(workingProfile);
        }
        isDirty.set(false);
    }

    @FXML
    void cancelEdit(ActionEvent event)
    {
        bindToNewSettings(masterProfile);
        isDirty.set(false);
    }

    @FXML
    void deleteProfile(ActionEvent event)
    {
        PrintProfileContainer.deleteProfile(masterProfile.getProfileName());
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

    private RoboxProfile masterProfile = null;
    private RoboxProfile workingProfile = null;
    private RoboxProfile lastBoundProfile = null;

    private int boundToNozzle = -1;

    private final ChangeListener<String> dirtyStringListener = (ObservableValue<? extends String> ov, String t, String t1) ->
    {
        isDirty.set(true);
    };

    private final ChangeListener<Boolean> dirtyBooleanListener = (ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) ->
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
        nozzlePreejectionVolumeLabel.disableProperty().bind(isMutable.not());
        nozzlePreejectionVolumeHBox.disableProperty().bind(isMutable.not());
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
                        saveNozzleParametersToWorkingProfile();
                        unbindNozzleParameters();
                        bindNozzleParameters(newNozzle, lastBoundProfile);
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
        solidInfillSpeed.textProperty().addListener(dirtyStringListener);
        supportMaterialEnabled.textProperty().addListener(dirtyStringListener);
        supportOverhangThreshold.textProperty().addListener(dirtyStringListener);
        solidLayersTop.textProperty().addListener(dirtyStringListener);
        solidLayersBottom.textProperty().addListener(dirtyStringListener);
        numberOfPerimeters.textProperty().addListener(dirtyStringListener);
        topSolidInfillSpeed.textProperty().addListener(dirtyStringListener);
        perimeterSpeed.textProperty().addListener(dirtyStringListener);
        gapFillSpeed.textProperty().addListener(dirtyStringListener);
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
        bridgesFanSpeed.textProperty().addListener(dirtyStringListener);
        bridgesSpeed.textProperty().addListener(dirtyStringListener);
        layerHeight.textProperty().addListener(dirtyStringListener);
        externalPerimeterSpeed.textProperty().addListener(dirtyStringListener);
        supportPatternAngle.textProperty().addListener(dirtyStringListener);

        supportMaterialEnabled.selectedProperty().addListener(dirtyBooleanListener);
        enableAutoCooling.selectedProperty().addListener(dirtyBooleanListener);

        perimeterNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        fillNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        supportNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        supportInterfaceNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        supportPattern.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);

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

    private void bindNozzleParameters(int nozzleNumber, RoboxProfile newSettings)
    {
        boundToNozzle = nozzleNumber;

        nozzlePreejectionVolume.floatValueProperty().set(newSettings.getNozzle_ejection_volume().get(nozzleNumber).get());
        nozzleEjectionVolume.floatValueProperty().set(newSettings.getNozzle_ejection_volume().get(nozzleNumber).get());
        nozzleWipeVolume.floatValueProperty().set(newSettings.getNozzle_wipe_volume().get(nozzleNumber).get());
        nozzlePartialOpen.floatValueProperty().set(newSettings.getNozzle_partial_b_minimum().get(nozzleNumber).get());
        retractLength.floatValueProperty().set(newSettings.retract_lengthProperty().get(nozzleNumber).get());
        retractSpeed.intValueProperty().set(newSettings.retract_speedProperty().get(nozzleNumber).get());

        nozzlePreejectionVolume.textProperty().addListener(dirtyStringListener);
        nozzleEjectionVolume.textProperty().addListener(dirtyStringListener);
        nozzleWipeVolume.textProperty().addListener(dirtyStringListener);
        nozzlePartialOpen.textProperty().addListener(dirtyStringListener);
        retractLength.textProperty().addListener(dirtyStringListener);
        retractSpeed.textProperty().addListener(dirtyStringListener);
    }

    private void saveNozzleParametersToWorkingProfile()
    {
        if (boundToNozzle != -1)
        {
            workingProfile.getNozzle_preejection_volume().get(boundToNozzle).set(nozzlePreejectionVolume.floatValueProperty().get());
            workingProfile.getNozzle_ejection_volume().get(boundToNozzle).set(nozzleEjectionVolume.floatValueProperty().get());
            workingProfile.getNozzle_wipe_volume().get(boundToNozzle).set(nozzleWipeVolume.floatValueProperty().get());
            workingProfile.getNozzle_partial_b_minimum().get(boundToNozzle).set(nozzlePartialOpen.floatValueProperty().get());
            workingProfile.retract_lengthProperty().get(boundToNozzle).set(retractLength.floatValueProperty().get());
            workingProfile.retract_speedProperty().get(boundToNozzle).set(retractSpeed.intValueProperty().get());
        }
    }

    private void unbindNozzleParameters()
    {
        if (boundToNozzle != -1)
        {
            if (lastBoundProfile != null)
            {
                lastBoundProfile.getNozzle_preejection_volume().get(boundToNozzle).set(nozzlePreejectionVolume.floatValueProperty().get());
                lastBoundProfile.getNozzle_ejection_volume().get(boundToNozzle).set(nozzleEjectionVolume.floatValueProperty().get());
                lastBoundProfile.getNozzle_wipe_volume().get(boundToNozzle).set(nozzleWipeVolume.floatValueProperty().get());
                lastBoundProfile.getNozzle_partial_b_minimum().get(boundToNozzle).set(nozzlePartialOpen.floatValueProperty().get());
                lastBoundProfile.retract_lengthProperty().get(boundToNozzle).set(retractLength.floatValueProperty().get());
                lastBoundProfile.retract_speedProperty().get(boundToNozzle).set(retractSpeed.intValueProperty().get());
            }
            nozzlePreejectionVolume.textProperty().removeListener(dirtyStringListener);
            nozzleEjectionVolume.textProperty().removeListener(dirtyStringListener);
            nozzleWipeVolume.textProperty().removeListener(dirtyStringListener);
            nozzlePartialOpen.textProperty().removeListener(dirtyStringListener);
            retractLength.textProperty().removeListener(dirtyStringListener);
            retractSpeed.textProperty().removeListener(dirtyStringListener);
        }

        boundToNozzle = -1;
    }

    private void bindToNewSettings(RoboxProfile newSettings)
    {
        if (lastBoundProfile != null)
        {
            unbindNozzleParameters();
        }

        profileNameField.setText(newSettings.getProfileName());

        updateGUIFromSettings(newSettings);

        //Switch to nozzle 1 data by default
        nozzle1Button.setSelected(true);

        bindNozzleParameters(0, newSettings);

        lastBoundProfile = newSettings;
    }

    private void updateGUIFromSettings(RoboxProfile newSettings)
    {
        // Extrusion tab
        layerHeight.floatValueProperty().set(newSettings.getLayer_height().get());
        fillDensity.floatValueProperty().set(newSettings.fill_densityProperty().get());
        fillPatternChoice.valueProperty().set(newSettings.fill_patternProperty().get());
        infillEveryN.intValueProperty().set(newSettings.infill_every_layersProperty().get());
        solidLayersTop.intValueProperty().set(newSettings.top_solid_layersProperty().get());
        solidLayersBottom.intValueProperty().set(newSettings.bottom_solid_layersProperty().get());
        numberOfPerimeters.intValueProperty().set(newSettings.perimetersProperty().get());
        brimWidth.intValueProperty().set(newSettings.getBrim_width().get());

        //Nozzle tab
        perimeterNozzleChoice.getSelectionModel().select(newSettings.perimeter_nozzleProperty().get() - 1);
        fillNozzleChoice.getSelectionModel().select(newSettings.infill_nozzleProperty().get() - 1);
        supportNozzleChoice.getSelectionModel().select(newSettings.support_material_nozzleProperty().get() - 1);
        supportInterfaceNozzleChoice.getSelectionModel().select(newSettings.support_material_interface_nozzleProperty().get() - 1);

        //Support tab
        supportMaterialEnabled.selectedProperty().set(newSettings.support_materialProperty().get());
        supportOverhangThreshold.intValueProperty().set(newSettings.support_material_thresholdProperty().get());
        forcedSupportLayers.intValueProperty().set(newSettings.support_material_enforce_layersProperty().get());
        supportPattern.valueProperty().set(newSettings.support_material_patternProperty().get());
        supportPatternSpacing.floatValueProperty().set(newSettings.support_material_spacingProperty().get());
        supportPatternAngle.intValueProperty().set(newSettings.support_material_angleProperty().get());

        //Speed tab
        perimeterSpeed.intValueProperty().set(newSettings.perimeter_speedProperty().get());
        smallPerimeterSpeed.intValueProperty().set(newSettings.small_perimeter_speedProperty().get());
        externalPerimeterSpeed.intValueProperty().set(newSettings.external_perimeter_speedProperty().get());
        infillSpeed.intValueProperty().set(newSettings.infill_speedProperty().get());
        solidInfillSpeed.intValueProperty().set(newSettings.solid_infill_speedProperty().get());
        topSolidInfillSpeed.intValueProperty().set(newSettings.top_solid_infill_speedProperty().get());
        supportMaterialSpeed.intValueProperty().set(newSettings.support_material_speedProperty().get());
        bridgesSpeed.intValueProperty().set(newSettings.bridge_speedProperty().get());
        gapFillSpeed.intValueProperty().set(newSettings.gap_fill_speedProperty().get());

        //Cooling tab
        enableAutoCooling.selectedProperty().set(newSettings.getCooling().get());
        minFanSpeed.intValueProperty().set(newSettings.getMin_fan_speed().get());
        maxFanSpeed.intValueProperty().set(newSettings.getMax_fan_speed().get());
        bridgesFanSpeed.intValueProperty().set(newSettings.getBridge_fan_speed().get());
        disableFanForFirstNLayers.intValueProperty().set(newSettings.getDisable_fan_first_layers().get());
        enableFanIfLayerTimeBelow.intValueProperty().set(newSettings.getFan_below_layer_time().get());
        slowFanIfLayerTimeBelow.intValueProperty().set(newSettings.getSlowdown_below_layer_time().get());
        minPrintSpeed.intValueProperty().set(newSettings.getMin_print_speed().get());
    }

    private void updateSettingsFromGUI(RoboxProfile settingsToUpdate)
    {
        // Extrusion tab
        settingsToUpdate.getLayer_height().set(layerHeight.floatValueProperty().get());
        settingsToUpdate.fill_densityProperty().set(fillDensity.floatValueProperty().get());
        settingsToUpdate.fill_patternProperty().set(fillPatternChoice.valueProperty().get());
        settingsToUpdate.infill_every_layersProperty().set(infillEveryN.intValueProperty().get());
        settingsToUpdate.top_solid_layersProperty().set(solidLayersTop.intValueProperty().get());
        settingsToUpdate.bottom_solid_layersProperty().set(solidLayersBottom.intValueProperty().get());
        settingsToUpdate.perimetersProperty().set(numberOfPerimeters.intValueProperty().get());
        settingsToUpdate.getBrim_width().set(brimWidth.intValueProperty().get());

        //Nozzle tab
        settingsToUpdate.perimeter_nozzleProperty().set(perimeterNozzleChoice.getSelectionModel().getSelectedIndex() + 1);
        settingsToUpdate.infill_nozzleProperty().set(fillNozzleChoice.getSelectionModel().getSelectedIndex() + 1);
        settingsToUpdate.support_material_nozzleProperty().set(supportNozzleChoice.getSelectionModel().getSelectedIndex() + 1);
        settingsToUpdate.support_material_interface_nozzleProperty().set(supportInterfaceNozzleChoice.getSelectionModel().getSelectedIndex() + 1);

        //Support tab
        settingsToUpdate.support_materialProperty().set(supportMaterialEnabled.selectedProperty().get());
        settingsToUpdate.support_material_thresholdProperty().set(supportOverhangThreshold.intValueProperty().get());
        settingsToUpdate.support_material_enforce_layersProperty().set(forcedSupportLayers.intValueProperty().get());
        settingsToUpdate.support_material_patternProperty().set(supportPattern.valueProperty().get());
        settingsToUpdate.support_material_spacingProperty().set(supportPatternSpacing.floatValueProperty().get());
        settingsToUpdate.support_material_angleProperty().set(supportPatternAngle.intValueProperty().get());

        //Speed tab
        settingsToUpdate.perimeter_speedProperty().set(perimeterSpeed.intValueProperty().get());
        settingsToUpdate.small_perimeter_speedProperty().set(smallPerimeterSpeed.intValueProperty().get());
        settingsToUpdate.external_perimeter_speedProperty().set(externalPerimeterSpeed.intValueProperty().get());
        settingsToUpdate.infill_speedProperty().set(infillSpeed.intValueProperty().get());
        settingsToUpdate.solid_infill_speedProperty().set(solidInfillSpeed.intValueProperty().get());
        settingsToUpdate.top_solid_infill_speedProperty().set(topSolidInfillSpeed.intValueProperty().get());
        settingsToUpdate.support_material_speedProperty().set(supportMaterialSpeed.intValueProperty().get());
        settingsToUpdate.bridge_speedProperty().set(bridgesSpeed.intValueProperty().get());
        settingsToUpdate.gap_fill_speedProperty().set(gapFillSpeed.intValueProperty().get());

        //Cooling tab
        settingsToUpdate.getCooling().set(enableAutoCooling.selectedProperty().get());
        settingsToUpdate.getMin_fan_speed().set(minFanSpeed.intValueProperty().get());
        settingsToUpdate.getMax_fan_speed().set(maxFanSpeed.intValueProperty().get());
        settingsToUpdate.getBridge_fan_speed().set(bridgesFanSpeed.intValueProperty().get());
        settingsToUpdate.getDisable_fan_first_layers().set(disableFanForFirstNLayers.intValueProperty().get());
        settingsToUpdate.getFan_below_layer_time().set(enableFanIfLayerTimeBelow.intValueProperty().get());
        settingsToUpdate.getSlowdown_below_layer_time().set(slowFanIfLayerTimeBelow.intValueProperty().get());
        settingsToUpdate.getMin_print_speed().set(minPrintSpeed.intValueProperty().get());
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
            masterProfile = settings;

            workingProfile = masterProfile.clone();

            container.setVisible(true);
            bindToNewSettings(workingProfile);
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
        updateSettingsFromGUI(workingProfile);
        return workingProfile;
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
