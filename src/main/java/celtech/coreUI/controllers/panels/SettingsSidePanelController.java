package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.FilamentContainer;
import celtech.configuration.MaterialType;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.FillPattern;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.MaterialChoiceListCell;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.ProfileChoiceListCell;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.coreUI.controllers.popups.PopupCommandReceiver;
import celtech.coreUI.controllers.utilityPanels.MaterialDetailsController;
import celtech.coreUI.controllers.utilityPanels.ProfileDetailsController;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.printerControl.model.Reel;
import celtech.services.slicer.PrintQualityEnumeration;
import static celtech.utils.DeDuplicator.suggestNonDuplicateName;
import celtech.utils.PrinterListChangesListener;
import celtech.utils.SystemUtils;
import java.net.URL;
import java.util.Collection;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;
import jfxtras.styles.jmetro8.ToggleSwitch;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SettingsSidePanelController implements Initializable, SidePanelManager,
    PopupCommandReceiver, PrinterListChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        SettingsSidePanelController.class.getName());
    private ObservableList<Printer> printerStatusList = null;
    private SettingsScreenState settingsScreenState = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;

    private boolean suppressQualityOverrideTriggers = false;
    private boolean suppressCustomProfileChangeTriggers = false;

    @FXML
    private Label materialLabel;

    @FXML
    private Label printQualityLabel;

    @FXML
    private Slider brimSlider;

    @FXML
    private ComboBox<Filament> materialChooser;

    @FXML
    private ComboBox<SlicerParametersFile> customProfileChooser;

    @FXML
    private Slider supportSlider;

    @FXML
    private Slider qualityChooser;

    @FXML
    private Label customSettingsLabel;

    @FXML
    private VBox customProfileVBox;

    @FXML
    private ComboBox<Printer> printerChooser;

    @FXML
    private Slider fillDensitySlider;

    @FXML
    private VBox nonCustomProfileVBox;

