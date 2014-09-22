package celtech.coreUI.controllers.utilityPanels;

import celtech.CoreTest;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.RestrictedTextField;
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
import javafx.scene.control.Slider;
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
import jfxtras.styles.jmetro8.ToggleSwitch;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

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
    private int lastNozzleSelected = 0;
    private BooleanProperty nameEditable = new SimpleBooleanProperty(false);

    @FXML
    private VBox container;

//    @FXML
//    private Label nozzleCloseBValueLabel;
    @FXML
    private RestrictedNumberField fillDensity;

//    @FXML
//    private Label retractSpeedLabel;
//    @FXML
//    private RestrictedNumberField nozzleCloseBValue;
//    @FXML
//    private RestrictedNumberField nozzleCloseMidpoint;
//
    @FXML
    private ToggleGroup nozzleChoiceGroup;

    @FXML
    private RestrictedNumberField nozzleOpenVolume;

    @FXML
    private GridPane supportGrid;

    @FXML
    private RestrictedNumberField perimeterExtrusionWidth;

    @FXML
    private Label extrusionWidthLabel;

    @FXML
    private RestrictedNumberField enableFanIfLayerTimeBelow;

    @FXML
    private Label nozzleEjectionVolumeLabel;

    @FXML
    private Label firstLayerExtrusionWidthLabel;

    @FXML
    private HBox nozzleEjectionVolumeHBox;

    @FXML
    private RestrictedNumberField supportOverhangThreshold;

//    @FXML
//    private Label nozzleWipeVolumeLabel;
    @FXML
    private ComboBox<String> supportNozzleChoice;

    @FXML
    private RestrictedNumberField perimeterSpeed;

    @FXML
    private GridPane coolingGrid;

    @FXML
    private Label nozzleOpenVolumeLabel;

    @FXML
    private GridPane speedGrid;

    @FXML
    private RestrictedTextField profileNameField;

    @FXML
    private ComboBox<String> supportInterfaceNozzleChoice;

    @FXML
    private RestrictedNumberField supportMaterialSpeed;

    @FXML
    private RestrictedNumberField brimWidth;

//    @FXML
//    private RestrictedNumberField nozzleWipeVolume;
    @FXML
    private RestrictedNumberField minFanSpeed;

    @FXML
    private RestrictedNumberField solidLayersBottom;

    @FXML
    private RestrictedNumberField forcedSupportLayers;

    @FXML
    private HBox nozzleOpenVolumeHBox;

    @FXML
    private HBox notEditingOptions;

    @FXML
    private RestrictedNumberField solidLayersTop;

    @FXML
    private Label topSolidInfillLabel;

    @FXML
    private ComboBox<String> perimeterNozzleChoice;

    @FXML
    private Label infillLabel;

    @FXML
    private RestrictedNumberField smallPerimeterSpeed;

    @FXML
    private RestrictedNumberField disableFanForFirstNLayers;

    @FXML
    private Slider topSolidInfillExtrusionWidthSlider;

    @FXML
    private HBox nozzlePartialOpenHBox;

//    @FXML
//    private HBox retractLengthHBox;
    @FXML
    private ComboBox<String> supportPattern;

    @FXML
    private Slider supportExtrusionWidthSlider;

    @FXML
    private RestrictedNumberField bridgesSpeed;

    @FXML
    private RestrictedNumberField layerHeight;

//    @FXML
//    private Label nozzleCloseMidpointLabel;
    @FXML
    private GridPane extrusionGrid;

    @FXML
    private GridPane extrusionControls;

    @FXML
    private GridPane nozzleControls;

    @FXML
    private RestrictedNumberField supportPatternAngle;

    @FXML
    private RestrictedNumberField infillExtrusionWidth;

