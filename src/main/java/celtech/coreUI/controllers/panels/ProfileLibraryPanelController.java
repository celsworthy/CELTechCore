package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.coreUI.components.RestrictedTextField;
import celtech.roboxbase.configuration.PrintProfileSetting;
import celtech.roboxbase.configuration.PrintProfileSettingsWrapper;
import celtech.roboxbase.configuration.RoboxProfile;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.datafileaccessors.PrintProfileSettingsContainer;
import celtech.roboxbase.configuration.datafileaccessors.RoboxProfileSettingsContainer;
import celtech.roboxbase.configuration.fileRepresentation.HeadFile;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.utils.settingsgeneration.ProfileDetailsGenerator;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson and George Salter
 */
public class ProfileLibraryPanelController implements Initializable, MenuInnerPanel
{
    private static final Stenographer STENO = StenographerFactory.getStenographer(
            ProfileLibraryPanelController.class.getName());

    private static final PrintProfileSettingsContainer PRINT_PROFILE_SETTINGS_CONTAINER = PrintProfileSettingsContainer.getInstance();
    private static final RoboxProfileSettingsContainer ROBOX_PROFILE_SETTINGS_CONTAINER = RoboxProfileSettingsContainer.getInstance();
    
    private final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    enum State {
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

    private final ObjectProperty<ProfileLibraryPanelController.State> state = new SimpleObjectProperty<>();
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);

    private final BooleanProperty isEditable = new SimpleBooleanProperty(false);
    private final BooleanProperty canSave = new SimpleBooleanProperty(false);
    private final BooleanProperty canSaveAs = new SimpleBooleanProperty(false);
    private final BooleanProperty canDelete = new SimpleBooleanProperty(false);
    private final BooleanProperty isNameValid = new SimpleBooleanProperty(false);
    private final StringProperty currentHeadType = new SimpleStringProperty();
    private final IntegerProperty numNozzleHeaters = new SimpleIntegerProperty();
    private final IntegerProperty numNozzles = new SimpleIntegerProperty();
    private final BooleanProperty hasValves = new SimpleBooleanProperty(false);

    private ProfileDetailsGenerator profileDetailsFxmlGenerator;
    
    private String currentProfileName;
    
    @FXML
    private Label slicerInUseLabel;
    
    @FXML
    private VBox container;

    @FXML
    private ComboBox<String> cmbHeadType;

    @FXML
    private ComboBox<RoboxProfile> cmbPrintProfile;

    @FXML
    private GridPane extrusion;
    
    @FXML
    private GridPane extrusionControl;
        
    @FXML
    private GridPane nozzles;
    
    @FXML
    private GridPane support;

    @FXML
    private GridPane cooling;

    @FXML
    private GridPane speed;

    @FXML
    private RestrictedTextField profileNameField;

    private final ObservableList<String> nozzleOptions = FXCollections.observableArrayList(
            "0.3mm", "0.4mm", "0.6mm", "0.8mm");
    
    private final ChangeListener<String> dirtyStringListener = (ObservableValue<? extends String> ov, String t, String t1) -> {
        isDirty.set(true);
    };
    
    private Printer currentPrinter = null;

    private final ChangeListener<Head> headChangeListener = (ObservableValue<? extends Head> ov, Head t, Head t1) -> {
        headHasChanged(t1);
    };
    
    private final ChangeListener<SlicerType> slicerTypeChangeListener = 
            (ObservableValue<? extends SlicerType> observable, SlicerType oldValue, SlicerType newValue) -> {
        regenerateSettings(newValue);
        repopulateCmbPrintProfile();
        selectFirstPrintProfile();
        setupSlicerInUseLabel();
    };

    public ProfileLibraryPanelController() {}

