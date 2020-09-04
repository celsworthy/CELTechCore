package celtech.coreUI.controllers.utilityPanels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.roboxbase.camera.CameraInfo;
import celtech.roboxbase.comms.RemoteDetectedPrinter;
import celtech.roboxbase.comms.remote.RoboxRemoteCommandInterface;
import celtech.roboxbase.configuration.fileRepresentation.CameraProfile;
import celtech.roboxbase.configuration.fileRepresentation.CameraSettings;
import celtech.roboxbase.printerControl.model.Printer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/**
 * FXML Controller class
 *
 * @author Ian
 */
public class SnapshotPanelController extends SnapshotController
{
    private Printer connectedPrinter = null;

    private final ChangeListener<Boolean> cameraDetectedChangeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
        controlSnapshotTask();
    };
    
    private final ChangeListener<ApplicationMode> applicationModeChangeListener = (ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue) -> {
        controlSnapshotTask();
    };

    private final ChangeListener<CameraSettings> cameraSettingsChangeListener = (ObservableValue<? extends CameraSettings> observable, CameraSettings oldValue, CameraSettings newValue) -> {
        selectCameraAndProfile(newValue.getProfile(), newValue.getCamera());
    };

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        viewWidthFixed = true;
        super.initialize(url, rb);
        Lookup.getSelectedPrinterProperty().addListener((ObservableValue<? extends Printer> observable, Printer oldValue, Printer newValue) -> {
            if (connectedPrinter != null)
                unbindFromPrinter(connectedPrinter);
            
            if (newValue != null)
                bindToPrinter(newValue);
        });
        ApplicationStatus.getInstance().modeProperty().addListener(applicationModeChangeListener);
    }
    
    private void unbindFromPrinter(Printer printer)
    {
        if (connectedPrinter != null) {
            connectedPrinter = null;
        }
        
        if (connectedServer != null) {
            connectedServer.cameraDetectedProperty().removeListener(cameraDetectedChangeListener);
            connectedServer.cameraSettingsProperty().removeListener(cameraSettingsChangeListener);
            connectedServer = null;
        }
        controlSnapshotTask();
    }

    private void bindToPrinter(Printer printer)
    {
        connectedPrinter = printer;
        if (connectedPrinter != null && 
            connectedPrinter.getCommandInterface() instanceof RoboxRemoteCommandInterface) {
            connectedServer = ((RemoteDetectedPrinter)connectedPrinter.getCommandInterface().getPrinterHandle()).getServerPrinterIsAttachedTo();
            connectedServer.cameraDetectedProperty().addListener(cameraDetectedChangeListener);
            connectedServer.cameraSettingsProperty().addListener(cameraSettingsChangeListener);
            CameraSettings settings = connectedServer.cameraSettingsProperty().get();
            if (settings != null)
                selectCameraAndProfile(settings.getProfile(), settings.getCamera());
            else if (selectedProfile != null && selectedCamera != null) {
                connectedServer.cameraSettingsProperty().set(new CameraSettings(selectedProfile, selectedCamera));
            }
        }
        controlSnapshotTask();
    }
    
    private void controlSnapshotTask()
    {
        if (ApplicationStatus.getInstance().modeProperty().get() == ApplicationMode.STATUS &&
            connectedServer != null &&
            connectedServer.getCameraDetected())
        {
            repopulateCameraProfileChooser();
            repopulateCameraChooser();
            if (snapshotTask == null) {
                takeSnapshot();
            }
        }
        else {
            if (snapshotTask != null) {
                snapshotTask.cancel();
                snapshotTask = null;
            }
        }
    }
    
    @Override
    protected void selectProfile(CameraProfile profile) {
        super.selectProfile(profile);
        if (connectedServer != null)
            connectedServer.cameraSettingsProperty().get().setProfile(profile);
    }
    
    @Override
    protected void selectCamera(CameraInfo camera) {
        super.selectCamera(camera);
        if (connectedServer != null)
            connectedServer.cameraSettingsProperty().get().setCamera(camera);
    }
}