//    @FXML
//    private ToggleSwitch spiralPrintToggle;
    private SlicerParametersFile draftSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.draftSettingsProfileName);
    private SlicerParametersFile normalSettings = SlicerParametersContainer.
        getSettingsByProfileName(
            ApplicationConfiguration.normalSettingsProfileName);
    private SlicerParametersFile fineSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.fineSettingsProfileName);
    private SlicerParametersFile customSettings = null;
    private SlicerParametersFile lastSettings = null;

    private ChangeListener<Toggle> nozzleSelectionListener = null;

    private ObservableList<Filament> availableFilaments = FXCollections.observableArrayList();
    private ObservableList<SlicerParametersFile> availableProfiles = FXCollections.
        observableArrayList();

    private Printer currentPrinter = null;
    private Filament currentlyLoadedFilament = null;
    private Filament lastFilamentSelected = null;

    private VBox createMaterialPage = null;
    private ModalDialog createMaterialDialogue = null;
    private int saveMaterialAction = 0;
    private int cancelMaterialSaveAction = 0;
    private boolean inhibitMaterialSelection = false;

    private VBox createProfilePage = null;
    private ModalDialog createProfileDialogue = null;
    private int saveProfileAction = 0;
    private int cancelProfileSaveAction = 0;
    private SlicerParametersFile lastCustomProfileSelected = null;

    private SettingsSlideOutPanelController slideOutController = null;

    private MaterialDetailsController materialDetailsController = null;
    private ProfileDetailsController profileDetailsController = null;

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
        printerStatusList = Lookup.getConnectedPrinters();

        try
        {
            FXMLLoader createMaterialPageLoader = new FXMLLoader(getClass().getResource(
                ApplicationConfiguration.fxmlUtilityPanelResourcePath + "materialDetails.fxml"),
                                                                 Lookup.getLanguageBundle());
            createMaterialPage = createMaterialPageLoader.load();
            materialDetailsController = createMaterialPageLoader.getController();
            materialDetailsController.updateMaterialData(new Filament("", MaterialType.ABS, null,
                                                                      0, 0, 0, 0, 0, 0, 0, 0,
                                                                      Color.ALICEBLUE, true));
            materialDetailsController.showButtons(false);

            createMaterialDialogue = new ModalDialog(Lookup.i18n(
                "sidePanel_settings.createMaterialDialogueTitle"));
            createMaterialDialogue.setContent(createMaterialPage);
            saveMaterialAction = createMaterialDialogue.addButton(
                Lookup.i18n("genericFirstLetterCapitalised.Save"),
                materialDetailsController.
                getProfileNameInvalidProperty());
            cancelMaterialSaveAction = createMaterialDialogue.addButton(
                Lookup.i18n("genericFirstLetterCapitalised.Cancel"));

        } catch (Exception ex)
        {
            steno.error("Failed to load material creation page");
        }

        FilamentContainer.getUserFilamentList().addListener(new ListChangeListener<Filament>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends Filament> c)
            {
                updateFilamentList();
            }
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
            saveProfileAction = createProfileDialogue.addButton(
                Lookup.i18n("genericFirstLetterCapitalised.Save"),
                profileDetailsController.
                getProfileNameInvalidProperty());
            cancelProfileSaveAction = createProfileDialogue.addButton(
                Lookup.i18n("genericFirstLetterCapitalised.Cancel"));
        } catch (Exception ex)
        {
            steno.error("Failed to load profile creation page");
        }

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
//        // Hack to force slider font, don't know why we need this
//        String primaryFontFamily = displayManager.getPrimaryFontFamily();
//        qualityChooser.setStyle("-fx-tick-label-font-family: " + primaryFontFamily + ";");

        settingsScreenState.setPrintQuality(PrintQualityEnumeration.DRAFT);
        settingsScreenState.setSettings(draftSettings);

        setupQualityOverrideControls(settingsScreenState.getSettings());

        printQualityUpdate(PrintQualityEnumeration.DRAFT);

        qualityChooser.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number lastQualityValue,
                Number newQualityValue)
            {
                if (lastQualityValue != newQualityValue)
                {
                    PrintQualityEnumeration quality = PrintQualityEnumeration.fromEnumPosition(
                        newQualityValue.intValue());

                    printQualityUpdate(quality);
                }
            }
        });

        Callback<ListView<SlicerParametersFile>, ListCell<SlicerParametersFile>> profileChooserCellFactory
            = (ListView<SlicerParametersFile> list) -> new ProfileChoiceListCell();

        customProfileChooser.setCellFactory(profileChooserCellFactory);
        customProfileChooser.setButtonCell(profileChooserCellFactory.call(null));
        customProfileChooser.setItems(availableProfiles);

        updateProfileList();

        customProfileChooser.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<SlicerParametersFile>()
            {
                @Override
                public void changed(ObservableValue<? extends SlicerParametersFile> observable,
                    SlicerParametersFile oldValue, SlicerParametersFile newValue)
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

                            lastCustomProfileSelected = newValue;
                        }

                        if (newValue == SlicerParametersContainer.createNewProfile)
                        {
                            showCreateProfileDialogue(draftSettings.clone());
                        } else if (newValue != null)
                        {
                            if (settingsScreenState.getPrintQuality()
                            == PrintQualityEnumeration.CUSTOM)
                            {
                                slideOutController.updateProfileData(newValue);
                                settingsScreenState.setSettings(newValue);
                                DisplayManager.getInstance().getCurrentlyVisibleProject().
                                setCustomProfileName(
                                    newValue.getProfileName());
                            }
                            customSettings = newValue;
                        } else if (newValue == null && settingsScreenState.getPrintQuality()
                        == PrintQualityEnumeration.CUSTOM)
                        {
                            slideOutController.updateProfileData(null);
                            customSettings = null;
                        }
                    }
                }
            });

        SlicerParametersContainer.getUserProfileList().addListener(
            (ListChangeListener.Change<? extends SlicerParametersFile> c) ->
            {
                updateProfileList();
            });

        printerChooser.setCellFactory(
            new Callback<ListView<Printer>, ListCell<Printer>>()
            {
                @Override
                public ListCell<Printer> call(ListView<Printer> param)
                {
                    final ListCell<Printer> cell = new ListCell<Printer>()
                    {
                        {
                            super.setPrefWidth(100);
                        }

                        @Override
                        public void updateItem(Printer item,
                            boolean empty)
                        {
                            super.updateItem(item, empty);
                            if (item != null)
                            {
                                setText(
                                    item.getPrinterIdentity().printerFriendlyNameProperty().get());
                            } else
                            {
                                setText(null);
                            }
                        }
                    };
                    return cell;
                }
            });

        printerChooser.setItems(printerStatusList);

        printerChooser.getSelectionModel()
            .clearSelection();

        printerChooser.getItems().addListener(
            (ListChangeListener.Change<? extends Printer> change) ->
            {
                while (change.next())
                {
                    if (change.wasAdded())
                    {
                        for (Printer addedPrinter : change.getAddedSubList())
                        {
                            printerChooser.setValue(addedPrinter);
                        }
                    } else if (change.wasRemoved())
                    {
                        for (Printer removedPrinter : change.getRemoved())
                        {
                            if (printerChooser.getItems().isEmpty())
                            {
                                printerChooser.getSelectionModel().select(null);
                            } else
                            {
                                printerChooser.getSelectionModel().selectFirst();
                            }
                        }
                    } else if (change.wasReplaced())
                    {
                        steno.info("Replace");
                    } else if (change.wasUpdated())
                    {
                        steno.info("Update");

                    }
                }
            });

        printerChooser.getSelectionModel()
            .selectedItemProperty().addListener(new ChangeListener<Printer>()
                {
                    @Override
                    public void changed(ObservableValue<? extends Printer> ov,
                        Printer lastSelectedPrinter, Printer selectedPrinter
                    )
                    {
                        if (lastSelectedPrinter != null)
                        {
                        }
                        if (selectedPrinter != null && selectedPrinter != lastSelectedPrinter)
                        {
                            currentPrinter = selectedPrinter;
                        }

                        if (selectedPrinter == null)
                        {
                            currentPrinter = null;
                        }

                        settingsScreenState.setSelectedPrinter(selectedPrinter);

                    }
            }
            );

        Callback<ListView<Filament>, ListCell<Filament>> materialChooserCellFactory
            = (ListView<Filament> list) -> new MaterialChoiceListCell();

        materialChooser.setCellFactory(materialChooserCellFactory);
        materialChooser.setButtonCell(new MaterialChoiceListCell());
        materialChooser.setItems(availableFilaments);

        materialChooser.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<Filament>()
            {
                @Override
                public void changed(ObservableValue<? extends Filament> observable,
                    Filament oldValue, Filament newValue)
                {
                    if (oldValue != newValue)
                    {
                        if (slideOutController != null)
                        {
                            slideOutController.showMaterialTab();
                        }
                        lastFilamentSelected = newValue;
                    }

                    if (inhibitMaterialSelection == false)
                    {
                        if (newValue == FilamentContainer.createNewFilament)
                        {
                            showCreateMaterialDialogue();
                        } else if (newValue == null)
                        {
                            if (slideOutController != null)
                            {
                                slideOutController.updateFilamentData(newValue);
                            }
                            settingsScreenState.setFilament(null);
                        } else
                        {
                            if (slideOutController != null)
                            {
                                slideOutController.updateFilamentData(newValue);
                            }
                            settingsScreenState.setFilament(newValue);
                        }
                    }
                }
            }
        );

        nonCustomProfileVBox.visibleProperty()
            .bind(qualityChooser.valueProperty().isNotEqualTo(
                    PrintQualityEnumeration.CUSTOM.getEnumPosition()));

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

        supportSlider.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number lastSupportValue,
                Number newSupportValue)
            {
                if (suppressQualityOverrideTriggers == false)
                {
                    if (lastSupportValue != newSupportValue)
                    {
                        boolean supportSelected = (newSupportValue.doubleValue() >= 1.0) ? true : false;
                        DisplayManager.getInstance().getCurrentlyVisibleProject().
                            projectModified();

                        settingsScreenState.getSettings().
                            setGenerateSupportMaterial(supportSelected);
                    }
                }
            }
        });

        fillDensitySlider.valueProperty()
            .addListener(new ChangeListener<Number>()
                {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable,
                        Number oldValue,
                        Number newValue
                    )
                    {
                        if (suppressQualityOverrideTriggers == false)
                        {
                            if (newValue != oldValue)
                            {
                                DisplayManager.getInstance().getCurrentlyVisibleProject().
                                projectModified();
                            }

                            settingsScreenState.getSettings().setFillDensity_normalised(
                                newValue.floatValue() / 100.0f);
                        }
                    }
            }
            );

        brimSlider.valueProperty()
            .addListener(new ChangeListener<Number>()
                {
                    @Override
                    public void changed(ObservableValue<? extends Number> observable,
                        Number oldValue,
                        Number newValue
                    )
                    {
                        if (suppressQualityOverrideTriggers == false)
                        {
                            if (newValue != oldValue)
                            {
                                DisplayManager.getInstance().getCurrentlyVisibleProject().
                                projectModified();
                            }

                            settingsScreenState.getSettings().setBrimWidth_mm(newValue.intValue());
                        }
                    }
            }
            );

