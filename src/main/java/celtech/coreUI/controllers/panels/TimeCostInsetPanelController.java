package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.modelcontrol.ModelContainer;
import celtech.services.slicer.SlicerTask;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * FXML Controller class
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class TimeCostInsetPanelController implements Initializable
{

    private final Stenographer steno = StenographerFactory.getStenographer(
        TimeCostInsetPanelController.class.getName());

    @FXML
    private HBox timeCostInsetRoot;

    @FXML
    private Label lblDraftTime;
    @FXML
    private Label lblNormalTime;
    @FXML
    private Label lblFineTime;
    @FXML
    private Label lblCustomTime;
    @FXML
    private Label lblDraftWeight;
    @FXML
    private Label lblNormalWeight;
    @FXML
    private Label lblFineWeight;
    @FXML
    private Label lblCustomWeight;
    @FXML
    private Label lblDraftCost;
    @FXML
    private Label lblNormalCost;
    @FXML
    private Label lblFineCost;
    @FXML
    private Label lblCustomCost;

    private final SlicerParametersFile draftSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.draftSettingsProfileName);
    private final SlicerParametersFile normalSettings = SlicerParametersContainer.
        getSettingsByProfileName(
            ApplicationConfiguration.normalSettingsProfileName);
    private final SlicerParametersFile fineSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.fineSettingsProfileName);

    private Project currentProject;

    /**
     * Initialises the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        try
        {
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
                        timeCostInsetRoot.setVisible(true);
                    } else
                    {
                        timeCostInsetRoot.setVisible(false);
                    }
                });

            if (Lookup.getSelectedProjectProperty().get() != null)
            {
                updateFields(Lookup.getSelectedProjectProperty().get());
            }
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void unbindProject(Project project)
    {
        project.removeProjectChangesListener(projectChangesListener);
    }

    Project.ProjectChangesListener projectChangesListener;

    private void bindProject(Project project)
    {
        projectChangesListener = new Project.ProjectChangesListener()
        {

            @Override
            public void whenModelAdded(ModelContainer modelContainer)
            {
                steno.info("model added");
                updateFields(project);
            }

            @Override
            public void whenModelRemoved(ModelContainer modelContainer)
            {
                steno.info("model removed");
                updateFields(project);
            }

            @Override
            public void whenAutoLaidOut()
            {
            }

            @Override
            public void whenModelsTransformed(Set<ModelContainer> modelContainers)
            {
                steno.info("model transformed");
                updateFields(project);
            }

            @Override
            public void whenModelChanged(ModelContainer modelContainer, String propertyName)
            {
                steno.info("model changed");
                updateFields(project);
            }

            @Override
            public void whenPrinterSettingsChanged(PrinterSettings printerSettings)
            {
                steno.info("printer settings changed");
                updateFields(project);
            }
        };
        updateFields(project);
        project.addProjectChangesListener(projectChangesListener);
    }

    private void whenProjectChanged(Project project)
    {
        currentProject = project;
        if (project != null)
        {
            if (currentProject != null)
            {
                unbindProject(currentProject);
            }
            bindProject(project);
            currentProject = project;
        }
    }

    /**
     * Update the time, cost and weight fields. Long running calculations must be performed in a
     * background thread.
     */
    private void updateFields(Project project)
    {
        Runnable runNormal = () ->
        {
            Runnable runFine = () ->
            {
                updateFieldsForProfile(project, fineSettings, lblFineTime,
                                       lblFineWeight,
                                       lblFineCost, null);
            };
            updateFieldsForProfile(project, normalSettings, lblNormalTime,
                                   lblNormalWeight,
                                   lblNormalCost, (Runnable) runFine);
        };
        updateFieldsForProfile(project, draftSettings, lblDraftTime,
                               lblDraftWeight,
                               lblDraftCost, runNormal);
    }

    /**
     * Update the time, cost and weight fields for the given profile and fields. Long running
     * calculations must be performed in a background thread.
     */
    private void updateFieldsForProfile(Project project, SlicerParametersFile settings,
        Label lblTime, Label lblWeight, Label lblCost, Runnable whenComplete)
    {
        lblTime.setText("working");
        lblWeight.setText("working");
        lblCost.setText("working");

        GetTimeWeightCost updateDetails = new GetTimeWeightCost(project, settings,
                                                                lblTime, lblWeight,
                                                                lblCost, whenComplete);
        SlicerTask slicerTask = updateDetails.setupSlicerTask();
        slicerTask.setOnCancelled((WorkerStateEvent event) ->
        {
            lblTime.setText("cancelled");
            lblWeight.setText("cancelled");
            lblCost.setText("cancelled");
        });

        Lookup.getTaskExecutor().runTaskAsDaemon(slicerTask);

    }

}