//    @FXML
//    private RestrictedNumberField nozzlePreejectionVolume;
    @FXML
    private RestrictedNumberField slowFanIfLayerTimeBelow;

    @FXML
    private Slider firstLayerExtrusionWidthSlider;

    @FXML
    private Label solidInfillLabel;

    @FXML
    private Label supportInterfaceLabel;

    @FXML
    private ToggleButton nozzle2Button;

    @FXML
    private Button saveAsButton;

    @FXML
    private CheckBox enableAutoCooling;

    @FXML
    private RestrictedNumberField solidInfillSpeed;

    @FXML
    private RestrictedNumberField nozzlePartialOpen;

    @FXML
    private ComboBox<String> firstLayerNozzleChoice;

    @FXML
    private Slider solidInfillExtrusionWidthSlider;

//    @FXML
//    private Label nozzlePreejectionVolumeLabel;
    @FXML
    private ComboBox<String> fillNozzleChoice;

    @FXML
    private RestrictedNumberField nozzleEjectionVolume;

//    @FXML
//    private HBox nozzleCloseBValueHBox;
//
    @FXML
    private CheckBox supportMaterialEnabled;

    @FXML
    private RestrictedNumberField numberOfPerimeters;

//    @FXML
//    private HBox nozzleCloseMidpointHBox;
    @FXML
    private RestrictedNumberField topSolidInfillSpeed;

    @FXML
    private Button deleteProfileButton;

    @FXML
    private Label supportLabel;

    @FXML
    private Slider infillExtrusionWidthSlider;

//    @FXML
//    private HBox retractSpeedHBox;
//
    @FXML
    private Slider perimeterExtrusionWidthSlider;

    @FXML
    private RestrictedNumberField gapFillSpeed;

//    @FXML
//    private RestrictedNumberField retractLength;
    @FXML
    private ComboBox<String> fillPatternChoice;

    @FXML
    private Label perimeterNozzleChoiceLabel;

//    @FXML
//    private HBox nozzlePreejectionVolumeHBox;
    @FXML
    private RestrictedNumberField infillSpeed;

    @FXML
    private Button cancelButton;

    @FXML
    private RestrictedNumberField minPrintSpeed;

    @FXML
    private ToggleButton nozzle1Button;

    @FXML
    private RestrictedNumberField infillEveryN;

    @FXML
    private Button saveButton;

    @FXML
    private RestrictedNumberField solidInfillExtrusionWidth;

    @FXML
    private RestrictedNumberField firstLayerExtrusionWidth;

    @FXML
    private TabPane customTabPane;

    @FXML
    private RestrictedNumberField supportPatternSpacing;

    @FXML
    private RestrictedNumberField maxFanSpeed;

    @FXML
    private HBox editingOptions;

    @FXML
    private RestrictedNumberField topSolidInfillExtrusionWidth;

    @FXML
    private Label nozzlePartialOpenLabel;

//    @FXML
//    private HBox nozzleWipeVolumeHBox;
//    @FXML
//    private Label retractLengthLabel;
//    @FXML
//    private RestrictedNumberField retractSpeed;
    @FXML
    private RestrictedNumberField bridgesFanSpeed;

    @FXML
    private HBox immutableOptions;

    @FXML
    private Label nozzleLabel;

    @FXML
    private RestrictedNumberField supportExtrusionWidth;

    @FXML
    private RestrictedNumberField externalPerimeterSpeed;

