package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.appManager.TimelapseSettingsData;
import celtech.coreUI.DisplayManager;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.utils.CameraInfoStringConverter;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.camera.CameraInfo;
import celtech.roboxbase.comms.DetectedServer;
import celtech.roboxbase.comms.RemoteDetectedPrinter;
import celtech.roboxbase.comms.remote.RoboxRemoteCommandInterface;
import celtech.roboxbase.configuration.datafileaccessors.CameraProfileContainer;
import celtech.roboxbase.configuration.datafileaccessors.RoboxProfileSettingsContainer;
import celtech.roboxbase.configuration.fileRepresentation.CameraProfile;
import celtech.roboxbase.configuration.fileRepresentation.CameraSettings;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.utils.CameraProfileStringConverter;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class TimelapseInsetPanelController implements Initializable, ProjectAwareController, ModelContainerProject.ProjectChangesListener
{

    private final Stenographer STENO = StenographerFactory.getStenographer(TimelapseInsetPanelController.class.getName());

    private static final RoboxProfileSettingsContainer ROBOX_PROFILE_SETTINGS_CONTAINER = RoboxProfileSettingsContainer.getInstance();
    
    @FXML
    private GridPane timelapseInsetRoot;

    @FXML
    private CheckBox timelapseEnableButton;

    @FXML
    private ComboBox<CameraProfile> cameraProfileChooser;

    @FXML
    private Button editCameraProfileButton;

    @FXML
    private ComboBox<CameraInfo> cameraChooser;

    @FXML
    private Button testCameraButton;

    @FXML
    private ImageView snapshotView;
    private Task<Void> snapshotTask = null;
    private Printer currentPrinter = null;
    private DetectedServer currentServer = null;
    private Project currentProject = null;
    private CameraInfo selectedCamera = null;
    
    
    private final ChangeListener<Printer> selectedPrinterChangeListener = 
            (ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) -> {
        whenPrinterChanged(newValue);
    };

    private final ChangeListener<ApplicationMode> applicationModeChangeListener = new ChangeListener<ApplicationMode>()
    {
        @Override
        public void changed(ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue)
        {
            setPanelVisibility();
        }
    };

    @FXML
    void editCameraProfile(ActionEvent event)
    {
        DisplayManager.getInstance().showAndSelectCameraProfile(cameraProfileChooser.getValue());
    }

    @FXML
    void testCamera(ActionEvent event)
    {
        takeSnapshot();
    }
    
    @FXML
    void timelapseEnableAction(ActionEvent event)
    {
        if (currentProject != null) {
            currentProject.getTimelapseSettings().setTimelapseTriggerEnabled(timelapseEnableButton.isSelected());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try {
            timelapseEnableButton.setSelected(false);
            // Disable all elements as they are not all in a common sub-panel.
            //cameraChooser.disableProperty().bind(timelapseEnableButton.selectedProperty().not());
            //cameraProfileChooser.disableProperty().bind(timelapseEnableButton.selectedProperty().not());
            //editCameraProfileButton.disableProperty().bind(timelapseEnableButton.selectedProperty().not());
            //testCameraButton.disableProperty().bind(timelapseEnableButton.selectedProperty().not());
            
            cameraProfileChooser.setConverter(new CameraProfileStringConverter(cameraProfileChooser::getItems));

            cameraProfileChooser.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (currentProject != null) {
                    currentProject.getTimelapseSettings().setTimelapseProfile(Optional.ofNullable(newValue));
                    populateCameraChooser();
                    if (newValue != null) {
                        cameraChooser.getItems()
                                     .stream()
                                     .filter(ci -> newValue.getCameraName().isBlank() ||
                                                   ci.getCameraName().equalsIgnoreCase(newValue.getCameraName()))
                                     .findFirst()
                                     .ifPresentOrElse(cameraChooser::setValue, 
                                                      () -> { cameraChooser.setValue(cameraChooser.getItems().size() > 0 ? cameraChooser.getItems().get(0) : null); });
                    }
                    else
                        cameraChooser.setValue(cameraChooser.getItems().size() > 0 ? cameraChooser.getItems().get(0) : null);
                }
            });

            DisplayManager.getInstance().libraryModeEnteredProperty().addListener((observable, oldValue, enteredLibraryMode) -> {
                if (!enteredLibraryMode)
                {
                    String selectedProfileName = cameraProfileChooser.getValue().getProfileName();
                    populateCameraProfileChooser();
                    List<CameraProfile> itemList = cameraProfileChooser.getItems();
                    itemList.stream()
                            .filter(p -> p.getProfileName().equals(selectedProfileName))
                            .findAny()
                            .ifPresentOrElse(cameraProfileChooser::setValue, 
                                             () -> { cameraProfileChooser.setValue(CameraProfileContainer.getInstance().getDefaultProfile()); });
                }
            });

            cameraChooser.setConverter(new CameraInfoStringConverter(cameraChooser::getItems));
            BaseLookup.getConnectedCameras().addListener((ListChangeListener.Change<? extends CameraInfo> c) -> {
                repopulateCameraChooser();
            });

            cameraChooser.valueProperty().addListener((observable, oldValue, newValue) -> {
                selectedCamera = newValue;
                if (currentProject != null) {
                    currentProject.getTimelapseSettings().setTimelapseCamera(Optional.ofNullable(newValue));
                    if (selectedCamera == null)
                        snapshotView.setImage(null);
                    else
                        takeSnapshot();
                }
            });

            if (cameraChooser.getItems().size() > 0) {
                cameraChooser.setValue(cameraChooser.getItems().get(0));
            }

            Lookup.getSelectedPrinterProperty().addListener(selectedPrinterChangeListener);

            ApplicationStatus.getInstance().modeProperty().addListener(applicationModeChangeListener);


            whenPrinterChanged(Lookup.getSelectedPrinterProperty().get());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void whenPrinterChanged(Printer printer)
    {
        if (currentPrinter != null)
        {
        }

        currentPrinter = printer;
        if (currentPrinter != null && 
            currentPrinter.getCommandInterface() instanceof RoboxRemoteCommandInterface) {
            currentServer = ((RemoteDetectedPrinter)currentPrinter.getCommandInterface().getPrinterHandle()).getServerPrinterIsAttachedTo();
            repopulateCameraProfileChooser();
            repopulateCameraChooser();
        }
        else
            currentServer = null;
        
        setPanelVisibility();
    }

    private void repopulateCameraChooser() {
        CameraInfo chosenCamera = cameraChooser.getValue();
        populateCameraChooser();
        cameraChooser.getItems()
                     .stream()
                     .filter(ci -> ci.equals(chosenCamera))
                     .findAny()
                     .ifPresentOrElse(cameraChooser::setValue, 
                                      () -> { cameraChooser.setValue(cameraChooser.getItems().size() > 0 ? cameraChooser.getItems().get(0) : null); });
    }

    private void populateCameraChooser() {
        String cameraName = (cameraProfileChooser.getValue() != null ? cameraProfileChooser.getValue().getCameraName()
                                                                     : "");
        if (currentServer != null) {
            ObservableList<CameraInfo> itemList = BaseLookup.getConnectedCameras().stream()
                    .filter(cc -> cc.getServer() == currentServer &&
                                  (cameraName.isBlank() ||
                                   cameraName.equalsIgnoreCase(cc.getCameraName())))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList))
                    .sorted();
            cameraChooser.setItems(itemList);
        }
        else
        {
            cameraChooser.setItems(FXCollections.emptyObservableList());
        }
    }

    private void setPanelVisibility()
    {
        if (ApplicationStatus.getInstance().modeProperty().get() == ApplicationMode.SETTINGS &&
            currentServer != null &&
            currentServer.getCameraDetected())
        {
            timelapseInsetRoot.setVisible(true);
            timelapseInsetRoot.setMouseTransparent(false);
            if (snapshotTask == null) {
                takeSnapshot();
            }
            populateCameraProfileChooser();
            populateCameraChooser();
        }
        else {
            timelapseInsetRoot.setVisible(false);
            timelapseInsetRoot.setMouseTransparent(true);
            if (snapshotTask != null) {
                snapshotTask.cancel();
                snapshotTask = null;
            }
        }
    }

    @Override
    public void setProject(Project project)
    {
        if (snapshotTask != null) {
            snapshotTask.cancel();
            snapshotTask = null;
        }

        if (currentProject != null)
        {
            currentProject.removeProjectChangesListener(this);
        }

        currentProject = project;
        if (project != null)
        {
            project.addProjectChangesListener(this);
            whenProjectChanged(project);
        }
    }

    private void whenProjectChanged(Project project)
    {
        whenTimelapseSettingsChanged(project.getTimelapseSettings());
    }

    @Override
    public void whenModelAdded(ProjectifiableThing modelContainer)
    {
    }

    @Override
    public void whenModelsRemoved(Set<ProjectifiableThing> modelContainers)
    {
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
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings)
    {
    }

    @Override
    public void whenTimelapseSettingsChanged(TimelapseSettingsData timelapseSettings)
    {
        timelapseEnableButton.setSelected(timelapseSettings.getTimelapseTriggerEnabled());
        timelapseSettings.getTimelapseProfile()
                         .ifPresentOrElse(cameraProfileChooser::setValue, 
                                          () -> { cameraProfileChooser.setValue(CameraProfileContainer.getInstance().getDefaultProfile()); });
        
        timelapseSettings.getTimelapseCamera()
                         .ifPresentOrElse(cameraChooser::setValue, 
                                          () -> { 
                                            if (cameraChooser.getItems().size() > 0) {
                                                // This will recursively call whenTimeLapseSettingsChanged and set the cameraChooser value.
                                                timelapseSettings.setTimelapseCamera(Optional.ofNullable(cameraChooser.getItems().get(0)));
                                            }
                                            else
                                                cameraChooser.setValue(null); 
                                          });
    }

    @Override
    public void shutdownController()
    {
        if (snapshotTask != null)
            snapshotTask.cancel();
            
        if (currentPrinter != null)
        {
        }

        if (currentProject != null)
        {
            currentProject.removeProjectChangesListener(this);
        }
        currentProject = null;

        Lookup.getSelectedPrinterProperty().removeListener(selectedPrinterChangeListener);

        ApplicationStatus.getInstance().modeProperty().removeListener(applicationModeChangeListener);
    }

    private void populateCameraProfileChooser() {
        // Get the names of all the cameras on the current server.
        if (currentServer != null) {
            List<String> cameraNames = BaseLookup.getConnectedCameras()
                                                 .stream()
                                                 .filter(cc -> cc.getServer() == currentServer)
                                                 .map(CameraInfo::getCameraName)
                                                 .distinct()
                                                 .collect(Collectors.toList());
            Map<String, CameraProfile> cameraProfilesMap = CameraProfileContainer.getInstance().getCameraProfilesMap();
            ObservableList<CameraProfile> items = cameraProfilesMap.values()
                                        .stream()
                                        .filter(pp -> pp.getCameraName().isBlank() ||
                                                      cameraNames.contains(pp.getCameraName()))
                                        .collect(Collectors.toCollection(FXCollections::observableArrayList));
            cameraProfileChooser.setItems(items);
        }
        else
            cameraProfileChooser.setItems(FXCollections.emptyObservableList());
    }
    
    private void repopulateCameraProfileChooser() {
        CameraProfile selectedProfile = cameraProfileChooser.getValue();
        populateCameraProfileChooser();
        // Interesting mix of programming styles.
        if (selectedProfile != null) {
            String selectedProfileName = selectedProfile.getProfileName();
            List<CameraProfile> itemList = cameraProfileChooser.getItems();
            itemList.stream()
                    .filter(p -> p.getProfileName().equals(selectedProfileName))
                    .findAny()
                    .ifPresentOrElse(cameraProfileChooser::setValue, 
                                     () -> { cameraProfileChooser.setValue(CameraProfileContainer.getInstance().getDefaultProfile()); });
        }
        else
            cameraProfileChooser.setValue(CameraProfileContainer.getInstance().getDefaultProfile());
    }
    
    private void takeSnapshot()
    {
        if (snapshotTask != null) {
            snapshotTask.cancel();
            snapshotTask = null;
        }
        CameraProfile selectedProfile = cameraProfileChooser.getValue();
        if (selectedProfile != null && selectedCamera != null) {
            CameraSettings snapshotSettings = new CameraSettings(selectedProfile, selectedCamera);
            snapshotTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    DetectedServer server = snapshotSettings.getCamera().getServer();
                    while (!isCancelled()) {
                        Image snapshotImage = server.takeCameraSnapshot(snapshotSettings);
                        BaseLookup.getTaskExecutor().runOnGUIThread(() -> {
                            if (selectedCamera == snapshotSettings.getCamera()) {
                                snapshotView.setImage(snapshotImage);
                            }
                        });
                        if (!isCancelled()) {
                            Thread.sleep(500);
                        }
                    }
                    return null;
                }
            };
            Thread snapshotThread = new Thread(snapshotTask);
            snapshotThread.start();
        }
    }
}
