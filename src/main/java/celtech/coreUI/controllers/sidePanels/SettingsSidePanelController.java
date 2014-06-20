package celtech.coreUI.controllers.sidePanels;

import celtech.appManager.ApplicationStatus;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.FilamentContainer;
import celtech.configuration.MaterialType;
import celtech.configuration.PrintProfileContainer;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.MaterialChoiceListCell;
import celtech.coreUI.components.ModalDialog;
import celtech.coreUI.components.ProfileChoiceListCell;
import celtech.coreUI.controllers.SettingsScreenState;
import celtech.coreUI.controllers.popups.PopupCommandReceiver;
import celtech.coreUI.controllers.utilityPanels.MaterialDetailsController;
import celtech.coreUI.controllers.utilityPanels.ProfileDetailsController;
import celtech.printerControl.Printer;
import celtech.printerControl.comms.RoboxCommsManager;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.RoboxProfile;
import celtech.utils.SystemUtils;
import java.net.URL;
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
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.StringConverter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SettingsSidePanelController implements Initializable, SidePanelManager, PopupCommandReceiver
{

    private Stenographer steno = StenographerFactory.getStenographer(SettingsSidePanelController.class.getName());
    private ObservableList<Printer> printerStatusList = null;
    private SettingsScreenState settingsScreenState = null;
    private ApplicationStatus applicationStatus = null;
    private DisplayManager displayManager = null;

    @FXML
    private ComboBox<Printer> printerChooser;

    @FXML
    private ComboBox<Filament> materialChooser;

    @FXML
    private ComboBox<RoboxProfile> customProfileChooser;

    @FXML
    private Label customSettingsLabel;

    @FXML
    private Slider qualityChooser;

    @FXML
    private VBox supportVBox;

    @FXML
    private VBox customProfileVBox;

    @FXML
    private ToggleGroup supportMaterialGroup;

    @FXML
    private RadioButton noSupportRadioButton;

    @FXML
    private RadioButton autoSupportRadioButton;

    @FXML
    void go(MouseEvent event)
    {
        settingsScreenState.getSettings().writeToFile("/tmp/settings.dat");
    }

    private RoboxProfile draftSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.draftSettingsProfileName);
    private RoboxProfile normalSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.normalSettingsProfileName);
    private RoboxProfile fineSettings = PrintProfileContainer.getSettingsByProfileName(ApplicationConfiguration.fineSettingsProfileName);
    private RoboxProfile customSettings = null;
    private RoboxProfile lastSettings = null;

    private ChangeListener<Toggle> nozzleSelectionListener = null;
    private ChangeListener<Boolean> reelDataChangedListener = null;

    private ObservableList<Filament> availableFilaments = FXCollections.observableArrayList();
    private ObservableList<RoboxProfile> availableProfiles = FXCollections.observableArrayList();

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
    private RoboxProfile lastCustomProfileSelected = null;

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
        printerStatusList = RoboxCommsManager.getInstance().getPrintStatusList();

        try
        {
            FXMLLoader createMaterialPageLoader = new FXMLLoader(getClass().getResource(ApplicationConfiguration.fxmlUtilityPanelResourcePath + "materialDetails.fxml"), DisplayManager.getLanguageBundle());
            createMaterialPage = createMaterialPageLoader.load();
            materialDetailsController = createMaterialPageLoader.getController();
            materialDetailsController.updateMaterialData(new Filament("", MaterialType.ABS, null,
                                                                      0, 0, 0, 0, 0, 0, 0, 0, Color.ALICEBLUE, true));
            materialDetailsController.showButtons(false);

            createMaterialDialogue = new ModalDialog(DisplayManager.getLanguageBundle().getString("sidePanel_settings.createMaterialDialogueTitle"));
            createMaterialDialogue.setContent(createMaterialPage);
            saveMaterialAction = createMaterialDialogue.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Save"), materialDetailsController.getProfileNameInvalidProperty());
            cancelMaterialSaveAction = createMaterialDialogue.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Cancel"));

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
            FXMLLoader createProfilePageLoader = new FXMLLoader(getClass().getResource(ApplicationConfiguration.fxmlUtilityPanelResourcePath + "profileDetails.fxml"), DisplayManager.getLanguageBundle());
            createProfilePage = createProfilePageLoader.load();
            profileDetailsController = createProfilePageLoader.getController();
            profileDetailsController.showButtons(false);

            createProfileDialogue = new ModalDialog(DisplayManager.getLanguageBundle().getString("sidePanel_settings.createProfileDialogueTitle"));
            createProfileDialogue.setContent(createProfilePage);
            saveProfileAction = createProfileDialogue.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Save"), profileDetailsController.getProfileNameInvalidProperty());
            cancelProfileSaveAction = createProfileDialogue.addButton(DisplayManager.getLanguageBundle().getString("genericFirstLetterCapitalised.Cancel"));
        } catch (Exception ex)
        {
            steno.error("Failed to load profile creation page");
        }

        qualityChooser.setLabelFormatter(new StringConverter<Double>()
        {
            @Override
            public String toString(Double n)
            {
                PrintQualityEnumeration selectedQuality = PrintQualityEnumeration.fromEnumPosition(n.intValue());
                return selectedQuality.getFriendlyName();
            }

            @Override
            public Double fromString(String s)
            {
                PrintQualityEnumeration selectedQuality = PrintQualityEnumeration.valueOf(s);
                return (double) selectedQuality.getEnumPosition();
            }
        });
        // Hack to force slider font, don't know why we need this
        String primaryFontFamily = displayManager.getPrimaryFontFamily();
        qualityChooser.setStyle("-fx-tick-label-font-family: " + primaryFontFamily + ";");

        settingsScreenState.setPrintQuality(PrintQualityEnumeration.DRAFT);
        settingsScreenState.setSettings(draftSettings);
        if (draftSettings.support_materialProperty().get() == true)
        {
            autoSupportRadioButton.selectedProperty().set(true);
        } else
        {
            noSupportRadioButton.selectedProperty().set(true);
        }

        qualityChooser.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number lastQualityValue, Number newQualityValue)
            {
                if (newQualityValue != lastQualityValue)
                {
                    DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
                    slideOutController.showProfileTab();
                }

                PrintQualityEnumeration quality = PrintQualityEnumeration.fromEnumPosition(newQualityValue.intValue());
                settingsScreenState.setPrintQuality(quality);

                RoboxProfile settings = null;

                switch (quality)
                {
                    case DRAFT:
                        settings = draftSettings;
                        break;
                    case NORMAL:
                        settings = normalSettings;
                        break;
                    case FINE:
                        settings = fineSettings;
                        break;
                    case CUSTOM:
                        if (newQualityValue != lastQualityValue)
                        {
                            displayManager.slideOutAdvancedPanel();
                        }
                        settings = customSettings;
                        break;
                    default:
                        break;
                }

                if (settings != null)
                {
                    if (settings.support_materialProperty().get() == true)
                    {
                        autoSupportRadioButton.selectedProperty().set(true);
                    } else
                    {
                        noSupportRadioButton.selectedProperty().set(true);
                    }
                }

                slideOutController.updateProfileData(settings);
                settingsScreenState.setSettings(settings);
            }
        });

        qualityChooser.setValue(PrintQualityEnumeration.DRAFT.getEnumPosition());

        customProfileVBox.visibleProperty().bind(qualityChooser.valueProperty().isEqualTo(PrintQualityEnumeration.CUSTOM.getEnumPosition()));

        Callback<ListView<RoboxProfile>, ListCell<RoboxProfile>> profileChooserCellFactory
                = new Callback<ListView<RoboxProfile>, ListCell<RoboxProfile>>()
                {
                    @Override
                    public ListCell<RoboxProfile> call(ListView<RoboxProfile> list)
                    {
                        return new ProfileChoiceListCell();
                    }
                };

        customProfileChooser.setCellFactory(profileChooserCellFactory);
        customProfileChooser.setButtonCell(profileChooserCellFactory.call(null));
        customProfileChooser.setItems(availableProfiles);

        updateProfileList();

        customProfileChooser.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<RoboxProfile>()
        {
            @Override
            public void changed(ObservableValue<? extends RoboxProfile> observable, RoboxProfile oldValue, RoboxProfile newValue)
            {
                if (oldValue != newValue)
                {
                    displayManager.slideOutAdvancedPanel();
                    slideOutController.showProfileTab();

                    lastCustomProfileSelected = newValue;
                }

                if (newValue == PrintProfileContainer.createNewProfile)
                {
                    showCreateProfileDialogue(draftSettings.clone());
                }
                else if (newValue != null)
                {
                    slideOutController.updateProfileData(newValue);
                    customSettings = newValue;
                    settingsScreenState.setSettings(newValue);
                }
                else if (newValue == null && settingsScreenState.getPrintQuality() == PrintQualityEnumeration.CUSTOM)
                {
                    slideOutController.updateProfileData(null);
                }
            }
        });

        PrintProfileContainer.getUserProfileList().addListener(new ListChangeListener<RoboxProfile>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends RoboxProfile> c)
            {
                updateProfileList();
            }
        });
        printerChooser.setItems(printerStatusList);

        printerChooser.getSelectionModel()
                .clearSelection();

        printerChooser.getItems().addListener(new ListChangeListener<Printer>()
        {
            @Override
            public void onChanged(ListChangeListener.Change<? extends Printer> change
            )
            {
                while (change.next())
                {
                    if (change.wasAdded())
                    {
                        for (Printer addedPrinter : change.getAddedSubList())
                        {
                            Platform.runLater(new Runnable()
                            {

                                @Override
                                public void run()
                                {
                                    printerChooser.setValue(addedPrinter);
                                }
                            });
                        }
                    } else if (change.wasRemoved())
                    {
                        for (Printer removedPrinter : change.getRemoved())
                        {
                            if (printerChooser.getItems().isEmpty())
                            {
                                Platform.runLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        printerChooser.getSelectionModel().select(null);
                                    }
                                });
                            } else
                            {
                                Platform.runLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        printerChooser.getSelectionModel().selectFirst();
                                    }
                                });
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
            }
        }
        );

        printerChooser.getSelectionModel()
                .selectedItemProperty().addListener(new ChangeListener<Printer>()
                        {
                            @Override
                            public void changed(ObservableValue<? extends Printer> ov, Printer lastSelectedPrinter, Printer selectedPrinter
                            )
                            {
                                if (lastSelectedPrinter != null)
                                {
                                    lastSelectedPrinter.reelDataChangedProperty().removeListener(reelDataChangedListener);
                                }
                                if (selectedPrinter != null && selectedPrinter != lastSelectedPrinter)
                                {
                                    currentPrinter = selectedPrinter;
                                    selectedPrinter.reelDataChangedProperty().addListener(reelDataChangedListener);
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
                = new Callback<ListView<Filament>, ListCell<Filament>>()
                {
                    @Override
                    public ListCell<Filament> call(ListView<Filament> list)
                    {
                        return new MaterialChoiceListCell();
                    }
                };

        materialChooser.setCellFactory(materialChooserCellFactory);
        materialChooser.setButtonCell(new MaterialChoiceListCell());
        materialChooser.setItems(availableFilaments);

        materialChooser.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Filament>()
        {
            @Override
            public void changed(ObservableValue<? extends Filament> observable, Filament oldValue, Filament newValue)
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
                    } else if (newValue == null || currentPrinter == null || newValue == currentPrinter.loadedFilamentProperty().get())
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

        reelDataChangedListener = new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                currentlyLoadedFilament = currentPrinter.loadedFilamentProperty().get();
                updateFilamentList();
                materialChooser.getSelectionModel().select(currentlyLoadedFilament);
            }
        };

        supportVBox.visibleProperty()
                .bind(qualityChooser.valueProperty().isNotEqualTo(PrintQualityEnumeration.CUSTOM.getEnumPosition()));

        supportMaterialGroup.selectedToggleProperty()
                .addListener(new ChangeListener<Toggle>()
                        {
                            @Override
                            public void changed(ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue
                            )
                            {
                                if (newValue != oldValue)
                                {
                                    DisplayManager.getInstance().getCurrentlyVisibleProject().projectModified();
                                }

                                if (newValue == noSupportRadioButton)
                                {
                                    settingsScreenState.getSettings().setSupport_material(false);
                                } else if (newValue == autoSupportRadioButton)
                                {
                                    settingsScreenState.getSettings().setSupport_material(true);
                                }
                            }
                }
                );

        updateFilamentList();
    }

    private void updateProfileList()
    {
        RoboxProfile currentSelection = customProfileChooser.getSelectionModel().getSelectedItem();

        availableProfiles.clear();
        availableProfiles.addAll(PrintProfileContainer.getUserProfileList());
        availableProfiles.add(PrintProfileContainer.createNewProfile);

        if (currentSelection != null && availableProfiles.contains(currentSelection) && currentSelection != PrintProfileContainer.createNewProfile)
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

        if (currentSelection != null && availableFilaments.contains(currentSelection) && currentSelection != FilamentContainer.createNewFilament)
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
        this.slideOutController.updateProfileData(draftSettings);
        this.slideOutController.updateFilamentData(settingsScreenState.getFilament());
        updateFilamentList();
        if (availableFilaments.size() > 1)
        {
            materialChooser.getSelectionModel().selectFirst();
        }
        updateProfileList();
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
            String filename = SystemUtils.getIncrementalFilenameOnly(ApplicationConfiguration.getUserFilamentDirectory(), originalFilamentName, ApplicationConfiguration.filamentFileExtension);
            clonedFilament.setFriendlyFilamentName(filename);
            clonedFilament.setMutable(true);
            materialDetailsController.updateMaterialData(clonedFilament);
            showCreateMaterialDialogue();
        } else if (source instanceof ProfileDetailsController)
        {
            RoboxProfile settings = settingsScreenState.getSettings().clone();
            String originalProfileName = settings.getProfileName();
            String filename = SystemUtils.getIncrementalFilenameOnly(ApplicationConfiguration.getUserPrintProfileDirectory(), originalProfileName, ApplicationConfiguration.printProfileFileExtension);
            settings.getProfileNameProperty().set(filename);
            settings.setMutable(true);
            profileDetailsController.updateProfileData(settings);
            showCreateProfileDialogue(settings);
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

                    Filament chosenFilament = FilamentContainer.getFilamentByID(filamentToSave.getReelID());
                    materialChooser.getSelectionModel().select(chosenFilament);

//            String profileNameToSave = profileDetailsController.getProfileName();
//            SlicerSettings settingsToSave = profileDetailsController.getProfileData();
//            settingsToSave.getProfileNameProperty().set(profileNameToSave);
//            PrintProfileContainer.saveProfile(settingsToSave);
//            updateProfileList();
//            for (SlicerSettings settings : availableProfiles)
//            {
//                if (settings.getProfileName().equals(profileNameToSave))
//                {
//                    customProfileChooser.getSelectionModel().select(settings);
//                    break;
//                }
//            }
//            qualityChooser.adjustValue(PrintQualityEnumeration.CUSTOM.getEnumPosition());
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

    private int showCreateProfileDialogue(RoboxProfile dataToUse)
    {
        dataToUse.setMutable(true);
        profileDetailsController.updateProfileData(dataToUse);
        int response = createProfileDialogue.show();
        if (response == saveProfileAction)
        {
            String profileNameToSave = profileDetailsController.getProfileName();
            RoboxProfile settingsToSave = profileDetailsController.getProfileData();
            settingsToSave.getProfileNameProperty().set(profileNameToSave);
            PrintProfileContainer.saveProfile(settingsToSave);
            updateProfileList();
            for (RoboxProfile settings : availableProfiles)
            {
                if (settings.getProfileName().equals(profileNameToSave))
                {
                    customProfileChooser.getSelectionModel().select(settings);
                    break;
                }
            }
            qualityChooser.adjustValue(PrintQualityEnumeration.CUSTOM.getEnumPosition());
        } else
        {
            if (lastCustomProfileSelected != null)
            {
                if (lastCustomProfileSelected == PrintProfileContainer.createNewProfile)
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
}