//    @FXML
//    private ToggleSwitch spiralPrintToggle;
    @FXML
    void saveData(ActionEvent event)
    {
        updateSettingsFromGUI(workingProfile);
        saveNozzleParametersToWorkingProfile();
        if (commandReceiver != null)
        {
            inhibitAutoExtrusionWidth = true;
            commandReceiver.triggerSave(workingProfile);
            inhibitAutoExtrusionWidth = false;
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

//    @FXML
//    jfxtras.
    private BooleanProperty profileNameInvalid = new SimpleBooleanProperty(false);

    private final Image redcrossImage = new Image(CoreTest.class.getResource(ApplicationConfiguration.imageResourcePath + "redcross.png").toExternalForm());
    private final ImageView redcrossHolder = new ImageView(redcrossImage);

    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private final BooleanProperty isMutable = new SimpleBooleanProperty(false);
    private final BooleanProperty showButtons = new SimpleBooleanProperty(true);

    private final ObservableList<String> forceNozzleFirstLayerOptions = FXCollections.observableArrayList();
    private final ObservableList<String> nozzleOptions = FXCollections.observableArrayList(new String("0.3mm"), new String("0.8mm"));
    private final ObservableList<String> fillPatternOptions = FXCollections.observableArrayList(new String("rectilinear"),
                                                                                                new String("line"),
                                                                                                new String("concentric"),
                                                                                                new String("honeycomb"),
                                                                                                new String("hilbertcurve"),
                                                                                                new String("archimedeanchords"),
                                                                                                new String("octagramspiral"));
    private final ObservableList<String> supportPatternOptions = FXCollections.observableArrayList(new String("rectilinear"),
                                                                                                   new String("rectilinear grid"),
                                                                                                   new String("honeycomb"),
                                                                                                   new String("pillars"));

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

    private final ChangeListener<Number> dirtyNumberListener = (ObservableValue<? extends Number> ov, Number t, Number t1) ->
    {
        isDirty.set(true);
    };

    private PopupCommandReceiver commandReceiver = null;

    private final float minPoint8ExtrusionWidth = 0.5f;
    private final float maxPoint8ExtrusionWidth = 1.2f;
    private final float minPoint3ExtrusionWidth = 0.2f;
    private final float maxPoint3ExtrusionWidth = 0.6f;

    private boolean inhibitAutoExtrusionWidth = false;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        nameEditable.set(false);
        isMutable.set(true);

        applicationStatus = ApplicationStatus.getInstance();
        displayManager = DisplayManager.getInstance();
        settingsScreenState = SettingsScreenState.getInstance();

        editingOptions.visibleProperty().bind(isDirty.and(showButtons).and(isMutable));
        notEditingOptions.visibleProperty().bind(isDirty.not().and(showButtons).and(isMutable));
        immutableOptions.visibleProperty().bind(isDirty.not().and(showButtons).and(isMutable.not()));

        profileNameField.disableProperty().bind(nameEditable.not());
        coolingGrid.disableProperty().bind(isMutable.not());
        extrusionGrid.disableProperty().bind(isMutable.not());
        extrusionControls.disableProperty().bind(isMutable.not());
        nozzleControls.disableProperty().bind(isMutable.not());
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
                        lastNozzleSelected = newNozzle;
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

        forceNozzleFirstLayerOptions.addAll(nozzleOptions);
        firstLayerNozzleChoice.setItems(forceNozzleFirstLayerOptions);
        firstLayerNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                switch (newValue.intValue())
                {
                    case 0:
                        // The point 3 nozzle has been selected
                        if (!inhibitAutoExtrusionWidth)
                        {
                            firstLayerExtrusionWidth.floatValueProperty().set(0.3f);
                        }
                        firstLayerExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                        firstLayerExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                        break;
                    case 1:
                        // The point 8 nozzle has been selected
                        if (!inhibitAutoExtrusionWidth)
                        {
                            firstLayerExtrusionWidth.floatValueProperty().set(0.8f);
                        }
                        firstLayerExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                        firstLayerExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                        break;
                }
            }
        });

        perimeterNozzleChoice.setItems(nozzleOptions);
        perimeterNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                switch (newValue.intValue())
                {
                    case 0:
                        // The point 3 nozzle has been selected
                        if (!inhibitAutoExtrusionWidth)
                        {
                            perimeterExtrusionWidth.floatValueProperty().set(0.3f);
                        }
                        perimeterExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                        perimeterExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                        break;
                    case 1:
                        // The point 8 nozzle has been selected
                        if (!inhibitAutoExtrusionWidth)
                        {
                            perimeterExtrusionWidth.floatValueProperty().set(0.8f);
                        }
                        perimeterExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                        perimeterExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                        break;
                }
            }
        });

        fillNozzleChoice.setItems(nozzleOptions);
        fillNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(
                ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                switch (newValue.intValue())
                {
                    case 0:
                        // The point 3 nozzle has been selected
                        if (!inhibitAutoExtrusionWidth)
                        {
                            infillExtrusionWidth.floatValueProperty().set(0.3f);
                            solidInfillExtrusionWidth.floatValueProperty().set(0.3f);
                            topSolidInfillExtrusionWidth.floatValueProperty().set(0.3f);
                        }
                        infillExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                        infillExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                        solidInfillExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                        solidInfillExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                        topSolidInfillExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                        topSolidInfillExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                        break;
                    case 1:
                        // The point 8 nozzle has been selected
                        if (!inhibitAutoExtrusionWidth)
                        {
                            infillExtrusionWidth.floatValueProperty().set(0.8f);
                            solidInfillExtrusionWidth.floatValueProperty().set(0.8f);
                            topSolidInfillExtrusionWidth.floatValueProperty().set(0.8f);
                        }
                        infillExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                        infillExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                        solidInfillExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                        solidInfillExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                        topSolidInfillExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                        topSolidInfillExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                        break;
                }
            }
        }
        );

        supportNozzleChoice.setItems(nozzleOptions);

        supportNozzleChoice.getSelectionModel()
            .selectedIndexProperty().addListener(new ChangeListener<Number>()
                {
                    @Override
                    public void changed(
                        ObservableValue<? extends Number> observable, Number oldValue, Number newValue
                    )
                    {
                        switch (newValue.intValue())
                        {
                            case 0:
                                // The point 3 nozzle has been selected
                                if (!inhibitAutoExtrusionWidth)
                                {
                                    supportExtrusionWidth.floatValueProperty().set(0.3f);
                                }
                                supportExtrusionWidthSlider.setMin(minPoint3ExtrusionWidth);
                                supportExtrusionWidthSlider.setMax(maxPoint3ExtrusionWidth);
                                break;
                            case 1:
                                // The point 8 nozzle has been selected
                                if (!inhibitAutoExtrusionWidth)
                                {
                                    supportExtrusionWidth.floatValueProperty().set(0.8f);
                                }
                                supportExtrusionWidthSlider.setMin(minPoint8ExtrusionWidth);
                                supportExtrusionWidthSlider.setMax(maxPoint8ExtrusionWidth);
                                break;
                        }
                    }
            }
            );
        supportInterfaceNozzleChoice.setItems(nozzleOptions);

        fillPatternChoice.setItems(fillPatternOptions);

        supportPattern.setItems(supportPatternOptions);

        //Dirty listeners...
        profileNameField.textProperty()
            .addListener(dirtyStringListener);

        //Nozzle Page
        firstLayerExtrusionWidthSlider.valueProperty()
            .bindBidirectional(firstLayerExtrusionWidth.floatValueProperty());
        firstLayerExtrusionWidth.textProperty()
            .addListener(dirtyStringListener);
        firstLayerNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);

        perimeterExtrusionWidthSlider.valueProperty()
            .bindBidirectional(perimeterExtrusionWidth.floatValueProperty());
        perimeterExtrusionWidth.textProperty()
            .addListener(dirtyStringListener);
        perimeterNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);

        infillExtrusionWidthSlider.valueProperty()
            .bindBidirectional(infillExtrusionWidth.floatValueProperty());
        infillExtrusionWidth.textProperty()
            .addListener(dirtyStringListener);
        solidInfillExtrusionWidthSlider.valueProperty()
            .bindBidirectional(solidInfillExtrusionWidth.floatValueProperty());
        solidInfillExtrusionWidth.textProperty()
            .addListener(dirtyStringListener);
        fillNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);
        topSolidInfillExtrusionWidthSlider.valueProperty()
            .bindBidirectional(topSolidInfillExtrusionWidth.floatValueProperty());
        topSolidInfillExtrusionWidth.textProperty()
            .addListener(dirtyStringListener);

        supportExtrusionWidthSlider.valueProperty()
            .bindBidirectional(supportExtrusionWidth.floatValueProperty());
        supportExtrusionWidth.textProperty()
            .addListener(dirtyStringListener);
        supportNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);

        supportInterfaceNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);

        fillDensity.textProperty()
            .addListener(dirtyStringListener);
        slowFanIfLayerTimeBelow.textProperty()
            .addListener(dirtyStringListener);
        enableFanIfLayerTimeBelow.textProperty()
            .addListener(dirtyStringListener);
        solidInfillSpeed.textProperty()
            .addListener(dirtyStringListener);
        supportMaterialEnabled.textProperty()
            .addListener(dirtyStringListener);
        supportOverhangThreshold.textProperty()
            .addListener(dirtyStringListener);
        solidLayersTop.textProperty()
            .addListener(dirtyStringListener);
        solidLayersBottom.textProperty()
            .addListener(dirtyStringListener);
        numberOfPerimeters.textProperty()
            .addListener(dirtyStringListener);
        topSolidInfillSpeed.textProperty()
            .addListener(dirtyStringListener);
        perimeterSpeed.textProperty()
            .addListener(dirtyStringListener);
        gapFillSpeed.textProperty()
            .addListener(dirtyStringListener);
        fillPatternChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);
        supportMaterialSpeed.textProperty()
            .addListener(dirtyStringListener);
        brimWidth.textProperty()
            .addListener(dirtyStringListener);
        infillSpeed.textProperty()
            .addListener(dirtyStringListener);
        minPrintSpeed.textProperty()
            .addListener(dirtyStringListener);
        minFanSpeed.textProperty()
            .addListener(dirtyStringListener);
        infillEveryN.textProperty()
            .addListener(dirtyStringListener);
        forcedSupportLayers.textProperty()
            .addListener(dirtyStringListener);
        supportPatternSpacing.textProperty()
            .addListener(dirtyStringListener);
        smallPerimeterSpeed.textProperty()
            .addListener(dirtyStringListener);
        maxFanSpeed.textProperty()
            .addListener(dirtyStringListener);
        disableFanForFirstNLayers.textProperty()
            .addListener(dirtyStringListener);
        bridgesFanSpeed.textProperty()
            .addListener(dirtyStringListener);
        bridgesSpeed.textProperty()
            .addListener(dirtyStringListener);
        layerHeight.textProperty()
            .addListener(dirtyStringListener);
        externalPerimeterSpeed.textProperty()
            .addListener(dirtyStringListener);
        supportPatternAngle.textProperty()
            .addListener(dirtyStringListener);

        supportMaterialEnabled.selectedProperty()
            .addListener(dirtyBooleanListener);
        enableAutoCooling.selectedProperty()
            .addListener(dirtyBooleanListener);

        perimeterNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);
        fillNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);
        supportNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);
        supportInterfaceNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);
        supportPattern.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);

