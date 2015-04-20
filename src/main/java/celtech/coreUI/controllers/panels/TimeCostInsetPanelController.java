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
                        updateFields(Lookup.getSelectedProjectProperty().get());
                    } else
                    {
                        timeCostInsetRoot.setVisible(false);
                        cancelRunningTimeCostTasks();
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

    private SlicerTask draftSlicerTask;
    private SlicerTask normalSlicerTask;
    private SlicerTask fineSlicerTask;

    /**
     * Update the time, cost and weight fields. Long running calculations must be performed in a
     * background thread. Run draft, normal and fine sequentially to avoid flooding the CPU(s).
     */
    private void updateFields(Project project)
    {
        if (ApplicationStatus.getInstance().modeProperty().get() != ApplicationMode.SETTINGS) {
            return;
        }
        
        lblDraftTime.setText("...");
        lblNormalTime.setText("...");
        lblFineTime.setText("...");
        lblDraftWeight.setText("...");
        lblNormalWeight.setText("...");
        lblFineWeight.setText("...");
        lblDraftCost.setText("...");
        lblNormalCost.setText("...");
        lblFineCost.setText("...");

        cancelRunningTimeCostTasks();

        Runnable runNormal = () ->
        {
            Runnable runFine = () ->
            {
                fineSlicerTask = updateFieldsForProfile(project, fineSettings, lblFineTime,
                                                        lblFineWeight,
                                                        lblFineCost, null);
            };
            normalSlicerTask = updateFieldsForProfile(project, normalSettings, lblNormalTime,
                                                      lblNormalWeight,
                                                      lblNormalCost, (Runnable) runFine);
        };
        draftSlicerTask = updateFieldsForProfile(project, draftSettings, lblDraftTime,
                                                 lblDraftWeight,
                                                 lblDraftCost, runNormal);
    }

    private void cancelRunningTimeCostTasks()
    {
        System.out.println("Cancel any running time tasks");
        System.out.println("slicer tasks " + draftSlicerTask + " " + normalSlicerTask + " " + fineSlicerTask);
        if (draftSlicerTask != null ) {
            System.out.println("draft status " + draftSlicerTask.getState().name());
        }
        if (normalSlicerTask != null ) {
            System.out.println("normal status " + normalSlicerTask.getState().name());
        }
        if (fineSlicerTask != null ) {
            System.out.println("fine status " + fineSlicerTask.getState().name());
        }
        
        if (draftSlicerTask != null && draftSlicerTask.isRunning())
        {
            System.out.println("CANCEL draft");
            draftSlicerTask.cancel();
        }
        if (normalSlicerTask != null && normalSlicerTask.isRunning())
        {
            System.out.println("CANCEL normal");
            normalSlicerTask.cancel();
        }
        if (fineSlicerTask != null && fineSlicerTask.isRunning())
        {
            System.out.println("CANCEL fine");
            fineSlicerTask.cancel();
        }
    }

    /**
     * Update the time, cost and weight fields for the given profile and fields. Long running
     * calculations must be performed in a background thread.
     */
    private SlicerTask updateFieldsForProfile(Project project, SlicerParametersFile settings,
        Label lblTime, Label lblWeight, Label lblCost, Runnable whenComplete)
    {
        lblTime.setText("working");
        lblWeight.setText("working");
        lblCost.setText("working");

        GetTimeWeightCost updateDetails = new GetTimeWeightCost(project, settings,
                                                                lblTime, lblWeight,
                                                                lblCost, whenComplete);
        SlicerTask slicerTask = updateDetails.setupAndRunSlicerTask();
        return slicerTask;

    }

}
