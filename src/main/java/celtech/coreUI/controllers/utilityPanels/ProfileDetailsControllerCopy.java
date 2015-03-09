package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.configuration.CustomSlicerType;
import celtech.configuration.SlicerType;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerMappings;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.FillPattern;
import celtech.configuration.slicer.SupportPattern;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.components.RestrictedTextField;
import celtech.coreUI.controllers.panels.ExtrasMenuInnerPanel;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class ProfileDetailsControllerCopy implements Initializable, ExtrasMenuInnerPanel
{

    enum State
    {
        /**
         * Editing a new profile that has not yet been saved
         */
        NEW,
        /**
         * Editing a custom profile
         */
        CUSTOM,
        /**
         * Viewing a standard profile
         */
        ROBOX
    };

    private final ObjectProperty<ProfileDetailsControllerCopy.State> state = new SimpleObjectProperty<>();
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    private final BooleanProperty isEditable = new SimpleBooleanProperty(false);
    private final BooleanProperty canSave = new SimpleBooleanProperty(false);
    private final BooleanProperty canDelete = new SimpleBooleanProperty(false);
    private String currentProfileName;

    private Stenographer steno = StenographerFactory.getStenographer(
        ProfileDetailsController.class.getName());
    private int lastNozzleSelected = 0;
    private final BooleanProperty nameEditable = new SimpleBooleanProperty(false);

    @FXML
    private VBox container;
    
    @FXML
    private ComboBox<SlicerParametersFile> cmbPrintProfile;

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
    private RestrictedNumberField enableFanIfLayerTimeBelow;

    @FXML
    private RestrictedNumberField supportOverhangThreshold;

    @FXML
    private ComboBox<String> supportNozzleChoice;

    @FXML
    private RestrictedNumberField firstLayerSpeed;

    @FXML
    private RestrictedNumberField perimeterSpeed;

    @FXML
    private GridPane coolingGrid;

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
    private RestrictedNumberField solidLayersTop;

    @FXML
    private ComboBox<String> perimeterNozzleChoice;

    @FXML
    private RestrictedNumberField smallPerimeterSpeed;

    @FXML
    private RestrictedNumberField disableFanForFirstNLayers;

    @FXML
    private Slider topSolidInfillExtrusionWidthSlider;

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
    private ToggleButton nozzle2Button;

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
    private Slider infillExtrusionWidthSlider;

    @FXML
    private Slider perimeterExtrusionWidthSlider;

    @FXML
    private RestrictedNumberField gapFillSpeed;

    @FXML
    private ComboBox<FillPattern> fillPatternChoice;

    @FXML
    private RestrictedNumberField infillSpeed;

    @FXML
    private RestrictedNumberField minPrintSpeed;

    @FXML
    private ToggleButton nozzle1Button;

    @FXML
    private RestrictedNumberField infillEveryN;

    @FXML
    private RestrictedNumberField solidInfillExtrusionWidth;

    @FXML
    private RestrictedNumberField firstLayerExtrusionWidth;

    @FXML
    private RestrictedNumberField supportPatternSpacing;

    @FXML
    private RestrictedNumberField maxFanSpeed;

    @FXML
    private RestrictedNumberField topSolidInfillExtrusionWidth;

    @FXML
    private RestrictedNumberField bridgesFanSpeed;

    @FXML
    private RestrictedNumberField supportExtrusionWidth;

    @FXML
    private RestrictedNumberField externalPerimeterSpeed;

    @FXML
    private ComboBox<CustomSlicerType> slicerChooser;

    public ProfileDetailsControllerCopy()
    {
    }
    
//    @FXML
//    private ToggleSwitch spiralPrintToggle;
    @FXML
    void saveData(ActionEvent event)
    {
        updateSettingsFromGUI(workingProfile);
        saveNozzleParametersToWorkingProfile();
            inhibitAutoExtrusionWidth = true;
//            commandReceiver.triggerSave(workingProfile);
            inhibitAutoExtrusionWidth = false;
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

    private BooleanProperty profileNameInvalid = new SimpleBooleanProperty(false);

//    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private final BooleanProperty isMutable = new SimpleBooleanProperty(true);
    private final BooleanProperty showButtons = new SimpleBooleanProperty(true);

    private final ObservableList<String> forceNozzleFirstLayerOptions = FXCollections.observableArrayList();
    private final ObservableList<String> nozzleOptions = FXCollections.observableArrayList(
        "0.3mm", "0.8mm");
    private final ObservableList<FillPattern> fillPatternOptions = FXCollections.observableArrayList(
        FillPattern.values());

    private final ObservableList<SupportPattern> supportPatternOptions = FXCollections.observableArrayList(
        SupportPattern.values());

    private ChangeListener<Toggle> nozzleSelectionListener = null;

    private SlicerParametersFile masterProfile = null;
    private SlicerParametersFile workingProfile = null;
    private SlicerParametersFile lastBoundProfile = null;

    private int boundToNozzle = -1;

    private final ChangeListener<String> dirtyStringListener = (ObservableValue<? extends String> ov, String t, String t1) ->
    {
        isDirty.set(true);
    };

    private final ChangeListener<Boolean> dirtyBooleanListener = (ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1) ->
    {
        isDirty.set(true);
    };

    private final float minPoint8ExtrusionWidth = 0.5f;
    private final float maxPoint8ExtrusionWidth = 1.2f;
    private final float minPoint3ExtrusionWidth = 0.2f;
    private final float maxPoint3ExtrusionWidth = 0.6f;

    private boolean inhibitAutoExtrusionWidth = false;

    private SlicerMappings slicerMappings;
    private SlicerType currentSlicerType = SlicerType.Cura;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        slicerMappings = Lookup.getSlicerMappings();
        
        canSave.bind(isDirty.and(
            state.isEqualTo(State.NEW).
            or(state.isEqualTo(State.CUSTOM))));

        canDelete.bind(state.isNotEqualTo(State.ROBOX));

        isEditable.bind(state.isNotEqualTo(State.ROBOX));


        setupWidgetChangeListeners();

        setupPrintProfileCombo();

        selectFirstPrintProfile();        
        
        setupWidgetEditableBindings();

        setupNozzle12Buttons();

        setupFirstLayerNozzleChoice();

        setupPerimeterNozzleChoice();

        setupFillNozzleChoice();

        setupSupportNozzleChoice();
        
        setupSlicerChooser();
        
        nozzleChoiceGroup.selectedToggleProperty().addListener(nozzleSelectionListener);

        forceNozzleFirstLayerOptions.addAll(nozzleOptions);
        
        supportInterfaceNozzleChoice.setItems(nozzleOptions);

        fillPatternChoice.setItems(fillPatternOptions);

        supportPattern.setItems(supportPatternOptions);


    }
    
    private void setupPrintProfileCombo()
    {
        cmbPrintProfile.setCellFactory(new Callback<ListView<SlicerParametersFile>, ListCell<SlicerParametersFile>>() {
            @Override public ListCell<SlicerParametersFile> call(ListView<SlicerParametersFile> p) {
                return new ListCell<SlicerParametersFile>() {
                    
                    @Override protected void updateItem(SlicerParametersFile item, boolean empty) {
                        super.updateItem(item, empty);
                        
                        if (item == null || empty) {
                            setGraphic(null);
                        } else {
                            setText(item.getProfileName());
                        }
                   }
              };
          }
       });

        repopulateCmbPrintProfile();

        cmbPrintProfile.valueProperty().addListener(
            (ObservableValue<? extends SlicerParametersFile> observable, SlicerParametersFile oldValue, SlicerParametersFile newValue) ->
            {
                selectPrintProfile(newValue);
            });
    }
    
    private void selectFirstPrintProfile()
    {
        cmbPrintProfile.setValue(cmbPrintProfile.getItems().get(0));
    }    
    
    private void selectPrintProfile(SlicerParametersFile printProfile)
    {
        currentProfileName = printProfile.getProfileName();
        updateWidgets(printProfile);
        if (currentProfileName.startsWith("U"))
        {
            state.set(State.CUSTOM);
        } else
        {
            state.set(State.ROBOX);
        }
    }    

    private void repopulateCmbPrintProfile()
    {
        try
        {
//            allFilaments.addAll(FilamentContainer.getUserFilamentList().sorted(
//                (Filament o1, Filament o2)
//                -> o1.getFriendlyFilamentName().compareTo(o2.getFriendlyFilamentName())));
            cmbPrintProfile.setItems(SlicerParametersContainer.getCompleteProfileList());
        } catch (NoClassDefFoundError exception)
        {
            // this should only happen in SceneBuilder            
        }
    }
    

    private void setupSlicerChooser()
    {
        slicerChooser.setItems(FXCollections.observableArrayList(CustomSlicerType.values()));
        
        slicerChooser.valueProperty().addListener(new ChangeListener<CustomSlicerType>()
        {
            @Override
            public void changed(ObservableValue<? extends CustomSlicerType> ov,
                CustomSlicerType lastSlicer, CustomSlicerType newSlicer)
            {
                if (lastSlicer != newSlicer)
                {
                    workingProfile.setSlicerOverride(newSlicer.getSlicerType());
                    updateFieldDisabledState(workingProfile);
                }
            }
        });
    }

    private void setupNozzle12Buttons()
    {
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
                if (nozzle1Button.equals(
                    nozzleChoiceGroup.getSelectedToggle()))
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
                if (nozzle2Button.equals(
                    nozzleChoiceGroup.getSelectedToggle()))
                {
                    mouseEvent.consume();
                }
            }
        });
    }

    private void setupFirstLayerNozzleChoice()
    {
        firstLayerNozzleChoice.setItems(forceNozzleFirstLayerOptions);
        firstLayerNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(
            new ChangeListener<Number>()
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
    }

    private void setupSupportNozzleChoice()
    {
        supportNozzleChoice.setItems(nozzleOptions);
        
        supportNozzleChoice.getSelectionModel()
            .selectedIndexProperty().addListener(new ChangeListener<Number>()
            {
                @Override
                public void changed(
                    ObservableValue<? extends Number> observable, Number oldValue,
                    Number newValue
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
    }

    private void setupFillNozzleChoice()
    {
        fillNozzleChoice.setItems(nozzleOptions);
        fillNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(
            new ChangeListener<Number>()
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
    }

    private void setupPerimeterNozzleChoice()
    {
        perimeterNozzleChoice.setItems(nozzleOptions);
        perimeterNozzleChoice.getSelectionModel().selectedIndexProperty().addListener(
            new ChangeListener<Number>()
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
    }

    private void setupWidgetEditableBindings()
    {
        profileNameField.disableProperty().bind(nameEditable.not());
        slicerChooser.disableProperty().bind(isMutable.not());
        coolingGrid.disableProperty().bind(isMutable.not());
        extrusionGrid.disableProperty().bind(isMutable.not());
        extrusionControls.disableProperty().bind(isMutable.not());
        nozzleControls.disableProperty().bind(isMutable.not());
        supportGrid.disableProperty().bind(isMutable.not());
        speedGrid.disableProperty().bind(isMutable.not());
    }

    private void setupWidgetChangeListeners()
    {
        //Dirty listeners...
        profileNameField.textProperty()
            .addListener(dirtyStringListener);

        slicerChooser.valueProperty().addListener(new ChangeListener<CustomSlicerType>()
        {
            @Override
            public void changed(
                ObservableValue<? extends CustomSlicerType> observable, CustomSlicerType oldValue,
                CustomSlicerType newValue)
            {
                isDirty.set(true);
            }
        });

        //Nozzle Page
        firstLayerExtrusionWidthSlider.valueProperty()
            .bindBidirectional(firstLayerExtrusionWidth.floatValueProperty());
        firstLayerExtrusionWidth.textProperty().addListener(dirtyStringListener);
        firstLayerNozzleChoice.getSelectionModel().selectedItemProperty().addListener(
            dirtyStringListener);

        perimeterExtrusionWidthSlider.valueProperty().bindBidirectional(
            perimeterExtrusionWidth.floatValueProperty());
        perimeterExtrusionWidth.textProperty().addListener(dirtyStringListener);
        perimeterNozzleChoice.getSelectionModel().selectedItemProperty().addListener(
            dirtyStringListener);

        infillExtrusionWidthSlider.valueProperty().bindBidirectional(
            infillExtrusionWidth.floatValueProperty());
        infillExtrusionWidth.textProperty().addListener(dirtyStringListener);
        solidInfillExtrusionWidthSlider.valueProperty().bindBidirectional(
            solidInfillExtrusionWidth.floatValueProperty());
        solidInfillExtrusionWidth.textProperty().addListener(dirtyStringListener);
        fillNozzleChoice.getSelectionModel().selectedItemProperty().addListener(dirtyStringListener);
        topSolidInfillExtrusionWidthSlider.valueProperty().bindBidirectional(
            topSolidInfillExtrusionWidth.floatValueProperty());
        topSolidInfillExtrusionWidth.textProperty().addListener(dirtyStringListener);

        supportExtrusionWidthSlider.valueProperty()
            .bindBidirectional(supportExtrusionWidth.floatValueProperty());
        supportExtrusionWidth.textProperty()
            .addListener(dirtyStringListener);
        supportNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);

        supportInterfaceNozzleChoice.getSelectionModel()
            .selectedItemProperty().addListener(dirtyStringListener);

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
        firstLayerSpeed.textProperty().addListener(dirtyStringListener);
        perimeterSpeed.textProperty().addListener(dirtyStringListener);
        gapFillSpeed.textProperty().addListener(dirtyStringListener);
        fillPatternChoice.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<FillPattern>()
            {

                @Override
                public void changed(
                    ObservableValue<? extends FillPattern> observable, FillPattern oldValue,
                    FillPattern newValue)
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
                    ObservableValue<? extends SupportPattern> observable,
                    SupportPattern oldValue, SupportPattern newValue)
                {
                    isDirty.set(true);
                }
            });
    }

    private void bindNozzleParameters(int nozzleNumber, SlicerParametersFile newSettings)
    {
        boundToNozzle = nozzleNumber;

        nozzleOpenVolume.floatValueProperty().set(
            newSettings.getNozzleParameters().get(nozzleNumber).getOpenOverVolume());
        nozzleEjectionVolume.floatValueProperty().set(newSettings.getNozzleParameters().get(
            nozzleNumber).getEjectionVolume());
        nozzlePartialOpen.floatValueProperty().set(newSettings.getNozzleParameters().get(
            nozzleNumber).getPartialBMinimum());
        nozzleOpenVolume.textProperty().addListener(dirtyStringListener);
        nozzleEjectionVolume.textProperty().addListener(dirtyStringListener);
        nozzlePartialOpen.textProperty().addListener(dirtyStringListener);
    }

    private void saveNozzleParametersToWorkingProfile()
    {
        if (boundToNozzle != -1)
        {
            workingProfile.getNozzleParameters().get(boundToNozzle).setOpenOverVolume(
                nozzleOpenVolume.floatValueProperty().get());
            workingProfile.getNozzleParameters().get(boundToNozzle).setEjectionVolume(
                nozzleEjectionVolume.floatValueProperty().get());
            workingProfile.getNozzleParameters().get(boundToNozzle).setPartialBMinimum(
                nozzlePartialOpen.floatValueProperty().get());
        }
    }

    private void unbindNozzleParameters()
    {
        if (boundToNozzle != -1)
        {
            if (lastBoundProfile != null)
            {
                lastBoundProfile.getNozzleParameters().get(boundToNozzle).setOpenOverVolume(
                    nozzleOpenVolume.floatValueProperty().get());
                lastBoundProfile.getNozzleParameters().get(boundToNozzle).setEjectionVolume(
                    nozzleEjectionVolume.floatValueProperty().get());
                lastBoundProfile.getNozzleParameters().get(boundToNozzle).setPartialBMinimum(
                    nozzlePartialOpen.floatValueProperty().get());
            }
            nozzleOpenVolume.textProperty().removeListener(dirtyStringListener);
            nozzleEjectionVolume.textProperty().removeListener(dirtyStringListener);
            nozzlePartialOpen.textProperty().removeListener(dirtyStringListener);
        }

        boundToNozzle = -1;
    }

    private void bindToNewSettings(SlicerParametersFile newSettings)
    {
        if (lastBoundProfile != null)
        {
            unbindNozzleParameters();
        }

        profileNameField.setText(newSettings.getProfileName());

        if (newSettings.getSlicerOverride() != null)
        {
            slicerChooser.setValue(CustomSlicerType.customTypefromSettings(
                newSettings.getSlicerOverride()));
        } else
        {
            slicerChooser.setValue(CustomSlicerType.Default);
        }

        inhibitAutoExtrusionWidth = true;
        updateWidgets(newSettings);
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

    private void updateWidgets(SlicerParametersFile parametersFile)
    {
        // Extrusion tab
        layerHeight.floatValueProperty().set(parametersFile.getLayerHeight_mm());
        fillDensity.floatValueProperty().set(parametersFile.getFillDensity_normalised());
        fillPatternChoice.valueProperty().set(parametersFile.getFillPattern());
        infillEveryN.intValueProperty().set(parametersFile.getFillEveryNLayers());
        solidLayersTop.intValueProperty().set(parametersFile.getSolidLayersAtTop());
        solidLayersBottom.intValueProperty().set(parametersFile.getSolidLayersAtBottom());
        numberOfPerimeters.intValueProperty().set(parametersFile.getNumberOfPerimeters());
        brimWidth.intValueProperty().set(parametersFile.getBrimWidth_mm());
//        spiralPrintToggle.selectedProperty().set(newSettings.spiral_vaseProperty().get());

        //Nozzle tab
        firstLayerExtrusionWidth.floatValueProperty().set(
            parametersFile.getFirstLayerExtrusionWidth_mm());
        firstLayerNozzleChoice.getSelectionModel().select(parametersFile.getFirstLayerNozzle());

        perimeterExtrusionWidth.floatValueProperty().set(parametersFile.getPerimeterExtrusionWidth_mm());
        perimeterNozzleChoice.getSelectionModel().select(parametersFile.getPerimeterNozzle());

        infillExtrusionWidth.floatValueProperty().set(parametersFile.getFillExtrusionWidth_mm());
        solidInfillExtrusionWidth.floatValueProperty().set(
            parametersFile.getSolidFillExtrusionWidth_mm());
        fillNozzleChoice.getSelectionModel().select(parametersFile.getFillNozzle());
        topSolidInfillExtrusionWidth.floatValueProperty().set(
            parametersFile.getTopSolidFillExtrusionWidth_mm());

        supportExtrusionWidth.floatValueProperty().set(parametersFile.getSupportExtrusionWidth_mm());
        supportNozzleChoice.getSelectionModel().select(parametersFile.getSupportNozzle());

        supportInterfaceNozzleChoice.getSelectionModel().select(
            parametersFile.getSupportInterfaceNozzle());

        //Support tab
        supportMaterialEnabled.selectedProperty().set(parametersFile.getGenerateSupportMaterial());
        supportOverhangThreshold.intValueProperty().set(
            parametersFile.getSupportOverhangThreshold_degrees());
        forcedSupportLayers.intValueProperty().set(parametersFile.getForcedSupportForFirstNLayers());
        supportPattern.valueProperty().set(parametersFile.getSupportPattern());
        supportPatternSpacing.floatValueProperty().set(parametersFile.getSupportPatternSpacing_mm());
        supportPatternAngle.intValueProperty().set(parametersFile.getSupportPatternAngle_degrees());

        //Speed tab
        firstLayerSpeed.intValueProperty().set(parametersFile.getFirstLayerSpeed_mm_per_s());
        perimeterSpeed.intValueProperty().set(parametersFile.getPerimeterSpeed_mm_per_s());
        smallPerimeterSpeed.intValueProperty().set(parametersFile.getSmallPerimeterSpeed_mm_per_s());
        externalPerimeterSpeed.intValueProperty().set(
            parametersFile.getExternalPerimeterSpeed_mm_per_s());
        infillSpeed.intValueProperty().set(parametersFile.getFillSpeed_mm_per_s());
        solidInfillSpeed.intValueProperty().set(parametersFile.getSolidFillSpeed_mm_per_s());
        topSolidInfillSpeed.intValueProperty().set(parametersFile.getTopSolidFillSpeed_mm_per_s());
        supportMaterialSpeed.intValueProperty().set(parametersFile.getSupportSpeed_mm_per_s());
        bridgesSpeed.intValueProperty().set(parametersFile.getBridgeSpeed_mm_per_s());
        gapFillSpeed.intValueProperty().set(parametersFile.getGapFillSpeed_mm_per_s());

        //Cooling tab
        enableAutoCooling.selectedProperty().set(parametersFile.getEnableCooling());
        minFanSpeed.intValueProperty().set(parametersFile.getMinFanSpeed_percent());
        maxFanSpeed.intValueProperty().set(parametersFile.getMaxFanSpeed_percent());
        bridgesFanSpeed.intValueProperty().set(parametersFile.getBridgeFanSpeed_percent());
        disableFanForFirstNLayers.intValueProperty().set(parametersFile.getDisableFanFirstNLayers());
        enableFanIfLayerTimeBelow.intValueProperty().set(
            parametersFile.getCoolIfLayerTimeLessThan_secs());
        slowFanIfLayerTimeBelow.intValueProperty().set(
            parametersFile.getSlowDownIfLayerTimeLessThan_secs());
        minPrintSpeed.intValueProperty().set(parametersFile.getMinPrintSpeed_mm_per_s());

        updateFieldDisabledState(parametersFile);
    }

    private void updateFieldDisabledState(SlicerParametersFile settings)
    {
        SlicerType slicerType;

        if (settings.getSlicerOverride() == null)
        {
            slicerType = Lookup.getUserPreferences().getSlicerType();
        } else
        {
            slicerType = settings.getSlicerOverride();
        }

        layerHeight.setDisable(!slicerMappings.isMapped(slicerType, "layerHeight_mm"));
        fillDensity.setDisable(!slicerMappings.isMapped(slicerType, "fillDensity_normalised"));
        infillEveryN.setDisable(!slicerMappings.isMapped(slicerType, "fillEveryNLayers"));
        solidLayersTop.setDisable(!slicerMappings.isMapped(slicerType, "solidLayersAtTop"));
        solidLayersBottom.setDisable(!slicerMappings.isMapped(slicerType, "solidLayersAtBottom"));
        numberOfPerimeters.setDisable(!slicerMappings.isMapped(slicerType, "numberOfPerimeters"));
        brimWidth.setDisable(!slicerMappings.isMapped(slicerType, "brimWidth_mm"));

        //Nozzle tab
        firstLayerExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                                                                     "firstLayerExtrusionWidth_mm"));
        perimeterExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                                                                    "perimeterExtrusionWidth_mm"));
        infillExtrusionWidth.setDisable(
            !slicerMappings.isMapped(slicerType, "fillExtrusionWidth_mm"));
        solidInfillExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                                                                      "solidFillExtrusionWidth_mm"));
        topSolidInfillExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                                                                         "topSolidFillExtrusionWidth_mm"));
        supportExtrusionWidth.setDisable(!slicerMappings.isMapped(slicerType,
                                                                  "supportExtrusionWidth_mm"));

        //Support tab
        supportOverhangThreshold.setDisable(!slicerMappings.isMapped(slicerType,
                                                                     "supportOverhangThreshold_degrees"));
        forcedSupportLayers.setDisable(!slicerMappings.isMapped(slicerType,
                                                                "forcedSupportForFirstNLayers"));
        supportPatternSpacing.setDisable(!slicerMappings.isMapped(slicerType,
                                                                  "supportPatternSpacing_mm"));
        supportPatternAngle.setDisable(!slicerMappings.isMapped(slicerType,
                                                                "supportPatternAngle_degrees"));

        //Speed tab
        firstLayerSpeed.setDisable(!slicerMappings.isMapped(slicerType, "firstLayerSpeed_mm_per_s"));
        perimeterSpeed.setDisable(!slicerMappings.isMapped(slicerType, "perimeterSpeed_mm_per_s"));
        smallPerimeterSpeed.setDisable(!slicerMappings.isMapped(slicerType,
                                                                "smallPerimeterSpeed_mm_per_s"));
        externalPerimeterSpeed.setDisable(!slicerMappings.isMapped(slicerType,
                                                                   "externalPerimeterSpeed_mm_per_s"));
        infillSpeed.setDisable(!slicerMappings.isMapped(slicerType, "fillSpeed_mm_per_s"));
        solidInfillSpeed.setDisable(!slicerMappings.isMapped(slicerType, "solidFillSpeed_mm_per_s"));
        topSolidInfillSpeed.setDisable(!slicerMappings.isMapped(slicerType,
                                                                "topSolidFillSpeed_mm_per_s"));
        supportMaterialSpeed.setDisable(
            !slicerMappings.isMapped(slicerType, "supportSpeed_mm_per_s"));
        bridgesSpeed.setDisable(!slicerMappings.isMapped(slicerType, "bridgeSpeed_mm_per_s"));
        gapFillSpeed.setDisable(!slicerMappings.isMapped(slicerType, "gapFillSpeed_mm_per_s"));

        //Cooling tab
        minFanSpeed.setDisable(!slicerMappings.isMapped(slicerType, "minFanSpeed_percent"));
        maxFanSpeed.setDisable(!slicerMappings.isMapped(slicerType, "maxFanSpeed_percent"));
        bridgesFanSpeed.setDisable(!slicerMappings.isMapped(slicerType, "bridgeFanSpeed_percent"));
        disableFanForFirstNLayers.setDisable(!slicerMappings.isMapped(slicerType,
                                                                      "disableFanFirstNLayers"));
        enableFanIfLayerTimeBelow.setDisable(!slicerMappings.isMapped(slicerType,
                                                                      "coolIfLayerTimeLessThan_secs"));
        slowFanIfLayerTimeBelow.setDisable(!slicerMappings.isMapped(slicerType,
                                                                    "slowDownIfLayerTimeLessThan_secs"));
        minPrintSpeed.setDisable(!slicerMappings.isMapped(slicerType, "minPrintSpeed_mm_per_s"));
    }

    private void updateSettingsFromGUI(SlicerParametersFile settingsToUpdate)
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
        settingsToUpdate.setFirstLayerExtrusionWidth_mm(
            firstLayerExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setFirstLayerNozzle(
            firstLayerNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setPerimeterExtrusionWidth_mm(
            perimeterExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setPerimeterNozzle(
            perimeterNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setFillExtrusionWidth_mm(infillExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setSolidFillExtrusionWidth_mm(
            solidInfillExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setFillNozzle(fillNozzleChoice.getSelectionModel().getSelectedIndex());
        settingsToUpdate.setTopSolidFillExtrusionWidth_mm(
            topSolidInfillExtrusionWidth.floatValueProperty().get());

        settingsToUpdate.setSupportExtrusionWidth_mm(
            supportExtrusionWidth.floatValueProperty().get());
        settingsToUpdate.setSupportNozzle(supportNozzleChoice.getSelectionModel().getSelectedIndex());

        settingsToUpdate.setSupportInterfaceNozzle(
            supportInterfaceNozzleChoice.getSelectionModel().getSelectedIndex());

        //Support tab
        settingsToUpdate.setGenerateSupportMaterial(supportMaterialEnabled.isSelected());
        settingsToUpdate.setSupportInterfaceNozzle(
            supportInterfaceNozzleChoice.getSelectionModel().getSelectedIndex());
        settingsToUpdate.setSupportOverhangThreshold_degrees(
            supportOverhangThreshold.intValueProperty().get());
        settingsToUpdate.setForcedSupportForFirstNLayers(
            forcedSupportLayers.intValueProperty().get());
        settingsToUpdate.setSupportPattern(supportPattern.valueProperty().get());
        settingsToUpdate.setSupportPatternSpacing_mm(
            supportPatternSpacing.floatValueProperty().get());
        settingsToUpdate.setSupportPatternAngle_degrees(supportPatternAngle.intValueProperty().get());

        //Speed tab
        settingsToUpdate.setFirstLayerSpeed_mm_per_s(firstLayerSpeed.intValueProperty().get());
        settingsToUpdate.setPerimeterSpeed_mm_per_s(perimeterSpeed.intValueProperty().get());
        settingsToUpdate.setSmallPerimeterSpeed_mm_per_s(
            smallPerimeterSpeed.intValueProperty().get());
        settingsToUpdate.setExternalPerimeterSpeed_mm_per_s(
            externalPerimeterSpeed.intValueProperty().get());
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
        settingsToUpdate.setDisableFanFirstNLayers(
            disableFanForFirstNLayers.intValueProperty().get());
        settingsToUpdate.setCoolIfLayerTimeLessThan_secs(
            enableFanIfLayerTimeBelow.intValueProperty().get());
        settingsToUpdate.setSlowDownIfLayerTimeLessThan_secs(
            slowFanIfLayerTimeBelow.intValueProperty().get());
        settingsToUpdate.setMinPrintSpeed_mm_per_s(minPrintSpeed.intValueProperty().get());
    }

    /**
     *
     * @param settings
     */
    public void updateProfileData(SlicerParametersFile settings)
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
            boolean settingsAreInApplicationProfileList = SlicerParametersContainer.applicationProfileListContainsProfile(
                settings.getProfileName());
            isMutable.set(!settingsAreInApplicationProfileList);
            isDirty.set(false);
        }
    }

    /**
     *
     * @return
     */
    public SlicerParametersFile getProfileData()
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

    private void validateProfileName()
    {
        boolean invalid = false;
        String profileNameText = profileNameField.getText();

        if (profileNameText.equals(""))
        {
            invalid = true;
        } else
        {
            ObservableList<SlicerParametersFile> existingProfileList = SlicerParametersContainer.getUserProfileList();
            for (SlicerParametersFile settings : existingProfileList)
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

    void whenSavePressed()
    {
        assert (state.get() != ProfileDetailsControllerCopy.State.ROBOX);
//        Filament filament = getFilament(currentFilamentID);
//        FilamentContainer.saveFilament(filament);
//        repopulateCmbFilament();
//        cmbFilament.setValue(FilamentContainer.getFilamentByID(filament.getFilamentID()));
    }

    void whenNewPressed()
    {
        state.set(ProfileDetailsControllerCopy.State.NEW);
//        clearWidgets();
//        currentFilamentID = null;
    }

    void whenCopyPressed()
    {
//        Filament filament = getFilament(null);
//        Set<String> allCurrentNames = new HashSet<>();
//        allFilaments.forEach((Filament filament1) ->
//        {
//            allCurrentNames.add(filament1.getFriendlyFilamentName());
//        });
//        String newName = DeDuplicator.suggestNonDuplicateName(filament.getFriendlyFilamentName(),
//                                                              allCurrentNames);
//        filament.setFriendlyFilamentName(newName);
//        FilamentContainer.saveFilament(filament);
//        repopulateCmbFilament();
//        cmbFilament.setValue(FilamentContainer.getFilamentByID(filament.getFilamentID()));
    }

    void whenDeletePressed()
    {
//        if (state.get() != ProfileDetailsController.State.NEW)
//        {
//            FilamentContainer.deleteFilament(FilamentContainer.getFilamentByID(currentFilamentID));
//        }
//        repopulateCmbFilament();
//        clearWidgets();
//        selectFirstFilament();
    }

    @Override
    public String getMenuTitle()
    {
        return "extrasMenu.printProfile";
    }

    @Override
    public List<ExtrasMenuInnerPanel.OperationButton> getOperationButtons()
    {
        List<ExtrasMenuInnerPanel.OperationButton> operationButtons = new ArrayList<>();
        ExtrasMenuInnerPanel.OperationButton newButton = new ExtrasMenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "projectLoader.newButtonLabel";
            }

            @Override
            public String getFXMLName()
            {
                return "newButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "projectLoader.newButtonLabele";
            }

            @Override
            public void whenClicked()
            {
                whenNewPressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return new SimpleBooleanProperty(true);
            }

        };
        operationButtons.add(newButton);
        ExtrasMenuInnerPanel.OperationButton saveButton = new ExtrasMenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.Save";
            }

            @Override
            public String getFXMLName()
            {
                return "saveButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "genericFirstLetterCapitalised.Save";
            }

            @Override
            public void whenClicked()
            {
                whenSavePressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canSave;
            }

        };
        operationButtons.add(saveButton);
        ExtrasMenuInnerPanel.OperationButton copyButton = new ExtrasMenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.Copy";
            }

            @Override
            public String getFXMLName()
            {
                return "copyButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "genericFirstLetterCapitalised.Copy";
            }

            @Override
            public void whenClicked()
            {
                whenCopyPressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return new SimpleBooleanProperty(true);
            }

        };
        operationButtons.add(copyButton);
        ExtrasMenuInnerPanel.OperationButton deleteButton = new ExtrasMenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.Delete";
            }

            @Override
            public String getFXMLName()
            {
                return "deleteModelButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "genericFirstLetterCapitalised.Delete";
            }

            @Override
            public void whenClicked()
            {
                whenDeletePressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canDelete;
            }

        };
        operationButtons.add(deleteButton);
        return operationButtons;
    }
}