//        spiralPrintToggle.selectedProperty().addListener(dirtyBooleanListener);
        isDirty.addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
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
            }
        );
    }

    private void bindNozzleParameters(int nozzleNumber, RoboxProfile newSettings)
    {
        boundToNozzle = nozzleNumber;

        nozzleOpenVolume.floatValueProperty().set(newSettings.getNozzle_open_over_volume().get(nozzleNumber).get());

//        nozzlePreejectionVolume.floatValueProperty().set(newSettings.getNozzle_preejection_volume().get(nozzleNumber).get());
        nozzleEjectionVolume.floatValueProperty().set(newSettings.getNozzle_ejection_volume().get(nozzleNumber).get());

//        nozzleCloseBValue.floatValueProperty().set(newSettings.getNozzle_close_at_midpoint().get(nozzleNumber).get());
//        nozzleCloseMidpoint.floatValueProperty().set(newSettings.getNozzle_close_midpoint_percent().get(nozzleNumber).get());
//        nozzleWipeVolume.floatValueProperty().set(newSettings.getNozzle_wipe_volume().get(nozzleNumber).get());
        nozzlePartialOpen.floatValueProperty().set(newSettings.getNozzle_partial_b_minimum().get(nozzleNumber).get());
//        retractLength.floatValueProperty().set(newSettings.retract_lengthProperty().get(nozzleNumber).get());
//        retractSpeed.intValueProperty().set(newSettings.retract_speedProperty().get(nozzleNumber).get());

        nozzleOpenVolume.textProperty().addListener(dirtyStringListener);

//        nozzlePreejectionVolume.textProperty().addListener(dirtyStringListener);
        nozzleEjectionVolume.textProperty().addListener(dirtyStringListener);

//        nozzleCloseMidpoint.textProperty().addListener(dirtyStringListener);
//        nozzleCloseBValue.textProperty().addListener(dirtyStringListener);
//        nozzleWipeVolume.textProperty().addListener(dirtyStringListener);
        nozzlePartialOpen.textProperty().addListener(dirtyStringListener);
//        retractLength.textProperty().addListener(dirtyStringListener);
//        retractSpeed.textProperty().addListener(dirtyStringListener);
    }

    private void saveNozzleParametersToWorkingProfile()
    {
        if (boundToNozzle != -1)
        {
            workingProfile.getNozzle_open_over_volume().get(boundToNozzle).set(nozzleOpenVolume.floatValueProperty().get());
//            workingProfile.getNozzle_preejection_volume().get(boundToNozzle).set(nozzlePreejectionVolume.floatValueProperty().get());
            workingProfile.getNozzle_ejection_volume().get(boundToNozzle).set(nozzleEjectionVolume.floatValueProperty().get());
//            workingProfile.getNozzle_close_at_midpoint().get(boundToNozzle).set(nozzleCloseBValue.floatValueProperty().get());
//            workingProfile.getNozzle_close_midpoint_percent().get(boundToNozzle).set(nozzleCloseMidpoint.floatValueProperty().get());
//            workingProfile.getNozzle_wipe_volume().get(boundToNozzle).set(nozzleWipeVolume.floatValueProperty().get());
            workingProfile.getNozzle_partial_b_minimum().get(boundToNozzle).set(nozzlePartialOpen.floatValueProperty().get());
//            workingProfile.retract_lengthProperty().get(boundToNozzle).set(retractLength.floatValueProperty().get());
//            workingProfile.retract_speedProperty().get(boundToNozzle).set(retractSpeed.intValueProperty().get());
        }
    }

    private void unbindNozzleParameters()
    {
        if (boundToNozzle != -1)
        {
            if (lastBoundProfile != null)
            {
                lastBoundProfile.getNozzle_open_over_volume().get(boundToNozzle).set(nozzleOpenVolume.floatValueProperty().get());
//                lastBoundProfile.getNozzle_preejection_volume().get(boundToNozzle).set(nozzlePreejectionVolume.floatValueProperty().get());
                lastBoundProfile.getNozzle_ejection_volume().get(boundToNozzle).set(nozzleEjectionVolume.floatValueProperty().get());
//                lastBoundProfile.getNozzle_close_at_midpoint().get(boundToNozzle).set(nozzleCloseBValue.floatValueProperty().get());
//                lastBoundProfile.getNozzle_close_midpoint_percent().get(boundToNozzle).set(nozzleCloseMidpoint.floatValueProperty().get());
//                lastBoundProfile.getNozzle_wipe_volume().get(boundToNozzle).set(nozzleWipeVolume.floatValueProperty().get());
                lastBoundProfile.getNozzle_partial_b_minimum().get(boundToNozzle).set(nozzlePartialOpen.floatValueProperty().get());
//                lastBoundProfile.retract_lengthProperty().get(boundToNozzle).set(retractLength.floatValueProperty().get());
//                lastBoundProfile.retract_speedProperty().get(boundToNozzle).set(retractSpeed.intValueProperty().get());
            }
            nozzleOpenVolume.textProperty().removeListener(dirtyStringListener);
//            nozzlePreejectionVolume.textProperty().removeListener(dirtyStringListener);
            nozzleEjectionVolume.textProperty().removeListener(dirtyStringListener);
//            nozzleCloseBValue.textProperty().removeListener(dirtyStringListener);
//            nozzleCloseMidpoint.textProperty().removeListener(dirtyStringListener);
//            nozzleWipeVolume.textProperty().removeListener(dirtyStringListener);
            nozzlePartialOpen.textProperty().removeListener(dirtyStringListener);
//            retractLength.textProperty().removeListener(dirtyStringListener);
//            retractSpeed.textProperty().removeListener(dirtyStringListener);
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

        inhibitAutoExtrusionWidth = true;
        updateGUIFromSettings(newSettings);
        inhibitAutoExtrusionWidth = false;

        if (lastNozzleSelected == 0)
        {
            nozzle1Button.setSelected(true);
        } else if (lastNozzleSelected == 1)
        {
            nozzle2Button.setSelected(true);
        }

        bindNozzleParameters(lastNozzleSelected, newSettings);

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
//        spiralPrintToggle.selectedProperty().set(newSettings.spiral_vaseProperty().get());

        //Nozzle tab
        firstLayerExtrusionWidth.floatValueProperty().set(newSettings.getFirst_layer_extrusion_width().get());
        firstLayerNozzleChoice.getSelectionModel().select(newSettings.getForce_nozzle_on_first_layer().get());

        perimeterExtrusionWidth.floatValueProperty().set(newSettings.getPerimeter_extrusion_width().get());
        perimeterNozzleChoice.getSelectionModel().select(newSettings.getPerimeterNozzleProperty().get());

        infillExtrusionWidth.floatValueProperty().set(newSettings.getInfill_extrusion_width().get());
        solidInfillExtrusionWidth.floatValueProperty().set(newSettings.getSolid_infill_extrusion_width().get());
        fillNozzleChoice.getSelectionModel().select(newSettings.getFillNozzleProperty().get());
        topSolidInfillExtrusionWidth.floatValueProperty().set(newSettings.getTop_infill_extrusion_width().get());

        supportExtrusionWidth.floatValueProperty().set(newSettings.getSupport_material_extrusion_width().get());
        supportNozzleChoice.getSelectionModel().select(newSettings.getSupportNozzleProperty().get());

        supportInterfaceNozzleChoice.getSelectionModel().select(newSettings.getSupportInterfaceNozzleProperty().get());

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
//        settingsToUpdate.spiral_vaseProperty().set(spiralPrintToggle.selectedProperty().get());

        //Nozzle tab
        settingsToUpdate.getFirst_layer_extrusion_width().set(firstLayerExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.getForce_nozzle_on_first_layer().set(firstLayerNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.getPerimeter_extrusion_width().set(perimeterExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.getPerimeterNozzleProperty().set(perimeterNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.getInfill_extrusion_width().set(infillExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.getSolid_infill_extrusion_width().set(solidInfillExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.getFillNozzleProperty().set(fillNozzleChoice.getSelectionModel().getSelectedIndex());
        settingsToUpdate.getTop_infill_extrusion_width().set(topSolidInfillExtrusionWidth.floatValueProperty().get());

        settingsToUpdate.getSupport_material_extrusion_width().set(supportExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.getSupportNozzleProperty().set(supportNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.getSupportInterfaceNozzleProperty().set(supportInterfaceNozzleChoice.getSelectionModel().getSelectedIndex());

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

    public void setNameEditable(boolean editable)
    {
        nameEditable.set(editable);
    }
}

