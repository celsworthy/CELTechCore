package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.RestrictedComboBox;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.camera.CameraInfo;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.datafileaccessors.CameraProfileContainer;
import celtech.roboxbase.configuration.fileRepresentation.CameraProfile;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author George Salter
 */
public class CameraProfilesPanelController implements Initializable, MenuInnerPanel
{
    private static final Stenographer STENO = StenographerFactory.getStenographer(CameraProfilesPanelController.class.getName());
    
    private static final CameraProfileContainer CAMERA_PROFILE_CONTAINER = CameraProfileContainer.getInstance();
    private static final String ANY_CAMERA_NAME = Lookup.i18n("cameraProfiles.anyCamera");
    
    private final BooleanProperty canSave = new SimpleBooleanProperty(false);
    private final BooleanProperty canCreateNew = new SimpleBooleanProperty(false);
    private final BooleanProperty canDelete = new SimpleBooleanProperty(false);
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private final BooleanProperty isNameValid = new SimpleBooleanProperty(false);
    
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
    
    private final ObjectProperty<State> state = new SimpleObjectProperty<>();
    
    @FXML
    private RestrictedComboBox<String> cmbCameraProfile;
    
    @FXML
    private RestrictedNumberField captureHeight;
    
    @FXML
    private RestrictedNumberField captureWidth;
    
    @FXML
    private CheckBox moveBeforeSnapshot;
    
    @FXML
    private RestrictedNumberField moveToX;
    
    @FXML
    private RestrictedNumberField moveToY;

    @FXML
    private CheckBox headLightOn;
    
    @FXML
    private  CheckBox ambientLightOn;
    
    @FXML
    private RestrictedComboBox<String> cmbCameraName;
    
    private Map<String, CameraProfile> cameraProfilesMap;
    
    @FXML
    private GridPane controlGrid;

    private CameraProfile currentCameraProfile = null;
    private CameraProfilesControlSettingsManager controlSettingsManager = new CameraProfilesControlSettingsManager();
    private String selectedProfileName = null;
    
    private final ChangeListener<Object> dirtyFieldListener = (ObservableValue<? extends Object> ov, Object oldEntry, Object newEntry) -> {
            isDirty.set(true);
    };
    
    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        setupProfileNameChangeListeners();
        setupProfileFieldListeners();
        controlSettingsManager.initialise(controlGrid, isDirty);
        
        canSave.bind(isNameValid
                .and(isDirty
                        .and(state.isEqualTo(State.NEW)
                                .or(state.isEqualTo(State.CUSTOM)))));
        canCreateNew.bind(state.isNotEqualTo(State.NEW));
        canDelete.bind(state.isEqualTo(State.CUSTOM));
        
        cmbCameraProfile.valueProperty().addListener((observable, oldValue, newValue) -> {
            selectCameraProfile(newValue);
        });
        
        moveToX.disableProperty().bind(moveBeforeSnapshot.selectedProperty().not());
        moveToY.disableProperty().bind(moveBeforeSnapshot.selectedProperty().not());
        
