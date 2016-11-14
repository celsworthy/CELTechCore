package celtech.coreUI.controllers.panels;

import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.appManager.ShapeContainerProject;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.coreUI.components.RestrictedNumberField;
import celtech.coreUI.controllers.ProjectAwareController;
import celtech.roboxbase.printerControl.model.Printer;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class TwoDSettingsInsetPanelController implements Initializable, ProjectAwareController
{
    
    private final Stenographer steno = StenographerFactory.getStenographer(
            TwoDSettingsInsetPanelController.class.getName());
    
    @FXML
    private HBox settingsInsetRoot;
    
    @FXML
    private TabPane modelSelectionTabPane;
    
    @FXML
    private RestrictedNumberField bladeOffsetField;
    
    @FXML
    private RestrictedNumberField materialThicknessField;
    
    @FXML
    private RestrictedNumberField passesField;
    
    private Printer currentPrinter;
    private ShapeContainerProject currentProject;
    private String currentHeadType = HeadContainer.defaultHeadID;
    private boolean populatingForProject = false;

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
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
            
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void setProject(Project project)
    {
        whenProjectChanged(project);
    }
    
    ChangeListener<Boolean> materialThicknessChangeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        if (currentProject != null)
        {
            currentProject.getSettings().setMaterialThickness(materialThicknessField.getAsFloat());
        }
    };
    
    ChangeListener<Boolean> bladeOffsetChangeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        if (currentProject != null)
        {
            currentProject.getSettings().setBladeOffset(bladeOffsetField.getAsFloat());
        }
    };
    
    ChangeListener<Boolean> cuttingPassesChangeListener = (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) ->
    {
        if (currentProject != null)
        {
            currentProject.getSettings().setCuttingPasses(passesField.getAsInt());
        }
    };
    
    private void whenProjectChanged(Project project)
    {
        populatingForProject = true;
        
        if (currentProject != null)
        {
            materialThicknessField.valueChangedProperty().removeListener(materialThicknessChangeListener);
            bladeOffsetField.valueChangedProperty().removeListener(bladeOffsetChangeListener);
            passesField.valueChangedProperty().removeListener(cuttingPassesChangeListener);
        }
        
        if (project instanceof ShapeContainerProject)
        {
            currentProject = (ShapeContainerProject) project;
            
            materialThicknessField.setValue(currentProject.getSettings().getMaterialThickness());
            materialThicknessField.valueChangedProperty().addListener(materialThicknessChangeListener);
            
            bladeOffsetField.setValue(currentProject.getSettings().getBladeOffset());
            bladeOffsetField.valueChangedProperty().addListener(bladeOffsetChangeListener);
            
            passesField.setValue(currentProject.getSettings().getCuttingPasses());
            passesField.valueChangedProperty().addListener(cuttingPassesChangeListener);
        }
        populatingForProject = false;
    }
    
}
