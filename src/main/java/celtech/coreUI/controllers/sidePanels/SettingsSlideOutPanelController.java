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
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.coreUI.controllers.utilityPanels.MaterialDetailsController;
import celtech.printerControl.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SlicerSettings;
import celtech.utils.FXUtils;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SettingsSlideOutPanelController implements Initializable
{

    private Stenographer steno = StenographerFactory.getStenographer(SettingsSlideOutPanelController.class.getName());
    private SettingsScreenState settingsScreenState = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;

    @FXML
    private MaterialDetailsController materialDetailsController;

    @FXML
    private CheckBox autoUnretract;

    @FXML
    private VBox basicModeControls;

    @FXML
    private TabPane customTabPane;

    @FXML
    private TextField partialOpenValue;

    @FXML
    private TextField nozzleFinishClose;

    @FXML
    private TextField nozzleFinishOpen;

    @FXML
    private TextField nozzleFinishRetract;

    @FXML
    private TextField nozzleFinishUnretract;

    @FXML
    private TextField nozzleStartClose;

    @FXML
    private TextField nozzleStartRetract;

    @FXML
    private TextField retractLength;

    @FXML
    private TextField retractSpeed;

    @FXML
    private TextField layerHeight;

    @FXML
    private RadioButton autoSupportRadioButton;

    @FXML
    private TextField unretractLength;

    @FXML
    private ToggleGroup nozzleChoiceGroup;

    @FXML
    private ToggleButton nozzle1Button;

    @FXML
    private ToggleButton nozzle2Button;

    @FXML
    private ChoiceBox<String> perimeterNozzleChoice;

    @FXML
    private ChoiceBox<String> fillNozzleChoice;

    @FXML
    private ChoiceBox<String> supportNozzleChoice;

    @FXML
    private CheckBox supportMaterialEnabled;

    @FXML
    private TextField fillDensity;

    @FXML
    private ChoiceBox<String> fillPatternChoice;

    @FXML
    private TextField supportOverhangThreshold;

    @FXML
    private TextField forcedSupportLayers;

    @FXML
    private ChoiceBox<String> supportPattern;

    @FXML
    private TextField supportPatternAngle;

    @FXML
    private TextField supportPatternSpacing;

    @FXML
    private TextField perimeterSpeed;
    @FXML
    private TextField smallPerimeterSpeed;
    @FXML
    private TextField externalPerimeterSpeed;
    @FXML
    private TextField infillSpeed;
    @FXML
    private TextField solidInfillSpeed;
    @FXML
    private TextField topSolidInfillSpeed;
    @FXML
    private TextField supportMaterialSpeed;
    @FXML
    private TextField bridgesSpeed;
    @FXML
    private TextField gapFillSpeed;

    @FXML
    private TextField numberOfPerimeters;

    @FXML
    private TextField infillEveryN;

    @FXML
    private TextField solidLayers;
    @FXML
    private CheckBox spiralVase;
    @FXML
    private TextField brimWidth;

    //Cooling
    @FXML
    private CheckBox enableAutoCooling;
    @FXML
    private TextField minFanSpeed;
    @FXML
    private TextField maxFanSpeed;
    @FXML
    private TextField bridgesFanSpeed;
    @FXML
    private TextField disableFanForFirstNLayers;
    @FXML
    private TextField enableFanIfLayerTimeBelow;
    @FXML
    private TextField slowFanIfLayerTimeBelow;
    @FXML
    private TextField minPrintSpeed;

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

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        applicationStatus = ApplicationStatus.getInstance();
        displayManager = DisplayManager.getInstance();

        settingsScreenState = SettingsScreenState.getInstance();
//        printerStatusList = RoboxCommsManager.getInstance().getPrintStatusList();
//
//        printerSelectionCombo.setItems(printerStatusList);
////        printerSelectionCombo.getItems().clear();
////        populatePrinterChooser();
//        printerSelectionCombo.getSelectionModel().clearSelection();
//
//        printerSelectionCombo.getItems().addListener(new ListChangeListener<Printer>()
//        {
//
//            @Override
//            public void onChanged(ListChangeListener.Change<? extends Printer> change)
//            {
//                while (change.next())
//                {
//                    if (change.wasAdded())
//                    {
//                        for (Printer addedPrinter : change.getAddedSubList())
//                        {
//                            if (printerSelectionCombo.getSelectionModel().getSelectedItem() == null)
//                            {
//                                printerSelectionCombo.getSelectionModel().select(0);
//                                break;
//                            }
//                        }
//                    } else if (change.wasRemoved())
//                    {
//                        if (printerSelectionCombo.getItems().isEmpty() && applicationStatus.getMode() == ApplicationMode.SETTINGS)
//                        {
//                            applicationStatus.setMode(ApplicationMode.STATUS);
//                        }
//                    } else if (change.wasReplaced())
//                    {
//                    } else if (change.wasUpdated())
//                    {
//                    }
//                }
//            }
//        });
//        settingsScreenState.selectedPrinterProperty().bind(printerSelectionCombo.valueProperty());
//        printerSelectionCombo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Printer>()
//        {
//            @Override
//            public void changed(ObservableValue<? extends Printer> ov, Printer lastSelectedPrinter, Printer selectedPrinter)
//            {
//                if (lastSelectedPrinter != null)
//                {
//                    lastSelectedPrinter.reelDataChangedProperty().removeListener(reelDataChangedListener);
//                    lastSelectedPrinter.loadedFilamentProperty().removeListener(filamentChangeListener);
//                }
//                if (selectedPrinter != null && selectedPrinter != lastSelectedPrinter)
//                {
//                    currentPrinter = selectedPrinter;
//                    selectedPrinter.reelDataChangedProperty().addListener(reelDataChangedListener);
//                    selectedPrinter.loadedFilamentProperty().addListener(filamentChangeListener);
//                }
//
//                if (selectedPrinter == null)
//                {
//                    currentPrinter = null;
//                }
//            }
//        });
//
//        materialSelectionChoice.setItems(availableFilaments);
//
//        printQualityChoice.setItems(FXCollections.observableArrayList(PrintQualityEnumeration.values()));
//        printQualityChoice.getSelectionModel().selectFirst();
//        settingsScreenState.printQualityProperty().bind(printQualityChoice.valueProperty());
//
//        customTabPane.visibleProperty().bind(printQualityChoice.valueProperty().isEqualTo(PrintQualityEnumeration.CUSTOM));
//        basicModeControls.visibleProperty().bind(printQualityChoice.valueProperty().isNotEqualTo(PrintQualityEnumeration.CUSTOM));

//        bindQualitySpecificSettings(printQualityChoice.getValue());
//
//        printQualityChoice.valueProperty().addListener(new ChangeListener<PrintQualityEnumeration>()
//        {
//            @Override
//            public void changed(ObservableValue<? extends PrintQualityEnumeration> ov, PrintQualityEnumeration t, PrintQualityEnumeration t1)
//            {
//                switch (t1)
//                {
//                    case DRAFT:
//                        settingsScreenState.setSettings(draftSettings);
//                        break;
//                    case NORMAL:
//                        settingsScreenState.setSettings(normalSettings);
//                        break;
//                    case FINE:
//                        settingsScreenState.setSettings(fineSettings);
//                        break;
//                    case CUSTOM:
//                        settingsScreenState.setSettings(customSettings);
//                        break;
//                    default:
//                        break;
//                }
//                unbindQualitySpecificSettings(t);
//                bindQualitySpecificSettings(t1);
//            }
//        });
//        applicationStatus.modeProperty().addListener(new ChangeListener<ApplicationMode>()
//        {
//            @Override
//            public void changed(ObservableValue<? extends ApplicationMode> ov, ApplicationMode t, ApplicationMode t1)
//            {
//                if (t1 == ApplicationMode.SETTINGS)
//                {
//                    if (displayManager.getCurrentlyVisibleProject().getProjectMode() == ProjectMode.GCODE)
//                    {
//                        meshControls.setVisible(false);
//                        printQualityLabel.setVisible(false);
//                        printQualityChoice.setVisible(false);
//                        materialLabel.setVisible(false);
//                        materialSelectionChoice.setVisible(false);
//                    } else
//                    {
//                        meshControls.setVisible(true);
//                        printQualityLabel.setVisible(true);
//                        printQualityChoice.setVisible(true);
//                        materialLabel.setVisible(true);
//                        materialSelectionChoice.setVisible(true);
//                    }
//                }
//            }
//        });
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

        nozzleChoiceGroup.selectedToggleProperty().addListener(nozzleSelectionListener);

        perimeterNozzleChoice.setItems(nozzleOptions);
        fillNozzleChoice.setItems(nozzleOptions);
        supportNozzleChoice.setItems(nozzleOptions);

        fillPatternChoice.setItems(fillPatternOptions);

        supportPattern.setItems(supportPatternOptions);

    }

    private void bindNozzleParameters(int nozzleNumber)
    {
        boundToNozzle = nozzleNumber;

        Bindings.bindBidirectional(nozzleFinishUnretract.textProperty(), customSettings.getNozzle_finish_unretract_by().get(nozzleNumber), floatConverter);

        Bindings.bindBidirectional(nozzleFinishOpen.textProperty(), customSettings.getNozzle_finish_open_by().get(nozzleNumber), floatConverter);

        Bindings.bindBidirectional(nozzleStartRetract.textProperty(), customSettings.getNozzle_start_retract_by().get(nozzleNumber), floatConverter);

        Bindings.bindBidirectional(nozzleFinishRetract.textProperty(), customSettings.getNozzle_finish_retract_by().get(nozzleNumber), floatConverter);

        Bindings.bindBidirectional(nozzleStartClose.textProperty(), customSettings.getNozzle_start_close_by().get(nozzleNumber), floatConverter);

        Bindings.bindBidirectional(nozzleFinishClose.textProperty(), customSettings.getNozzle_finish_close_by().get(nozzleNumber), floatConverter);
        Bindings.bindBidirectional(partialOpenValue.textProperty(), customSettings.getNozzle_partial_open_angle().get(nozzleNumber), floatConverter);

    }

    private void unbindNozzleParameters()
    {
        if (boundToNozzle != -1)
        {
            Bindings.unbindBidirectional(nozzleFinishUnretract.textProperty(), customSettings.getNozzle_finish_unretract_by().get(boundToNozzle));
            Bindings.unbindBidirectional(nozzleFinishOpen.textProperty(), customSettings.getNozzle_finish_open_by().get(boundToNozzle));
            Bindings.unbindBidirectional(nozzleStartRetract.textProperty(), customSettings.getNozzle_start_retract_by().get(boundToNozzle));
            Bindings.unbindBidirectional(nozzleFinishRetract.textProperty(), customSettings.getNozzle_finish_retract_by().get(boundToNozzle));
            Bindings.unbindBidirectional(nozzleStartClose.textProperty(), customSettings.getNozzle_start_close_by().get(boundToNozzle));
            Bindings.unbindBidirectional(nozzleFinishClose.textProperty(), customSettings.getNozzle_finish_close_by().get(boundToNozzle));
            Bindings.unbindBidirectional(partialOpenValue.textProperty(), customSettings.getNozzle_partial_open_angle().get(boundToNozzle));
        }

        boundToNozzle = -1;

    }

    private void unbindQualitySpecificSettings(PrintQualityEnumeration lastQuality)
    {
        if (lastQuality != PrintQualityEnumeration.CUSTOM)
        {
            Bindings.unbindBidirectional(lastSettings.support_materialProperty(), autoSupportRadioButton.selectedProperty());
        }
    }

    private void bindQualitySpecificSettings(PrintQualityEnumeration currentQuality)
    {
        SlicerSettings currentSettings = settingsScreenState.getSettings();

        if (currentQuality != PrintQualityEnumeration.CUSTOM)
        {
            Bindings.bindBidirectional(currentSettings.support_materialProperty(), autoSupportRadioButton.selectedProperty());
        }

        lastSettings = currentSettings;
    }

    public void bindLoadedModels(Project project)
    {
        if (customSettings != null)
        {
            //Nozzle independent custom settings
            autoUnretract.selectedProperty().unbindBidirectional(customSettings.auto_unretractProperty());
            Bindings.unbindBidirectional(unretractLength.textProperty(), customSettings.unretract_lengthProperty());
            Bindings.unbindBidirectional(retractLength.textProperty(), customSettings.retract_lengthProperty());
            Bindings.unbindBidirectional(retractSpeed.textProperty(), customSettings.retract_speedProperty());

            Bindings.unbindBidirectional(layerHeight.textProperty(), customSettings.getLayer_height());

            customSettings.perimeter_nozzleProperty().unbind();
            customSettings.getPerimeter_extrusion_width().unbind();

            customSettings.infill_nozzleProperty().unbind();
            customSettings.getInfill_extrusion_width().unbind();
            customSettings.getSolid_infill_extrusion_width().unbind();

            customSettings.support_material_nozzleProperty().unbind();

            Bindings.unbindBidirectional(customSettings.support_materialProperty(), supportMaterialEnabled.selectedProperty());

            Bindings.unbindBidirectional(fillDensity.textProperty(), customSettings.fill_densityProperty());

            Bindings.unbindBidirectional(fillPatternChoice.valueProperty(), customSettings.fill_patternProperty());
            Bindings.unbindBidirectional(infillEveryN.textProperty(), customSettings.infill_every_layersProperty());

            Bindings.unbindBidirectional(supportOverhangThreshold.textProperty(), customSettings.support_material_thresholdProperty());
            Bindings.unbindBidirectional(forcedSupportLayers.textProperty(), customSettings.support_material_enforce_layersProperty());

            Bindings.unbindBidirectional(supportPattern.valueProperty(), customSettings.support_material_patternProperty());

            Bindings.unbindBidirectional(supportPatternSpacing.textProperty(), customSettings.support_material_spacingProperty());
            Bindings.unbindBidirectional(supportPatternAngle.textProperty(), customSettings.support_material_angleProperty());

            Bindings.unbindBidirectional(numberOfPerimeters.textProperty(), customSettings.perimetersProperty());

            Bindings.unbindBidirectional(perimeterSpeed.textProperty(), customSettings.perimeter_speedProperty());

            Bindings.unbindBidirectional(smallPerimeterSpeed.textProperty(), customSettings.small_perimeter_speedProperty());
            Bindings.unbindBidirectional(externalPerimeterSpeed.textProperty(), customSettings.external_perimeter_speedProperty());
            Bindings.unbindBidirectional(infillSpeed.textProperty(), customSettings.infill_speedProperty());
            Bindings.unbindBidirectional(solidInfillSpeed.textProperty(), customSettings.solid_infill_speedProperty());
            Bindings.unbindBidirectional(topSolidInfillSpeed.textProperty(), customSettings.top_solid_infill_speedProperty());
            Bindings.unbindBidirectional(supportMaterialSpeed.textProperty(), customSettings.support_material_speedProperty());
            Bindings.unbindBidirectional(bridgesSpeed.textProperty(), customSettings.bridge_speedProperty());
            Bindings.unbindBidirectional(gapFillSpeed.textProperty(), customSettings.gap_fill_speedProperty());

            Bindings.unbindBidirectional(solidLayers.textProperty(), customSettings.top_solid_layersProperty());
            customSettings.bottom_solid_layersProperty().unbind();

            Bindings.unbindBidirectional(spiralVase.selectedProperty(), customSettings.spiral_vaseProperty());

            Bindings.unbindBidirectional(brimWidth.textProperty(), customSettings.getBrim_width());

            Bindings.unbindBidirectional(enableAutoCooling.textProperty(), customSettings.getCooling());
            Bindings.unbindBidirectional(minFanSpeed.textProperty(), customSettings.getMin_fan_speed());
            Bindings.unbindBidirectional(maxFanSpeed.textProperty(), customSettings.getMax_fan_speed());
            Bindings.unbindBidirectional(bridgesFanSpeed.textProperty(), customSettings.getBridge_fan_speed());
            Bindings.unbindBidirectional(disableFanForFirstNLayers.textProperty(), customSettings.getDisable_fan_first_layers());
            Bindings.unbindBidirectional(enableFanIfLayerTimeBelow.textProperty(), customSettings.getFan_below_layer_time());
            Bindings.unbindBidirectional(slowFanIfLayerTimeBelow.textProperty(), customSettings.getSlowdown_below_layer_time());
            Bindings.unbindBidirectional(minPrintSpeed.textProperty(), customSettings.getMin_print_speed());

            unbindNozzleParameters();

        }

        SlicerSettings localCustomSettings = project.getCustomSettings();
        customSettings = localCustomSettings;

        //Nozzle independent custom settings
        autoUnretract.selectedProperty().bindBidirectional(localCustomSettings.auto_unretractProperty());
        Bindings.bindBidirectional(unretractLength.textProperty(), localCustomSettings.unretract_lengthProperty(), intConverter);
        Bindings.bindBidirectional(retractLength.textProperty(), localCustomSettings.retract_lengthProperty(), intConverter);
        Bindings.bindBidirectional(retractSpeed.textProperty(), localCustomSettings.retract_speedProperty(), intConverter);

        Bindings.bindBidirectional(layerHeight.textProperty(), localCustomSettings.getLayer_height(), floatConverter);

        perimeterNozzleChoice.getSelectionModel().select(localCustomSettings.perimeter_nozzleProperty().get() - 1);
        localCustomSettings.perimeter_nozzleProperty().bind(perimeterNozzleChoice.getSelectionModel().selectedIndexProperty().add(1));
        localCustomSettings.getPerimeter_extrusion_width().bind(localCustomSettings.getNozzle_diameter().get(perimeterNozzleChoice.getSelectionModel().selectedIndexProperty().get()));

        fillNozzleChoice.getSelectionModel().select(localCustomSettings.infill_nozzleProperty().get() - 1);
        localCustomSettings.infill_nozzleProperty().bind(fillNozzleChoice.getSelectionModel().selectedIndexProperty().add(1));
        localCustomSettings.getInfill_extrusion_width().bind(localCustomSettings.getNozzle_diameter().get(fillNozzleChoice.getSelectionModel().selectedIndexProperty().get()));
        localCustomSettings.getSolid_infill_extrusion_width().bind(localCustomSettings.getNozzle_diameter().get(fillNozzleChoice.getSelectionModel().selectedIndexProperty().get()));

        supportNozzleChoice.getSelectionModel().select(localCustomSettings.support_material_nozzleProperty().get() - 1);
        localCustomSettings.support_material_nozzleProperty().bind(supportNozzleChoice.getSelectionModel().selectedIndexProperty().add(1));

        Bindings.bindBidirectional(localCustomSettings.support_materialProperty(), supportMaterialEnabled.selectedProperty());

        Bindings.bindBidirectional(fillDensity.textProperty(), localCustomSettings.fill_densityProperty(), floatConverter);

        Bindings.bindBidirectional(fillPatternChoice.valueProperty(), localCustomSettings.fill_patternProperty());
        Bindings.bindBidirectional(infillEveryN.textProperty(), localCustomSettings.infill_every_layersProperty(), intConverter);

        Bindings.bindBidirectional(supportOverhangThreshold.textProperty(), localCustomSettings.support_material_thresholdProperty(), intConverter);
        Bindings.bindBidirectional(forcedSupportLayers.textProperty(), localCustomSettings.support_material_enforce_layersProperty(), intConverter);

        Bindings.bindBidirectional(supportPattern.valueProperty(), localCustomSettings.support_material_patternProperty());

        Bindings.bindBidirectional(supportPatternSpacing.textProperty(), localCustomSettings.support_material_spacingProperty(), floatConverter);
        Bindings.bindBidirectional(supportPatternAngle.textProperty(), localCustomSettings.support_material_angleProperty(), intConverter);

        Bindings.bindBidirectional(numberOfPerimeters.textProperty(), localCustomSettings.perimetersProperty(), intConverter);

        Bindings.bindBidirectional(perimeterSpeed.textProperty(), localCustomSettings.perimeter_speedProperty(), intConverter);

        Bindings.bindBidirectional(smallPerimeterSpeed.textProperty(), localCustomSettings.small_perimeter_speedProperty(), intConverter);
        Bindings.bindBidirectional(externalPerimeterSpeed.textProperty(), localCustomSettings.external_perimeter_speedProperty(), intConverter);
        Bindings.bindBidirectional(infillSpeed.textProperty(), localCustomSettings.infill_speedProperty(), intConverter);
        Bindings.bindBidirectional(solidInfillSpeed.textProperty(), localCustomSettings.solid_infill_speedProperty(), intConverter);
        Bindings.bindBidirectional(topSolidInfillSpeed.textProperty(), localCustomSettings.top_solid_infill_speedProperty(), intConverter);
        Bindings.bindBidirectional(supportMaterialSpeed.textProperty(), localCustomSettings.support_material_speedProperty(), intConverter);
        Bindings.bindBidirectional(bridgesSpeed.textProperty(), localCustomSettings.bridge_speedProperty(), intConverter);
        Bindings.bindBidirectional(gapFillSpeed.textProperty(), localCustomSettings.gap_fill_speedProperty(), intConverter);

        Bindings.bindBidirectional(solidLayers.textProperty(), localCustomSettings.top_solid_layersProperty(), intConverter);
        localCustomSettings.bottom_solid_layersProperty().bind(localCustomSettings.top_solid_layersProperty());

        Bindings.bindBidirectional(spiralVase.selectedProperty(), localCustomSettings.spiral_vaseProperty());

        Bindings.bindBidirectional(brimWidth.textProperty(), localCustomSettings.getBrim_width(), intConverter);

        Bindings.bindBidirectional(enableAutoCooling.selectedProperty(), customSettings.getCooling());
        Bindings.bindBidirectional(minFanSpeed.textProperty(), customSettings.getMin_fan_speed(), intConverter);
        Bindings.bindBidirectional(maxFanSpeed.textProperty(), customSettings.getMax_fan_speed(), intConverter);
        Bindings.bindBidirectional(bridgesFanSpeed.textProperty(), customSettings.getBridge_fan_speed(), intConverter);
        Bindings.bindBidirectional(disableFanForFirstNLayers.textProperty(), customSettings.getDisable_fan_first_layers(), intConverter);
        Bindings.bindBidirectional(enableFanIfLayerTimeBelow.textProperty(), customSettings.getFan_below_layer_time(), intConverter);
        Bindings.bindBidirectional(slowFanIfLayerTimeBelow.textProperty(), customSettings.getSlowdown_below_layer_time(), intConverter);
        Bindings.bindBidirectional(minPrintSpeed.textProperty(), customSettings.getMin_print_speed(), intConverter);

        //Switch to nozzle 1 data by default
        nozzle1Button.setSelected(true);

        bindNozzleParameters(0);
    }

    public void updateFilamentData(Filament filament)
    {
        materialDetailsController.updateMaterialData(filament);
    }
}
