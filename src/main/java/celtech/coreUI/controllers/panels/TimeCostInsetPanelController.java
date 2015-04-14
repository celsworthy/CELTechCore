package celtech.coreUI.controllers.panels;

import celtech.Lookup;
import celtech.appManager.ApplicationMode;
import celtech.appManager.ApplicationStatus;
import celtech.appManager.Project;
import celtech.configuration.ApplicationConfiguration;
import celtech.configuration.SlicerType;
import celtech.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.configuration.fileRepresentation.SlicerParametersFile;
import celtech.configuration.slicer.SlicerConfigWriter;
import celtech.configuration.slicer.SlicerConfigWriterFactory;
import celtech.coreUI.controllers.PrinterSettings;
import celtech.gcodetranslator.PrintJobStatistics;
import celtech.services.postProcessor.GCodePostProcessingResult;
import celtech.services.postProcessor.PostProcessorTask;
import celtech.services.slicer.PrintQualityEnumeration;
import celtech.services.slicer.SliceResult;
import celtech.services.slicer.SlicerTask;
import celtech.utils.SystemUtils;
import celtech.utils.threed.ThreeDUtils;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

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
    private PrinterSettings printerSettings = null;

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
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void whenProjectChanged(Project project)
    {
        currentProject = project;
        printerSettings = project.getPrinterSettings();
        if (project != null)
        {
            updateFields(project);
        }
    }

    /**
     * Update the time, cost and weight fields. Long running calculations must be performed in a
     * background thread.
     */
    private void updateFields(Project project)
    {
        updateFieldsForProfile(project, draftSettings, lblDraftTime, lblDraftWeight, lblDraftCost);
    }

    /**
     * Update the time, cost and weight fields for the given profile and fields. Long running
     * calculations must be performed in a background thread.
     */
    private void updateFieldsForProfile(Project project, SlicerParametersFile draftSettings,
        Label lblDraftTime, Label lblDraftWeight, Label lblDraftCost)
    {
        lblDraftTime.setText("...");
        lblDraftWeight.setText("...");
        lblDraftCost.setText("...");

        GetTimeWeightCostTask getDetails = new GetTimeWeightCostTask(project, fineSettings);

        getDetails.setOnSucceeded((WorkerStateEvent event) ->
        {
            TimeWeightCost timeWeightCost = getDetails.getValue();
            lblDraftTime.setText(String.valueOf(timeWeightCost.timeSeconds));
            lblDraftWeight.setText(String.valueOf(timeWeightCost.weightGrams));
            lblDraftCost.setText(String.valueOf(timeWeightCost.costPence));
        });

        getDetails.setOnFailed((WorkerStateEvent event) ->
        {
            lblDraftTime.setText("NA");
            lblDraftWeight.setText(String.valueOf("NA"));
            lblDraftCost.setText(String.valueOf("NA"));
        });

        Thread th = new Thread(getDetails);
        th.setDaemon(true);
        th.start();

    }

    class TimeWeightCost
    {

        double timeSeconds = 0;
        double weightGrams = 0;
        double costPence = 0;
    }

    class GetTimeWeightCostTask extends Task<TimeWeightCost>
    {

        private final Project project;
        private final SlicerParametersFile settings;

        public GetTimeWeightCostTask(Project project, SlicerParametersFile settings)
        {
            this.project = project;
            this.settings = settings;
        }

        @Override
        protected TimeWeightCost call() throws Exception
        {
            try
            {
                SlicerTask slicerTask = makeSlicerTask(project, settings);
                slicerTask.run();
                SliceResult sliceResult = slicerTask.getValue();

                PostProcessorTask postProcessorTask = new PostProcessorTask(
                    sliceResult.getPrintJobUUID(),
                    settings, null);
                postProcessorTask.run();
                GCodePostProcessingResult result = postProcessorTask.getValue();
                PrintJobStatistics printJobStatistics = result.getRoboxiserResult().getPrintJobStatistics();
                double duration = printJobStatistics.getLayerNumberToPredictedDuration().get(
                    printJobStatistics.getNumberOfLines());
                TimeWeightCost timeWeightCost = new TimeWeightCost();
                timeWeightCost.timeSeconds = duration;
                return timeWeightCost;
            } catch (Exception ex)
            {
                ex.printStackTrace();
                return null;
            }
        }

        private SlicerTask makeSlicerTask(Project project, SlicerParametersFile settings)
        {
            //Create the print job directory
            String printUUID = SystemUtils.generate16DigitID();
            String printJobDirectoryName = ApplicationConfiguration.
                getPrintSpoolDirectory() + printUUID;
            File printJobDirectory = new File(printJobDirectoryName);
            printJobDirectory.mkdirs();

            //Write out the slicer config
            SlicerType slicerTypeToUse = null;
            if (settings.getSlicerOverride() != null)
            {
                slicerTypeToUse = settings.getSlicerOverride();
            } else
            {
                slicerTypeToUse = Lookup.getUserPreferences().getSlicerType();
            }

            SlicerConfigWriter configWriter = SlicerConfigWriterFactory.getConfigWriter(
                slicerTypeToUse);

            //We need to tell the slicers where the centre of the printed objects is - otherwise everything is put in the centre of the bed...
            Vector3D centreOfPrintedObject = ThreeDUtils.calculateCentre(project.getLoadedModels());
            configWriter.setPrintCentre((float) (centreOfPrintedObject.getX()
                + ApplicationConfiguration.xPrintOffset),
                                        (float) (centreOfPrintedObject.getZ()
                                        + ApplicationConfiguration.yPrintOffset));
            configWriter.generateConfigForSlicer(settings,
                                                 printJobDirectoryName
                                                 + File.separator
                                                 + printUUID
                                                 + ApplicationConfiguration.printProfileFileExtension);

            return new SlicerTask(printUUID, project, PrintQualityEnumeration.DRAFT,
                                  settings, null);

        }

    }

}
