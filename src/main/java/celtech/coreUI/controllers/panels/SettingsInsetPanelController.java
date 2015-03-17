package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.Filament;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.components.ProfileChoiceListCell;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.services.slicer.PrintQualityEnumeration;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
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
public class SettingsInsetPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        SettingsInsetPanelController.class.getName());
    private PrinterSettings printerSettings = null;
    private ApplicationStatus applicationStatus = null;

    @FXML
    private HBox root;

    @FXML
    private Slider brimSlider;

    @FXML
    private ComboBox<SlicerParametersFile> customProfileChooser;

    @FXML
    private Slider supportSlider;

    @FXML
    private Slider qualityChooser;

    @FXML
    private VBox customProfileVBox;

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

    private Project currentProject;

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

        applicationStatus.modeProperty().addListener(
            (ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue) ->
            {
                if (newValue == ApplicationMode.SETTINGS)
                {
                    root.setVisible(true);
                } else
                {
                    root.setVisible(false);
                }
            });
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
                if (newValue != null)
                {
                    if (printerSettings != null && printerSettings.getPrintQuality()
                    == PrintQualityEnumeration.CUSTOM)
                    {
                        printerSettings.setSettingsName(newValue.getProfileName());
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
                customProfileVBox.setVisible(true);
                customProfileChooser.getSelectionModel().select(settings);
                break;
            default:
                break;
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
}
