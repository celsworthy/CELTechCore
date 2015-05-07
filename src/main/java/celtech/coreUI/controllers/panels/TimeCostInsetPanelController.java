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
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SlicerTask;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
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
    @FXML
    private RadioButton rbDraft;
    @FXML
    private RadioButton rbNormal;
    @FXML
    private RadioButton rbFine;
    @FXML
    private RadioButton rbCustom;

    private ToggleGroup qualityToggleGroup;

    private final SlicerParametersFile draftSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.draftSettingsProfileName);
    private final SlicerParametersFile normalSettings = SlicerParametersContainer.
        getSettingsByProfileName(
            ApplicationConfiguration.normalSettingsProfileName);
    private final SlicerParametersFile fineSettings = SlicerParametersContainer.getSettingsByProfileName(
        ApplicationConfiguration.fineSettingsProfileName);

    private Project currentProject;
    private PrinterSettings printerSettings;

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

            qualityToggleGroup = new ToggleGroup();
            rbDraft.setToggleGroup(qualityToggleGroup);
            rbDraft.setUserData(PrintQualityEnumeration.DRAFT);
            rbNormal.setToggleGroup(qualityToggleGroup);
            rbNormal.setUserData(PrintQualityEnumeration.NORMAL);
            rbFine.setToggleGroup(qualityToggleGroup);
            rbFine.setUserData(PrintQualityEnumeration.FINE);
            rbCustom.setToggleGroup(qualityToggleGroup);
            rbCustom.setUserData(PrintQualityEnumeration.CUSTOM);
            rbDraft.setSelected(true);
            qualityToggleGroup.selectedToggleProperty().addListener(
                (ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) ->
            {
                printerSettings.setPrintQuality((PrintQualityEnumeration) newValue.getUserData());
            });

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
        printerSettings = project.getPrinterSettings();

        projectChangesListener = new Project.ProjectChangesListener()
        {

            @Override
            public void whenModelAdded(ModelContainer modelContainer)
            {
                steno.debug("model added");
                updateFields(project);
            }

            @Override
            public void whenModelRemoved(ModelContainer modelContainer)
            {
                steno.debug("model removed");
                updateFields(project);
            }

            @Override
            public void whenAutoLaidOut()
            {
            }

            @Override
            public void whenModelsTransformed(Set<ModelContainer> modelContainers)
            {
                steno.debug("model transformed");
                updateFields(project);
            }

            @Override
            public void whenModelChanged(ModelContainer modelContainer, String propertyName)
            {
                steno.debug("model changed");
                updateFields(project);
            }

            @Override
            public void whenPrinterSettingsChanged(PrinterSettings printerSettings)
            {
                steno.debug("printer settings changed");
                updateFields(project);
            }
        };
        updateFields(project);
        updatePrintQuality(printerSettings);
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
        if (ApplicationStatus.getInstance().modeProperty().get() != ApplicationMode.SETTINGS)
        {
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

        Runnable runDraft = () ->
        {
            Runnable runNormal = () ->
            {
                Runnable runFine = () ->
                {

                    fineSlicerTask = updateFieldsForProfile(project, fineSettings, lblFineTime,
                                                            lblFineWeight,
                                                            lblFineCost, (Runnable) null);
                };
                normalSlicerTask = updateFieldsForProfile(project, normalSettings, lblNormalTime,
                                                          lblNormalWeight,
                                                          lblNormalCost, runFine);
            };
            draftSlicerTask = updateFieldsForProfile(project, draftSettings, lblDraftTime,
                                                     lblDraftWeight,
                                                     lblDraftCost, runNormal);
        };
        if (currentProject.getPrintQuality() == PrintQualityEnumeration.CUSTOM
            && !currentProject.getPrinterSettings().getSettingsName().equals(""))
        {
            SlicerParametersFile customSettings = currentProject.getPrinterSettings().getSettings();
            updateFieldsForProfile(project, customSettings, lblCustomTime,
                                   lblCustomWeight,
                                   lblCustomCost, runDraft);
        } else
        {
            lblCustomTime.setText("---");
            lblCustomWeight.setText("---");
            lblCustomCost.setText("---");
            runDraft.run();
        }
    }

    private void cancelRunningTimeCostTasks()
    {
        cancelTask(draftSlicerTask);
        cancelTask(normalSlicerTask);
        cancelTask(fineSlicerTask);
    }

    /**
     * Update the time, cost and weight fields for the given profile and fields. Long running
     * calculations must be performed in a background thread.
     */
    private SlicerTask updateFieldsForProfile(Project project, SlicerParametersFile settings,
        Label lblTime, Label lblWeight, Label lblCost, Runnable whenComplete)
    {
        String working = Lookup.i18n("timeCost.working");
        lblTime.setText(working);
        lblWeight.setText(working);
        lblCost.setText(working);

        GetTimeWeightCost updateDetails = new GetTimeWeightCost(project, settings,
                                                                lblTime, lblWeight,
                                                                lblCost, whenComplete);
        SlicerTask slicerTask = updateDetails.setupAndRunSlicerTask();
        return slicerTask;

    }

    private void updatePrintQuality(PrinterSettings printerSettings)
    {
        switch (printerSettings.getPrintQuality())
        {
            case DRAFT:
                rbDraft.setSelected(true);
                break;
            case NORMAL:
                rbNormal.setSelected(true);
                break;
            case FINE:
                rbFine.setSelected(true);
                break;
            case CUSTOM:
                rbCustom.setSelected(true);
                break;
        }
    }

    private void cancelTask(SlicerTask slicerTask)
    {
        if (slicerTask != null && !(slicerTask.isDone() || slicerTask.isCancelled()))
        {
            slicerTask.cancel();
        }
    }

}
