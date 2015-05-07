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
import celtech.coreUI.components.ProfileChoiceListCell;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.services.slicer.PrintQualityEnumeration;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
public class SettingsInsetPanelController implements Initializable
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
    private Slider supportSlider;

    @FXML
    private VBox customProfileVBox;

    @FXML
    private Slider fillDensitySlider;

    @FXML
    private VBox nonCustomProfileVBox;

    @FXML
    private Label createProfileLabel;

    private final SlicerParametersFile draftSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.draftSettingsProfileName);
    private final SlicerParametersFile normalSettings = SlicerParametersContainer.
        getSettingsByProfileName(
            ApplicationConfiguration.normalSettingsProfileName);
    private final SlicerParametersFile fineSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.fineSettingsProfileName);

    private Project currentProject;
    private PrinterSettings printerSettings = null;
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

            Lookup.getSelectedProjectProperty().addListener(
                (ObservableValue<? extends Project> observable, Project oldValue, Project newValue) ->
                {
                    whenProjectChanged(newValue);
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

            Lookup.getFilamentContainer().getUserFilamentList().addListener(
                (ListChangeListener.Change<? extends Filament> c) ->
                {
                    showPleaseCreateProfile(
                        Lookup.getFilamentContainer().getUserFilamentList().isEmpty());
                });
            showPleaseCreateProfile(
                Lookup.getFilamentContainer().getUserFilamentList().isEmpty());
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void setupCustomProfileChooser()
    {
        Callback<ListView<SlicerParametersFile>, ListCell<SlicerParametersFile>> profileChooserCellFactory
            = (ListView<SlicerParametersFile> list) -> new ProfileChoiceListCell();

        customProfileChooser.setCellFactory(profileChooserCellFactory);
        customProfileChooser.setButtonCell(profileChooserCellFactory.call(null));
        customProfileChooser.setItems(SlicerParametersContainer.getUserProfileList());

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
                    } else if (printerSettings != null)
                    {
                        steno.error("custom profile chosen but quality not CUSTOM");
                    }
                }
            });

        SlicerParametersContainer.getUserProfileList().addListener(
            (ListChangeListener.Change<? extends SlicerParametersFile> c) ->
            {
                updateProfileList();
            });
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

    private void setupQualityOverrideControls(PrinterSettings printerSettings)
    {
        fillDensitySlider.setValue(printerSettings.getFillDensityOverride() * 100.0);
        brimSlider.setValue(printerSettings.getBrimOverride());
        supportSlider.setValue(printerSettings.getPrintSupportOverride() ? 1 : 0);
    }

    @FXML
    void editPrintProfile(ActionEvent event)
    {
        DisplayManager.getInstance().showAndSelectPrintProfile(customProfileChooser.getValue());
    }

    /**
     * Update the profile combo box with the list of user profiles. If there are no user profiles
     * then hide the combo box and show the 'Please create a custom profile' message.
     */
    private void updateProfileList()
    {

        if (SlicerParametersContainer.getUserProfileList().size() == 0)
        {
            if (printerSettings != null)
            {
                printerSettings.setSettingsName("");
            }
        }
    }

    private void whenProjectChanged(Project project)
    {
        currentProject = project;
        printerSettings = project.getPrinterSettings();
        printQuality = printerSettings.printQualityProperty();

        int saveBrim = printerSettings.getBrimOverride();
        float saveFillDensity = printerSettings.getFillDensityOverride();
        boolean saveSupports = printerSettings.getPrintSupportOverride();

        // printer settings name is cleared by combo population so must be saved
        String savePrinterSettingsName = project.getPrinterSettings().getSettingsName();

        // UGH quality chooser has (rightly) stamped on the overrides so restore them
        printerSettings.setBrimOverride(saveBrim);
        printerSettings.setFillDensityOverride(saveFillDensity);
        printerSettings.setPrintSupportOverride(saveSupports);

        setupQualityOverrideControls(printerSettings);

        if (project.getPrintQuality() == PrintQualityEnumeration.CUSTOM)
        {
            if (savePrinterSettingsName.length() > 0)
            {
                SlicerParametersFile chosenProfile = SlicerParametersContainer.
                    getSettingsByProfileName(savePrinterSettingsName);
                customProfileChooser.getSelectionModel().select(chosenProfile);
            }
        }

        printQuality.addListener(
            (ObservableValue<? extends PrintQualityEnumeration> observable, PrintQualityEnumeration oldValue, PrintQualityEnumeration newValue) ->
            {
                printQualityWidgetsUpdate(newValue);
            });
        printQualityWidgetsUpdate(printQuality.get());

    }

    private void showCustomWidgets(boolean show)
    {
        nonCustomProfileVBox.visibleProperty().set(!show);
        customProfileVBox.visibleProperty().set(show);
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
                settings = draftSettings;
                showCustomWidgets(false);
                break;
            case NORMAL:
                settings = normalSettings;
                showCustomWidgets(false);
                break;
            case FINE:
                settings = fineSettings;
                showCustomWidgets(false);
                break;
            case CUSTOM:
                showCustomWidgets(true);
                break;
            default:
                break;
        }

        if (currentProject != null)
        {
            if (printQuality.get() != PrintQualityEnumeration.CUSTOM)
            {
                printerSettings.setBrimOverride(settings.getBrimWidth_mm());
                printerSettings.setFillDensityOverride(settings.getFillDensity_normalised());
                printerSettings.setPrintSupportOverride(settings.getGenerateSupportMaterial());
                setupQualityOverrideControls(printerSettings);
            } else
            {
                customProfileChooser.getSelectionModel().select(printerSettings.getSettings());
            }
        }
    }
}
