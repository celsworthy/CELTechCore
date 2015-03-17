package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.ProfileChoiceListCell;
import celtech.coreUI.components.material.MaterialComponent;
import celtech.coreUI.components.printerstatus.PrinterGridComponent;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.coreUI.controllers.utilityPanels.ProfileDetailsController;
import celtech.printerControl.model.Extruder;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.utils.PrinterListChangesListener;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SettingsSidePanelController implements Initializable, SidePanelManager,
    PrinterListChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        SettingsSidePanelController.class.getName());
    private PrinterSettings printerSettings = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;

    private boolean suppressCustomProfileChangeTriggers = false;

    @FXML
    private Slider brimSlider;

    @FXML
    private VBox materialContainer;

    @FXML
    private ComboBox<SlicerParametersFile> customProfileChooser;

    @FXML
    private Slider supportSlider;

    @FXML
    private Slider qualityChooser;

    @FXML
    private VBox customProfileVBox;

    @FXML
    private PrinterGridComponent printerGrid;

    @FXML
    private Slider fillDensitySlider;

    @FXML
    private VBox nonCustomProfileVBox;

    private final SlicerParametersFile draftSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.draftSettingsProfileName);
    private final SlicerParametersFile normalSettings = SlicerParametersContainer.
        getSettingsByProfileName(
            ApplicationConfiguration.normalSettingsProfileName);
    private final SlicerParametersFile fineSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.fineSettingsProfileName);

    private final ObservableList<SlicerParametersFile> availableProfiles = FXCollections.
        observableArrayList();

    private Printer previouslySelectedPrinter = null;
    private ObjectProperty<Printer> currentPrinter = new SimpleObjectProperty<>();
    private Project currentProject;
    /**
     * filament0 is updated by the MaterialComponent for extruder 0, then changes to filament0 are
     * reflected in PrinterSettings filament 0.
     */
    private ObjectProperty<Filament> filament0 = new SimpleObjectProperty<>(null);
    private ObjectProperty<Filament> filament1 = new SimpleObjectProperty<>(null);

    private VBox createProfilePage = null;
    private ModalDialog createProfileDialogue = null;

    private SettingsSlideOutPanelController slideOutController = null;

    private ProfileDetailsController profileDetailsController = null;

    /**
     * Initialises the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        applicationStatus = ApplicationStatus.getInstance();
        displayManager = DisplayManager.getInstance();
        
        currentPrinter.bind(printerGrid.getSelectedPrinter());
        currentPrinter.addListener(
            (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
        {
            if (previouslySelectedPrinter != null)
            {
                unbindPrinter(previouslySelectedPrinter);
            }
            previouslySelectedPrinter = currentPrinter.get();
            
            if (printerSettings != null)
            {
                printerSettings.setSelectedPrinter(currentPrinter.get());
            }
            bindPrinter(currentPrinter.get());
            configureMaterialComponents(currentPrinter.get());
        });

        try
        {
            FXMLLoader createProfilePageLoader = new FXMLLoader(getClass().getResource(
                ApplicationConfiguration.fxmlUtilityPanelResourcePath + "profileDetails.fxml"),
                                                                Lookup.getLanguageBundle());
            createProfilePage = createProfilePageLoader.load();
            profileDetailsController = createProfilePageLoader.getController();
            profileDetailsController.showButtons(false);

            createProfileDialogue = new ModalDialog(Lookup.i18n(
                "sidePanel_settings.createProfileDialogueTitle"));
            createProfileDialogue.setContent(createProfilePage);
        } catch (Exception ex)
        {
            steno.error("Failed to load profile creation page");
        }

        setupQualityChooser();

        setupCustomProfileChooser();

        nonCustomProfileVBox.visibleProperty()
            .bind(qualityChooser.valueProperty().isNotEqualTo(
                    PrintQualityEnumeration.CUSTOM.getEnumPosition()));

        setupOverrides();

        Lookup.getSelectedProjectProperty().addListener(
            (ObservableValue<? extends Project> observable, Project oldValue, Project newValue) ->
            {
                whenProjectChanged(newValue);
            });
        Lookup.getPrinterListChangesNotifier().addListener(this);

    }

    private void setupQualityChooser()
    {
        qualityChooser.setLabelFormatter(new StringConverter<Double>()
        {
            @Override
            public String toString(Double n)
            {
                PrintQualityEnumeration selectedQuality = PrintQualityEnumeration.fromEnumPosition(
                    n.intValue());
                return selectedQuality.getFriendlyName();
            }

            @Override
            public Double fromString(String s)
            {
                PrintQualityEnumeration selectedQuality = PrintQualityEnumeration.valueOf(s);
                return (double) selectedQuality.getEnumPosition();
            }
        });

        printQualityUpdate(PrintQualityEnumeration.DRAFT);

        qualityChooser.valueProperty().addListener(
            (ObservableValue<? extends Number> ov, Number lastQualityValue, Number newQualityValue) ->
            {
                if (lastQualityValue != newQualityValue)
                {
                    PrintQualityEnumeration quality = PrintQualityEnumeration.fromEnumPosition(
                        newQualityValue.intValue());

                    printQualityUpdate(quality);
                }
            });
    }

    private void setupCustomProfileChooser()
    {
        Callback<ListView<SlicerParametersFile>, ListCell<SlicerParametersFile>> profileChooserCellFactory
            = (ListView<SlicerParametersFile> list) -> new ProfileChoiceListCell();

        customProfileChooser.setCellFactory(profileChooserCellFactory);
        customProfileChooser.setButtonCell(profileChooserCellFactory.call(null));
        customProfileChooser.setItems(availableProfiles);

        updateProfileList();

        customProfileChooser.getSelectionModel().selectedItemProperty().addListener(
            (ObservableValue<? extends SlicerParametersFile> observable, SlicerParametersFile oldValue, SlicerParametersFile newValue) ->
        {
            if (!suppressCustomProfileChangeTriggers)
            {
                if (oldValue != newValue)
                {
                    if (applicationStatus.getMode() == ApplicationMode.SETTINGS)
                    {
                        displayManager.slideOutAdvancedPanel();
                    }
                    slideOutController.showProfileTab();
                }
                
                if (newValue != null)
                {
                    if (printerSettings != null && printerSettings.getPrintQuality()
                        == PrintQualityEnumeration.CUSTOM)
                    {
                        slideOutController.updateProfileData(newValue);
                        printerSettings.setSettingsName(newValue.getProfileName());
                    }
                } else if (printerSettings != null && newValue == null
                    && printerSettings.getPrintQuality()
                    == PrintQualityEnumeration.CUSTOM)
                {
                    slideOutController.updateProfileData(null);
                }
            }
        });

        SlicerParametersContainer.getUserProfileList().addListener(
            (ListChangeListener.Change<? extends SlicerParametersFile> c) ->
            {
                updateProfileList();
            });
    }

    private ChangeListener<Filament> filament0Listener;
    private ChangeListener<Filament> filament1Listener;

    private void removeFilamentListeners()
    {
        if (filament0Listener != null)
        {
            filament0.removeListener(filament0Listener);
        }
        if (filament1Listener != null)
        {
            filament1.removeListener(filament1Listener);
        }
    }

    private void setupFilamentListeners()
    {

        filament0Listener = (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
        {
            if (printerSettings != null)
            {
                printerSettings.setFilament0(newValue);
            }
        };

        filament1Listener = (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
        {
            if (printerSettings != null)
            {
                printerSettings.setFilament1(newValue);
            }
        };

        filament0.addListener(filament0Listener);
        filament1.addListener(filament1Listener);

        printerSettings.setFilament0(filament0.get());
        printerSettings.setFilament1(filament1.get());
    }

    private void setupOverrides()
    {
        supportSlider.setLabelFormatter(new StringConverter<Double>()
        {
            @Override
            public String toString(Double n)
            {
                String returnedText = "";

                if (n <= 0)
                {
                    returnedText = Lookup.i18n("sidePanel_settings.supportMaterialNo");
                } else
                {
                    returnedText = Lookup.i18n("sidePanel_settings.supportMaterialAuto");
                }
                return returnedText;
            }

            @Override
            public Double fromString(String s)
            {
                double returnVal = 0;

                if (s.equals(Lookup.i18n("sidePanel_settings.supportMaterialNo")))
                {
                    returnVal = 0;
                } else if (s.equals(Lookup.i18n("sidePanel_settings.supportMaterialAuto")))
                {
                    returnVal = 1;
                }
                return returnVal;
            }
        }
        );

        supportSlider.valueProperty().addListener(
            (ObservableValue<? extends Number> ov, Number lastSupportValue, Number newSupportValue) ->
            {
                    if (lastSupportValue != newSupportValue)
                    {
                        boolean supportSelected = (newSupportValue.doubleValue() >= 1.0);
                        printerSettings.setPrintSupportOverride(supportSelected);
                    }
            });

        fillDensitySlider.valueProperty()
            .addListener(
                (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                {
                        printerSettings.setFillDensityOverride(newValue.floatValue() / 100.0f);
                });

        brimSlider.valueProperty().addListener(
            (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
            {
                    if (newValue != oldValue)
                    {
                        printerSettings.setBrimOverride(newValue.intValue());
                    }
            });
    }

    private ChangeListener<Filament> materialFilament0Listener = (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
    {
        filament0.set(newValue);
    };

    private ChangeListener<Filament> materialFilament1Listener = (ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue) ->
    {
        filament1.set(newValue);
    };

    /**
     * Show the correct number of MaterialComponents according to the number of extruders, and
     * configure them to the printer and extruder number. Listen to changes on the chosen filament
     * and update the settingsScreenState accordingly.
     */
    private void configureMaterialComponents(Printer printer)
    {
        for (Node materialNode : materialContainer.getChildren())
        {
            MaterialComponent previousMaterialComponent = (MaterialComponent) materialNode;
            previousMaterialComponent.getSelectedFilamentProperty().removeListener(
                materialFilament0Listener);
            previousMaterialComponent.getSelectedFilamentProperty().removeListener(
                materialFilament1Listener);
        }

        materialContainer.getChildren().clear();
        for (int extruderNumber = 0; extruderNumber < 2; extruderNumber++)
        {
            Extruder extruder = printer.extrudersProperty().get(extruderNumber);
            if (extruder.isFittedProperty().get())
            {
                MaterialComponent materialComponent
                    = new MaterialComponent(MaterialComponent.Mode.SETTINGS, printer, extruderNumber);
                materialContainer.getChildren().add(materialComponent);

                if (extruderNumber == 0)
                {
                    materialComponent.getSelectedFilamentProperty().addListener(
                        materialFilament0Listener);
                } else
                {
                    materialComponent.getSelectedFilamentProperty().addListener(
                        materialFilament1Listener);
                }

                if (materialComponent.getSelectedFilamentProperty().get() != null)
                {
                    // the materialComponent has detected a reel and set its filament
                    // accordingly.
                    if (extruderNumber == 0)
                    {
                        filament0.set(materialComponent.getSelectedFilamentProperty().get());
                    } else
                    {
                        filament1.set(materialComponent.getSelectedFilamentProperty().get());
                    }
                } else
                {
                    // use printer settings value as the default because there is no reel
                    // loaded
                    if (currentProject != null)
                    {
                        if (extruderNumber == 0)
                        {
                            materialComponent.setSelectedFilamentInComboBox(
                                currentProject.getPrinterSettings().getFilament0());
                        } else
                        {
                            materialComponent.setSelectedFilamentInComboBox(
                                currentProject.getPrinterSettings().getFilament1());
                        }
                    }
                }
            }
        }
    }

    private ChangeListener<Boolean> extruder0Listener;
    private ChangeListener<Boolean> extruder1Listener;

    private void bindPrinter(Printer printer)
    {
        // in case the extruder is added to the model after the printer is detected
        Extruder extruder0 = printer.extrudersProperty().get(0);
        if (extruder0 != null)
        {
            extruder0Listener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                configureMaterialComponents(printer);
            };
            extruder0.isFittedProperty().addListener(extruder0Listener);
        }

        Extruder extruder1 = printer.extrudersProperty().get(1);
        if (extruder1 != null)
        {
            extruder1Listener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
            {
                configureMaterialComponents(printer);
            };
            extruder1.isFittedProperty().addListener(extruder1Listener);
        }
    }

    private void unbindPrinter(Printer printer)
    {
        if (extruder0Listener != null)
        {
            Extruder extruder0 = printer.extrudersProperty().get(0);
            extruder0.isFittedProperty().removeListener(extruder0Listener);
        }
        if (extruder1Listener != null)
        {
            Extruder extruder1 = printer.extrudersProperty().get(1);
            extruder1.isFittedProperty().removeListener(extruder1Listener);
        }
    }

    private void setupQualityOverrideControls(PrinterSettings printerSettings)
    {
        fillDensitySlider.setValue(printerSettings.getFillDensityOverride() * 100.0);
        brimSlider.setValue(printerSettings.getBrimOverride());
        supportSlider.setValue(printerSettings.getPrintSupportOverride() ? 1 : 0);
    }

    private void updateProfileList()
    {
        SlicerParametersFile currentSelection = customProfileChooser.getSelectionModel().
            getSelectedItem();

        availableProfiles.clear();
        availableProfiles.addAll(SlicerParametersContainer.getUserProfileList());
        availableProfiles.add(SlicerParametersContainer.createNewProfile);

        if (currentSelection != null && availableProfiles.contains(currentSelection)
            && currentSelection != SlicerParametersContainer.createNewProfile)
        {
            customProfileChooser.getSelectionModel().select(currentSelection);
        } else if (customProfileChooser.getItems().size() > 1)
        {
            // Only pick the first element if there is something to select
            // If size == 1 then we only have the Create new filament entry
            customProfileChooser.getSelectionModel().selectFirst();
        }
    }

    @Override
    public void configure(Initializable slideOutController)
    {
        this.slideOutController = (SettingsSlideOutPanelController) slideOutController;
        updateProfileList();
        this.slideOutController.updateProfileData(draftSettings);
    }

    private void selectPrintProfileByName(String profileNameToSave)
    {
        for (SlicerParametersFile settings : availableProfiles)
        {

            if (settings.getProfileName() != null && settings.getProfileName().equals(
                profileNameToSave))
            {
                customProfileChooser.getSelectionModel().select(settings);
                break;
            }
        }
    }

    private void whenProjectChanged(Project project)
    {
        removeFilamentListeners();

        currentProject = project;
        printerSettings = project.getPrinterSettings();

        int saveBrim = printerSettings.getBrimOverride();
        float saveFillDensity = printerSettings.getFillDensityOverride();
        boolean saveSupports = printerSettings.getPrintSupportOverride();

        qualityChooser.setValue(project.getPrintQuality().getEnumPosition());
        // UGH quality chooser has (rightly) stamped on the overrides so restore them
        printerSettings.setBrimOverride(saveBrim);
        printerSettings.setFillDensityOverride(saveFillDensity);
        printerSettings.setPrintSupportOverride(saveSupports);

        setupQualityOverrideControls(printerSettings);

        if (project.getPrintQuality() == PrintQualityEnumeration.CUSTOM)
        {
            if (project.getPrinterSettings().getSettingsName().length() > 0)
            {
                SlicerParametersFile chosenProfile = SlicerParametersContainer.
                    getSettingsByProfileName(
                        project.getPrinterSettings().getSettingsName());
                customProfileChooser.getSelectionModel().select(chosenProfile);
            }
        }
        if (printerSettings.getSelectedPrinter() == null && currentPrinter.get() != null)
        {
            printerSettings.setSelectedPrinter(currentPrinter.get());
        }

        printerSettings.setFilament0(filament0.get());
        printerSettings.setFilament0(filament1.get());

        setupFilamentListeners();
    }

    private void printQualityUpdate(PrintQualityEnumeration quality)
    {
        SlicerParametersFile settings = null;

        switch (quality)
        {
            case DRAFT:
                settings = draftSettings;
                customProfileVBox.setVisible(false);
                break;
            case NORMAL:
                settings = normalSettings;
                customProfileVBox.setVisible(false);
                break;
            case FINE:
                settings = fineSettings;
                customProfileVBox.setVisible(false);
                break;
            case CUSTOM:
                if (applicationStatus.getMode() == ApplicationMode.SETTINGS)
                {
                    displayManager.slideOutAdvancedPanel();
                }
                customProfileVBox.setVisible(true);
                suppressCustomProfileChangeTriggers = true;
                customProfileChooser.getSelectionModel().select(settings);
                suppressCustomProfileChangeTriggers = false;
                break;
            default:
                break;
        }

        if (slideOutController != null)
        {
            slideOutController.updateProfileData(settings);
            slideOutController.showProfileTab();
        }

        if (currentProject != null)
        {
            printerSettings.setPrintQuality(quality);
            if (quality != PrintQualityEnumeration.CUSTOM)
            {
                printerSettings.setBrimOverride(settings.getBrimWidth_mm());
                printerSettings.setFillDensityOverride(settings.getFillDensity_normalised());
                printerSettings.setPrintSupportOverride(settings.getGenerateSupportMaterial());
                setupQualityOverrideControls(printerSettings);
            }
        }
    }

    @Override
    public void whenPrinterAdded(Printer printer)
    {
    }

    @Override
    public void whenPrinterRemoved(Printer printer)
    {
    }

    @Override
    public void whenHeadAdded(Printer printer)
    {
    }

    @Override
    public void whenHeadRemoved(Printer printer, Head head)
    {
    }

    @Override
    public void whenReelAdded(Printer printer, int reelIndex)
    {
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelNumber)
    {
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
    }

    @Override
    public void whenExtruderAdded(Printer printer, int extruderIndex)
    {
        if (printer == currentPrinter.get())
        {
            configureMaterialComponents(printer);
        }
    }

    @Override
    public void whenExtruderRemoved(Printer printer, int extruderIndex)
    {
        if (printer == currentPrinter.get())
        {
            configureMaterialComponents(printer);
        }
    }
}
