package celtech.coreUI.controllers.utilityPanels;

import celtech.CoreTest;
import celtech.Lookup;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.SlicerType;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParameters;
import celtech.configuration.slicer.FillPattern;
import celtech.configuration.slicer.SupportPattern;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.RestrictedTextField;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.coreUI.controllers.popups.PopupCommandReceiver;
import celtech.coreUI.controllers.popups.PopupCommandTransmitter;
import celtech.services.slicer.PrintQualityEnumeration;
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
import javafx.util.StringConverter;
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

    @FXML
    private RestrictedNumberField fillDensity;

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

    @FXML
    private ComboBox<SupportPattern> supportPattern;

    @FXML
    private Slider supportExtrusionWidthSlider;

    @FXML
    private RestrictedNumberField bridgesSpeed;

    @FXML
    private RestrictedNumberField layerHeight;

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

    @FXML
    private ComboBox<String> fillNozzleChoice;

    @FXML
    private RestrictedNumberField nozzleEjectionVolume;

    @FXML
    private CheckBox supportMaterialEnabled;

    @FXML
    private RestrictedNumberField numberOfPerimeters;

    @FXML
    private RestrictedNumberField topSolidInfillSpeed;

    @FXML
    private Button deleteProfileButton;

    @FXML
    private Label supportLabel;

    @FXML
    private Slider infillExtrusionWidthSlider;

    @FXML
    private Slider perimeterExtrusionWidthSlider;

    @FXML
    private RestrictedNumberField gapFillSpeed;

    @FXML
    private ComboBox<FillPattern> fillPatternChoice;

    @FXML
    private Label perimeterNozzleChoiceLabel;

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

    @FXML
    private Slider slicerChooser;

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
        SlicerParametersContainer.deleteProfile(masterProfile.getProfileName());
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

    private final ObservableList<String> forceNozzleFirstLayerOptions = FXCollections.observableArrayList();
    private final ObservableList<String> nozzleOptions = FXCollections.observableArrayList(new String("0.3mm"), new String("0.8mm"));
    private final ObservableList<FillPattern> fillPatternOptions = FXCollections.observableArrayList(FillPattern.values());

    private final ObservableList<SupportPattern> supportPatternOptions = FXCollections.observableArrayList(SupportPattern.values());

    private ChangeListener<Toggle> nozzleSelectionListener = null;

    private SlicerParameters masterProfile = null;
    private SlicerParameters workingProfile = null;
    private SlicerParameters lastBoundProfile = null;

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
        slicerChooser.disableProperty().bind(isMutable.not());
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

        slicerChooser.valueProperty().addListener(dirtyNumberListener);

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
            .selectedItemProperty().addListener(new ChangeListener<FillPattern>()
                {

                    @Override
                    public void changed(
                        ObservableValue<? extends FillPattern> observable, FillPattern oldValue, FillPattern newValue)
                    {
                        isDirty.set(true);
                        }
            });

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
            .selectedItemProperty().addListener(new ChangeListener<SupportPattern>()
                {
                    @Override
                    public void changed(
                        ObservableValue<? extends SupportPattern> observable, SupportPattern oldValue, SupportPattern newValue)
                    {
                        isDirty.set(true);
                        }
            });

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

        slicerChooser.setMax(SlicerType.values().length - 1);
        slicerChooser.setMin(0);
        slicerChooser.setValue(Lookup.getUserPreferences().getSlicerType().getEnumPosition());

        slicerChooser.setLabelFormatter(new StringConverter<Double>()
        {
            @Override
            public String toString(Double n)
            {
                SlicerType selectedSlicer = SlicerType.fromEnumPosition(n.intValue());
                return selectedSlicer.name();
            }

            @Override
            public Double fromString(String s)
            {
                SlicerType selectedSlicer = SlicerType.valueOf(s);
                return (double) selectedSlicer.getEnumPosition();
            }
        });

        slicerChooser.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number lastQualityValue, Number newQualityValue)
            {
                if (lastQualityValue != newQualityValue)
                {
                    SlicerType selectedSlicer = SlicerType.fromEnumPosition(newQualityValue.intValue());

                    workingProfile.setSlicerOverride(selectedSlicer);
                }
            }
        });
    }

    private void bindNozzleParameters(int nozzleNumber, SlicerParameters newSettings)
    {
        boundToNozzle = nozzleNumber;

        nozzleOpenVolume.floatValueProperty().set(newSettings.getNozzleParameters().get(nozzleNumber).getOpenOverVolume());
        nozzleEjectionVolume.floatValueProperty().set(newSettings.getNozzleParameters().get(nozzleNumber).getEjectionVolume());
        nozzlePartialOpen.floatValueProperty().set(newSettings.getNozzleParameters().get(nozzleNumber).getPartialBMinimum());
        nozzleOpenVolume.textProperty().addListener(dirtyStringListener);
        nozzleEjectionVolume.textProperty().addListener(dirtyStringListener);
        nozzlePartialOpen.textProperty().addListener(dirtyStringListener);
    }

    private void saveNozzleParametersToWorkingProfile()
    {
        if (boundToNozzle != -1)
        {
            workingProfile.getNozzleParameters().get(boundToNozzle).setOpenOverVolume(nozzleOpenVolume.floatValueProperty().get());
            workingProfile.getNozzleParameters().get(boundToNozzle).setEjectionVolume(nozzleEjectionVolume.floatValueProperty().get());
            workingProfile.getNozzleParameters().get(boundToNozzle).setPartialBMinimum(nozzlePartialOpen.floatValueProperty().get());
        }
    }

    private void unbindNozzleParameters()
    {
        if (boundToNozzle != -1)
        {
            if (lastBoundProfile != null)
            {
                lastBoundProfile.getNozzleParameters().get(boundToNozzle).setOpenOverVolume(nozzleOpenVolume.floatValueProperty().get());
                lastBoundProfile.getNozzleParameters().get(boundToNozzle).setEjectionVolume(nozzleEjectionVolume.floatValueProperty().get());
                lastBoundProfile.getNozzleParameters().get(boundToNozzle).setPartialBMinimum(nozzlePartialOpen.floatValueProperty().get());
            }
            nozzleOpenVolume.textProperty().removeListener(dirtyStringListener);
            nozzleEjectionVolume.textProperty().removeListener(dirtyStringListener);
            nozzlePartialOpen.textProperty().removeListener(dirtyStringListener);
        }

        boundToNozzle = -1;
    }

    private void bindToNewSettings(SlicerParameters newSettings)
    {
        if (lastBoundProfile != null)
        {
            unbindNozzleParameters();
        }

        profileNameField.setText(newSettings.getProfileName());

        if (newSettings.getSlicerOverride() != null)
        {
            slicerChooser.setValue(newSettings.getSlicerOverride().getEnumPosition());
        } else
        {
            slicerChooser.setValue(Lookup.getUserPreferences().getSlicerType().getEnumPosition());
        }

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

    private void updateGUIFromSettings(SlicerParameters newSettings)
    {
        // Extrusion tab
        layerHeight.floatValueProperty().set(newSettings.getLayerHeight_mm());
        fillDensity.floatValueProperty().set(newSettings.getFillDensity_normalised());
        fillPatternChoice.valueProperty().set(newSettings.getFillPattern());
        infillEveryN.intValueProperty().set(newSettings.getFillEveryNLayers());
        solidLayersTop.intValueProperty().set(newSettings.getSolidLayersAtTop());
        solidLayersBottom.intValueProperty().set(newSettings.getSolidLayersAtBottom());
        numberOfPerimeters.intValueProperty().set(newSettings.getNumberOfPerimeters());
        brimWidth.intValueProperty().set(newSettings.getBrimWidth_mm());
//        spiralPrintToggle.selectedProperty().set(newSettings.spiral_vaseProperty().get());

        //Nozzle tab
        firstLayerExtrusionWidth.floatValueProperty().set(newSettings.getFirstLayerExtrusionWidth_mm());
        firstLayerNozzleChoice.getSelectionModel().select(newSettings.getFirstLayerNozzle());

        perimeterExtrusionWidth.floatValueProperty().set(newSettings.getPerimeterExtrusionWidth_mm());
        perimeterNozzleChoice.getSelectionModel().select(newSettings.getPerimeterNozzle());

        infillExtrusionWidth.floatValueProperty().set(newSettings.getFillExtrusionWidth_mm());
        solidInfillExtrusionWidth.floatValueProperty().set(newSettings.getSolidFillExtrusionWidth_mm());
        fillNozzleChoice.getSelectionModel().select(newSettings.getFillNozzle());
        topSolidInfillExtrusionWidth.floatValueProperty().set(newSettings.getTopSolidFillExtrusionWidth_mm());

        supportExtrusionWidth.floatValueProperty().set(newSettings.getSupportExtrusionWidth_mm());
        supportNozzleChoice.getSelectionModel().select(newSettings.getSupportNozzle());

        supportInterfaceNozzleChoice.getSelectionModel().select(newSettings.getSupportInterfaceNozzle());

        //Support tab
        supportMaterialEnabled.selectedProperty().set(newSettings.getGenerateSupportMaterial());
        supportOverhangThreshold.intValueProperty().set(newSettings.getSupportOverhangThreshold_degrees());
        forcedSupportLayers.intValueProperty().set(newSettings.getForcedSupportForFirstNLayers());
        supportPattern.valueProperty().set(newSettings.getSupportPattern());
        supportPatternSpacing.floatValueProperty().set(newSettings.getSupportPatternSpacing_mm());
        supportPatternAngle.intValueProperty().set(newSettings.getSupportPatternAngle_degrees());

        //Speed tab
        perimeterSpeed.intValueProperty().set(newSettings.getPerimeterSpeed_mm_per_s());
        smallPerimeterSpeed.intValueProperty().set(newSettings.getSmallPerimeterSpeed_mm_per_s());
        externalPerimeterSpeed.intValueProperty().set(newSettings.getExternalPerimeterSpeed_mm_per_s());
        infillSpeed.intValueProperty().set(newSettings.getFillSpeed_mm_per_s());
        solidInfillSpeed.intValueProperty().set(newSettings.getSolidFillSpeed_mm_per_s());
        topSolidInfillSpeed.intValueProperty().set(newSettings.getTopSolidFillSpeed_mm_per_s());
        supportMaterialSpeed.intValueProperty().set(newSettings.getSupportSpeed_mm_per_s());
        bridgesSpeed.intValueProperty().set(newSettings.getBridgeSpeed_mm_per_s());
        gapFillSpeed.intValueProperty().set(newSettings.getGapFillSpeed_mm_per_s());

        //Cooling tab
        enableAutoCooling.selectedProperty().set(newSettings.getEnableCooling());
        minFanSpeed.intValueProperty().set(newSettings.getMinFanSpeed_percent());
        maxFanSpeed.intValueProperty().set(newSettings.getMaxFanSpeed_percent());
        bridgesFanSpeed.intValueProperty().set(newSettings.getBridgeFanSpeed_percent());
        disableFanForFirstNLayers.intValueProperty().set(newSettings.getDisableFanFirstNLayers());
        enableFanIfLayerTimeBelow.intValueProperty().set(newSettings.getCoolIfLayerTimeLessThan_secs());
        slowFanIfLayerTimeBelow.intValueProperty().set(newSettings.getSlowDownIfLayerTimeLessThan_secs());
        minPrintSpeed.intValueProperty().set(newSettings.getMinPrintSpeed_mm_per_s());
    }

    private void updateSettingsFromGUI(SlicerParameters settingsToUpdate)
    {
        // Extrusion tab
        settingsToUpdate.setLayerHeight_mm(layerHeight.floatValueProperty().get());
        settingsToUpdate.setFillDensity_normalised(fillDensity.floatValueProperty().get());
        settingsToUpdate.setFillPattern(fillPatternChoice.valueProperty().get());
        settingsToUpdate.setFillEveryNLayers(infillEveryN.intValueProperty().get());
        settingsToUpdate.setSolidLayersAtTop(solidLayersTop.intValueProperty().get());
        settingsToUpdate.setSolidLayersAtBottom(solidLayersBottom.intValueProperty().get());
        settingsToUpdate.setNumberOfPerimeters(numberOfPerimeters.intValueProperty().get());
        settingsToUpdate.setBrimWidth_mm(brimWidth.intValueProperty().get());
//        settingsToUpdate.spiral_vaseProperty().set(spiralPrintToggle.selectedProperty().get());

        //Nozzle tab
        settingsToUpdate.setFirstLayerExtrusionWidth_mm(firstLayerExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setFirstLayerNozzle(firstLayerNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setPerimeterExtrusionWidth_mm(perimeterExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setPerimeterNozzle(perimeterNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setFillExtrusionWidth_mm(infillExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setSolidFillExtrusionWidth_mm(solidInfillExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setFillNozzle(fillNozzleChoice.getSelectionModel().getSelectedIndex());
        settingsToUpdate.setTopSolidFillExtrusionWidth_mm(topSolidInfillExtrusionWidth.floatValueProperty().get());

        settingsToUpdate.setSupportExtrusionWidth_mm(supportExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setSupportNozzle(supportNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setSupportInterfaceNozzle(supportInterfaceNozzleChoice.getSelectionModel().getSelectedIndex());

        //Support tab
        settingsToUpdate.setSupportInterfaceNozzle(supportInterfaceNozzleChoice.getSelectionModel().getSelectedIndex());
        settingsToUpdate.setSupportOverhangThreshold_degrees(supportOverhangThreshold.intValueProperty().get());
        settingsToUpdate.setForcedSupportForFirstNLayers(forcedSupportLayers.intValueProperty().get());
        settingsToUpdate.setSupportPattern(supportPattern.valueProperty().get());
        settingsToUpdate.setSupportPatternSpacing_mm(supportPatternSpacing.floatValueProperty().get());
        settingsToUpdate.setSupportPatternAngle_degrees(supportPatternAngle.intValueProperty().get());

        //Speed tab
        settingsToUpdate.setPerimeterSpeed_mm_per_s(perimeterSpeed.intValueProperty().get());
        settingsToUpdate.setSmallPerimeterSpeed_mm_per_s(smallPerimeterSpeed.intValueProperty().get());
        settingsToUpdate.setExternalPerimeterSpeed_mm_per_s(externalPerimeterSpeed.intValueProperty().get());
        settingsToUpdate.setFillSpeed_mm_per_s(infillSpeed.intValueProperty().get());
        settingsToUpdate.setSolidFillSpeed_mm_per_s(solidInfillSpeed.intValueProperty().get());
        settingsToUpdate.setTopSolidFillSpeed_mm_per_s(topSolidInfillSpeed.intValueProperty().get());
        settingsToUpdate.setSupportSpeed_mm_per_s(supportMaterialSpeed.intValueProperty().get());
        settingsToUpdate.setBridgeSpeed_mm_per_s(bridgesSpeed.intValueProperty().get());
        settingsToUpdate.setGapFillSpeed_mm_per_s(gapFillSpeed.intValueProperty().get());

        //Cooling tab
        settingsToUpdate.setEnableCooling(enableAutoCooling.selectedProperty().get());
        settingsToUpdate.setMinFanSpeed_percent(minFanSpeed.intValueProperty().get());
        settingsToUpdate.setMaxFanSpeed_percent(maxFanSpeed.intValueProperty().get());
        settingsToUpdate.setBridgeFanSpeed_percent(bridgesFanSpeed.intValueProperty().get());
        settingsToUpdate.setDisableFanFirstNLayers(disableFanForFirstNLayers.intValueProperty().get());
        settingsToUpdate.setCoolIfLayerTimeLessThan_secs(enableFanIfLayerTimeBelow.intValueProperty().get());
        settingsToUpdate.setSlowDownIfLayerTimeLessThan_secs(slowFanIfLayerTimeBelow.intValueProperty().get());
        settingsToUpdate.setMinPrintSpeed_mm_per_s(minPrintSpeed.intValueProperty().get());
    }

    /**
     *
     * @param settings
     */
    public void updateProfileData(SlicerParameters settings)
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
            isMutable.set(!SlicerParametersContainer.getApplicationProfileList().contains(settings));
            isDirty.set(false);
        }
    }

    /**
     *
     * @return
     */
    public SlicerParameters getProfileData()
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
            ObservableList<SlicerParameters> existingProfileList = SlicerParametersContainer.getUserProfileList();
            for (SlicerParameters settings : existingProfileList)
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
