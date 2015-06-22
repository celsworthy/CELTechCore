package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.HeadContainer;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.fileRepresentation.SlicerParametersFile.HeadType;
import celtech.configuration.fileRepresentation.SlicerParametersFile.SupportType;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.ProfileChoiceListCell;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.printerControl.model.Head;
import celtech.printerControl.model.Printer;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.utils.PrinterListChangesAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
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
public class SettingsInsetPanelController implements Initializable, ProjectAwareController
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
    private Slider raftSlider;

    @FXML
    private VBox customProfileVBox;

    @FXML
    private Slider fillDensitySlider;

    @FXML
    private Label createProfileLabel;

    @FXML
    private CheckBox cbSupport;

    private Printer currentPrinter;
    private Project currentProject;
    private PrinterSettings printerSettings;
    private HeadType currentHeadType = HeadContainer.defaultHeadType;
    private ObjectProperty<PrintQualityEnumeration> printQuality;

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
                    } else
                    {
                        settingsInsetRoot.setVisible(false);
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
        supportComboBox.getItems().addAll(SupportType.values());
        supportComboBox.getItems().remove(SupportType.NO_SUPPORT);

        supportComboBox.valueProperty().addListener(
            (ObservableValue<? extends Object> ov, Object lastSupportValue, Object newSupportValue) ->
            {
                if (lastSupportValue != newSupportValue)
                {
                    printerSettings.setPrintSupportOverride((SupportType) newSupportValue);
                }
            });

        cbSupport.selectedProperty().addListener(
            (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean selected) ->
            {
                if (currentPrinter != null && selected && getNumExtruders(currentPrinter) == 2)
                {
                    supportComboBox.setDisable(false);
                    if (supportComboBox.getValue() == null)
                    {
                        // this happens for new projects
                        supportComboBox.setValue(SupportType.OBJECT_MATERIAL);
                    }
                    printerSettings.setPrintSupportOverride(supportComboBox.getValue());
                } else if (currentPrinter != null && selected && getNumExtruders(currentPrinter)
                == 1)
                {
                    printerSettings.setPrintSupportOverride(SupportType.OBJECT_MATERIAL);
                } else if (selected) //selected but no printer connected
                {
                    printerSettings.setPrintSupportOverride(SupportType.OBJECT_MATERIAL);
                } else
                {
                    supportComboBox.setDisable(true);
                    printerSettings.setPrintSupportOverride(SupportType.NO_SUPPORT);
                }
            });

        raftSlider.setLabelFormatter(new StringConverter<Double>()
        {
            @Override
            public String toString(Double n)
            {
                String returnedText = "";

                if (n <= 0)
                {
                    returnedText = Lookup.i18n("genericFirstLetterCapitalised.Off");
                } else
                {
                    returnedText = Lookup.i18n("genericFirstLetterCapitalised.On");
                }
                return returnedText;
            }

            @Override
            public Double fromString(String s)
            {
                double returnVal = 0;

                if (s.equals(Lookup.i18n("genericFirstLetterCapitalised.Off")))
                {
                    returnVal = 0;
                } else if (s.equals(Lookup.i18n("genericFirstLetterCapitalised.On")))
                {
                    returnVal = 1;
                }
                return returnVal;
            }
        }
        );

        raftSlider.valueProperty().addListener(
            (ObservableValue<? extends Number> ov, Number lastRaftValue, Number newRaftValue) ->
            {
                if (lastRaftValue != newRaftValue)
                {
                    boolean raftSelected = (newRaftValue.doubleValue() >= 1.0);
                    printerSettings.setRaftOverride(raftSelected);
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
                currentHeadType = printer.headProperty().get().headTypeProperty().get();
            } else
            {
                currentHeadType = HeadContainer.defaultHeadType;
            }
            populateCustomProfileChooser();
        }
    }

    private int getNumExtruders(Printer printer)
    {
        int numExtruders = 1;
        if (printer.extrudersProperty().get(1).isFittedProperty().get())
        {
            numExtruders = 2;
        }
        return numExtruders;
    }

    private void updateSupportCombo(Printer printer)
    {
        if (getNumExtruders(printer) == 1 || !cbSupport.isSelected())
        {
            supportComboBox.setDisable(true);
        } else
        {
            supportComboBox.setDisable(false);
        }
    }

    @Override
    public void setProject(Project project)
    {
        whenProjectChanged(project);
    }

    private void whenProjectChanged(Project project)
    {
        currentProject = project;
        printerSettings = project.getPrinterSettings();
        printQuality = printerSettings.printQualityProperty();

        int saveBrim = printerSettings.getBrimOverride();
        float saveFillDensity = printerSettings.getFillDensityOverride();
        SupportType saveSupports = printerSettings.getPrintSupportOverride();
        boolean savePrintRaft = printerSettings.getRaftOverride();

        // printer settings name is cleared by combo population so must be saved
        String savePrinterSettingsName = project.getPrinterSettings().getSettingsName();

        if (project.getPrintQuality() == PrintQualityEnumeration.CUSTOM)
        {
            if (savePrinterSettingsName.length() > 0)
            {
                SlicerParametersFile chosenProfile = SlicerParametersContainer.getSettings(
                    savePrinterSettingsName, currentHeadType);
                customProfileChooser.getSelectionModel().select(chosenProfile);
            }
        }

        printQuality.addListener(
            (ObservableValue<? extends PrintQualityEnumeration> observable, PrintQualityEnumeration oldValue, PrintQualityEnumeration newValue) ->
            {
                printQualityWidgetsUpdate(newValue);
            });
        printQualityWidgetsUpdate(printQuality.get());

        // just in case custom settings are changing through some other mechanism
        printerSettings.getSettingsNameProperty().addListener(
            (ObservableValue<? extends String> observable, String oldValue, String newValue) ->
            {
                Head currentHead = currentPrinter.headProperty().get();
                SlicerParametersFile.HeadType headType = SlicerParametersFile.HeadType.SINGLE_MATERIAL_HEAD;
                if (currentHead != null)
                {
                    headType = currentHead.headTypeProperty().get();
                }
                customProfileChooser.getSelectionModel().select(
                    printerSettings.getSettings(headType));
            });

        brimSlider.setValue(saveBrim);
        fillDensitySlider.setValue(saveFillDensity * 100);
        raftSlider.setValue(savePrintRaft ? 1 : 0);

        if (saveSupports != SupportType.NO_SUPPORT)
        {
            supportComboBox.setValue(saveSupports);
        }
        cbSupport.setSelected(saveSupports == SupportType.NO_SUPPORT ? false : true);
    }

    private void enableCustomChooser(boolean enable)
    {
        customProfileVBox.setDisable(!enable);
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

}