    /**
     * Initialises the controller class.
     * 
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {   
        Lookup.getSelectedPrinterProperty().addListener((ObservableValue<? extends Printer> ov, Printer oldValue, Printer newValue) -> {
            bindToPrinter(newValue);
        });

        if (Lookup.getSelectedPrinterProperty().get() != null) {
            bindToPrinter(Lookup.getSelectedPrinterProperty().get());
        }

        canSave.bind(isNameValid
                .and(isDirty
                        .and(state.isEqualTo(State.NEW)
                                .or(state.isEqualTo(State.CUSTOM)))));
        canSaveAs.bind(state.isNotEqualTo(State.NEW));
        canDelete.bind(state.isNotEqualTo(State.NEW).and(state.isNotEqualTo(State.ROBOX)));
        isEditable.bind(state.isNotEqualTo(State.ROBOX));

        PrintProfileSettingsWrapper printProfileSettings = PRINT_PROFILE_SETTINGS_CONTAINER
                .getPrintProfileSettingsForSlicer(getSlicerType());
        profileDetailsFxmlGenerator = new ProfileDetailsGenerator(printProfileSettings, isDirty);
        
        setupSlicerInUseLabel();
        setupProfileNameChangeListeners();
        setupHeadType();
        setupPrintProfileCombo();
        selectFirstPrintProfile();
        setupWidgetEditableBindings();

        Lookup.getUserPreferences().getSlicerTypeProperty().addListener(slicerTypeChangeListener);
    }
    
    private void regenerateSettings(SlicerType slicerType) {
        STENO.debug("========== Begin regenerating settings ==========");
        profileDetailsFxmlGenerator.setPrintProfilesettings(PRINT_PROFILE_SETTINGS_CONTAINER.getPrintProfileSettingsForSlicer(slicerType));
        profileDetailsFxmlGenerator.setHeadType(currentHeadType.get());
        profileDetailsFxmlGenerator.setNozzleOptions(nozzleOptions);
        profileDetailsFxmlGenerator.generateProfileSettingsForTab(extrusion);
        profileDetailsFxmlGenerator.generateProfileSettingsForTab(extrusionControl);
        profileDetailsFxmlGenerator.generateProfileSettingsForTab(nozzles);
        profileDetailsFxmlGenerator.generateProfileSettingsForTab(support);
        profileDetailsFxmlGenerator.generateProfileSettingsForTab(speed);
        profileDetailsFxmlGenerator.generateProfileSettingsForTab(cooling);
        FXMLUtilities.addColonsToLabels(container);
        STENO.debug("========== Finished regenerating settings ==========");
    }

    private void headHasChanged(Head head) {
        if (head != null) {
            if (isDirty.get()) {
                whenSavePressed();
            }
            cmbHeadType.getSelectionModel().select(head.typeCodeProperty().get());
        }
    }

    private void bindToPrinter(Printer printer) {
        if (currentPrinter != null) {
            currentPrinter.headProperty().removeListener(headChangeListener);
        }

        if (printer != null) {
            printer.headProperty().addListener(headChangeListener);

            if (printer.headProperty().get() != null) {
                headHasChanged(printer.headProperty().get());
            }
        }

        currentPrinter = printer;
    }

    private void setupHeadType() {
        HeadContainer.getCompleteHeadList().forEach((head) -> {
            cmbHeadType.getItems().add(head.getTypeCode());
        });

        cmbHeadType.valueProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            HeadFile headDetails = HeadContainer.getHeadByID(newValue);
            currentHeadType.set(newValue);
            numNozzleHeaters.set(headDetails.getNozzleHeaters().size());
            numNozzles.set(headDetails.getNozzles().size());

            List<Float> nozzleSizes = headDetails.getNozzles()
                                                 .stream()
                                                 .map(n -> n.getDiameter())
                                                 .collect(Collectors.toList());

            List<String> nozzleSizeStrings = nozzleSizes.stream()
                                                        .map(n -> n.toString() + "mm")
                                                        .collect(Collectors.toList());
            
            nozzleOptions.setAll(nozzleSizeStrings);
            
            repopulateCmbPrintProfile();
            selectFirstPrintProfile();
        });

        cmbHeadType.setValue(HeadContainer.defaultHeadID);
    }

    private void setupPrintProfileCombo() {
        repopulateCmbPrintProfile();

        cmbPrintProfile.valueProperty().addListener(
                (ObservableValue<? extends RoboxProfile> observable, RoboxProfile oldValue, RoboxProfile newValue) -> {
                    selectPrintProfile();
                }
        );

        selectPrintProfile();
    }

    private void selectFirstPrintProfile() {
        if (cmbPrintProfile.getItems().size() > 0) {
            cmbPrintProfile.setValue(cmbPrintProfile.getItems().get(0));
        }
    }
    
    public void setAndSelectPrintProfile(RoboxProfile printProfile) {
        if (printProfile != null) {
            cmbHeadType.setValue(printProfile.getHeadType());
            cmbPrintProfile.setValue(printProfile);
        }
    }

    private void selectPrintProfile() {
        RoboxProfile printProfile = cmbPrintProfile.getValue();

        if (printProfile == null) {
            return;
        }
        currentProfileName = printProfile.getName();
        updateSettingsFromProfile(printProfile);
        if (printProfile.isStandardProfile()) {
            state.set(State.ROBOX);
        } else {
            state.set(State.CUSTOM);
        }
        isDirty.set(false);
    }

    private void repopulateCmbPrintProfile() {
        Map<String, List<RoboxProfile>> roboxProfiles = ROBOX_PROFILE_SETTINGS_CONTAINER.getRoboxProfilesForSlicer(getSlicerType());
        String headType = cmbHeadType.getValue();
        List<RoboxProfile> filesForHeadType = roboxProfiles.get(headType);
        cmbPrintProfile.setItems(FXCollections.observableArrayList(filesForHeadType));
    }

    private void setupWidgetEditableBindings() {
        profileNameField.disableProperty().bind(isEditable.not());
        cooling.disableProperty().bind(isEditable.not());
        extrusion.disableProperty().bind(isEditable.not());
        extrusionControl.disableProperty().bind(isEditable.not());
        nozzles.disableProperty().bind(isEditable.not());
        support.disableProperty().bind(isEditable.not());
        speed.disableProperty().bind(isEditable.not());
    }
    
    private void setupSlicerInUseLabel() {
        String selectedSlicerStr = Lookup.i18n("profileLibrary.slicerInUse");
        selectedSlicerStr = selectedSlicerStr + " " + getSlicerType().name();
        slicerInUseLabel.setText(selectedSlicerStr);
    }

    private void setupProfileNameChangeListeners() {
        profileNameField.textProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    if (!validateProfileName()) {
                        isNameValid.set(false);
                        profileNameField.pseudoClassStateChanged(ERROR, true);
                    } else {
                        isNameValid.set(true);
                        profileNameField.pseudoClassStateChanged(ERROR, false);
                    }
                });

        profileNameField.textProperty().addListener(dirtyStringListener);
    }

    private void updateSettingsFromProfile(RoboxProfile roboxProfile) {
        profileNameField.setText(roboxProfile.getName());
        
        PrintProfileSettingsWrapper printProfileSettings = PRINT_PROFILE_SETTINGS_CONTAINER
                .getPrintProfileSettingsForSlicer(getSlicerType());
        PrintProfileSettingsWrapper defaultPrintProfileSettings = PRINT_PROFILE_SETTINGS_CONTAINER
                .getDefaultPrintProfileSettingsForSlicer(getSlicerType());
        printProfileSettings.setPrintProfileSettings(defaultPrintProfileSettings.copy().getPrintProfileSettings());
        
        overwriteSettingsFromProfle(printProfileSettings, roboxProfile);
        regenerateSettings(getSlicerType());
    }
    
    private void overwriteSettingsFromProfle(PrintProfileSettingsWrapper settingsToOverwrite, RoboxProfile roboxProfile) {
        Map<String, PrintProfileSetting> printProfileSettingsMap = new HashMap<>();
        
        settingsToOverwrite.getAllSettings().forEach(setting -> printProfileSettingsMap.put(setting.getId(), setting));
        
        Map<String, String> roboxProfileSettings = roboxProfile.getSettings();
        roboxProfileSettings.entrySet().forEach((roboxSetting) -> {
            if(printProfileSettingsMap.containsKey(roboxSetting.getKey())) {
                PrintProfileSetting profileSetting = printProfileSettingsMap.get(roboxSetting.getKey());
                profileSetting.setValue(roboxSetting.getValue());
            }
        });
    }

    private boolean validateProfileName() {
        boolean valid = true;
        String profileNameText = profileNameField.getText();

        if (profileNameText.equals("")) {
            valid = false;
        } else {
            Map<String, List<RoboxProfile>> existingProfileMapForSlicer = 
                    ROBOX_PROFILE_SETTINGS_CONTAINER.getRoboxProfilesForSlicer(getSlicerType());
            List<RoboxProfile> profilesForHead = existingProfileMapForSlicer.get(currentHeadType.get());
            for (RoboxProfile profile : profilesForHead) {
                if (!profile.getName().equals(currentProfileName)
                        && profile.getName().equals(profileNameText)) {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }

    void whenSavePressed() {
        assert (state.get() != ProfileLibraryPanelController.State.ROBOX);
        if (validateProfileName()) {
            PrintProfileSettingsWrapper defaultSettings = PRINT_PROFILE_SETTINGS_CONTAINER.getDefaultPrintProfileSettingsForSlicer(getSlicerType());
            PrintProfileSettingsWrapper settingsForHead = defaultSettings.copy();
            RoboxProfile headProfile = ROBOX_PROFILE_SETTINGS_CONTAINER.loadHeadProfileForSlicer(currentHeadType.get(), getSlicerType());
            overwriteSettingsFromProfle(settingsForHead, headProfile);
            
            PrintProfileSettingsWrapper currentSettings = PRINT_PROFILE_SETTINGS_CONTAINER.getPrintProfileSettingsForSlicer(getSlicerType());
            Map<String, List<PrintProfileSetting>> settingsToWrite = 
                    PRINT_PROFILE_SETTINGS_CONTAINER.compareAndGetDifferencesBetweenSettings(settingsForHead, currentSettings);

            RoboxProfile savedProfile = ROBOX_PROFILE_SETTINGS_CONTAINER.saveCustomProfile(settingsToWrite, profileNameField.getText(), 
                    currentHeadType.get(), getSlicerType());
            
            isDirty.set(false);
            repopulateCmbPrintProfile();
            state.set(ProfileLibraryPanelController.State.CUSTOM);
            
            repopulateCmbPrintProfile();
            cmbPrintProfile.setValue(savedProfile);
        }
    }

    void whenSaveAsPressed() {
        isNameValid.set(false);
        state.set(ProfileLibraryPanelController.State.NEW);

        profileNameField.requestFocus();
        profileNameField.selectAll();
        currentProfileName = "";
        profileNameField.pseudoClassStateChanged(ERROR, true);
    }

    void whenDeletePressed() {
        if (state.get() != ProfileLibraryPanelController.State.NEW) {
            ROBOX_PROFILE_SETTINGS_CONTAINER.deleteCustomProfile(currentProfileName, getSlicerType(), currentHeadType.get());
        }
        repopulateCmbPrintProfile();
        selectFirstPrintProfile();
    }

    @Override
    public String getMenuTitle() {
        return "extrasMenu.printProfile";
    }

    @Override
    public List<MenuInnerPanel.OperationButton> getOperationButtons()
    {
        List<MenuInnerPanel.OperationButton> operationButtons = new ArrayList<>();
        MenuInnerPanel.OperationButton saveButton = new MenuInnerPanel.OperationButton()
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
        MenuInnerPanel.OperationButton saveAsButton = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.SaveAs";
            }

            @Override
            public String getFXMLName()
            {
                return "saveAsButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "genericFirstLetterCapitalised.SaveAs";
            }

            @Override
            public void whenClicked()
            {
                whenSaveAsPressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canSaveAs;
            }

        };
        operationButtons.add(saveAsButton);
        MenuInnerPanel.OperationButton deleteButton = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "genericFirstLetterCapitalised.Delete";
            }

            @Override
            public String getFXMLName()
            {
                return "deleteButton";
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
    
    private SlicerType getSlicerType() {
        return Lookup.getUserPreferences().getSlicerType();
    }
}
