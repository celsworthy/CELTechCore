package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile.SupportType;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ProfileChoiceListCell;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.modelcontrol.ModelContainer;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.utils.PrinterListChangesAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class SettingsInsetPanelController implements Initializable, ProjectAwareController, Project.ProjectChangesListener
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            SettingsInsetPanelController.class.getName());

    @FXML
    private HBox settingsInsetRoot;

    @FXML
    private Slider brimSlider;

    @FXML
    private ComboBox<SlicerParametersFile> customProfileChooser;

    @FXML
    private ComboBox<SupportType> supportComboBox;

    @FXML
    private RadioButton supportButton;

    @FXML
    private RadioButton raftButton;

    @FXML
    private HBox customProfileBox;

    @FXML
    private Slider fillDensitySlider;

    @FXML
    private Label fillDensityPercent;

    @FXML
    private Label createProfileLabel;

    private Printer currentPrinter;
    private Project currentProject;
    private PrinterSettings printerSettings;
    private String currentHeadType = HeadContainer.defaultHeadID;
    private ObjectProperty<PrintQualityEnumeration> printQuality;
    private boolean populatingForProject = false;

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            setupCustomProfileChooser();

            setupOverrides();

            Lookup.getSelectedPrinterProperty().addListener(
                    (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) ->
                    {
                        whenPrinterChanged(newValue);
                    });

            ApplicationStatus.getInstance().modeProperty().addListener(
                    (ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue) ->
                    {
                        if (newValue == ApplicationMode.SETTINGS)
                        {
                            settingsInsetRoot.setVisible(true);
                            settingsInsetRoot.setMouseTransparent(false);
                        } else
                        {
                            settingsInsetRoot.setVisible(false);
                            settingsInsetRoot.setMouseTransparent(true);
                        }
                    });

            SlicerParametersContainer.getUserProfileList().addListener(
                    (ListChangeListener.Change<? extends SlicerParametersFile> c) ->
                    {
                        populateCustomProfileChooser();
                        showPleaseCreateProfile(
                                SlicerParametersContainer.getUserProfileList().isEmpty());
                    });

            showPleaseCreateProfile(
                    SlicerParametersContainer.getUserProfileList().isEmpty());

            whenPrinterChanged(Lookup.getSelectedPrinterProperty().get());

            Lookup.getPrinterListChangesNotifier().addListener(new PrinterListChangesAdapter()
            {

                @Override
                public void whenHeadAdded(Printer printer)
                {
                    if (printer == currentPrinter)
                    {
                        whenPrinterChanged(printer);
                        updateSupportCombo(printer);
                    }
                }

                @Override
                public void whenExtruderAdded(Printer printer, int extruderIndex)
                {
                    if (printer == currentPrinter)
                    {
                        updateSupportCombo(printer);
                    }
                }

            });

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        fillDensityPercent.textProperty().bind(fillDensitySlider.valueProperty().asString("%.0f"));
    }

    PropertyChangeListener customSettingsListener = (PropertyChangeEvent evt) ->
    {
        if (evt.getPropertyName().equals("fillDensity_normalised"))
        {
            fillDensitySlider.valueProperty().set(((Number) evt.getNewValue()).doubleValue()
                    * 100);
        }
    };

    private void setupCustomProfileChooser()
    {
        Callback<ListView<SlicerParametersFile>, ListCell<SlicerParametersFile>> profileChooserCellFactory
                = (ListView<SlicerParametersFile> list) -> new ProfileChoiceListCell();

        customProfileChooser.setCellFactory(profileChooserCellFactory);
        customProfileChooser.setButtonCell(profileChooserCellFactory.call(null));
        populateCustomProfileChooser();

        clearSettingsIfNoCustomProfileAvailable();

        customProfileChooser.getSelectionModel().selectedItemProperty().addListener(
                (ObservableValue<? extends SlicerParametersFile> observable, SlicerParametersFile oldValue, SlicerParametersFile newValue) ->
                {

                    if (populatingForProject)
                    {
                        return;
                    }

                    if (newValue != null)
                    {
                        if (printerSettings != null && printerSettings.getPrintQuality()
                        == PrintQualityEnumeration.CUSTOM)
                        {
                            whenCustomProfileChanges(newValue);
                        } else if (printerSettings != null)
                        {
                            steno.error("custom profile chosen but quality not CUSTOM");
                        }

                    }
                });

        SlicerParametersContainer.getUserProfileList().addListener(
                (ListChangeListener.Change<? extends SlicerParametersFile> c) ->
                {
                    clearSettingsIfNoCustomProfileAvailable();
                });
    }

    private void populateCustomProfileChooser()
    {
        List filesForHeadType = SlicerParametersContainer.getUserProfileList().stream().
                filter(profile -> profile.getHeadType() != null
                        && profile.getHeadType().equals(currentHeadType)).collect(Collectors.toList());
        customProfileChooser.setItems(FXCollections.observableArrayList(filesForHeadType));
        if (currentProject != null
                && currentProject.getPrinterSettings().getPrintQuality() == PrintQualityEnumeration.CUSTOM)
        {
            selectCurrentCustomSettings();
        }

    }

    private void whenCustomProfileChanges(SlicerParametersFile newValue)
    {
        if (getCustomSettings() != null)
        {
            getCustomSettings().removePropertyChangeListener(customSettingsListener);
        }
        printerSettings.setSettingsName(newValue.getProfileName());
        if (getCustomSettings() != null)
        {
            getCustomSettings().addPropertyChangeListener(customSettingsListener);
        }
        printQualityWidgetsUpdate(PrintQualityEnumeration.CUSTOM);
    }

    private void setupOverrides()
    {
        supportComboBox.valueProperty().addListener(
                (ObservableValue<? extends SlicerParametersFile.SupportType> ov, SlicerParametersFile.SupportType lastSupportValue, SlicerParametersFile.SupportType newSupportValue) ->
                {
                    if (populatingForProject)
                    {
                        return;
                    }

                    if (printerSettings != null
                    && lastSupportValue != newSupportValue)
                    {
                        printerSettings.setPrintSupportTypeOverride(newSupportValue);
                    }
                });

        supportButton.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean selected) ->
                {
                    if (populatingForProject)
                    {
                        return;
                    }

                    updateSupportCombo(currentPrinter);

                    printerSettings.setPrintSupportOverride(selected);
                });

        raftButton.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean selected) ->
                {
                    if (populatingForProject)
                    {
                        return;
                    }

                    printerSettings.setRaftOverride(selected);
                });

        fillDensitySlider.valueProperty()
                .addListener(
                        (ObservableValue<? extends Number> observable, Number was, Number now) ->
                        {
                            if (!fillDensitySlider.isValueChanging())
                            {
                                printerSettings.setFillDensityOverride(now.floatValue() / 100.0f);
                            }
                        });

        brimSlider.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
            {
                if (!brimSlider.isValueChanging())
                {
                    printerSettings.setBrimOverride(newValue.intValue());
                }
            }
        });
    }

    @FXML
    void editPrintProfile(ActionEvent event)
    {
        DisplayManager.getInstance().showAndSelectPrintProfile(customProfileChooser.getValue());
    }

    /**
     * If no profiles are left available then clear the settingsName.
     */
    private void clearSettingsIfNoCustomProfileAvailable()
    {
        if (SlicerParametersContainer.getUserProfileList().size() == 0)
        {
            if (printerSettings != null)
            {
                printerSettings.setSettingsName("");
            }
        }
    }

    private void whenPrinterChanged(Printer printer)
    {
        currentPrinter = printer;
        if (printer != null)
        {
            updateSupportCombo(printer);
            if (printer.headProperty().get() != null)
            {
                currentHeadType = printer.headProperty().get().typeCodeProperty().get();
            } else
            {
                currentHeadType = HeadContainer.defaultHeadID;
            }
            populateCustomProfileChooser();
            updateSupportCombo(currentPrinter);
        }
    }

    private int getNumExtruders(Printer printer)
    {
        int numExtruders = 1;
        if (printer != null && printer.extrudersProperty().get(1).isFittedProperty().get())
        {
            numExtruders = 2;
        }
        return numExtruders;
    }

    private void updateSupportCombo(Printer printer)
    {
        if (printer != null
                && printer.headProperty().get() != null)
        {
            populatingForProject = true;

            SupportType selectionBefore = supportComboBox.getSelectionModel().getSelectedItem();

            // Support Type
            // Material 1 - the only option for single extruder machines - default
            // Material 2 - only available on dual extruder machines
            supportComboBox.getItems().clear();
            supportComboBox.getItems().add(SupportType.MATERIAL_1);
            if (getNumExtruders(printer) > 1
                    && (printer.headProperty().get() != null
                    && printer.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD))
            {
                supportComboBox.getItems().add(SupportType.MATERIAL_2);
                if (supportComboBox.getItems().contains(selectionBefore))
                {
                    // The old selection is still available - select it again
                    supportComboBox.getSelectionModel().select(selectionBefore);
                } else
                {
                    if (currentProject != null
                            && currentProject.getUsedExtruders(printer).size() == 1)
                    {
                        // Only one extruder used in this project
                        // Auto select the same material that is being used
                        supportComboBox.getSelectionModel().select((currentProject.getUsedExtruders(printer).contains(0) == true) ? SupportType.MATERIAL_1 : SupportType.MATERIAL_2);
                    } else
                    {
                        // More than one extruder used in the project or the current project isn't set
                        // Select the first option
                        supportComboBox.getSelectionModel().selectFirst();
                    }
                }

                supportComboBox.setDisable(false);
            } else
            {
                supportComboBox.getSelectionModel().selectFirst();
                supportComboBox.setDisable(true);
            }

            populatingForProject = false;
        }
    }

    @Override
    public void setProject(Project project)
    {
        if (currentProject != null)
        {
            currentProject.removeProjectChangesListener(this);
        }
        project.addProjectChangesListener(this);
        whenProjectChanged(project);
    }

    private void whenProjectChanged(Project project)
    {
        populatingForProject = true;

        currentProject = project;
        printerSettings = project.getPrinterSettings();
        printQuality = printerSettings.printQualityProperty();

        int saveBrim = printerSettings.getBrimOverride();
        float saveFillDensity = printerSettings.getFillDensityOverride();
        boolean autoSupport = printerSettings.getPrintSupportOverride();
        SupportType saveSupports = printerSettings.getPrintSupportTypeOverride();
        boolean savePrintRaft = printerSettings.getRaftOverride();

        // printer settings name is cleared by combo population so must be saved
        String savePrinterSettingsName = project.getPrinterSettings().getSettingsName();

        printQuality.addListener(
                (ObservableValue<? extends PrintQualityEnumeration> observable, PrintQualityEnumeration oldValue, PrintQualityEnumeration newValue) ->
                {
                    printQualityWidgetsUpdate(newValue);
                });
        printQualityWidgetsUpdate(printQuality.get());

        // just in case custom settings are changing through some other mechanism
        printerSettings.getSettingsNameProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) ->
        {
            selectCurrentCustomSettings();
        });

        brimSlider.setValue(saveBrim);
        fillDensitySlider.setValue(saveFillDensity * 100);

        raftButton.setSelected(savePrintRaft);

        supportComboBox.setValue(saveSupports);

        printerSettings.getPrintSupportTypeOverrideProperty().addListener(new ChangeListener<SupportType>()
        {
            @Override
            public void changed(ObservableValue<? extends SupportType> observable, SupportType oldValue, SupportType newValue)
            {
                supportComboBox.getSelectionModel().select(newValue);
                updateSupportCombo(currentPrinter);
            }
        });

        supportButton.setSelected(autoSupport);

        if (project.getPrintQuality() == PrintQualityEnumeration.CUSTOM)
        {
            if (savePrinterSettingsName.length() > 0)
            {
                SlicerParametersFile chosenProfile = SlicerParametersContainer.getSettings(
                        savePrinterSettingsName, currentHeadType);
                customProfileChooser.getSelectionModel().select(chosenProfile);
            }
        }
        populatingForProject = false;
    }

    private void selectCurrentCustomSettings()
    {
        Head currentHead = currentPrinter.headProperty().get();
        String headType = HeadContainer.defaultHeadID;
        if (currentHead != null)
        {
            headType = currentHead.typeCodeProperty().get();
        }
        customProfileChooser.getSelectionModel().select(
                printerSettings.getSettings(headType));
    }

    private void enableCustomChooser(boolean enable)
    {
        customProfileBox.setDisable(!enable);
    }

    private void showPleaseCreateProfile(boolean show)
    {
        customProfileChooser.setVisible(!show);
        createProfileLabel.setVisible(show);
    }

    private void printQualityWidgetsUpdate(PrintQualityEnumeration quality)
    {
        SlicerParametersFile settings = null;

        switch (quality)
        {
            case DRAFT:
                settings = SlicerParametersContainer.getSettings(
                        ApplicationConfiguration.draftSettingsProfileName, currentHeadType);
                enableCustomChooser(false);
                break;
            case NORMAL:
                settings = SlicerParametersContainer.getSettings(
                        ApplicationConfiguration.normalSettingsProfileName, currentHeadType);
                enableCustomChooser(false);
                break;
            case FINE:
                settings = SlicerParametersContainer.getSettings(
                        ApplicationConfiguration.fineSettingsProfileName, currentHeadType);
                enableCustomChooser(false);
                break;
            case CUSTOM:
                settings = getCustomSettings();
                enableCustomChooser(true);
                break;
            default:
                break;
        }

        if (currentProject != null)
        {
            if (settings != null)
            {
                printerSettings.setFillDensityOverride(settings.getFillDensity_normalised());
                fillDensitySlider.setValue(settings.getFillDensity_normalised() * 100.0);
            }

            if (printQuality.get() == PrintQualityEnumeration.CUSTOM)
            {
                if (settings == null)
                {
                    customProfileChooser.setValue(null);
                } else
                {
                    customProfileChooser.getSelectionModel().select(settings);
                }
            }
        }
    }

    private SlicerParametersFile getCustomSettings()
    {
        String customSettingsName = printerSettings.getSettingsName();
        if (customSettingsName.equals(""))
        {
            return null;
        } else
        {
            return SlicerParametersContainer.getSettings(customSettingsName, currentHeadType);
        }
    }

    @Override
    public void whenModelAdded(ModelContainer modelContainer)
    {
        updateSupportCombo(currentPrinter);
    }

    @Override
    public void whenModelsRemoved(Set<ModelContainer> modelContainers)
    {
        updateSupportCombo(currentPrinter);
    }

    @Override
    public void whenAutoLaidOut()
    {
    }

    @Override
    public void whenModelsTransformed(Set<ModelContainer> modelContainers)
    {
    }

    @Override
    public void whenModelChanged(ModelContainer modelContainer, String propertyName)
    {
        updateSupportCombo(currentPrinter);
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettings printerSettings)
    {
    }

}
