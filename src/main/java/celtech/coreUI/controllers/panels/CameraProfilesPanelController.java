package celtech.coreUI.controllers.panels;

import celtech.coreUI.DisplayManager;
import celtech.coreUI.components.RestrictedComboBox;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.datafileaccessors.CameraProfileContainer;
import celtech.roboxbase.configuration.fileRepresentation.CameraProfile;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
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
    private CheckBox lockFocus;
    
    @FXML
    private RestrictedNumberField focusValue;
    
    @FXML
    private CheckBox headLight;
    
    @FXML
    private  CheckBox ambientLight;
    
    private Map<String, CameraProfile> cameraProfilesMap;
    
    private CameraProfile currentCameraProfile;
    private String selectedProfileName;
    
    private final ChangeListener<Object> dirtyFieldListener = (ObservableValue<? extends Object> ov, Object oldEntry, Object newEntry) -> {
            isDirty.set(true);
    };
    
    @Override
    public void initialize(URL location, ResourceBundle resources) 
    {
        setupProfileNameChangeListeners();
        setupProfileFieldListeners();
        
        canSave.bind(isNameValid
                .and(isDirty
                        .and(state.isEqualTo(State.NEW)
                                .or(state.isEqualTo(State.CUSTOM)))));
        canCreateNew.bind(state.isNotEqualTo(State.NEW));
        canDelete.bind(state.isEqualTo(State.CUSTOM));
        
        cmbCameraProfile.valueProperty().addListener((observable, oldValue, newValue) -> {
            selectCameraProfile(newValue);
        });
        
        DisplayManager.getInstance().libraryModeEnteredProperty().addListener((observable, oldValue, enteredLibraryMode) -> {
            if (enteredLibraryMode)
            {
                repopulateCmbCameraProfiles();
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

    private void repopulateCmbCameraProfiles()
    {
       cameraProfilesMap = CAMERA_PROFILE_CONTAINER.getCameraProfilesMap();
       cmbCameraProfile.setItems(FXCollections.observableArrayList(cameraProfilesMap.keySet()));
    }
    
    private void selectDefaultCameraProfile()
    {
        cmbCameraProfile.getSelectionModel().select(0);
    }
    
    private void selectCameraProfile(String profileName)
    {
        if (cameraProfilesMap.containsKey(profileName))
        {
            currentCameraProfile = cameraProfilesMap.get(profileName);
            updateValuesFromProfile(currentCameraProfile);
            selectedProfileName = currentCameraProfile.getProfileName();
            
            State newState = profileName.equals(BaseConfiguration.defaultCameraProfileName)
                    ? State.ROBOX : State.CUSTOM;
            state.set(newState);
        }
    }
    
    private void updateValuesFromProfile(CameraProfile cameraProfile)
    {
        captureHeight.setValue(cameraProfile.getCaptureHeight());
        captureWidth.setValue(cameraProfile.getCaptureWidth());
        lockFocus.selectedProperty().set(cameraProfile.isLockFocus());
        focusValue.setValue(cameraProfile.getFocusValue());
        headLight.selectedProperty().set(cameraProfile.isHeadLight());
        ambientLight.selectedProperty().set(cameraProfile.isAmbientLight());
    }
    
    private boolean validateProfileName() 
    {
        boolean valid = true;
        String profileNameText = cmbCameraProfile.getEditor().getText();

        if (profileNameText.equals("")) 
        {
            valid = false;
        } else 
        {
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
            repopulateCmbCameraProfiles();
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
        repopulateCmbCameraProfiles();
        selectDefaultCameraProfile();
    }
    
    private void setupProfileFieldListeners()
    {
        captureHeight.valueChangedProperty().addListener(dirtyFieldListener);
        captureWidth.valueChangedProperty().addListener(dirtyFieldListener);
        lockFocus.selectedProperty().addListener(dirtyFieldListener);
        focusValue.valueChangedProperty().addListener(dirtyFieldListener);
        headLight.selectedProperty().addListener(dirtyFieldListener);
        ambientLight.selectedProperty().addListener(dirtyFieldListener);
    }
    
    private void updateProfileWithCurrentValues()
    {
        currentCameraProfile.setCaptureHeight(captureHeight.getAsInt());
        currentCameraProfile.setCaptureWidth(captureWidth.getAsInt());
        currentCameraProfile.setLockFocus(lockFocus.selectedProperty().get());
        currentCameraProfile.setFocusValue(focusValue.getAsInt());
        currentCameraProfile.setHeadLight(headLight.selectedProperty().get());
        currentCameraProfile.setAmbientLight(ambientLight.selectedProperty().get());
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