        DisplayManager.getInstance().libraryModeEnteredProperty().addListener((observable, oldValue, enteredLibraryMode) -> {
            if (enteredLibraryMode)
            {
                populateCmbCameraProfiles();
                populateCmbCameraNames();
                if (currentCameraProfile != null)
                    selectCameraProfile(currentCameraProfile.getProfileName());
                else
                    selectDefaultCameraProfile();
            }
        });
    }
    
    private void setupProfileNameChangeListeners()
    {
        cmbCameraProfile.getEditor().textProperty().addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                    currentCameraProfile.setProfileName(newValue);
                    if (!validateProfileName()) 
                    {
                        isNameValid.set(false);
                    } else 
                    {
                        isNameValid.set(true);
                    }
                });

        cmbCameraProfile.getEditor().textProperty().addListener(dirtyFieldListener);
    }

    private void populateCmbCameraProfiles()
    {
        cameraProfilesMap = CAMERA_PROFILE_CONTAINER.getCameraProfilesMap();
        List<String> profileNames = cameraProfilesMap.values()
                                                            .stream()
                                                            .map(CameraProfile::getProfileName)
                                                            .distinct()
                                                            .collect(Collectors.toList());
        Collections.sort(profileNames);
        cmbCameraProfile.setItems(FXCollections.observableArrayList(profileNames));
    }
    
    private void populateCmbCameraNames()
    {
        // Get all the distinct camera names from the currently connected cameras.
        List<String> cameraNames = BaseLookup.getConnectedCameras()
                                             .stream()
                                             .map(CameraInfo::getCameraName)
                                             .distinct()
                                             .collect(Collectors.toList());
        
        // Add any missing names from current set of profiles.
        if (cameraProfilesMap != null) {
            List<String> additionalNames = cameraProfilesMap.values()
                                                            .stream()
                                                            .map(CameraProfile::getCameraName)
                                                            .distinct()
                                                            .filter((n) -> !cameraNames.contains(n))
                                                            .collect(Collectors.toList());

            cameraNames.addAll(additionalNames);
        }
        cameraNames.add(ANY_CAMERA_NAME); 
        Collections.sort(cameraNames);
        cmbCameraName.setItems(FXCollections.observableArrayList(cameraNames));
    }
    
    private void selectDefaultCameraProfile()
    {
        cmbCameraProfile.setValue(BaseConfiguration.defaultCameraProfileName);
        selectCameraProfile(BaseConfiguration.defaultCameraProfileName);
    }
    
    private boolean selectCameraProfile(String profileName)
    {
        CameraProfile profile = cameraProfilesMap.get(profileName.toLowerCase());
        if (profile != null)
        {
            currentCameraProfile = profile;
            updateValuesFromProfile(currentCameraProfile);
            selectedProfileName = currentCameraProfile.getProfileName();
            
            controlSettingsManager.setControlSettings(currentCameraProfile.getControlSettings());
            
            State newState = profileName.equalsIgnoreCase(BaseConfiguration.defaultCameraProfileName)
                    ? State.ROBOX : State.CUSTOM;
            state.set(newState);
            isNameValid.set(true);
            isDirty.set(false);
            return true;
        }
        return false;
    }
    
    private void updateValuesFromProfile(CameraProfile cameraProfile)
    {
        captureHeight.setValue(cameraProfile.getCaptureHeight());
        captureWidth.setValue(cameraProfile.getCaptureWidth());
        headLightOn.selectedProperty().set(cameraProfile.isHeadLightOn());
        ambientLightOn.selectedProperty().set(cameraProfile.isAmbientLightOn());
        moveBeforeSnapshot.selectedProperty().set(cameraProfile.isMoveBeforeSnapshot());
        moveToX.setValue(cameraProfile.getMoveToX());
        moveToY.setValue(cameraProfile.getMoveToY());
        
        String cameraName = cameraProfile.getCameraName();
        if (cameraName.isBlank())
            cameraName = ANY_CAMERA_NAME;
        cmbCameraName.setValue(cameraName);
    }
    
    private boolean validateProfileName() 
    {
        boolean valid = true;
        String profileNameText = cmbCameraProfile.getValue();

        if (profileNameText.isBlank()) 
        {
            // Can't change name to empty string.
            valid = false;
        }
        else {
            // Can't change name to the name of another existing profile.
            for (String profileName : cameraProfilesMap.keySet()) 
            {
                if (!profileName.equalsIgnoreCase(selectedProfileName)
                        && profileName.equalsIgnoreCase(profileNameText)) 
                {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }
    
    private void whenSavePressed()
    {
        if (validateProfileName())
        {
            updateProfileWithCurrentValues();
            CAMERA_PROFILE_CONTAINER.saveCameraProfile(currentCameraProfile);
            populateCmbCameraProfiles();
            selectCameraProfile(currentCameraProfile.getProfileName());
        }
    }
    
    private void whenNewPressed()
    {
        currentCameraProfile = new CameraProfile(currentCameraProfile);
        isNameValid.set(false);
        state.set(State.NEW);
        cmbCameraProfile.setEditable(true);
        cmbCameraProfile.getEditor().requestFocus();
        selectedProfileName = "";
        String newProfileName = "";
        cmbCameraProfile.getItems().add(newProfileName);
        cmbCameraProfile.setValue(newProfileName);
    }
    
    private void whenDeletePressed()
    {
        if (state.get() != State.NEW) 
        {
            CAMERA_PROFILE_CONTAINER.deleteCameraProfile(currentCameraProfile);
        }
        populateCmbCameraProfiles();
        selectDefaultCameraProfile();
    }
    
    private void setupProfileFieldListeners()
    {
        captureHeight.valueChangedProperty().addListener(dirtyFieldListener);
        captureWidth.valueChangedProperty().addListener(dirtyFieldListener);
        headLightOn.selectedProperty().addListener(dirtyFieldListener);
        ambientLightOn.selectedProperty().addListener(dirtyFieldListener);
        moveBeforeSnapshot.selectedProperty().addListener(dirtyFieldListener);
        moveToX.valueChangedProperty().addListener(dirtyFieldListener);
        moveToY.valueChangedProperty().addListener(dirtyFieldListener);
        cmbCameraName.valueProperty().addListener(dirtyFieldListener);
    }
    
    private void updateProfileWithCurrentValues()
    {
        currentCameraProfile.setCaptureHeight(captureHeight.getAsInt());
        currentCameraProfile.setCaptureWidth(captureWidth.getAsInt());
        currentCameraProfile.setHeadLightOn(headLightOn.selectedProperty().get());
        currentCameraProfile.setAmbientLightOn(ambientLightOn.selectedProperty().get());
        currentCameraProfile.setMoveBeforeSnapshot(moveBeforeSnapshot.selectedProperty().get());
        currentCameraProfile.setMoveToX(moveToX.getAsInt());
        currentCameraProfile.setMoveToY(moveToY.getAsInt());
        String cameraName = cmbCameraName.getValue().strip();
        if (cameraName.equalsIgnoreCase(ANY_CAMERA_NAME))
            cameraName = "";
        currentCameraProfile.setCameraName(cameraName);
    }
    
    public void setAndSelectCameraProfile(CameraProfile profile) {
        if (profile != null && 
            cameraProfilesMap.containsKey(profile.getProfileName().toLowerCase())) {
            cmbCameraProfile.setValue(profile.getProfileName());
            selectCameraProfile(profile.getProfileName());
        }
        else {
            selectDefaultCameraProfile();
        }
    }
    
    @Override
    public String getMenuTitle() 
    {
        return "extrasMenu.cameraProfile";
    }
    
    @Override
    public void panelSelected() 
    {
        
    }
    
    @Override
    public List<OperationButton> getOperationButtons() 
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

        MenuInnerPanel.OperationButton newButton = new MenuInnerPanel.OperationButton()
        {
            @Override
            public String getTextId()
            {
                return "projectLoader.newButtonLabel";
            }

            @Override
            public String getFXMLName()
            {
                return "saveAsButton";
            }

            @Override
            public String getTooltipTextId()
            {
                return "projectLoader.newButtonLabel";
            }

            @Override
            public void whenClicked()
            {
                whenNewPressed();
            }

            @Override
            public BooleanProperty whenEnabled()
            {
                return canCreateNew;
            }

        };

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
        
        operationButtons.add(saveButton);
        operationButtons.add(newButton);
        operationButtons.add(deleteButton);
        
        return operationButtons;
    }
}
