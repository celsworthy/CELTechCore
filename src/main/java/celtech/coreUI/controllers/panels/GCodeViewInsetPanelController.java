package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.ModelContainerProject;
import celtech.appManager.Project;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.modelcontrol.ProjectifiableThing;
import celtech.roboxbase.configuration.datafileaccessors.RoboxProfileSettingsContainer;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import org.eclipse.fx.drift.DriftFXSurface;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class GCodeViewInsetPanelController implements Initializable, ProjectAwareController, ModelContainerProject.ProjectChangesListener
{
    private final Stenographer steno = StenographerFactory.getStenographer(GCodeViewInsetPanelController.class.getName());

    private static final RoboxProfileSettingsContainer ROBOX_PROFILE_SETTINGS_CONTAINER = RoboxProfileSettingsContainer.getInstance();
    private static boolean driftInitialised = false;
    
    @FXML
    private HBox gCodeViewInsetRoot;

    private AMTriangleRenderer triangleRenderer = null;
    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        if (!driftInitialised) {
            driftInitialised = true;
            DriftFXSurface.initialize();
        }

        DriftFXSurface surface0 = new DriftFXSurface();
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(surface0.widthProperty());
        clip.heightProperty().bind(surface0.heightProperty());
        surface0.setClip(clip);

        triangleRenderer = new AMTriangleRenderer(surface0);
        gCodeViewInsetRoot.getChildren().add(surface0);
        Button startButton = new Button("Start");
        startButton.setMinWidth(Region.USE_PREF_SIZE);
        startButton.setMinHeight(Region.USE_PREF_SIZE);
        startButton.setOnAction(a -> {
            triangleRenderer.start();
	});

        gCodeViewInsetRoot.getChildren().add(startButton);
        
        Button stopButton = new Button("Stop");
        stopButton.setMinWidth(Region.USE_PREF_SIZE);
        stopButton.setMinHeight(Region.USE_PREF_SIZE);
        stopButton.setOnAction(a -> {
            triangleRenderer.stop();
	});

        gCodeViewInsetRoot.getChildren().add(stopButton);

        ApplicationStatus.getInstance().modeProperty().addListener(applicationModeChangeListener);
    }

    @Override
    public void setProject(Project project) {
    }

    @Override
    public void shutdownController() {
        if (triangleRenderer != null)
            triangleRenderer.stop();
        ApplicationStatus.getInstance().modeProperty().removeListener(applicationModeChangeListener);
   }

    @Override
    public void whenModelAdded(ProjectifiableThing projectifiableThing) {
    }

    @Override
    public void whenModelsRemoved(Set<ProjectifiableThing> projectifiableThing) {
    }

    @Override
    public void whenAutoLaidOut() {
    }

    @Override
    public void whenModelsTransformed(Set<ProjectifiableThing> projectifiableThing) {
    }

    @Override
    public void whenModelChanged(ProjectifiableThing modelContainer, String propertyName) {
    }

    @Override
    public void whenPrinterSettingsChanged(PrinterSettingsOverrides printerSettings) {
    }
    
    private final ChangeListener<ApplicationMode> applicationModeChangeListener = new ChangeListener<ApplicationMode>()
    {
        @Override
        public void changed(ObservableValue<? extends ApplicationMode> observable, ApplicationMode oldValue, ApplicationMode newValue)
        {
            if (newValue == ApplicationMode.SETTINGS)
            {
                gCodeViewInsetRoot.setVisible(true);
                gCodeViewInsetRoot.setMouseTransparent(false);
            } else
            {
                gCodeViewInsetRoot.setVisible(false);
                gCodeViewInsetRoot.setMouseTransparent(true);
            }
        }
    };

}