//        spiralPrintToggle.selectedProperty().addListener(new ChangeListener<Boolean>()
//        {
//            @Override
//            public void changed(
//                ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
//            {
//                if (suppressQualityOverrideTriggers == false)
//                {
//                    if (newValue != oldValue)
//                    {
//                        DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
//                    }
//
//                    settingsScreenState.getSettings().setSpiral_vase(newValue);
//                }
//            }
//        });
//
//        updateFilamentList();
        Lookup.getPrinterListChangesNotifier()
            .addListener(this);
    }

    private void setupQualityOverrideControls(SlicerParametersFile settings)
    {
        supportSlider.setValue((settings.getGenerateSupportMaterial()==true)?1.0:0.0);
        
        fillDensitySlider.setValue(settings.getFillDensity_normalised() * 100.0);
        if (settings.getFillPattern().equals(FillPattern.LINE))
        {
            fillDensitySlider.setMax(99);
        } else
        {
            fillDensitySlider.setMax(100);
        }
        brimSlider.setValue(settings.getBrimWidth_mm());
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

    private void updateFilamentList()
    {
        Filament currentSelection = materialChooser.getSelectionModel().getSelectedItem();

        availableFilaments.clear();

        if (currentlyLoadedFilament != null)
        {
            availableFilaments.add(currentlyLoadedFilament);
        }

        availableFilaments.addAll(FilamentContainer.getUserFilamentList());
        availableFilaments.add(FilamentContainer.createNewFilament);

        if (currentSelection != null && availableFilaments.contains(currentSelection)
            && currentSelection != FilamentContainer.createNewFilament)
        {
            materialChooser.getSelectionModel().select(currentSelection);
        } else if (materialChooser.getItems().size() > 1)
        {
            // Only pick the first element if there is something to select
            // If size == 1 then we only have the Create new filament entry
            materialChooser.getSelectionModel().selectFirst();
        }
    }

    private void populatePrinterChooser()
    {
        for (Printer printer : printerStatusList)
        {
            printerChooser.getItems().add(printer);
        }
    }

    /**
     *
     * @param slideOutController
     */
    @Override
    public void configure(Initializable slideOutController)
    {
        this.slideOutController = (SettingsSlideOutPanelController) slideOutController;
        this.slideOutController.provideReceiver(this);
        this.slideOutController.updateFilamentData(settingsScreenState.getFilament());
        updateFilamentList();
        if (availableFilaments.size() > 1)
        {
            materialChooser.getSelectionModel().selectFirst();
        }
        updateProfileList();
        this.slideOutController.updateProfileData(draftSettings);
    }

    /**
     *
     * @param source
     */
    @Override
    public void triggerSaveAs(Object source)
    {
        if (source instanceof MaterialDetailsController)
        {
            Filament clonedFilament = null;
            if (settingsScreenState.getFilament() != null)
            {
                clonedFilament = settingsScreenState.getFilament().clone();
            } else
            {
                // We must be trying to save the loaded filament...
                clonedFilament = currentlyLoadedFilament.clone();
            }
            String originalFilamentName = clonedFilament.getFriendlyFilamentName();
            String filename = SystemUtils.getIncrementalFilenameOnly(
                ApplicationConfiguration.getUserFilamentDirectory(), originalFilamentName,
                ApplicationConfiguration.filamentFileExtension);
            clonedFilament.setFriendlyFilamentName(filename);
            clonedFilament.setMutable(true);
            materialDetailsController.updateMaterialData(clonedFilament);
            showCreateMaterialDialogue();
        } else if (source instanceof ProfileDetailsController)
        {
            SlicerParametersFile settings = settingsScreenState.getSettings().clone();
            String originalProfileName = settings.getProfileName();
            String filename = SystemUtils.getIncrementalFilenameOnly(
                ApplicationConfiguration.getUserPrintProfileDirectory(), originalProfileName,
                ApplicationConfiguration.printProfileFileExtension);
            settings.setProfileName(filename);
            profileDetailsController.updateProfileData(settings);
            showCreateProfileDialogue(settings);
        }
    }

    /**
     *
     * @param profile
     * @param source
     */
    @Override
    public void triggerSave(Object profile)
    {
        if (profile instanceof Filament)
        {
            Filament filamentToSave = (Filament) profile;

            FilamentContainer.saveFilament(filamentToSave);
            Filament chosenFilament = FilamentContainer.getFilamentByID(
                filamentToSave.getFilamentID());
            materialChooser.getSelectionModel().select(chosenFilament);
        } else if (profile instanceof SlicerParametersFile)
        {
            SlicerParametersFile profiletoSave = (SlicerParametersFile) profile;
            SlicerParametersContainer.saveProfile(profiletoSave);
            selectPrintProfileByName(profiletoSave.getProfileName());
            if (displayManager != null)
            {
                projectChanged(displayManager.getCurrentlyVisibleProject());
            }
        }
    }

    private void showCreateMaterialDialogue()
    {
        Platform.runLater(new Runnable()
        {

            @Override
            public void run()
            {
                int response = createMaterialDialogue.show();
                if (response == saveMaterialAction)
                {
                    Filament filamentToSave = materialDetailsController.getMaterialData();
                    FilamentContainer.saveFilament(filamentToSave);

                    Filament chosenFilament = FilamentContainer.getFilamentByID(
                        filamentToSave.getFilamentID());
                    materialChooser.getSelectionModel().select(chosenFilament);
                } else
                {
                    if (lastFilamentSelected != null)
                    {
                        if (lastFilamentSelected == FilamentContainer.createNewFilament)
                        {
                            materialChooser.getSelectionModel().clearSelection();
                        } else
                        {
                            materialChooser.getSelectionModel().select(lastFilamentSelected);
                        }
                    }
                }
            }
        });
    }

    private int showCreateProfileDialogue(SlicerParametersFile dataToUse)
    {
        profileDetailsController.updateProfileData(dataToUse);
        profileDetailsController.setNameEditable(true);
        int response = createProfileDialogue.show();
        if (response == saveProfileAction)
        {
            String profileNameToSave = profileDetailsController.getProfileName();
            SlicerParametersFile settingsToSave = profileDetailsController.getProfileData();
            Collection<String> profileNames = SlicerParametersContainer.getProfileNames();
            profileNameToSave = suggestNonDuplicateName(profileNameToSave, profileNames);
            settingsToSave.setProfileName(profileNameToSave);
            SlicerParametersContainer.saveProfile(settingsToSave);
            updateProfileList();
            selectPrintProfileByName(profileNameToSave);
            qualityChooser.adjustValue(PrintQualityEnumeration.CUSTOM.getEnumPosition());
        } else
        {
            if (lastCustomProfileSelected != null)
            {
                if (lastCustomProfileSelected == SlicerParametersContainer.createNewProfile)
                {
                    customProfileChooser.getSelectionModel().clearSelection();
                } else
                {
                    customProfileChooser.getSelectionModel().select(lastCustomProfileSelected);
                }
            }
        }

        return response;
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

    public void projectChanged(Project project)
    {
        if (project.getPrintQuality() != null)
        {
            if (project.getPrintQuality() != settingsScreenState.getPrintQuality())
            {
                qualityChooser.setValue(project.getPrintQuality().getEnumPosition());
            }
        }

        if (project.getCustomProfileName() != null)
        {
            if (customSettings == null || project.getCustomProfileName().equals(
                customSettings.getProfileName()) == false)
            {
                SlicerParametersFile chosenProfile = SlicerParametersContainer.
                    getSettingsByProfileName(
                        project.getCustomProfileName());
                customProfileChooser.getSelectionModel().select(chosenProfile);
            }
        }

    }

    private void printQualityUpdate(PrintQualityEnumeration quality)
    {
        DisplayManager displayManager = DisplayManager.getInstance();
        Project currentlyVisibleProject = null;

        if (displayManager != null)
        {
            currentlyVisibleProject = displayManager.getCurrentlyVisibleProject();
        }

        settingsScreenState.setPrintQuality(quality);

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
                settings = customSettings;
                customProfileVBox.setVisible(true);
                suppressCustomProfileChangeTriggers = true;
                customProfileChooser.getSelectionModel().select(settings);
                suppressCustomProfileChangeTriggers = false;
                break;
            default:
                break;
        }

        if (settings != null)
        {
            suppressQualityOverrideTriggers = true;
            setupQualityOverrideControls(settings);
            suppressQualityOverrideTriggers = false;

            if (currentlyVisibleProject != null)
            {
                currentlyVisibleProject.setPrintQuality(quality);
            }
        }

        if (slideOutController != null)
        {
            slideOutController.updateProfileData(settings);
            slideOutController.showProfileTab();
        }

        if (currentlyVisibleProject != null)
        {
            currentlyVisibleProject.projectModified();
        }

        settingsScreenState.setSettings(settings);
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
        //TODO modify for multiple reels
        if (printer == currentPrinter)
        {
            currentlyLoadedFilament = new Filament(currentPrinter.reelsProperty().get(0));
            updateFilamentList();
            materialChooser.getSelectionModel().select(currentlyLoadedFilament);
        }
    }

    @Override
    public void whenReelRemoved(Printer printer, Reel reel, int reelNumber)
    {
        currentlyLoadedFilament = null;
        updateFilamentList();
    }

    @Override
    public void whenReelChanged(Printer printer, Reel reel)
    {
    }
}
