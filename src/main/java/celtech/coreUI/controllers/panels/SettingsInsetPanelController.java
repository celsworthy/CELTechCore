package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile.SupportType;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ProfileChoiceListCell;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import celtech.roboxbase.printerControl.model.PrinterListChangesAdapter;
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
import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
public class SettingsInsetPanelController implements Initializable, ProjectAwareController, ModelContainerProject.ProjectChangesListener
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
    private HBox customProfileBox;

    @FXML
    private Label createProfileLabel;

    @FXML
    private Button editPrintProfileButton;

    @FXML
    private HBox raftHBox;

    @FXML
    private CheckBox raftButton;

    @FXML
    private HBox supportHBox;

    @FXML
    private CheckBox supportButton;

    @FXML
    private HBox supportGapHBox;

    @FXML
    private CheckBox supportGapButton;

    @FXML
    private HBox brimHBox;

    @FXML
    private HBox raftSupportBrimChooserBox;

    @FXML
    private CheckBox spiralPrintCheckbox;

    @FXML
    private HBox fillDensityHBox;

    @FXML
    private HBox spiralPrintHBox;

    @FXML
    private RestrictedNumberField fillDensityPercentEntry;

    @FXML
    private Slider fillDensitySlider;

    private Printer currentPrinter;
    private Project currentProject;
    private PrinterSettingsOverrides printerSettings;
    private String currentHeadType = HeadContainer.defaultHeadID;
    private ObjectProperty<PrintQualityEnumeration> printQuality;
    private boolean populatingForProject = false;

    private MapChangeListener<Integer, Filament> filamentListener = new MapChangeListener<Integer, Filament>()
    {
        @Override
        public void onChanged(MapChangeListener.Change<? extends Integer, ? extends Filament> change)
        {
            dealWithSupportGap();
        }
    };

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
            supportComboBox.getItems().clear();
            supportComboBox.getItems().addAll(SupportType.values());

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

            BaseLookup.getPrinterListChangesNotifier().addListener(new PrinterListChangesAdapter()
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

        fillDensitySlider.valueProperty().addListener(new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov, Number t, Number t1)
            {
                fillDensityPercentEntry.setValue(t1.doubleValue());
            }
        });
        fillDensityPercentEntry.valueChangedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                fillDensitySlider.setValue(fillDensityPercentEntry.getAsDouble());
            }
        });

        spiralPrintCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean t, Boolean t1)
            {
                if (t1.booleanValue())
                {
                    raftButton.setSelected(false);
                    supportButton.setSelected(false);
                    supportGapButton.setSelected(false);
                    brimSlider.setValue(0);
                }

                raftHBox.setDisable(t1);
                supportHBox.setDisable(t1);
                supportGapHBox.setDisable(t1);
                supportComboBox.setDisable(t1);
                brimHBox.setDisable(t1);
                fillDensityHBox.setDisable(t1);
                fillDensityPercentEntry.setDisable(t1);
                fillDensitySlider.setDisable(t1);
            }
        });

        ApplicationStatus.getInstance().modeProperty().addListener(new ChangeListener<ApplicationMode>()
        {
            @Override
            public void changed(ObservableValue<? extends ApplicationMode> ov, ApplicationMode t, ApplicationMode t1)
            {
                if (t1 == ApplicationMode.SETTINGS)
                {
                    if (currentProject != null
                            && currentPrinter != null)
                    {
                        dealWithSpiralness();
                        dealWithSupportGap();
                    }
                }
            }
        });

        raftSupportBrimChooserBox.disableProperty().bind(spiralPrintCheckbox.selectedProperty()
                .or(supportButton.selectedProperty().not()
                        .and(brimSlider.valueProperty().lessThanOrEqualTo(0))
                        .and(raftButton.selectedProperty().not())));
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

                    dealWithSupportGap();

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
                    dealWithSupportGap();

                    printerSettings.setPrintSupportOverride(selected);
                });

        supportGapButton.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean selected) ->
                {
                    if (populatingForProject)
                    {
                        return;
                    }

                    printerSettings.setPrintSupportGapEnabledOverride(selected);
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
                            if (!fillDensitySlider.isValueChanging()
                            || now.doubleValue() >= fillDensitySlider.getMax()
                            || now.doubleValue() <= fillDensitySlider.getMin())
                            {
                                printerSettings.setFillDensityOverride(now.floatValue() / 100.0f);
                            }
                        });

        brimSlider.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        {
            if (!brimSlider.isValueChanging()
                    || newValue.doubleValue() >= brimSlider.getMax()
                    || newValue.doubleValue() <= brimSlider.getMin())
            {
                printerSettings.setBrimOverride(newValue.intValue());
            }
        });

        spiralPrintCheckbox.selectedProperty().addListener(
                (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean selected) ->
                {
                    if (populatingForProject)
                    {
                        return;
                    }

                    printerSettings.setSpiralPrintOverride(selected);
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
        if (currentPrinter != null)
        {
            currentPrinter.effectiveFilamentsProperty().removeListener(filamentListener);
        }

        currentPrinter = printer;
        if (printer != null)
        {
            updateSupportCombo(printer);
            String headTypeCode;
            if (printer.headProperty().get() != null)
            {
                headTypeCode = printer.headProperty().get().typeCodeProperty().get();
            } else
            {
                headTypeCode = HeadContainer.defaultHeadID;
            }
            if (!headTypeCode.equals(currentHeadType))
            {
                if (currentProject != null)
                {
                    currentProject.invalidate();
                }
            }
            currentHeadType = headTypeCode;

            populateCustomProfileChooser();
            updateSupportCombo(currentPrinter);

            currentPrinter.effectiveFilamentsProperty().addListener(filamentListener);
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

            if (getNumExtruders(printer) > 1
                    && (printer.headProperty().get() != null
                    && printer.headProperty().get().headTypeProperty().get() == Head.HeadType.DUAL_MATERIAL_HEAD))
            {
                raftSupportBrimChooserBox.setVisible(true);
                raftSupportBrimChooserBox.setMinHeight(-1);
                raftSupportBrimChooserBox.setPrefHeight(-1);
            } else
            {
                raftSupportBrimChooserBox.setVisible(false);
                raftSupportBrimChooserBox.setMinHeight(0);
                raftSupportBrimChooserBox.setPrefHeight(0);
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
        boolean saveSpiralPrint = printerSettings.getSpiralPrintOverride();
        boolean saveSupportGapEnabled = printerSettings.getPrintSupportGapEnabledOverride();

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

        spiralPrintCheckbox.setSelected(saveSpiralPrint);
        dealWithSpiralness();

        supportGapButton.setSelected(saveSupportGapEnabled);
        dealWithSupportGap();

        populatingForProject = false;
    }

    private void dealWithSpiralness()
    {
        if (currentProject instanceof ModelContainerProject)
        {
            spiralPrintHBox.disableProperty().set(currentProject.getAllModels().size() != 1
                    || !((ModelContainerProject) currentProject).allModelsOnSameExtruder(currentPrinter));

            spiralPrintCheckbox.setSelected(spiralPrintCheckbox.selectedProperty().get()
                    && currentProject.getAllModels().size() == 1
                    && ((ModelContainerProject) currentProject).allModelsOnSameExtruder(currentPrinter));
        }
    }

    private void dealWithSupportGap()
    {
        supportGapHBox.disableProperty().set(!supportButton.isSelected());

        boolean supportGapEnabledDriver = currentPrinter != null
                && supportButton.isSelected()
                && !(currentPrinter.effectiveFilamentsProperty().get(0).getMaterial() != currentPrinter.effectiveFilamentsProperty().get(1).getMaterial()
                && !((ModelContainerProject) currentProject).getPrintingExtruders(currentPrinter).get(supportComboBox.getSelectionModel().getSelectedItem().getExtruderNumber()));

        supportGapButton.setSelected(supportGapEnabledDriver);
    }

    private void selectCurrentCustomSettings()
    {
        if (currentPrinter != null)
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
                        BaseConfiguration.draftSettingsProfileName, currentHeadType);
                enableCustomChooser(false);
                break;
            case NORMAL:
                settings = SlicerParametersContainer.getSettings(
                        BaseConfiguration.normalSettingsProfileName, currentHeadType);
                enableCustomChooser(false);
                break;
            case FINE:
                settings = SlicerParametersContainer.getSettings(
                        BaseConfiguration.fineSettingsProfileName, currentHeadType);
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
    public void whenModelAdded(ProjectifiableThing modelContainer)
    {
        updateSupportCombo(currentPrinter);
    }

    @Override
    public void whenModelsRemoved(Set<ProjectifiableThing> modelContainers)
    {
        updateSupportCombo(currentPrinter);
    }

    @Override
    public void whenAutoLaidOut()
    {
    }

    @Override
    public void whenModelsTransformed(Set<ProjectifiableThing> modelContainers)
    {
    }

    @Override
    public void whenModelChanged(ProjectifiableThing modelContainer, String propertyName)
    {
        updateSupportCombo(currentPrinter);
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings)
    {
    }

}
